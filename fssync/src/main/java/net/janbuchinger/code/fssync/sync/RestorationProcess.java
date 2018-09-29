/*
 * Copyright 2017-2018 Jan Buchinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.janbuchinger.code.fssync.sync;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.apache.commons.io.FileUtils;

import net.janbuchinger.code.fssync.FSSync;
import net.janbuchinger.code.fssync.Operation;
import net.janbuchinger.code.fssync.sync.ui.GetRestorationMode;
import net.janbuchinger.code.fssync.sync.ui.GetRestoreSourceChoice;
import net.janbuchinger.code.fssync.sync.ui.GetRetryOnOutOfMemory;
import net.janbuchinger.code.fssync.sync.ui.GetSummaryApproval;
import net.janbuchinger.code.fssync.sync.ui.RestorationModePanel;
import net.janbuchinger.code.fssync.sync.ui.RunCancelled;
import net.janbuchinger.code.fssync.sync.ui.RunFinished;
import net.janbuchinger.code.fssync.sync.ui.RunSetDeterminate;
import net.janbuchinger.code.fssync.sync.ui.RunStatusMessageUpdate;
import net.janbuchinger.code.fssync.sync.ui.RunStatusTextUpdate;
import net.janbuchinger.code.fssync.sync.ui.StatusMessage;
import net.janbuchinger.code.fssync.sync.ui.SynchronizationProcessDialog;
import net.janbuchinger.code.mishmash.FSFx;
import net.janbuchinger.code.mishmash.ui.UIFx;

/**
 * The <code>RestorationProcess</code> class is a reverse synchronisazion
 * process.
 * 
 * @author Jan Buchinger
 * 
 * @see SynchronizationProcess
 */
public class RestorationProcess extends SwingWorker<Void, Void> implements PropertyChangeListener {

	/**
	 * The Synchronization Prozess Dialog
	 */
	private SynchronizationProcessDialog spd;

	/**
	 * The Batch of selected Operations
	 */
	private final Vector<Operation> operations;

	/**
	 * last status text update to avoid flooding the EDT during copying files, 0 by
	 * default
	 */
	private long lastStatusUpdate;
	/**
	 * last status message update to avoid flooding the EDT during copying files, 0
	 * by default
	 */
	private long lastStatusMessageUpdate;

	/**
	 * The temporary list of messages to be passed to the EDT, null by default
	 */
	private Vector<StatusMessage> messages;

	/**
	 * Constructs a new <code>RestorationProcess</code> to run the given operations.
	 * 
	 * @param operations
	 *            The batch of operations to be restored.
	 */
	public RestorationProcess(Vector<Operation> operations) {
		this.operations = operations;
		addPropertyChangeListener(this);
	}

	/**
	 * Sets The <code>SynchronizationProcessDialog</code>. This must be done before
	 * execution of the <code>SwingWorker</code>.
	 * 
	 * @param spd
	 *            The <code>SynchronizationProcessDialog</code> initialized on the
	 *            EDT.
	 */
	public void setSpd(SynchronizationProcessDialog spd) {
		this.spd = spd;
	}

