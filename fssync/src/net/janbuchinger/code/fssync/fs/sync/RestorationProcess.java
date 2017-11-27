/*
 * Copyright 2017 Jan Buchinger
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
package net.janbuchinger.code.fssync.fs.sync;

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

import net.janbuchinger.code.fssync.fs.FSSync;
import net.janbuchinger.code.fssync.fs.Operation;
import net.janbuchinger.code.fssync.fs.sync.ui.GetRestorationMode;
import net.janbuchinger.code.fssync.fs.sync.ui.GetRestoreSourceChoice;
import net.janbuchinger.code.fssync.fs.sync.ui.GetRetryOnOutOfMemory;
import net.janbuchinger.code.fssync.fs.sync.ui.GetSummaryApproval;
import net.janbuchinger.code.fssync.fs.sync.ui.RestorationModePanel;
import net.janbuchinger.code.fssync.fs.sync.ui.RunCancelled;
import net.janbuchinger.code.fssync.fs.sync.ui.RunFinished;
import net.janbuchinger.code.fssync.fs.sync.ui.RunSetDeterminate;
import net.janbuchinger.code.fssync.fs.sync.ui.RunStatusTextUpdate;
import net.janbuchinger.code.fssync.fs.sync.ui.RunStatusUpdate;
import net.janbuchinger.code.fssync.fs.sync.ui.SynchronisationProcessDialog;

import org.apache.commons.io.FileUtils;

public class RestorationProcess extends SwingWorker<Void, Void> implements PropertyChangeListener {

	private final SynchronisationProcessDialog spd;

	private final Vector<Operation> operations;

	private final String restoreTitle;

	// private final boolean[] runSegment;

	public RestorationProcess(Vector<Operation> operations, String restoreTitle,
			SynchronisationProcessDialog spd) {
		int len = operations.size();
		// boolean[] runSegmentRev = new boolean[len];
		Vector<Operation> operationsRev = new Vector<Operation>();
		for (int i = 0; i < len; i++) {
			// runSegmentRev[i] = runSegment[len - 1 - i];
			operationsRev.add(operations.get(len - 1 - i));
		}
		// this.runSegment = runSegmentRev;
		this.operations = operationsRev;
		this.spd = spd;
		this.restoreTitle = restoreTitle;
		addPropertyChangeListener(this);
	}

	@Override
	protected Void doInBackground() throws Exception {
		File dbDestination = null;
		File dbEdit = null;

		boolean changed = false;

		try {

			Vector<Operation> ops;
			Vector<Operation> opsDuplicates;
			GetRestoreSourceChoice getRestoreSourceChoice;
			int sourceChoice;
			Operation op, opPreserve;
			String path;

			GetRestorationMode getRestorationMode;
			boolean deleteNew;
			int mode;

			Iterator<Operation> iOp = null;
			Operation operation = null;
			OnlineDB db = null;
			Vector<CopyAction> copyActions;
			int ix;

			Vector<RelativeFile> allFiles;
			Iterator<RelativeFile> iOnlineFiles;
			long updateSize;
			long updateSizeOverwrite;

			RelativeFile file_db;
			File file_destination;
			File file_source;
			boolean sourceExists;
			boolean destinationExists;

			Path start;
			Vector<File> sourceFiles;
			LocalFileVisitor localFileVisitor;

			Vector<DeleteAction> deleteActions;
			Iterator<DeleteAction> iDeleteActions;
			DeleteAction da;
			Iterator<File> iSourceFiles;
			File f;
			int sourceBasePathLengthPlusOne;
			RelativeFile rf;

			OperationSummary operationSummary;
			GetSummaryApproval getSummaryApproval;

			boolean enoughSpace;
			GetRetryOnOutOfMemory getRetryOnOutOfMemory;

			Iterator<CopyAction> iCopyActions;

			long copied;
			long tSplit;
			long tStart;

			CopyAction ca;

			if (restoreTitle != null)
				if (!restoreTitle.equals(""))
					SwingUtilities.invokeLater(new RunStatusUpdate(
							"### " + restoreTitle + " Wiederherstellen", false, spd));

			ops = new Vector<Operation>();
			ops.addAll(operations);

			while ((opsDuplicates = getOpsWithSameSourceOnline(ops)) != null) {
				getRestoreSourceChoice = new GetRestoreSourceChoice(spd, opsDuplicates);

				SwingUtilities.invokeAndWait(getRestoreSourceChoice);

				sourceChoice = getRestoreSourceChoice.getSelection();
				path = opsDuplicates.get(0).getSourcePath();
				opPreserve = opsDuplicates.get(sourceChoice);

				iOp = ops.iterator();

				while (iOp.hasNext()) {
					op = iOp.next();
					if (op != opPreserve && op.getSourcePath().equals(path)) {
						iOp.remove();
					}
				}
			}

			iOp = ops.iterator();
			while (iOp.hasNext()) {
				operation = iOp.next();

				if ((!operation.getTarget().exists() || !operation.getSource().exists())
						|| !operation.isTargetOnline()) {
					SwingUtilities.invokeLater(new RunStatusUpdate("## Operation Offline: "
							+ operation.toString(), false, spd));
					continue;
				}

				getRestorationMode = new GetRestorationMode(spd);
				SwingUtilities.invokeAndWait(getRestorationMode);

				if (getRestorationMode.getAnswer() == JOptionPane.CANCEL_OPTION) {
					SwingUtilities.invokeLater(new RunStatusUpdate("## Abgebrochen ", false, spd));
					continue;
				}

				deleteNew = getRestorationMode.isDeleteNew();
				mode = getRestorationMode.getMode();

				SwingUtilities.invokeLater(new RunStatusUpdate("## Operation Wiederherstellen: "
						+ operation.getSourcePath() + " << " + operation.getRemotePath(), false, spd));

				SwingUtilities.invokeLater(new RunSetDeterminate(false, spd));

				dbDestination = new File(operation.getRemotePath(), ".fs.db");

				dbEdit = OnlineDB.getCorrectFile(operation.getSource().listFiles(new EditDBsFilenameFilter()),
						operation.getDbOriginal());
				dbDestination = new File(operation.getRemotePath(), ".fs.db");
				if (dbEdit == null) {
					dbEdit = new File(operation.getSource(), ".fs.edit.db");
					ix = 1;
					while (dbEdit.exists()) {
						dbEdit = new File(operation.getSource(), ".fs.edit" + (ix++) + ".db");
					}
					FileUtils.copyFile(dbDestination, dbEdit);
				}
				db = new OnlineDB(dbEdit);

				copyActions = new Vector<CopyAction>();

				changed = false;

				allFiles = db.listAll();
				iOnlineFiles = allFiles.iterator();

				updateSize = 0;
				updateSizeOverwrite = 0;

				while (iOnlineFiles.hasNext()) {
					file_db = iOnlineFiles.next();
					file_destination = new File(operation.getSource(), file_db.getRelativePath());
					destinationExists = file_destination.exists();
					file_source = new File(operation.getTarget(), file_db.getRelativePath());
					sourceExists = file_source.exists();
					switch (mode) {
					case RestorationModePanel.RESTORE_ALL:
						if (sourceExists) {
							copyActions.add(new CopyAction(file_destination, file_source, file_db
									.getRelativePath(), !destinationExists, CopyAction.DIR_RESTORE));
							updateSize += file_source.length();
							updateSizeOverwrite += destinationExists ? file_destination.length() : 0;
						}
						break;
					case RestorationModePanel.RESTORE_DAMAGED:
						if (sourceExists && destinationExists) {
							if (file_db.getModified() == file_destination.lastModified()
									&& !file_db.getChecksum().equals(FSSync.getChecksum(file_destination))) {
								if (file_db.getModified() == file_source.lastModified()
										&& file_db.getChecksum().equals(FSSync.getChecksum(file_source))) {
									copyActions.add(new CopyAction(file_destination, file_source, file_db
											.getRelativePath(), false, CopyAction.DIR_RESTORE));
									updateSize += file_source.length();
									updateSizeOverwrite += destinationExists ? file_destination.length() : 0;
								}
							}
						}
						break;
					default:
						if (destinationExists && sourceExists) {
							if (file_destination.lastModified() != file_db.getModified()) {
								copyActions.add(new CopyAction(file_destination, file_source, file_db
										.getRelativePath(), false, CopyAction.DIR_RESTORE));
								updateSize += file_source.length();
								updateSizeOverwrite += file_destination.length();
							}
						} else if (sourceExists) {
							copyActions.add(new CopyAction(new File(operation.getTarget(), file_db
									.getRelativePath()), file_destination, file_db.getRelativePath(), true,
									CopyAction.DIR_RESTORE));
							updateSize += file_db.getLength();
						}
						break;
					}

					if (!sourceExists && !destinationExists) {
						SwingUtilities.invokeLater(new RunStatusUpdate("Datei verschwunden: "
								+ file_db.getRelativePath(), false, spd));
						db.removeFileByPath(file_db.getRelativePath());
					}
				}

				deleteActions = new Vector<DeleteAction>();
				if (deleteNew) {

					SwingUtilities.invokeLater(new RunStatusUpdate("# Quelldateisystem Einlesen", false, spd));

					start = Paths.get(operation.getSourcePath());

					sourceFiles = new Vector<File>();

					localFileVisitor = new LocalFileVisitor(operation.getSource(), sourceFiles,
							operation.getExcludes(), spd);

					Files.walkFileTree(start, localFileVisitor);

					if (spd.isCancelled()) {
						SwingUtilities.invokeLater(new RunStatusUpdate("# Operation Abgebrochen", false, spd));
						return null;
					}
					iSourceFiles = sourceFiles.iterator();

					sourceBasePathLengthPlusOne = operation.getSourcePath().length() + 1;

					while (iSourceFiles.hasNext()) {
						f = iSourceFiles.next();
						rf = db.getFileByPath(f.getPath().substring(sourceBasePathLengthPlusOne));
						if (rf == null) {
							deleteActions.add(new DeleteAction(f, f.getPath().substring(
									sourceBasePathLengthPlusOne), DeleteAction.del_source));
						}
						if (spd.isCancelled()) {
							SwingUtilities.invokeLater(new RunStatusUpdate("# Operation Abgebrochen", false,
									spd));
							return null;
						}
					}
				}

				try {
					operationSummary = new OperationSummary(operation.getSource(), operation.getTarget(),
							new Vector<File>(), new Vector<File>(), new Vector<File>(), copyActions,
							deleteActions, spd, true);
				} catch (SpiderCancelledException e) {
					SwingUtilities.invokeLater(new RunStatusUpdate("# Während dem Summieren Abgebrochen",
							false, spd));
					return null;
				}

				if (operationSummary.shouldDisplayDialog()) {
					getSummaryApproval = new GetSummaryApproval(spd, operationSummary, true,
							operation.getPriorityOnConflict());
					SwingUtilities.invokeAndWait(getSummaryApproval);
					if (!getSummaryApproval.isApproved()) {
						SwingUtilities.invokeLater(new RunStatusUpdate("# Operation Abgebrochen", false, spd));
						break;
					}
				}

				iDeleteActions = deleteActions.iterator();
				while (iDeleteActions.hasNext()) {
					da = iDeleteActions.next();
					if (da.isSelected()) {
						da.getFile().delete();
						SwingUtilities.invokeLater(new RunStatusUpdate(da.toString(), true, spd));
					}
				}
				enoughSpace = true;

				while (operation.getSource().getFreeSpace() < (updateSize - updateSizeOverwrite)) {
					getRetryOnOutOfMemory = new GetRetryOnOutOfMemory(spd, "Quelldatenträger", updateSize);
					SwingUtilities.invokeAndWait(getRetryOnOutOfMemory);
					if (!getRetryOnOutOfMemory.isRetry()) {
						enoughSpace = false;
						break;
					}
				}

				if (!enoughSpace) {
					SwingUtilities.invokeLater(new RunStatusUpdate(
							"# Operation Abgebrochen, nicht genügend Speicherplatz auf dem Quelldatenträger",
							false, spd));
					break;
				}

				iCopyActions = copyActions.iterator();
				copied = 0;
				SwingUtilities.invokeLater(new RunSetDeterminate(true, spd));
				setProgress((int) 0);
				tSplit = 0;
				tStart = System.currentTimeMillis();
				while (iCopyActions.hasNext()) {
					ca = iCopyActions.next();
					if (!ca.isSelected())
						continue;
					changed = true;
					FileUtils.copyFile(ca.getDestination(), ca.getSource());
					SwingUtilities.invokeLater(new RunStatusUpdate(ca.toString(), true, spd));
					copied += ca.getSource().length();
					setProgress((int) ((100.0 / updateSize) * copied));
					tSplit = (System.currentTimeMillis() - tStart) / 1000;
					if (tSplit > 0)
						SwingUtilities.invokeLater(new RunStatusTextUpdate("Dateien Zurückkopieren ("
								+ ((copied / 1024 / 1024) / tSplit) + " MByte/sec)", spd));
					if (spd.isCancelled()) {
						removePropertyChangeListener(this);
						SwingUtilities.invokeLater(new RunCancelled("Während des Datenabgleichs Abgebrochen!",
								spd));
						return null;
					}
				}
				if (copied > 0) {
					SwingUtilities.invokeLater(new RunStatusUpdate("Dateien Kopiert " + (copied / 1024 / 1024)
							+ " MByte in " + tSplit + " Sekunden"
							+ (tSplit > 0 ? ", " + ((copied / 1024 / 1024) / tSplit) + " MByte/sec" : ""),
							false, spd));
				}
				if (!changed) {
					SwingUtilities
							.invokeLater(new RunStatusUpdate("## Keine Änderungen Gefunden", false, spd));
				} else {
					SwingUtilities
							.invokeLater(new RunStatusUpdate("## Alle Änderungen Angewandt", false, spd));
				}
				if (dbEdit.lastModified() > dbDestination.lastModified())
					FileUtils.copyFile(dbEdit, dbDestination);
			}
			removePropertyChangeListener(this);
			SwingUtilities.invokeLater(new RunFinished("Alles Erledigt", null, spd));
		} catch (Exception e) {
			SwingUtilities.invokeLater(new RunFinished("Fehler: " + e.getMessage(), e, spd));
			e.printStackTrace();
		} finally {
			if (dbEdit != null && dbDestination != null) {
				if (dbEdit.lastModified() > dbDestination.lastModified())
					FileUtils.copyFile(dbEdit, dbDestination);
			}
		}
		return null;
	}

	private Vector<Operation> getOpsWithSameSourceOnline(Vector<Operation> s) {
		Iterator<Operation> iOp;
		Vector<Operation> o = new Vector<Operation>();
		Operation op;
		int size = s.size();
		int i1;
		String src;
		for (int i = 0; i < size; i++) {
			src = s.get(i).getSourcePath();
			i1 = 0;
			iOp = s.iterator();
			o = new Vector<Operation>();
			if (s.get(i).isTargetOnline())
				o.add(s.get(i));
			while (iOp.hasNext()) {
				if (i1 <= i) {
					i1++;
					iOp.next();
					continue;
				}
				op = iOp.next();
				if (op.getSourcePath().equals(src)) {
					if (op.getDbOriginal().exists())
						o.add(op);
				}
			}
			if (o.size() > 1)
				return o;
		}
		return null;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		spd.setProgress(getProgress());
	}
}
