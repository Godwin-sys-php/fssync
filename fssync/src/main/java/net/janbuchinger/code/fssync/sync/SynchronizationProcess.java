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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.apache.commons.io.FileUtils;

import net.janbuchinger.code.fssync.FSSync;
import net.janbuchinger.code.fssync.Operation;
import net.janbuchinger.code.fssync.OperationArgument;
import net.janbuchinger.code.fssync.Settings;
import net.janbuchinger.code.fssync.sync.ui.GetForeignFileHandling;
import net.janbuchinger.code.fssync.sync.ui.GetRetryOnOutOfMemory;
import net.janbuchinger.code.fssync.sync.ui.GetSummaryApproval;
import net.janbuchinger.code.fssync.sync.ui.RunAbortCountDown;
import net.janbuchinger.code.fssync.sync.ui.RunCancelled;
import net.janbuchinger.code.fssync.sync.ui.RunFinished;
import net.janbuchinger.code.fssync.sync.ui.RunPauseCountDown;
import net.janbuchinger.code.fssync.sync.ui.RunStartCountDown;
import net.janbuchinger.code.fssync.sync.ui.RunStatusMessageUpdate;
import net.janbuchinger.code.fssync.sync.ui.RunStatusTextUpdate;
import net.janbuchinger.code.fssync.sync.ui.StatusMessage;
import net.janbuchinger.code.fssync.sync.ui.SynchronizationProcessDialog;
import net.janbuchinger.code.mishmash.FSFx;
import net.janbuchinger.code.mishmash.ui.UIFx;

/**
 * The SynchronizationProcess class is a process that works through a batch of
 * synchronization operations.
 * 
 * The first step is to read the contents of the remote file system and create
 * checksums of all files that are in the database and unchanged. This should
 * find defective files.
 * 
 * @author Jan Buchinger
 *
 */
public class SynchronizationProcess extends SwingWorker<Void, Void> {

	/**
	 * the synchronization process dialog
	 */
	private SynchronizationProcessDialog spd;

	/**
	 * the operation arguments to work through
	 */
	private final Vector<OperationArgument> operationArgs;

	/**
	 * indicates whether summary dialog should be shown
	 */
	private final boolean showSummary;

	/**
	 * the batch title
	 */
	private final String syncTitle;

	/**
	 * last Status label update to avoid flooding the EDT with unnecessary updates
	 * during copying files
	 */
	private long lastStatusTextUpdate;

	/**
	 * last Status message update to avoid flooding the EDT with unnecessary updates
	 * during copying files
	 */
	private long lastStatusMessageUpdate;

	/**
	 * the current list of status messages to post to the synchronization process
	 * dialog
	 */
	private Vector<StatusMessage> messages;

	/**
	 * Construct a SynchronizationProcess.
	 * 
	 * @param operations
	 *            The OperationArguments to be worked through.
	 * @param syncTitle
	 *            The batch title.
	 */
	public SynchronizationProcess(Vector<OperationArgument> operations, String syncTitle) {
		this.operationArgs = operations;
		this.syncTitle = syncTitle;
		// get the Settings
		Settings settings = Settings.getSettings();
		this.showSummary = settings.isShowSummary();
	}

	/**
	 * Sets the <code>SynchronizationProcessDialog</code> for this
	 * <code>SynchronizationProcess</code>.
	 * <p>
	 * <b>This must happen before the SynchronizationProcess is executed!</b>
	 * 
	 * @param spd
	 *            The <code>SynchronizationProcessDialog</code> initialized on the
	 *            EDT.
	 */
	public final void setSynchronisationProcessDialog(SynchronizationProcessDialog spd) {
		this.spd = spd;
	}