	/**
	 * The Restoration Process
	 * 
	 * @return null
	 * 
	 * @throws Exception
	 *             IllegalArgumentException if the
	 *             <code>SynchronizationProcessDialog</code> is not set upon
	 *             execution.
	 */
	@Override
	protected Void doInBackground() throws Exception {
		try {
			// indicates that the batch loop should be broken
			boolean breakk = false;
			// indicates changes to the database and filesystem
			boolean changed = false;
			// the database file in target directory
			File dbDestination = null;
			// the database editable file in source directory
			File dbEdit = null;
			// the file system database
			OnlineDB db = null;

			// temporary list of conflicting operations
			Vector<Operation> opsDuplicates;
			// Runnable to get user decision upon conflict
			GetRestoreSourceChoice getRestoreSourceChoice;
			// the user answer of getRestoreSourceChoice
			int sourceChoice;
			// temporary operations for comparing conflicting operations
			Operation op, opPreserve;
			// operation source path for comparison
			String path;
			// iterator for removing conflicting operations that were not chosen
			Iterator<Operation> iOp = null;

			// Runnable that displays the dialog to choose the restoration mode for the
			// batch
			GetRestorationMode getRestorationMode;
			// option to delete new (unknown to database) files
			boolean deleteNew;
			// user chosen mode
			int mode;

			// the list of files to be copied
			Vector<CopyAction> copyActions;

			// all files from database to determine files to be copied
			Vector<RelativeFile> allFiles;
			// the counter of bytes to be copied
			long updateSize;
			// the counter of bytes to be overwritten
			long updateSizeOverwrite;
			// the destination file, in restoration the file in the source directory
			File file_destination;
			// the source file is the file from the target directory
			File file_source;
			// true indicates that the source file exists
			boolean sourceExists;
			// true indicates that the destination file exists
			boolean destinationExists;

			// the starting path for the local file visitor
			Path start;
			// the source files found by the local file visitor
			Vector<File> sourceFiles;
			// the local file visitor for finding new files if delete new files is selected
			LocalFileVisitor localFileVisitor;
			// the new files to delete
			Vector<DeleteAction> deleteActions;
			// the index of the path string to the file to delete to String.substring() the
			// relative path for querying the database and for building the delete action
			int sourceBasePathLengthPlusOne;
			// the temporary file to query the database for
			RelativeFile rf;

			// the operation summary
			OperationSummary operationSummary;
			// the runnable to display the operation summary dialog
			GetSummaryApproval getSummaryApproval;

			// true indicates indicates that the files to be copied fit in the source file
			// system
			boolean enoughSpace;
			// runnable to let the user retry on low disk space
			GetRetryOnOutOfMemory getRetryOnOutOfMemory;

			// bytes copied counter
			long copied;
			// files copied counter
			int counter;
			// copy block start time
			long tStart;
			// copy running time current loop
			long tSplit;

			// search for conflicting operations within the batch of Operations
			// while there are operations online sharing the same source directory
			while ((opsDuplicates = getOpsWithSameSourceOnline(operations)) != null) {
				// initialize source selection dialog
				getRestoreSourceChoice = new GetRestoreSourceChoice(spd, opsDuplicates);
				// show source choice dialog (modal)
				SwingUtilities.invokeAndWait(getRestoreSourceChoice);

				// get the chosen source index
				sourceChoice = getRestoreSourceChoice.getSelection();
				// get the shared source path
				path = opsDuplicates.get(0).getSourcePath();
				// the chosen operation to preserve in the list
				opPreserve = opsDuplicates.get(sourceChoice);

				// eliminate all operations that were not chosen
				iOp = operations.iterator();
				while (iOp.hasNext()) {
					op = iOp.next();
					if (op != opPreserve && op.getSourcePath().equals(path)) {
						iOp.remove();
					}
				}
			}
			// initialize the dialog to let the user choose the restoration mode for the
			// batch
			getRestorationMode = new GetRestorationMode(spd);
			// show the restoration mode choice dialog (modal)
			SwingUtilities.invokeAndWait(getRestorationMode);
			// abort if the user chose cancel
			if (getRestorationMode.getAnswer() == JOptionPane.CANCEL_OPTION) {
				finish("## Abgebrochen", null);
				return null;
			}
			// indicates that new files in the SOURCE directory should be deleted
			deleteNew = getRestorationMode.isDeleteNew();
			// the restoration mode
			mode = getRestorationMode.getMode();

			// loop through the batch of operations to restore
			for (Operation operation : operations) {
				try {
					// fail save... continue if the operation state is not adequate
					if ((!operation.getTarget().exists() || !operation.getSource().exists())
							|| !operation.isTargetOnline()) {
						message("## Operation Offline: " + operation.toString());
						continue;
					}

					message("## Operation Wiederherstellen: " + operation.getSourcePath() + " << "
							+ operation.getTargetPath());

					setIndeterminate();

					// initialize the target database file
					dbDestination = operation.getDbOriginal();

					// try initialize the .fs.edit.db file
					dbEdit = OnlineDB.getEditableDBFile(operation);
					// if the correct file was not found
					if (dbEdit == null) {
						// then obtain the next valid and non existent file name for the editable data
						// base file
						dbEdit = OnlineDB.nextEditableDBFile(operation.getSource());
						// copy the file
						FileUtils.copyFile(dbDestination, dbEdit);
					}
					// initialize the data base
					db = new OnlineDB(dbEdit);

					// initialize the list of copy actions
					copyActions = new Vector<CopyAction>();

					// set unchanged
					changed = false;

					// list all files from data base
					allFiles = db.listAll();

					// initialize the update size
					updateSize = 0;
					updateSizeOverwrite = 0;
					// loop through all files from database
					for (RelativeFile file_db : allFiles) {
						// initialize the file in the source directory as target file
						file_destination = new File(operation.getSource(), file_db.getRelativePath());
						// does the target file exist?
						destinationExists = file_destination.exists();
						// initialize the file in the target directory as source file
						file_source = new File(operation.getTarget(), file_db.getRelativePath());
						// does the source file exist?
						sourceExists = file_source.exists();
						// add according to the restoration mode chosen by the user
						switch (mode) {
						// case "soft restoration"
						case RestorationModePanel.MODE_SOFT:
							// only add files that exist in the target directory and are missing in the
							// source directory
							if (!destinationExists && sourceExists) {
								copyActions.add(new CopyAction(file_source, file_destination,
										file_db.getRelativePath(), false, CopyAction.DIR_RESTORE));
								updateSize += file_source.length();
							}
							break;
						// case "restore all"
						case RestorationModePanel.MODE_ALL:
							// all files that exist in the target directory are fetched
							if (sourceExists) {
								copyActions.add(new CopyAction(file_source, file_destination,
										file_db.getRelativePath(), !destinationExists,
										CopyAction.DIR_RESTORE));
								updateSize += file_source.length();
								updateSizeOverwrite += destinationExists ? file_destination.length() : 0;
							}
							break;
						// case "undo changes"
						case RestorationModePanel.MODE_UNDO_CHANGES:
							// if both files exist
							if (destinationExists && sourceExists) {
								// and their modification date differs
								if (file_destination.lastModified() != file_db.getModified()) {
									// then restore the changed file
									copyActions.add(new CopyAction(file_source, file_destination,
											file_db.getRelativePath(), false, CopyAction.DIR_RESTORE));
									updateSize += file_source.length();
									updateSizeOverwrite += file_destination.length();
								}
								// if only the file in the target directory exists
							} else if (sourceExists) {
								// then restore the file as new file
								copyActions.add(new CopyAction(file_source, file_destination,
										file_db.getRelativePath(), true, CopyAction.DIR_RESTORE));
								updateSize += file_db.getLength();
							}
							break;
						default:
							throw new IllegalArgumentException("Restoration Mode Not Recognized");
						}

						// if the file pair is missing
						if (!sourceExists && !destinationExists) {
							// then notify the user
							message("Datei verschwunden: " + file_db.getRelativePath());
							// and remove the record from the database
							db.removeFileByPath(file_db.getRelativePath());
						}
					} // end of database files loop

					// initialize the list of files to delete
					deleteActions = new Vector<DeleteAction>();
					// if the "delete new" option was selected
					if (deleteNew) {
						// then list the source file system
						message("# Quelldateisystem Einlesen");
						// initialize the source file system list
						sourceFiles = new Vector<File>();
						// initialize the start path for the file visitor
						start = Paths.get(operation.getSourcePath());
						// initialize the local file visitor
						localFileVisitor = new LocalFileVisitor(operation.getSource(), sourceFiles,
								operation.getExcludes(), this);
						// spider file system
						Files.walkFileTree(start, localFileVisitor);
						// cancel if the user pressed the cancel button
						if (isCancelled()) {
							message("# Wiederherstellung Abgebrochen");
							breakk = true;
							break;
						}
						// the file path index to substring the relative path
						sourceBasePathLengthPlusOne = operation.getSourcePath().length() + 1;
						// loop through all files in the source file system
						for (File f : sourceFiles) {
							// get the corresponding file from the database
							rf = db.getFileByPath(f.getPath().substring(sourceBasePathLengthPlusOne));
							// if the file is not in the database yet
							if (rf == null) {
								// then it is a new file to delete
								deleteActions.add(
										new DeleteAction(f, f.getPath().substring(sourceBasePathLengthPlusOne),
												DeleteAction.del_source, true));
							}
							// cancel if the user pressed the cancel button
							if (isCancelled()) {
								message("# Wiederherstellung Abgebrochen");
								breakk = true;
								break;
							}
						}
						if (breakk) {
							break;
						}
					}

					// try initializing the OperationSummary for the restoration
					try {
						operationSummary = new OperationSummary(operation.getSource(), operation.getTarget(),
								new Vector<File>(), new Vector<File>(), new Vector<File>(), copyActions,
								deleteActions, this, true, false);
					} catch (SynchronizationCancelledException e) {
						// abort if the user pressed the cancel button
						message("# Während dem Summieren Abgebrochen");
						breakk = true;
						break;
					}

					// if there are any actions to perform
					if (operationSummary.shouldDisplayDialog()) {
						// then initialize the summary approval dialog
						getSummaryApproval = new GetSummaryApproval(spd, operationSummary, true,
								operation.getPriorityOnConflict());
						// and show it (modal)
						SwingUtilities.invokeAndWait(getSummaryApproval);
						// abort if the user cancelled the summary approval dialog
						if (!getSummaryApproval.isApproved()) {
							message("# Wiederherstellung Abgebrochen");
							continue;
						}
					}
					// begin restoration with deleting files
					// loop through delete actions
					for (DeleteAction da : deleteActions) {
						// if the delete action is selected
						if (da.isSelected()) {
							// delete the file
							if (da.getFile().delete()) {
								changed = true;
								message(da.toString(), true);
							} else {
								// delete was unsuccessful, notify the user
								message("Fehler beim Löschen: ".concat(da.toString()));
							}
						}
						// abort if the user pressed cancel
						if (isCancelled()) {
							message("# Während dem Löschen Abgebrochen");
							breakk = true;
							break;
						}
					}
					if (breakk) {
						break;
					}

					// assume enough space
					enoughSpace = true;

					// while there is not enough space to restore
					while (operation.getSource().getFreeSpace() < (updateSize - updateSizeOverwrite)) {
						// initialize new retry on out of memory dialog
						getRetryOnOutOfMemory = new GetRetryOnOutOfMemory(spd, "Quelldatenträger", updateSize);
						// and show it (modal)
						SwingUtilities.invokeAndWait(getRetryOnOutOfMemory);
						// if the answer was not retry then abort
						if (!getRetryOnOutOfMemory.isRetry()) {
							enoughSpace = false;
							break;
						}
					}
					// if there is not enough memory
					if (!enoughSpace) {
						// then abort the operation and continue
						message("# Wiederherstellung Abgebrochen, nicht genügend Speicherplatz auf dem Quelldatenträger");
						continue;
					}

					// initialize the total bytes copied counter
					copied = 0;
					// set the progress bar determinate
					setDeterminate();
					// start by 0
					setProgress(0);
					// initialize the split time
					tSplit = 0;
					// initialize the start time
					tStart = System.currentTimeMillis();
					// initialize the total files copied counter
					counter = 0;
					// loop through all copy actions
					for (CopyAction ca : copyActions) {
						// if the copy action was deselected
						if (!ca.isSelected()) {
							// then continue
							continue;
						}
						// copy the file
						FileUtils.copyFile(ca.getSource(), ca.getDestination());
						// indicate that changes were made
						if (!changed) {
							changed = true;
						}
						// increment file counter
						counter++;
						// message verbose
						message(ca.toString(), true);
						// increment total bytes copied counter
						copied += ca.getSource().length();
						// update progress bar
						setProgress((int) ((100.0 / updateSize) * copied));
						// set split time in seconds
						tSplit = (System.currentTimeMillis() - tStart) / 1000;
						// if the split time is longer than 0 seconds
						if (tSplit > 0) {
							// then update the status text
							status("Dateien Zurückkopieren " + FSFx.formatTransferSpeed(tStart, copied),
									false);
						}
						// abort if the user pressed cancel
						if (isCancelled()) {
							removePropertyChangeListener(this);
							SwingUtilities.invokeLater(
									new RunCancelled("Während des Datenabgleichs Abgebrochen!", spd));
							breakk = true;
							break;
						}
					}
					// inform about bytes copied and duration
					if (copied > 0) {
						message(counter + " Dateien Kopiert, " + FSFx.formatFileLength(copied) + " in "
								+ UIFx.formatMillisAsHoursMinutesSeconds(tSplit)
								+ (tSplit > 0 ? ", " + FSFx.formatTransferSpeed(tStart, copied) : ""));
					}
					if (breakk) {
						break;
					}
					if (!changed) {
						message("## Keine Änderungen Gefunden");
					} else {
						message("## Alle Änderungen Angewandt");
					}
				} finally {
					// finally synchronize the databases if necessary
					if (dbEdit != null && dbDestination != null) {
						if (dbEdit.lastModified() != dbDestination.lastModified())
							FileUtils.copyFile(dbEdit, dbDestination);
					}
				}
			} // end of batch loop

			removePropertyChangeListener(this);
			flushMessages();
			if (breakk) {
				finish("Abgebrochen", null);
			} else {
				finish("Alles Erledigt", null);
			}
		} catch (Exception e) {
			flushMessages();
			finish("Fehler: " + e.getMessage(), e);
			e.printStackTrace();
		}
		return null;
	}

	private void setIndeterminate() {
		SwingUtilities.invokeLater(new RunSetDeterminate(false, spd));
	}

	private void setDeterminate() {
		SwingUtilities.invokeLater(new RunSetDeterminate(true, spd));
	}

	/**
	 * sets the SynchronisationProcessDialog finished
	 * 
	 * @param message
	 *            The exit message
	 * @param e
	 *            The Exception or null
	 */
	private void finish(String message, Exception e) {
		SwingUtilities.invokeLater(new RunFinished(message, e, spd));
	}

	/**
	 * Gets the <code>Operation</code>s within the batch that share a source
	 * directory.
	 * 
	 * @param s
	 *            The batch of <code>Operation</code>s.
	 * 
	 * @return <code>null</code> if there are no <code>Operation</code>s sharing a
	 *         source directory within the batch.
	 *         <p>
	 *         Otherwise, if there are conflicts, the list of conflicting
	 *         <code>Operation</code>s is returned.
	 */
	private Vector<Operation> getOpsWithSameSourceOnline(Vector<Operation> s) {
		// the list of conflicting operations
		Vector<Operation> opsWithSameSource;
		// the potentially conflicting operation
		Operation op2;
		// the batch list size
		int size = s.size();
		// the source directory path string of the currently inspected operation (i1)
		String src;
		// loop through the batch of operations
		for (int i1 = 0; i1 < size; i1++) {
			// initialize the conflicts list
			opsWithSameSource = new Vector<Operation>();
			// inspect only if the current target is online
			if (s.get(i1).isTargetOnline()) {
				// add to current conflicts list
				opsWithSameSource.add(s.get(i1));
			} else {
				// continue if offline
				continue;
			}
			// get the source path of the currently inspected operation
			src = s.get(i1).getSourcePath();
			// start inner loop from i1 + 1
			for (int i2 = i1 + 1; i2 < size; i2++) {
				// get the potential conflict
				op2 = s.get(i2);
				// if the operations are in conflict
				if (op2.getSourcePath().equals(src)) {
					// and if the second operations target is online
					if (op2.getDbOriginal().exists()) {
						// then add the second operation as conflicting
						opsWithSameSource.add(op2);
					}
				}
			} // end of inner loop
				// if there were more than one operations with the same source online
			if (opsWithSameSource.size() > 1) {
				// then return the list of conflicts
				return opsWithSameSource;
			}
		} // end of outer loop
			// no conflicts found
		return null;
	}