	/**
	 * The synchronization process.
	 */
	@Override
	protected final Void doInBackground() throws Exception {
		if (spd == null) {
			throw new Exception("SynchronisationProcessDialog must not be null");
		}
		// synchronization start time
		long syncStart = System.currentTimeMillis();
		try {
			// database
			File dbDestination = null;
			File dbEdit = null;

			// indicates changes to the database
			boolean changed = false;

			// indicates that the operation is executed for the first time
			boolean firstRun;

			// operation start time
			long opStart;
			// times for statistics
			long tAnalysis;

			// current operation
			Operation operation;
			// indicates quick sync mode
			boolean quickSync;
			// indicates elastic time comparison (file1.lastModified() ==
			// file2.lastModified() +/-1sec)
			boolean compareElastic;
			// indicates bidirectional synchronization
			boolean isBiDirectional;
			// the database loaded for editing
			OnlineDB db = null;
			// the list of files to copy
			Vector<CopyAction> copyActions;

			// list of files to delete
			Vector<DeleteAction> deleteActions;

			// content of remote file system
			Vector<File> remoteFiles;
			// list of empty directories in remote file system
			Vector<File> emptyDirs;

			// files that appeared newly in the target file system
			Vector<File> newForeignFiles;
			// files in the target file system that were changed
			Vector<File> changedForeignFiles;

			// corrupted files in source file system
			Vector<File> corruptedFilesSource;
			// corrupted files in target file system
			Vector<File> corruptedFilesDestination;
			// files that are corrupted in both file systems
			Vector<File> lostFiles;

			// the base path lengths to String.substring() the relative paths
			int destinationBasePathLengthPlusOne;
			int sourceBasePathLengthPlusOne;

			// file visitor for remote file system
			RemoteFileVisitor remoteFileVisitor;
			// starting path for file visitor
			Path start;
			// counter for copied files
			int counter;

			// file integrity check
			// file data as remembered in database
			RelativeFile file_db;
			// source file
			File file_source;

			// file length
			long length_source;
			long length_destination;
			long length_db;
			// last modified time
			long modified_source;
			long modified_destination;
			long modified_db;
			// checksum
			String checksum_source;
			String checksum_destination;
			String checksum_db;
			// conclusion was modified
			boolean sourceFileWasModified;
			boolean destinationFileWasModified;

			// date format for printing time comparison when
			// operation.isIgnoreModifiedWhenEqual() and settings.isVerbose() to ui
			// SimpleDateFormat df = UIFx.initPreciseDisplayDateTimeFormat();

			// indicator that the destination file is intact
			boolean destinationFileIsGood;

			// multi purpose files
			File file1;
			File file2;
			String relativePath;

			// Runnables to get user decisions

			// dialog: corrupted files were found
			// GetContinueRestore getContinueRestore;
			// answer of corrupted files dialog
			// boolean continueRestore;

			// changes were found in remote file system in unidirectional mode
			GetForeignFileHandling getForeignFileHandling;
			// answer of dialog
			int answer;

			// show summary
			GetSummaryApproval getSummaryApproval;

			// out of memory error
			GetRetryOnOutOfMemory getRetryOnOutOfMemory;
			// answer of out of memory error
			boolean retry;

			// content of source file system
			Vector<File> sourceFiles;
			// file visitor for source file system
			LocalFileVisitor localFileVisitor;

			// iterator to loop through the source files
			Iterator<File> iSourceFiles;
			// current file
			File sourceFile;

			// list of all known files from database for finding files to delete
			Vector<RelativeFile> allFiles;
			// indicates whether the known file exists in the source directory
			boolean sourceFileExists;
			// indicates whether the known file exists in the remote directory
			boolean remoteFileExists;

			// operation summary data for dialog
			OperationSummary operationSummary;

			// checking if there is enough free space before copying
			boolean enoughSpace;
			// overwrite first if that frees up space...
			boolean overwriteFirst;

			// currently copied for progress bar
			long copied;

			// time markers for operation running time
			// long tSyncStart;
			long tCopyStart;
			long tSplit;

			// loop counter for overwriteFirst
			int ix;

			if (syncTitle != null) {
				if (!syncTitle.equals("")) {
					message("# # # " + syncTitle + " Ausführen", false);
				}
			}

			/*
			 * loop through the operations
			 */
			for (OperationArgument operationArgument : operationArgs) {
				try {
					dbEdit = null;
					dbDestination = null;
					changed = false;
					opStart = System.currentTimeMillis();

					/*
					 * set the current operation, continue if operation is offline
					 */
					operation = operationArgument.getOperation();
					if (!operation.isOnline()) {
						message("# # Operation offline " + operation.toString());
						continue;
					}
					message("# # Operation " + operation.toString() + " Ausführen");

					/*
					 * set general operation parameters
					 */
					quickSync = operationArgument.isQuickSync();
					compareElastic = operation.isCompareElastic();
					isBiDirectional = operation.isSyncBidirectional();

					if (quickSync) {
						status("1/2 - Analysieren (Schnell)...", true);
						message("# Analysieren (Schnell)...");
						setCountDown(operation.getAverageAnalyseTimeQuick());
					} else {
						status("1/2 - Analysieren (Genau)...", true);
						message("# Analysieren (Genau)...");
						setCountDown(operation.getAverageAnalyseTimeDeep());
					}
					// fallback from ignoreModifiedWhenEqual to compareElastic=true when quicksync
					// is forced
					if (quickSync && operation.isIgnoreModifiedWhenEqual() && !compareElastic) {
						compareElastic = true;
					}

					/*
					 * identify database file to edit, initialize db
					 */
					dbEdit = OnlineDB.getEditableDBFile(operation);
					dbDestination = operation.getDbOriginal();
					if (dbEdit == null) {
						dbEdit = OnlineDB.nextEditableDBFile(operation.getSource());
						FileUtils.copyFile(dbDestination, dbEdit);
					}
					db = new OnlineDB(dbEdit);

					firstRun = db.isEmpty();

					/*
					 * initialize lists
					 */
					copyActions = new Vector<CopyAction>();
					deleteActions = new Vector<DeleteAction>();

					remoteFiles = new Vector<File>();
					emptyDirs = new Vector<File>();

					newForeignFiles = new Vector<File>();
					changedForeignFiles = new Vector<File>();

					corruptedFilesSource = new Vector<File>();
					corruptedFilesDestination = new Vector<File>();
					lostFiles = new Vector<File>();

					/*
					 * String positions for relative paths substrings
					 */
					destinationBasePathLengthPlusOne = operation.getTargetPath().length() + 1;
					sourceBasePathLengthPlusOne = operation.getSourcePath().length() + 1;

					/*
					 * Check TARGET FILE SYSTEM for bidirectional synchronization and integrity
					 * check
					 */
					if (!quickSync || isBiDirectional) {
						message("# Zieldateisystem Einlesen");
						// initialize the remote file visitor
						remoteFileVisitor = new RemoteFileVisitor(operation.getTarget(), remoteFiles,
								emptyDirs, this);
						// file visitor start is operation remote path
						start = Paths.get(operation.getTargetPath());
						// walk file tree...
						Files.walkFileTree(start, remoteFileVisitor);
						// Abort process if cancel was pressed during walking the remote file system
						if (isCancelled()) {
							cancelSync("# Während des Einlesens des Zieldateisystems Abgebrochen!");
							return null;
						}
						message("# Fehlersuche und Erkennung von Änderungen im Zieldateisystem");

						// loop through all files found in remote file system
						for (File file_destination : remoteFiles) {
							// the current file
							// the current file relative path
							relativePath = file_destination.getPath()
									.substring(destinationBasePathLengthPlusOne);
							// skip if file is a file system database file in the operation root directory
							if (relativePath.startsWith(".fs.") && relativePath.endsWith(".db")) {
								continue;
							}
							// get the corresponding database file entry
							file_db = db.getFileByPath(relativePath);
							// create the theoretical counterpart (when synchronizing unidirectional) file
							// in the source file system
							file_source = new File(operation.getSourcePath(), relativePath);
							// if the file was found in the database and the source file exists
							// (if the source file doesn't exist, the file will be deleted later so there is
							// no need for checking it)
							if (file_db != null && file_source.exists()) {
								// then check for changes for
								// 1. - bidirectional synchronization
								// 2. - file integrity if not quickSync

								// initialize file lengths for comparison
								length_source = file_source.length();
								length_destination = file_destination.length();
								length_db = file_db.getLength();
								// initialize file modification times for comparison
								modified_source = file_source.lastModified();
								modified_destination = file_destination.lastModified();
								modified_db = file_db.getModified();
								// conclude if files were modified
								sourceFileWasModified = modified_source != modified_db;
								destinationFileWasModified = !(modified_destination == modified_db
										|| (compareElastic && ((modified_db / 1000) - 1 == modified_destination
												/ 1000
												|| (modified_db / 1000) + 1 == modified_destination / 1000)));
								// get checksums if not quickSync
								checksum_db = file_db.getChecksum();
								if (quickSync) {
									checksum_source = null;
									checksum_destination = null;
								} else {
									if (!sourceFileWasModified) {
										checksum_source = FSSync.createSHA384Hex(file_source);
									} else {
										checksum_source = null;
									}
									if (isCancelled()) {
										cancelSync("# Während dem Prüfen des Zieldateisystems Abgebrochen!");
										return null;
									}
									if (!destinationFileWasModified) {
										checksum_destination = FSSync.createSHA384Hex(file_destination);
									} else {
										checksum_destination = null;
									}
									if (isCancelled()) {
										cancelSync("# Während dem Prüfen des Zieldateisystems Abgebrochen!");
										return null;
									}
								}

								// assume file corrupt
								destinationFileIsGood = false;

								if (destinationFileWasModified) { // destination file was modified

									// workarounds for bug#1
									// consider file unchanged anyway
									// if (compareElastic) {
									// // if compare elastic and the target file modification date has not
									// // changed
									// // more than +/- 1 second
									// if ((modified_db / 1000) - 1 == (modified_destination / 1000)
									// || (modified_db / 1000) + 1 == (modified_destination / 1000)) {
									// continue;
									// }
									// } else if (!quickSync && operation.isIgnoreModifiedWhenEqual()
									// && checksum_destination != null && checksum_source != null) {
									// // or if checksum and file length are unchanged
									// if (checksum_db.equals(checksum_destination)
									// && length_db == length_destination) {
									// message("Modifikationsdatum ignoriert! Ziel: "
									// + df.format(modified_destination) + " db: "
									// + df.format(modified_db) + " " + file_db.getRelativePath(),
									// true);
									// continue;
									// }
									// }
									// sanity check if db is outdated:
									// if not quicksync and
									// if source file and destination file are equal
									// then update file in db
									// if (checksum_destination != null && checksum_source != null) {
									// if (!quickSync && modified_destination == modified_source
									// && length_destination == length_source
									// && checksum_destination.equals(checksum_source)) {
									// db.updateFile(new RelativeFile(file_db.getRelativePath(),
									// length_destination, modified_destination,
									// checksum_destination));
									// changed = true;
									// message("Datenbank aktualisiert: " + file_db.getRelativePath());
									// continue;
									// }
									// }
									// add the changed file to the list
									changedForeignFiles.add(file_destination);
									if (!isBiDirectional) {
										// warn if the change is not expected
										message("Fremd Geändert " + file_destination.getPath());
									}
								} else { // file in destination was not modified, integrity check is possible
									if (!quickSync) {
										// if destination file differs from database entry
										if (!checksum_db.equals(checksum_destination)
												|| length_db != length_destination) {
											// if source file is unmodified
											if (!sourceFileWasModified) {
												// if the source file is intact
												if (checksum_db.equals(checksum_source)
														&& length_db == length_source) {
													// the file is theoretically recoverable from source
													corruptedFilesDestination.add(file_destination);
												} else {
													// the source file is also corrupt, the file is lost
													lostFiles.add(file_destination);
												}
											}
										} else {
											// checksum and length are identical to database entry
											destinationFileIsGood = true;
										}
									}
								}
								// check source file
								// if the source file is unmodified and not quickSync
								if (!quickSync && !sourceFileWasModified) {
									// then check file integrity
									if (!checksum_db.equals(checksum_source) || length_db != length_source) {
										// source file is corrupt,
										// if destination file is intact
										if (destinationFileIsGood) {
											// the file is theoretically recoverable
											corruptedFilesSource.add(file_source);
										}
									}
								}
							} else if (file_source.exists()) {
								// should normally not happen..
								// file is not found in database but the file pair already exists
								// sanity check for out dated database: restore lost record
								length_destination = file_destination.length();
								modified_destination = file_destination.lastModified();
								checksum_destination = FSSync.createSHA384Hex(file_destination);
								relativePath = file_destination.getPath()
										.substring(destinationBasePathLengthPlusOne);
								if (checksum_destination != null) {
									db.add(relativePath, length_destination, modified_destination,
											checksum_destination);
									changed = true;
									message("Datenbankeintrag Wiederhergestellt: ".concat(relativePath));
								}
							} else { // new file in target directory
								newForeignFiles.add(file_destination);
								if (!isBiDirectional) {
									message("Fremd Neu " + file_destination.getPath());
								}
							}
							// abort synchronization if cancel button was pressed
							if (isCancelled()) {
								cancelSync("# Während der Suche nach Fehlern Abgebrochen!");
								return null;
							}
						}

						/*
						 * If There are corrupt files then inform user and abort synchronization
						 */
						if ((corruptedFilesDestination.size() + corruptedFilesSource.size()
								+ lostFiles.size()) > 0) {
							message("# " + (corruptedFilesDestination.size() + corruptedFilesSource.size()
									+ lostFiles.size()) + " korrupte Dateien Gefunden");
							// getContinueRestore = new GetContinueRestore(spd);
							// SwingUtilities.invokeAndWait(getContinueRestore);
							// continueRestore = getContinueRestore.isContinueRestore();
							//
							// if (continueRestore) {
							// fileIterator = corruptedFilesDestination.iterator();
							// while (fileIterator.hasNext()) {
							// file = fileIterator.next();
							// relativePath = file.getPath().substring(destinationBasePathLengthPlusOne);
							// file2 = new File(operation.getSourcePath(), relativePath);
							// copyActions.add(new CopyAction(file2, file, relativePath, false,
							// CopyAction.DIR_BACKUP));
							// message("Zieldatei zum Wiederherstellen Einreihen " + file.getPath());
							// if (isCancelled()) {
							// cancelSync("# W" + GC.ae()
							// + "hrend des Einreihens von Zieldateien Abgebrochen!");
							// return null;
							// }
							// }
							// fileIterator = corruptedFilesSource.iterator();
							// while (fileIterator.hasNext()) {
							// file = fileIterator.next();
							// relativePath = file.getPath().substring(sourceBasePathLengthPlusOne);
							// file2 = new File(operation.getRemotePath(), relativePath);
							// copyActions.add(new CopyAction(file2, file, relativePath, false,
							// CopyAction.DIR_RESTORE));
							// message("Quelldatei zum Wiederherstellen Einreihen " + file.getPath());
							// if (isCancelled()) {
							// cancelSync("# W" + GC.ae()
							// + "hrend des Einreihens von Quelldateien Abgebrochen!");
							// return null;
							// }
							// }
							// } else {

							/*
							 * output corrupt files to user and abort
							 */
							for (File file : corruptedFilesDestination) {
								message("Zieldatei fehlerhaft " + file.getPath());
							}
							for (File file : corruptedFilesSource) {
								message("Quelldatei fehlerhaft " + file.getPath());
							}
							for (File file : lostFiles) {
								relativePath = file.getPath()
										.substring(file.getPath().startsWith(operation.getTargetPath())
												? destinationBasePathLengthPlusOne
												: sourceBasePathLengthPlusOne);
								message("Ziel- und Quelldatei fehlerhaft " + relativePath);
							}
							cancelSync("# Synchronisation abgebrochen");
							return null;
							// }
						}

						/*
						 * remove empty directories in the target file system if there are any
						 */

						rmEmptyDirs(emptyDirs);
						// abort process if cancel button was pressed during removing empty dirs
						if (isCancelled()) {
							cancelSync("# Während des Löschens Leerer Verzeichnisse Abgebrochen!");
							return null;
						}

						// end of Integrity Check

						/*
						 * Enter files in List of files to copy or delete / remote
						 */

						// first consider unexpected changes in remote filesystem (if not bidirectional)
						if ((newForeignFiles.size() + changedForeignFiles.size()) > 0 && !isBiDirectional) {
							// get user input: how to handle unexpected changes
							getForeignFileHandling = new GetForeignFileHandling(spd);
							// remember the running time
							tSplit = System.currentTimeMillis() - opStart;
							setCountDownPaused(true);
							// show dialog
							SwingUtilities.invokeAndWait(getForeignFileHandling);
							// correct the running time
							opStart = System.currentTimeMillis() - tSplit;
							setCountDownPaused(false);
							// the user answer
							answer = getForeignFileHandling.getAnswer();
							if (answer == SynchronizationProcessDialog.foreign_cancelled) {
								cancelSync("Nach dem Auftauchen von fremden Änderungen Abgebrochen.");
								return null;
							} else if (answer == SynchronizationProcessDialog.foreign_integrate) {
								// for this round, bidirectional synchronization is activated
								isBiDirectional = true;
							} else if (answer == SynchronizationProcessDialog.foreign_restore) {
								// revert to previous state, delete changes
								// add new files to the list of files to delete
								for (File file : newForeignFiles) {
									if (isCancelled()) {
										break;
									}
									deleteActions.add(new DeleteAction(file,
											file.getPath().substring(destinationBasePathLengthPlusOne),
											DeleteAction.del_destination, false));
								}
								// add the changed (destination) files that are unchanged in the source
								// directory to the list of files to be copied
								for (File file : changedForeignFiles) {
									if (isCancelled()) {
										break;
									}
									relativePath = file.getPath().substring(destinationBasePathLengthPlusOne);
									file2 = new File(operation.getSourcePath(), relativePath);
									file_db = db.getFileByPath(relativePath);
									// file_db should never be null because it was recently queried
									if (file_db != null) {
										// avoid duplicates
										if (file_db.getModified() == file2.lastModified()) {
											// overwrite file
											copyActions.add(new CopyAction(file2, file, relativePath, false,
													CopyAction.DIR_BACKUP));
										} // else the file will be copied anyway, do nothing
									}
								}
							} // else answer is ignore changes, do nothing
						}
						// consider expected changes in remote file system (if bidirectional)
						if (newForeignFiles.size() + changedForeignFiles.size() > 0 && isBiDirectional) {
							// add new files from target file system to copy to the source file system to
							// list of files to copy
							for (File file : newForeignFiles) {
								if (isCancelled()) {
									break;
								}
								relativePath = file.getPath().substring(destinationBasePathLengthPlusOne);
								copyActions.add(
										new CopyAction(file, new File(operation.getSourcePath(), relativePath),
												relativePath, true, CopyAction.DIR_RESTORE));
							}
							// add changed remote files to copy back to source to list of files to copy
							for (File file : changedForeignFiles) {
								if (isCancelled()) {
									break;
								}
								relativePath = file.getPath().substring(destinationBasePathLengthPlusOne);
								copyActions.add(
										new CopyAction(file, new File(operation.getSourcePath(), relativePath),
												relativePath, false, CopyAction.DIR_RESTORE));
							}
						}
					}

					if (isCancelled()) {
						cancelSync("# Während des Einreihen von Änderungen im Zieldateisystem Abgebrochen!");
						return null;
					}

					/*
					 * Check SOURCE FILE SYSTEM for new and changed files
					 */
					message("# Quelldateisystem Einlesen");
					// start for local file visitor
					start = Paths.get(operation.getSourcePath());
					// initialize list for files of local file system
					sourceFiles = new Vector<File>();
					// initialize local file visitor
					localFileVisitor = new LocalFileVisitor(operation.getSource(), sourceFiles,
							operation.getExcludes(), this);
					// walk file tree...
					Files.walkFileTree(start, localFileVisitor);
					// abort synchronization if cancel button was pressed during listing the local
					// file system
					if (isCancelled()) {
						cancelSync("# Während des Einlesens des Quelldateisystems Abgebrochen!");
						return null;
					}
					message("# Quelldateisystem Analysieren");
					// iterate through local file system
					iSourceFiles = sourceFiles.iterator();
					while (iSourceFiles.hasNext()) {
						sourceFile = iSourceFiles.next();
						file_db = db
								.getFileByPath(sourceFile.getPath().substring(sourceBasePathLengthPlusOne));
						if (file_db != null) {
							// if the file was found in the database check if it was changed
							modified_source = sourceFile.lastModified();
							modified_db = file_db.getModified();
							if (modified_db != modified_source) {
								// if the file was changed the add it to the list of files to be copied
								relativePath = sourceFile.getPath().substring(sourceBasePathLengthPlusOne);
								file2 = new File(operation.getTargetPath(), relativePath);
								copyActions.add(new CopyAction(sourceFile, file2, relativePath, false,
										CopyAction.DIR_BACKUP));
							}
						} else {
							// the file is not found in the database, it is considered new and added to the
							// list of files to be copied
							relativePath = sourceFile.getPath().substring(sourceBasePathLengthPlusOne);
							file2 = new File(operation.getTargetPath(), relativePath);
							copyActions.add(new CopyAction(sourceFile, file2, relativePath, true,
									CopyAction.DIR_BACKUP));
						}
						// abort the operation if the cancel button was pressed during local file system
						// analysis
						if (isCancelled()) {
							cancelSync("# Während des Analysierens des Quelldateisystems Abgebrochen!");
							return null;
						}
					}

					/*
					 * list files to delete (or copy if existing file in source is missing in target
					 * in unidirectional mode)
					 */

					// query all files currently in database
					allFiles = db.listAll();
					// iterate through complete database
					for (RelativeFile file_db_del : allFiles) {
						// the file in the source file system
						file1 = new File(operation.getSourcePath(), file_db_del.getRelativePath());
						// does the source file exist?
						sourceFileExists = file1.exists();
						// the file in the remote/target file system
						file2 = new File(operation.getTargetPath(), file_db_del.getRelativePath());
						// does the remote file exist?
						remoteFileExists = file2.exists();
						if (!sourceFileExists && remoteFileExists) {
							// source file is missing, file should be deleted
							deleteActions.add(new DeleteAction(
									new File(operation.getTargetPath(), file_db_del.getRelativePath()),
									file_db_del.getRelativePath(), DeleteAction.del_destination, false));
						} else if (!remoteFileExists && sourceFileExists) {
							// remote file is missing, file should be deleted in source if bidirectional
							if (isBiDirectional) {
								deleteActions.add(new DeleteAction(
										new File(operation.getSourcePath(), file_db_del.getRelativePath()),
										file_db_del.getRelativePath(), DeleteAction.del_source, false));
							} else if (file_db_del.getModified() == file1.lastModified()) {
								// if not bidirectional the file is copied again
								copyActions.add(new CopyAction(file1, file2, file_db_del.getRelativePath(),
										false, CopyAction.DIR_BACKUP));
							}
						} else if (!sourceFileExists && !remoteFileExists) {
							// both files are missing, delete database entry
							message("Datei verschwunden! Lösche Eintrag: " + file_db_del.getRelativePath());
							changed = true;
							db.removeFileByPath(file_db_del.getRelativePath());
						}
						// if the cancel button is pressed during entering files to delete in the list
						// of files to delete then abort the synchronization
						if (isCancelled()) {
							cancelSync("# Während dem Suchen nach zu löschenden Dateien Abgebrochen!");
							return null;
						}
					}

					// initialize operation summary to find potential conflicts in bidirectional
					// mode and create summary
					try {
						operationSummary = new OperationSummary(operation.getSource(), operation.getTarget(),
								corruptedFilesSource, corruptedFilesDestination, lostFiles, copyActions,
								deleteActions, this, false, isBiDirectional);
					} catch (SynchronizationCancelledException e) {
						cancelSync("# Während dem Summieren abgebrochen");
						return null;
					}

					// Analysis done, save running time
					tAnalysis = System.currentTimeMillis() - opStart;
					abortCountDown();
					message("# Analyse fertig nach " + UIFx.formatMillisAsHoursMinutesSeconds(tAnalysis));

					// show summary dialog if option is on and there are changes to display or if
					// there are conflicts
					if ((showSummary && operationSummary.shouldDisplayDialog())
							|| operationSummary.hasConflicts()) {
						getSummaryApproval = new GetSummaryApproval(spd, operationSummary, isBiDirectional,
								operation.getPriorityOnConflict());
						SwingUtilities.invokeAndWait(getSummaryApproval);
						if (!getSummaryApproval.isApproved()) {
							message("# Operation Abgebrochen");
							break;
						}
					}

					/*
					 * Synchronization begins with deleting files and empty directories
					 */

					// message to user if there are delete actions selected
					if (operationSummary.getnDeleteActionsSelected() > 0) {
						status("2/2 - Löschen...", true);
						message("# " + operationSummary.getnDeleteActionsSelected() + " Dateien Löschen");
					}
					// initialize again for an empty list
					emptyDirs = new Vector<>();
					// iterate through delete actions
					for (DeleteAction deleteAction : deleteActions) {
						if (deleteAction.isSelected()) {
							// delete if selected
							if (deleteAction.getFile().delete()) {
								// confirm if wanted
								message(deleteAction.toString(), true);
								// remove file from database
								db.removeFileByPath(deleteAction.getRelativePath());
								changed = true;
								if (!FSFx.hasDirEntries(
										(file1 = deleteAction.getFile().getParentFile()).toPath())) {
									// add parent dir to empty dirs if the deleted file was the last file in
									// the directory
									emptyDirs.add(file1);
								}
							} else {
								// message if delete was not successful
								message("Fehler beim Löschen: " + deleteAction.getFile().getPath());
							}
						} else {
							// if the delete action was unselected, then restore the file
							file1 = deleteAction.getFile();
							file2 = new File(deleteAction.getLocation() == DeleteAction.del_destination
									? operation.getSource()
									: operation.getTarget(), deleteAction.getRelativePath());
							copyActions.add(new CopyAction(file1, file2, deleteAction.getRelativePath(), false,
									deleteAction.getLocation() == DeleteAction.del_destination
											? CopyAction.DIR_RESTORE
											: CopyAction.DIR_BACKUP));
						}
						// abort if the cancel button was pressed during deleting files
						if (isCancelled()) {
							cancelSync("# Während des Löschens Abgebrochen!");
							return null;
						}
					} // end of delete loop

					// remove empty directories if there are any
					rmEmptyDirs(emptyDirs);
					// abort if the cancel button was pressed during deleting empty directories
					if (isCancelled()) {
						cancelSync("# Während des Löschens von leeren Ordnern Abgebrochen!");
						return null;
					}

					/*
					 * Check if there is enough space for copying
					 */

					// assume enough space
					enoughSpace = true;

					// as long as there is not enough space in the target file system
					// ask the user if he wants to retry (after freeing up space otherwise)
					while (operation.getTarget().getFreeSpace() < operationSummary.getUpdateSizeDestination()
							- operationSummary.getUpdateSizeDestinationModifiedOld()) {
						getRetryOnOutOfMemory = new GetRetryOnOutOfMemory(spd, "Zieldatenträger",
								operationSummary.getUpdateSizeDestination()
										- operationSummary.getUpdateSizeDestinationModifiedOld());
						// // remember passed time
						// tSyncSplit = System.currentTimeMillis() - tSyncStart;
						// // pause progress bar
						// setCountDownPaused(true);
						// show dialog
						SwingUtilities.invokeAndWait(getRetryOnOutOfMemory);
						// // correct running time
						// tSyncStart = System.currentTimeMillis() - tSyncSplit;
						// // resume progress bar
						// setCountDownPaused(false);
						// get user answer, true for yes
						retry = getRetryOnOutOfMemory.isRetry();
						if (!retry) {
							// answer was no, exit loop
							enoughSpace = false;
							break;
						}
					}

					// there is not enough space, cancel Operation
					if (!enoughSpace) {
						message("# Operation Abgebrochen, nicht genügend Speicherplatz auf dem Zieldatenträger");
						break;
					}

					// overwrite first if that frees up necessary space
					overwriteFirst = false;
					if (operation.getTarget().getFreeSpace() < operationSummary.getUpdateSizeDestination()) {
						overwriteFirst = true;
					}

					if (isBiDirectional) {
						// in bidirectional mode, check if there is enough space in source file system
						// as long as there is not enough space, ask if the user wants to retry
						while (operation.getSource().getFreeSpace() < operationSummary.getUpdateSizeSource()
								- operationSummary.getUpdateSizeSourceModifiedOld()) {
							getRetryOnOutOfMemory = new GetRetryOnOutOfMemory(spd, "Quelldatenträger",
									operationSummary.getUpdateSizeSource()
											- operationSummary.getUpdateSizeSourceModifiedOld());
							// // remember passed time
							// tSyncSplit = System.currentTimeMillis() - tSyncStart;
							// // pause progress bar
							// setCountDownPaused(true);
							// show dialog
							SwingUtilities.invokeAndWait(getRetryOnOutOfMemory);
							// // correct running time
							// tSyncStart = System.currentTimeMillis() - tSyncSplit;
							// // resume progress bar
							// setCountDownPaused(false);
							// get user answer, true for yes
							retry = getRetryOnOutOfMemory.isRetry();
							if (!retry) {
								// answer is no, exit loop
								enoughSpace = false;
								break;
							}
						}
						// if there is not enough space then abort
						if (!enoughSpace) {
							message("# Operation Abgebrochen, nicht genügend Speicherplatz auf dem Quelldatenträger");
							break;
						}
						// overwrite first if necessary
						if (operation.getSource().getFreeSpace() < operationSummary.getUpdateSizeSource()) {
							overwriteFirst = true;
						}
					}

					if (operationSummary.getnCopyActionsSelected() > 0) {
						message("# " + operationSummary.getnCopyActionsSelected() + " Dateien kopieren");
						status("2/2 - Dateien Kopieren...", true);
					}

					// reset counters to zero
					copied = 0;
					tSplit = 0;
					ix = 0;
					counter = 0;
					tCopyStart = System.currentTimeMillis();
					// start the progress bar
					setCountDown(operation.getAverageSyncTime(operationSummary.getUpdateSizeTotal()));
					// if overwrite first
					if (overwriteFirst) {
						// then loop through the copy actions twice
						while (ix < 2) {
							// loop through copy actions
							for (CopyAction copyAction : copyActions) {
								/*
								 * COPY BLOCK
								 */
								// skip if copy action is not selected
								if (!copyAction.isSelected()) {
									continue;
								}
								// select modified files for the first loop and new files for the second
								if ((ix == 0 && !copyAction.isNew()) || (ix == 1 && copyAction.isNew())) {
									try {
										// try copying the current file
										copied = copy(copyAction, db, copied, tCopyStart);
										if (!changed) {
											changed = true;
										}
										counter++;
									} catch (IOException e) {}
									// save current running time
									tSplit = System.currentTimeMillis() - tCopyStart;
									// abort if the cancel button was pressed
									if (isCancelled()) {
										if (counter > 0) {
											// final status info of copied files
											message("# " + counter + " Dateien Kopiert, "
													+ FSFx.formatFileLength(copied) + " in "
													+ UIFx.formatMillisAsHoursMinutesSeconds(tSplit) + ", "
													+ FSFx.formatTransferSpeed(tCopyStart, copied));
										}
										cancelSync("# Während des Kopierens Abgebrochen!");
										return null;
									}
								}
								/*
								 * COPY BLOCK END
								 */
							}
							ix++;
						}
					} else {
						// normal copy mode
						// loop through copy actions
						for (CopyAction copyAction : copyActions) {
							/*
							 * COPY BLOCK
							 */
							// skip if the copy action is not selected
							if (!copyAction.isSelected()) {
								continue;
							}
							try {
								// try copying the current file
								copied = copy(copyAction, db, copied, tCopyStart);
								if (!changed) {
									changed = true;
								}
								counter++;
							} catch (IOException | SynchronizationCancelledException e) {}
							// save current running time
							tSplit = System.currentTimeMillis() - tCopyStart;
							// abort if the cancel button was pressed
							if (isCancelled()) {
								if (counter > 0) {
									// final files copied info
									message("# " + counter + " Dateien Kopiert, "
											+ FSFx.formatFileLength(copied) + " in "
											+ UIFx.formatMillisAsHoursMinutesSeconds(tSplit) + ", "
											+ FSFx.formatTransferSpeed(tCopyStart, copied));
								}
								cancelSync("# Während des Kopierens Abgebrochen!");
								return null;
							}
						}
						/*
						 * COPY BLOCK END
						 */
					}
					// final files copied info if there were any files copied
					abortCountDown();
					if (counter > 0) {
						message("# " + counter + " Dateien Kopiert, " + FSFx.formatFileLength(copied) + " in "
								+ UIFx.formatMillisAsHoursMinutesSeconds(tSplit) + ", "
								+ FSFx.formatTransferSpeed(tCopyStart, copied));
					}
					// message if there were no changes made
					if (!changed) {
						message("# Keine Änderungen Gefunden");
					}
					// output operation running time
					message("# # Operation fertig nach "
							+ UIFx.formatMillisAsHoursMinutesSeconds(System.currentTimeMillis() - opStart));

					/*
					 * save operation running time and data copied statistics
					 */
					operation.setLastSynced(System.currentTimeMillis());
					operation.setReminded(false);
					if (!firstRun) {
						if (quickSync) {
							operation.registerQuickAnalysis(tAnalysis);
						} else {
							operation.registerDeepAnalysis(tAnalysis);
						}
						if (counter > 0) {
							tSplit = operation.getLastSynced() - tCopyStart;
							operation.registerSynchronisation(tSplit, copied, counter);
						}
					}
				} catch (Exception e) {
					throw e;
				} finally {
					// finally synchronize the databases
					if (changed && dbEdit != null && dbDestination != null && db != null) {
						// increment dbVersion after each synchronization
						db.incrementVersion();
						try {
							FileUtils.copyFile(dbEdit, dbDestination);
						} catch (IOException e2) {
							e2.printStackTrace();
						}
					}
				} // end of finally of operation try
			} // end of batch loop

			flushMessages();
			finishSync(
					"# # # Alles Erledigt nach "
							+ UIFx.formatMillisAsHoursMinutesSeconds(System.currentTimeMillis() - syncStart),
					null);
		} catch (Exception e) {
			flushMessages();
			finishSync("# Fehler nach "
					+ UIFx.formatMillisAsHoursMinutesSeconds(System.currentTimeMillis() - syncStart) + " : "
					+ e.getMessage(), e);
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Removes a list of empty directories and its parent directories as long as
	 * they are empty.
	 * 
	 * @param emptyDirs
	 *            The list of empty directories to be removed.
	 */
	private void rmEmptyDirs(Vector<File> emptyDirs) {
		if (emptyDirs.size() > 0) {
			message("# " + emptyDirs.size() + " Leere Verzeichnisse Löschen");
			boolean hold;
			// iterate through empty directories
			for (File file : emptyDirs) {
				// assume the directory is the last entry in its parent directory
				hold = false;
				do {
					if (!FSFx.hasDirEntries(file.toPath())) {
						// try to remove the current directory only when it is empty
						if (file.delete()) {
							// positive message after directory was deleted
							message("Leeres Verzeichnis Löschen " + file.getPath(), true);
						} else {
							// negative message and end of loop if directory was not deleted
							message("Konnte leeres Verzeichnis nicht löschen! " + file.getPath());
							hold = true;
						}
						// step up
						file = file.getParentFile();
					} else {
						// end the loop, there are other entries in the parent directory
						hold = true;
					}
					// abort if cancel button was pressed
					if (isCancelled()) {
						return;
					}
				} while (!hold);
			}
		}
	}

	/**
	 * copy file and return new total files copied length
	 * 
	 * @param copyAction
	 *            the copy action to be executed
	 * @param db
	 *            the database
	 * @param copied
	 *            the currently total bytes copied
	 * @param tStart
	 *            the time the file copy process started.
	 * @return the new total bytes copied.
	 * @throws IOException
	 *             if the checksum creation was unsuccessful or the file could not
	 *             be copied.
	 * @throws SynchronizationCancelledException
	 *             when the user pressed the cancel button during checksum creation.
	 */
	private long copy(CopyAction copyAction, OnlineDB db, long copied, long tStart)
			throws IOException, SynchronizationCancelledException {
		// message file if wanted
		message("Kopiere ".concat(copyAction.toString()), true);
		// first create checksum
		String checksum = FSSync.createSHA384Hex(copyAction.getSource());
		if (checksum == null) {
			message("!!! Fehler beim Erstellen der Prüfsumme, Datei wurde übersprungen: "
					.concat(copyAction.getSource().getPath()));
			throw new IOException("Could not create checksum");
		}
		// abort if cancel button was pressed
		if (isCancelled()) {
			throw new SynchronizationCancelledException();
		}
		try {
			if (!copyAction.getDestination().getParentFile().exists()) {
				copyAction.getDestination().getParentFile().mkdirs();
			}
			// try copying the file
			FileUtils.copyFile(copyAction.getSource(), copyAction.getDestination());
		} catch (IOException e) {
			// message on error
			message("!!! Fehler: ".concat(e.getMessage()).concat(", Datei wurde übersprungen: ")
					.concat(copyAction.getSource().getPath()));
			// throw e, end function without return value
			throw e;
		}
		// increment the total copied file length
		copied += copyAction.getSource().length();
		// update status data
		// long tSplit = (System.currentTimeMillis() - tStart) / 1000;
		status("2/2 - Dateien Kopieren... ".concat(FSFx.formatTransferSpeed(tStart, copied)), false);
		if (copyAction.isNew()) {
			// enter new file in database
			db.add(copyAction.getRelativePath(), copyAction.getSource().length(),
					copyAction.getSource().lastModified(), checksum);
		} else {
			// update the file in the database
			db.updateFile(copyAction.getRelativePath(), copyAction.getSource().length(),
					copyAction.getSource().lastModified(), checksum);
		}
		// return the currently transferred file length
		return copied;
	}

	/**
	 * process() is not used because the output got disordered
	 */
	// @Override
	// protected void process(List<StatusMessage> chunks) {}

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
	 *            the time since the last message is longer than 450 milliseconds
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
			if (System.currentTimeMillis() - lastStatusMessageUpdate > 450 || !verbose) {
				if (messages != null) {
					SwingUtilities.invokeLater(new RunStatusMessageUpdate(spd, messages));
					messages = null;
					lastStatusMessageUpdate = System.currentTimeMillis();
				}
			}
		}
	}

	/**
	 * sets the SynchronisationProcessDialog finished
	 * 
	 * @param message
	 *            The exit message
	 * @param e
	 *            The Exception or null
	 */
	private void finishSync(String message, Exception e) {
		SwingUtilities.invokeLater(new RunFinished(message, e, spd));
	}

	/**
	 * set SynchronisationProcessDialog cancelled and copy database after editing
	 * 
	 * @param message
	 *            The abort message
	 */
	private final void cancelSync(String message) {
		SwingUtilities.invokeLater(new RunCancelled(message, spd));
	}

	/**
	 * status label update, limited to 1 update per half second
	 * 
	 * @param status
	 *            the current status label text
	 * @param force
	 *            true to prevent delay. false to limit to one message per half
	 *            second.
	 */
	private void status(String status, boolean force) {
		if (force || System.currentTimeMillis() - lastStatusTextUpdate > 500) {
			SwingUtilities.invokeLater(new RunStatusTextUpdate(status, spd));
			lastStatusTextUpdate = System.currentTimeMillis();
		}
	}

	/**
	 * start a new count down for the progress bar
	 * 
	 * @param t
	 *            the time in milliseconds to count down from.
	 */
	private void setCountDown(long t) {
		SwingUtilities.invokeLater(new RunStartCountDown(spd, t));
	}

	/**
	 * set count down paused or resume
	 * 
	 * @param paused
	 *            true to pause, false to resume
	 */
	private void setCountDownPaused(boolean paused) {
		SwingUtilities.invokeLater(new RunPauseCountDown(spd, paused));
	}

	/**
	 * abort the current count down
	 */
	private void abortCountDown() {
		SwingUtilities.invokeLater(new RunAbortCountDown(spd));
	}
}