	/**
	 * status label update, limited to 1 update per half second
	 * 
	 * @param status
	 *            The status <code>String</code> to be shown.
	 * @param force
	 *            <code>true</code> to ignore delay.
	 */
	private void status(String status, boolean force) {
		if (force || System.currentTimeMillis() - lastStatusUpdate > 500) {
			SwingUtilities.invokeLater(new RunStatusTextUpdate(status, spd));
			lastStatusUpdate = System.currentTimeMillis();
		}
	}

	/**
	 * used at the end of the synchronization process to ensure all messages are
	 * being posted to the user interface
	 */
	private void flushMessages() {
		message("");
	}

	/**
	 * Posts a message directly to the synchronization process dialog.
	 * 
	 * @param message
	 *            The message to be shown.
	 */
	private void message(String message) {
		message(message, false);
	}

	/**
	 * Posts a message to the current list of messages and posts messages to the
	 * synchronization process dialog
	 * 
	 * @param message
	 *            The message to be shown, not null
	 * 
	 * @param verbose
	 *            if true the message is posted to the current list of messages. if
	 *            the time since the last message is longer than 750 milliseconds
	 *            then the current list of messages is posted to the synchronization
	 *            process dialog.
	 *            <p>
	 *            if false then the message is not delayed and the list of current
	 *            messages is posted.
	 */
	private void message(String message, boolean verbose) {
		if (message != null) {
			// add message to list
			if (message.length() > 0) {
				if (messages == null) {
					messages = new Vector<>();
				}
				messages.add(new StatusMessage(message, verbose));
			}
			// send messages to the EDT
			if (System.currentTimeMillis() - lastStatusMessageUpdate > 750 || !verbose) {
				if (messages != null) {
					SwingUtilities.invokeLater(new RunStatusMessageUpdate(spd, messages));
					messages = null;
					lastStatusMessageUpdate = System.currentTimeMillis();
				}
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (!isCancelled()) {
			spd.setProgress(getProgress());
		}
	}
}
