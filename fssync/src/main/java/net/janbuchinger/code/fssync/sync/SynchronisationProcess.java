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
package net.janbuchinger.code.fssync.sync;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import net.janbuchinger.code.fssync.FSSync;
import net.janbuchinger.code.fssync.Operation;
import net.janbuchinger.code.fssync.Settings;
import net.janbuchinger.code.fssync.sync.ui.GetContinueRestore;
import net.janbuchinger.code.fssync.sync.ui.GetForeignFileHandling;
import net.janbuchinger.code.fssync.sync.ui.GetRetryOnOutOfMemory;
import net.janbuchinger.code.fssync.sync.ui.GetSummaryApproval;
import net.janbuchinger.code.fssync.sync.ui.RunCancelled;
import net.janbuchinger.code.fssync.sync.ui.RunFinished;
import net.janbuchinger.code.fssync.sync.ui.RunSetDeterminate;
import net.janbuchinger.code.fssync.sync.ui.RunStatusTextUpdate;
import net.janbuchinger.code.fssync.sync.ui.RunStatusUpdate;
import net.janbuchinger.code.fssync.sync.ui.SynchronisationProcessDialog;
import net.janbuchinger.code.mishmash.GC;
import net.janbuchinger.code.mishmash.ui.UIFx;

import org.apache.commons.io.FileUtils;

public class SynchronisationProcess extends SwingWorker<Void, Void> implements PropertyChangeListener {

	private final SynchronisationProcessDialog spd;

	private final Vector<Operation> operations;

	private final boolean showSummary;

	private final String syncTitle;

	public SynchronisationProcess(Vector<Operation> operations, String syncTitle, Settings settings,
			SynchronisationProcessDialog spd) {
		this.operations = operations;
		this.syncTitle = syncTitle;
		this.spd = spd;
		addPropertyChangeListener(this);
		showSummary = settings.isShowSummary();
	}

	@SuppressWarnings("unused")
	@Override
	protected final Void doInBackground() throws Exception {

		File dbDestination = null;
		File dbEdit = null;

		boolean changed = false;

		long syncStart = System.currentTimeMillis();
		try {
			long opStart;

			Iterator<Operation> iOp;
			Operation operation;

			boolean isBiDirectional;

			OnlineDB db;

			Vector<CopyAction> copyActions;
			CopyAction copyAction;
			Iterator<CopyAction> iCopyAction;

			Vector<DeleteAction> deleteActions;
			DeleteAction deleteAction;
			Iterator<DeleteAction> iDeleteActions;

			Vector<File> remoteFiles;
			Iterator<File> iRemoteFiles;
			Vector<File> emptyDirs;

			Vector<File> newForeignFiles;
			Vector<File> changedForeignFiles;

			Vector<File> corruptedFilesSource;
			Vector<File> corruptedFilesDestination;
			Vector<File> lostFiles;

			int destinationBasePathLengthPlusOne;
			int sourceBasePathLengthPlusOne;

			RemoteFileVisitor remoteFileVisitor;
			Path start;
			int nRemoteFiles;
			int counter;

			File file_destination;
			RelativeFile file_db;
			File file_source;

			long length_source;
			long length_destination;
			long length_db;
			long modified_source;
			long modified_destination;
			long modified_db;
			String checksum_source;
			String checksum_destination;
			String checksum_db;

			SimpleDateFormat df = UIFx.initPreciseDisplayDateTimeFormat();

			// boolean bothFilesCorrupted;
			boolean destinationFileIsGood;

			boolean hold;
			Iterator<File> fileIterator;
			File file;
			File file2;
			String relativePath;

			GetContinueRestore getContinueRestore;
			boolean continueRestore;

			GetForeignFileHandling getForeignFileHandling;
			int answer;

			GetSummaryApproval getSummaryApproval;

			GetRetryOnOutOfMemory getRetryOnOutOfMemory;
			boolean retry;

			Vector<File> sourceFiles;
			LocalFileVisitor localFileVisitor;
			int countSourceFiles;
			int nSourceFiles;
			Iterator<File> iSourceFiles;
			File sourceFile;

			Vector<RelativeFile> allFiles;
			Iterator<RelativeFile> iAllFiles;
			boolean sourceFileExists;
			boolean remoteFileExists;

			int i1;
			int i2;
			int direction;

			OperationSummary operationSummary;

			boolean enoughSpace;
			boolean overwriteFirst;

			long updateSize;
			long copied;

			long tSplit;
			long tStart;

			int ix;

			if (syncTitle != null)
				if (!syncTitle.equals(""))
					SwingUtilities.invokeLater(new RunStatusUpdate("# # # " + syncTitle + " Ausführen", false, spd));

			/*
			 * loop through the operations
			 */
			iOp = operations.iterator();
			while (iOp.hasNext()) {
				opStart = System.currentTimeMillis();
				/*
				 * set the current operation, continue if operation is offline
				 */
				operation = iOp.next();
				if (!operation.isOnline()) {
					SwingUtilities.invokeLater(
							new RunStatusUpdate("# # Operation offline " + operation.toString(), false, spd));
					continue;
				}

				SwingUtilities.invokeLater(new RunStatusUpdate("# # Operation " + operation.toString(), false, spd));
				SwingUtilities.invokeLater(new RunStatusTextUpdate("Analysieren...", spd));
				SwingUtilities.invokeLater(new RunSetDeterminate(false, spd));

				changed = false;
				isBiDirectional = operation.isSyncBidirectional();

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
				deleteActions = new Vector<DeleteAction>();

				remoteFiles = new Vector<File>();
				emptyDirs = new Vector<File>();

				newForeignFiles = new Vector<File>();
				changedForeignFiles = new Vector<File>();

				corruptedFilesSource = new Vector<File>();
				corruptedFilesDestination = new Vector<File>();
				lostFiles = new Vector<File>();

				destinationBasePathLengthPlusOne = operation.getRemotePath().length() + 1;
				sourceBasePathLengthPlusOne = operation.getSourcePath().length() + 1;

				/*
				 * Check target file system
				 */
				SwingUtilities.invokeLater(new RunStatusUpdate("# Zieldateisystem Einlesen", false, spd));

				remoteFileVisitor = new RemoteFileVisitor(operation.getTarget(), remoteFiles, emptyDirs, spd);

				start = Paths.get(operation.getRemotePath());

				Files.walkFileTree(start, remoteFileVisitor);

				if (spd.isCancelled()) {
					cancelSync("# W" + GC.ae() + "hrend des Einlesens des Zieldateisystems Abgebrochen!", dbEdit,
							dbDestination, changed);
					return null;
				}
				iRemoteFiles = remoteFiles.iterator();

				SwingUtilities.invokeLater(new RunSetDeterminate(true, spd));
				SwingUtilities.invokeLater(new RunStatusUpdate(
						"# Fehlersuche und Erkennung von " + GC.Ae() + "nderungen im Zieldateisystem", false, spd));

				nRemoteFiles = remoteFiles.size();
				counter = 0;

				while (iRemoteFiles.hasNext()) {
					file_destination = iRemoteFiles.next();
					relativePath = file_destination.getAbsolutePath().substring(destinationBasePathLengthPlusOne);
					if (relativePath.startsWith(".fs.") && relativePath.endsWith(".db")) {
						continue;
					}
					file_db = db.getFileByPath(
							file_destination.getAbsolutePath().substring(destinationBasePathLengthPlusOne));
					file_source = new File(operation.getSourcePath(), file_db.getRelativePath());
					if (file_db != null) {

						length_source = file_source.length();
						modified_source = file_source.lastModified();
						checksum_source = FSSync.getChecksum(file_source);
						length_destination = file_destination.length();
						modified_destination = file_destination.lastModified();
						checksum_destination = FSSync.getChecksum(file_destination);
						length_db = file_db.getLength();
						modified_db = file_db.getModified();
						checksum_db = file_db.getChecksum();

						destinationFileIsGood = false;

						if (modified_db != modified_destination) {
							if (modified_destination == modified_source && length_destination == length_source
									&& checksum_destination.equals(checksum_source)) {
								db.updateFile(new RelativeFile(file_db.getRelativePath(), length_destination,
										modified_destination, checksum_destination));
								changed = true;
								SwingUtilities.invokeLater(new RunStatusUpdate(
										"Datenbank aktualisiert: " + file_db.getRelativePath(), false, spd));
								continue;
							}
							/*
							 * print diagnostics
							 */
							// System.out.println("src: " + modified_source
							// + " " + checksum_source + " "
							// + length_source);
							// System.out
							// .println("db : " + modified_db + " " +
							// checksum_db + " " + length_db);
							// System.out.println("dst: " +
							// modified_destination + " " +
							// checksum_destination
							// + " " + length_destination);
							// System.out
							// .println("------------------------------------------------------------------");

							// output of weird modification time rollback by
							// one second in destination filesystem
							//
							// location: lastModified checksum length
							// ------------------------------------
							// src: 1494828291000 4a23d...2e8b 1211
							// db : 1494828291000 4a23d...2e8b 1211
							// dst: 1494828290000 4a23d...2e8b 1211
							// ------------------------------------
							// src: 1494973775000 1507f...d40d 472
							// db : 1494973775000 1507f...d40d 472
							// dst: 1494973774000 1507f...d40d 472
							// ------------------------------------
							/*
							 * ignore modification time difference when checksum and length in database
							 * stored and current destination are equal
							 */
							if (operation.isIgnoreModifiedWhenEqual()) {
								if (checksum_db.equals(checksum_destination) && length_db == length_destination) {
									SwingUtilities
											.invokeLater(new RunStatusUpdate(
													"Modifikationsdatum ignoriert! Ziel: "
															+ df.format(modified_destination) + " db: "
															+ df.format(modified_db) + " " + file_db.getRelativePath(),
													true, spd));
									continue;
								}
							}
							changedForeignFiles.add(file_destination);
							if (!isBiDirectional) {
								SwingUtilities.invokeLater(new RunStatusUpdate(
										"Fremd Geändert " + file_destination.getAbsolutePath(), false, spd));
							}
						} else {
							if (!checksum_db.equals(checksum_destination) || length_db != length_destination) {
								if (modified_db == modified_source) {
									if (checksum_db.equals(checksum_source) && length_db == length_source) {
										corruptedFilesDestination.add(file_destination);
									} else {
										lostFiles.add(file_destination);
										// bothFilesCorrupted = true;
									}
								}
							} else {
								destinationFileIsGood = true;
							}
						}

						if (modified_db == modified_source) {
							if (!checksum_db.equals(checksum_source) || length_db != length_source) {
								if (destinationFileIsGood) {
									corruptedFilesSource.add(file_source);
								}
							}
						}
					} else {
						if (file_source.exists()) {
							length_destination = file_destination.length();
							modified_destination = file_destination.lastModified();
							checksum_destination = FSSync.getChecksum(file_destination);
							db.add(new RelativeFile(
									file_destination.getPath().substring(destinationBasePathLengthPlusOne),
									length_destination, modified_destination, checksum_destination));
							changed = true;
							continue;
						}
						newForeignFiles.add(file_destination);
						if (!isBiDirectional)
							SwingUtilities.invokeLater(
									new RunStatusUpdate("Fremd Neu " + file_destination.getAbsolutePath(), false, spd));
					}
					counter++;
					setProgress((int) ((100.0 / nRemoteFiles) * counter));
					if (spd.isCancelled()) {
						cancelSync("# W" + GC.ae() + "hrend der Suche nach Fehlern Abgebrochen!", dbEdit, dbDestination,
								changed);
						return null;
					}
				}

				SwingUtilities.invokeLater(new RunSetDeterminate(false, spd));

				if (emptyDirs.size() > 0) {
					SwingUtilities.invokeLater(new RunStatusUpdate(
							"# " + emptyDirs.size() + " Leere Verzeichnisse L" + GC.oe() + "schen", false, spd));

					fileIterator = emptyDirs.iterator();
					while (fileIterator.hasNext()) {
						file = fileIterator.next();
						hold = false;
						do {
							if (file.listFiles().length == 0) {
								file.delete();
								SwingUtilities.invokeLater(new RunStatusUpdate(
										"Leeres Verzeichnis L" + GC.oe() + "schen " + file.getAbsolutePath(), true,
										spd));
								file = file.getParentFile();
							} else
								hold = true;
						} while (!hold);
						if (spd.isCancelled()) {
							cancelSync(
									"# W" + GC.ae() + "hrend des L" + GC.oe()
											+ "schens Leerer Verzeichnisse Abgebrochen!",
									dbEdit, dbDestination, changed);
							return null;
						}
					}
				}

				// restore damaged

				if ((corruptedFilesDestination.size() + corruptedFilesSource.size() + lostFiles.size()) > 0) {

					SwingUtilities.invokeLater(new RunStatusUpdate(
							"# " + (corruptedFilesDestination.size() + corruptedFilesSource.size() + lostFiles.size())
									+ " korrupte Dateien Gefunden",
							false, spd));

					getContinueRestore = new GetContinueRestore(spd);
					SwingUtilities.invokeAndWait(getContinueRestore);
					continueRestore = getContinueRestore.isContinueRestore();

					if (continueRestore) {
						fileIterator = corruptedFilesDestination.iterator();
						while (fileIterator.hasNext()) {
							file = fileIterator.next();
							relativePath = file.getAbsolutePath().substring(destinationBasePathLengthPlusOne);
							file2 = new File(operation.getSourcePath(), relativePath);
							copyActions.add(new CopyAction(file2, file, relativePath, false, CopyAction.DIR_BACKUP));
							SwingUtilities.invokeLater(new RunStatusUpdate(
									"Zieldatei zum Wiederherstellen Einreihen " + file.getAbsolutePath(), false, spd));
							if (spd.isCancelled()) {
								cancelSync("# W" + GC.ae() + "hrend des Einreihens von Zieldateien Abgebrochen!",
										dbEdit, dbDestination, changed);
								return null;
							}
						}
						fileIterator = corruptedFilesSource.iterator();
						while (fileIterator.hasNext()) {
							file = fileIterator.next();
							relativePath = file.getAbsolutePath().substring(sourceBasePathLengthPlusOne);
							file2 = new File(operation.getRemotePath(), relativePath);
							copyActions.add(new CopyAction(file2, file, relativePath, false, CopyAction.DIR_RESTORE));
							SwingUtilities.invokeLater(new RunStatusUpdate(
									"Quelldatei zum Wiederherstellen Einreihen " + file.getAbsolutePath(), false, spd));
							if (spd.isCancelled()) {
								cancelSync("# W" + GC.ae() + "hrend des Einreihens von Quelldateien Abgebrochen!",
										dbEdit, dbDestination, changed);
								return null;
							}
						}
					} else {
						fileIterator = corruptedFilesDestination.iterator();
						while (fileIterator.hasNext()) {
							file = fileIterator.next();
							SwingUtilities.invokeLater(
									new RunStatusUpdate("Zieldatei fehlerhaft " + file.getAbsolutePath(), false, spd));
						}
						fileIterator = corruptedFilesSource.iterator();
						while (fileIterator.hasNext()) {
							file = fileIterator.next();
							SwingUtilities.invokeLater(
									new RunStatusUpdate("Quelldatei fehlerhaft " + file.getAbsolutePath(), false, spd));
						}
						fileIterator = lostFiles.iterator();
						while (fileIterator.hasNext()) {
							file = fileIterator.next();
							relativePath = file.getAbsolutePath()
									.substring(file.getAbsolutePath().startsWith(operation.getRemotePath())
											? destinationBasePathLengthPlusOne
											: sourceBasePathLengthPlusOne);
							SwingUtilities.invokeLater(
									new RunStatusUpdate("Ziel- und Quelldatei fehlerhaft " + relativePath, false, spd));
						}
						cancelOp("# Operation Abgebrochen", dbEdit, dbDestination, changed);
						break;
					}
				}

				// sync files

				// first consider changes in remote filesystem

				if ((newForeignFiles.size() + changedForeignFiles.size()) > 0 && !isBiDirectional) {
					getForeignFileHandling = new GetForeignFileHandling(spd);
					SwingUtilities.invokeAndWait(getForeignFileHandling);
					answer = getForeignFileHandling.getAnswer();
					if (answer == SynchronisationProcessDialog.foreign_integrate) {
						isBiDirectional = true;
					} else if (answer == SynchronisationProcessDialog.foreign_restore) {
						fileIterator = newForeignFiles.iterator();
						while (fileIterator.hasNext()) {
							file = fileIterator.next();
							deleteActions.add(new DeleteAction(file,
									file.getAbsolutePath().substring(destinationBasePathLengthPlusOne),
									DeleteAction.del_destination, false));
						}
						fileIterator = changedForeignFiles.iterator();
						while (fileIterator.hasNext()) {
							file = fileIterator.next();
							relativePath = file.getAbsolutePath().substring(destinationBasePathLengthPlusOne);
							file2 = new File(operation.getSourcePath(), relativePath);
							copyActions.add(new CopyAction(file2, file, relativePath, false, CopyAction.DIR_BACKUP));
						}
					} // else ignore
				}
				if (isBiDirectional) {
					fileIterator = newForeignFiles.iterator();
					while (fileIterator.hasNext()) {
						file = fileIterator.next();
						relativePath = file.getAbsolutePath().substring(destinationBasePathLengthPlusOne);
						copyActions.add(new CopyAction(file, new File(operation.getSourcePath(), relativePath),
								relativePath, true, CopyAction.DIR_RESTORE));
					}
					fileIterator = changedForeignFiles.iterator();
					while (fileIterator.hasNext()) {
						file = fileIterator.next();
						relativePath = file.getAbsolutePath().substring(destinationBasePathLengthPlusOne);
						copyActions.add(new CopyAction(file, new File(operation.getSourcePath(), relativePath),
								relativePath, false, CopyAction.DIR_RESTORE));
					}
				}

				SwingUtilities.invokeLater(new RunStatusUpdate("# Quelldateisystem Einlesen", false, spd));

				start = Paths.get(operation.getSourcePath());

				sourceFiles = new Vector<File>();

				localFileVisitor = new LocalFileVisitor(operation.getSource(), sourceFiles, operation.getExcludes(),
						spd);

				Files.walkFileTree(start, localFileVisitor);

				if (spd.isCancelled()) {
					cancelSync("# W" + GC.ae() + "hrend des Einlesens des Quelldateisystems Abgebrochen!", dbEdit,
							dbDestination, changed);
					return null;
				}

				iSourceFiles = sourceFiles.iterator();

				SwingUtilities.invokeLater(new RunStatusUpdate("# Quelldateisystem Analysieren", false, spd));

				countSourceFiles = 1;
				nSourceFiles = sourceFiles.size();
				SwingUtilities.invokeLater(new RunSetDeterminate(true, spd));
				while (iSourceFiles.hasNext()) {
					sourceFile = iSourceFiles.next();
					file_db = db.getFileByPath(sourceFile.getAbsolutePath().substring(sourceBasePathLengthPlusOne));
					if (file_db != null) {
						modified_source = sourceFile.lastModified();
						modified_db = file_db.getModified();
						if (modified_db != modified_source) {
							if (operation.isIgnoreModifiedWhenEqual()) {
								if (file_db.getChecksum().equals(FSSync.getChecksum(sourceFile))
										&& file_db.getLength() == sourceFile.length()) {
									SwingUtilities
											.invokeLater(new RunStatusUpdate(
													"Modifikationsdatum ignoriert! Quelle: "
															+ df.format(modified_source) + " db: "
															+ df.format(modified_db) + " " + file_db.getRelativePath(),
													true, spd));
									continue;
								}
							}
							relativePath = sourceFile.getAbsolutePath().substring(sourceBasePathLengthPlusOne);
							file2 = new File(operation.getRemotePath(), relativePath);
							copyActions
									.add(new CopyAction(sourceFile, file2, relativePath, false, CopyAction.DIR_BACKUP));
						}
					} else {
						relativePath = sourceFile.getAbsolutePath().substring(sourceBasePathLengthPlusOne);
						copyActions.add(new CopyAction(sourceFile, new File(operation.getRemotePath(), relativePath),
								relativePath, true, CopyAction.DIR_BACKUP));
					}
					if (spd.isCancelled()) {
						cancelSync("# W" + GC.ae() + "hrend des Analysierens des Quelldateisystems Abgebrochen!",
								dbEdit, dbDestination, changed);
						return null;
					}
					setProgress((int) ((100.0 / nSourceFiles) * countSourceFiles++));
				}

				SwingUtilities.invokeLater(new RunSetDeterminate(false, spd));

				// list delete files

				allFiles = db.listAll();
				iAllFiles = allFiles.iterator();

				while (iAllFiles.hasNext()) {
					file_db = iAllFiles.next();
					sourceFileExists = new File(operation.getSourcePath(), file_db.getRelativePath()).exists();
					remoteFileExists = new File(operation.getRemotePath(), file_db.getRelativePath()).exists();
					if (!sourceFileExists && remoteFileExists) {
						deleteActions
								.add(new DeleteAction(new File(operation.getRemotePath(), file_db.getRelativePath()),
										file_db.getRelativePath(), DeleteAction.del_destination, false));
					} else if (!remoteFileExists && sourceFileExists) {
						if (isBiDirectional)
							deleteActions.add(
									new DeleteAction(new File(operation.getSourcePath(), file_db.getRelativePath()),
											file_db.getRelativePath(), DeleteAction.del_source, false));
					} else if (!sourceFileExists && !remoteFileExists) {
						SwingUtilities.invokeLater(new RunStatusUpdate(
								"Datei verschwunden! Lösche Eintrag: " + file_db.getRelativePath(), true, spd));
						changed = true;
						db.removeFileByPath(file_db.getRelativePath());
					}
					if (spd.isCancelled()) {
						cancelSync("# W" + GC.ae() + "hrend dem Suchen nach zu löschenden Dateien Abgebrochen!", dbEdit,
								dbDestination, changed);
						return null;
					}
				}

				/*
				 * eliminate duplicates
				 */

				i1 = 0;
				while (i1 < copyActions.size()) {
					relativePath = copyActions.get(i1).getRelativePath();
					direction = copyActions.get(i1).getDirection();
					iCopyAction = copyActions.iterator();
					i2 = 0;
					while (iCopyAction.hasNext()) {
						copyAction = iCopyAction.next();
						if (i2 <= i1) {
							i2++;
							continue;
						}
						if (copyAction.getRelativePath().equals(relativePath)
								&& copyAction.getDirection() == direction) {
							iCopyAction.remove();
						}
					}
					i1++;
				}

				try {
					operationSummary = new OperationSummary(operation.getSource(), operation.getTarget(),
							corruptedFilesSource, corruptedFilesDestination, lostFiles, copyActions, deleteActions, spd,
							false);
				} catch (SpiderCancelledException e) {
					cancelSync("# Während dem Summieren abgebrochen", dbEdit, dbDestination, changed);
					return null;
				}

				if ((showSummary && operationSummary.shouldDisplayDialog()) || operationSummary.hasCorruptFiles()) {
					getSummaryApproval = new GetSummaryApproval(spd, operationSummary, isBiDirectional,
							operation.getPriorityOnConflict());
					SwingUtilities.invokeAndWait(getSummaryApproval);
					if (!getSummaryApproval.isApproved()) {
						cancelOp("# Operation Abgebrochen", dbEdit, dbDestination, changed);
						break;
					}
				}

				if (operationSummary.getnDeleteActionsSelected() > 0)
					SwingUtilities.invokeLater(new RunStatusUpdate(
							"# " + operationSummary.getnDeleteActionsSelected() + " Dateien Löschen", false, spd));
				iDeleteActions = deleteActions.iterator();
				while (iDeleteActions.hasNext()) {
					deleteAction = iDeleteActions.next();
					if (deleteAction.isSelected()) {
						changed = true;
						SwingUtilities.invokeLater(new RunStatusUpdate(deleteAction.toString(), false, spd));
						deleteAction.getFile().delete();
						db.removeFileByPath(deleteAction.getRelativePath());
					} else {
						file = deleteAction.getFile();
						file2 = new File(
								deleteAction.getLocation() == DeleteAction.del_destination ? operation.getSource()
										: operation.getTarget(),
								deleteAction.getRelativePath());
						copyActions.add(new CopyAction(file, file2, deleteAction.getRelativePath(), false,
								deleteAction.getLocation() == DeleteAction.del_destination ? CopyAction.DIR_RESTORE
										: CopyAction.DIR_BACKUP));
					}
					if (spd.isCancelled()) {
						cancelSync("# Während des Löschens Abgebrochen!", dbEdit, dbDestination, changed);
						return null;
					}
				}

				enoughSpace = true;

				while (operation.getTarget().getFreeSpace() < operationSummary.getUpdateSizeDestination()
						- operationSummary.getUpdateSizeDestinationModifiedOld()) {
					getRetryOnOutOfMemory = new GetRetryOnOutOfMemory(spd, "Zieldatenträger",
							operationSummary.getUpdateSizeDestination()
									- operationSummary.getUpdateSizeDestinationModifiedOld());
					SwingUtilities.invokeAndWait(getRetryOnOutOfMemory);
					retry = getRetryOnOutOfMemory.isRetry();
					if (!retry) {
						enoughSpace = false;
						break;
					}
				}

				if (!enoughSpace) {
					cancelOp("# Operation Abgebrochen, nicht gen" + GC.ue() + "gend Speicherplatz auf dem Zieldatentr"
							+ GC.ae() + "ger", dbEdit, dbDestination, changed);
					break;
				}

				overwriteFirst = false;

				if (operation.getTarget().getFreeSpace() < operationSummary.getUpdateSizeDestination()) {
					overwriteFirst = true;
				}

				if (isBiDirectional) {
					while (operation.getSource().getFreeSpace() < operationSummary.getUpdateSizeSource()
							- operationSummary.getUpdateSizeSourceModifiedOld()) {
						getRetryOnOutOfMemory = new GetRetryOnOutOfMemory(spd, "Quelldatenträger",
								operationSummary.getUpdateSizeSource()
										- operationSummary.getUpdateSizeSourceModifiedOld());
						SwingUtilities.invokeAndWait(getRetryOnOutOfMemory);
						retry = getRetryOnOutOfMemory.isRetry();
						if (!retry) {
							enoughSpace = false;
							break;
						}
					}
					if (!enoughSpace) {
						cancelOp(
								"# Operation Abgebrochen, nicht gen" + GC.ue()
										+ "gend Speicherplatz auf dem Quelldatentr" + GC.ae() + "ger",
								dbEdit, dbDestination, changed);
						break;
					}
					if (operation.getSource().getFreeSpace() < operationSummary.getUpdateSizeSource()) {
						overwriteFirst = true;
					}
				}

				SwingUtilities.invokeLater(new RunSetDeterminate(true, spd));
				if (operationSummary.getnCopyActionsSelected() > 0)
					SwingUtilities.invokeLater(new RunStatusUpdate(
							"# " + operationSummary.getnCopyActionsSelected() + " Dateien kopieren", false, spd));

				updateSize = operationSummary.getUpdateSizeTotal();
				copied = 0;
				tStart = System.currentTimeMillis();
				tSplit = 0;

				ix = 0;

				counter = 0;

				if (overwriteFirst) {
					while (ix < 2) {
						iCopyAction = copyActions.iterator();
						while (iCopyAction.hasNext()) {
							/*
							 * COPY BLOCK
							 */
							copyAction = iCopyAction.next();
							if (!copyAction.isSelected())
								continue;
							if ((ix == 0 && !copyAction.isNew()) || (ix == 1 && copyAction.isNew())) {
								changed = true;

								copied = copy(copyAction, db, copied, updateSize, tStart);

								counter++;
								if (spd.isCancelled()) {
									if (copied > 0) {
										SwingUtilities
												.invokeLater(new RunStatusUpdate("# " + counter + " Dateien Kopiert "
														+ (copied / 1024 / 1024) + " MByte in " + tSplit + " Sekunden"
														+ (tSplit > 0
																? ", " + ((copied / 1024 / 1024) / tSplit) + " MiB/sec"
																: ""),
														false, spd));
									}
									cancelSync("# W" + GC.ae() + "hrend des Kopierens Abgebrochen!", dbEdit,
											dbDestination, changed);
									return null;
								}
								try {
									Thread.sleep(13);
								} catch (InterruptedException e) {
								}
							}
							/*
							 * COPY BLOCK END
							 */
						}
						ix++;
					}
				} else {
					iCopyAction = copyActions.iterator();
					while (iCopyAction.hasNext()) {
						/*
						 * COPY BLOCK
						 */
						copyAction = iCopyAction.next();
						if (!copyAction.isSelected())
							continue;
						changed = true;

						copied = copy(copyAction, db, copied, updateSize, tStart);

						counter++;
						if (spd.isCancelled()) {
							if (copied > 0) {
								SwingUtilities.invokeLater(new RunStatusUpdate("# " + counter + " Dateien Kopiert "
										+ (copied / 1024 / 1024) + " MByte in " + tSplit + " Sekunden"
										+ (tSplit > 0 ? ", " + ((copied / 1024 / 1024) / tSplit) + " MiB/sec" : ""),
										false, spd));
							}
							cancelSync("# W" + GC.ae() + "hrend des Kopierens Abgebrochen!", dbEdit, dbDestination,
									changed);
							return null;
						}
						try {
							Thread.sleep(13);
						} catch (InterruptedException e) {
						}
					}
					/*
					 * COPY BLOCK END
					 */
				}
				if (copied > 0) {
					SwingUtilities.invokeLater(new RunStatusUpdate(
							"# " + counter + " Dateien Kopiert " + (copied / 1024 / 1024) + " MiB in " + tSplit
									+ " Sekunden"
									+ (tSplit > 0 ? ", " + ((copied / 1024 / 1024) / tSplit) + " MiB/sec" : ""),
							false, spd));
				}

				if (!changed) {
					SwingUtilities.invokeLater(new RunStatusUpdate(
							"# # Keine " + GC.Ae() + "nderungen Gefunden nach "
									+ UIFx.formatMillisAsHoursMinutesSeconds(System.currentTimeMillis() - opStart),
							false, spd));
				} else {
					SwingUtilities.invokeLater(new RunStatusUpdate(
							"# # Alle " + GC.Ae() + "nderungen Angewandt nach "
									+ UIFx.formatMillisAsHoursMinutesSeconds(System.currentTimeMillis() - opStart),
							false, spd));
					db.incrementVersion();
				}
				try {
					if (dbEdit != null && dbDestination != null) {
						if (changed)
							FileUtils.copyFile(dbEdit, dbDestination);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				operation.setLastSynced(System.currentTimeMillis());
				operation.setReminded(false);
			}

			removePropertyChangeListener(this);
			SwingUtilities.invokeLater(new RunFinished(
					"# # # Alles Erledigt nach "
							+ UIFx.formatMillisAsHoursMinutesSeconds(System.currentTimeMillis() - syncStart),
					null, spd));
		} catch (Exception e) {
			SwingUtilities.invokeLater(new RunFinished(
					"# Fehler nach " + UIFx.formatMillisAsHoursMinutesSeconds(System.currentTimeMillis() - syncStart)
							+ " : " + e.getMessage(),
					e, spd));
			try {
				if (dbEdit != null && dbDestination != null) {
					if (changed)
						FileUtils.copyFile(dbEdit, dbDestination);
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		return null;
	}

	private long copy(CopyAction copyAction, OnlineDB db, long copied, long updateSize, long tStart)
			throws IOException {
		// SwingUtilities.invokeLater(new RunStatusUpdate("Kopiere " +
		// copyAction.toString(), true, spd));
		// FileUtils.copyFile(copyAction.getSource(), copyAction.getDestination());
		// copied += copyAction.getSource().length();
		// if (copyAction.isNew()) {
		// db.add(new RelativeFile(copyAction.getRelativePath(),
		// copyAction.getSource().length(),
		// copyAction.getSource().lastModified(),
		// FSSync.getChecksum(copyAction.getSource())));
		// if (!copyAction.getDestination().getParentFile().exists()) {
		// copyAction.getDestination().getParentFile().mkdirs();
		// }
		// } else {
		// db.updateFile(new RelativeFile(copyAction.getRelativePath(),
		// copyAction.getSource().length(),
		// copyAction.getSource().lastModified(),
		// FSSync.getChecksum(copyAction.getSource())));
		// }
		// try {
		// setProgress((int) ((100.0 / updateSize) * copied));
		// } catch (IllegalArgumentException e) {
		// setProgress(100);
		// }
		// tSplit = (System.currentTimeMillis() - tStart) / 1000;
		// if (tSplit > 0) {
		// SwingUtilities.invokeLater(new RunStatusTextUpdate(
		// "Dateien Kopieren... " + ((copied / 1024 / 1024) / tSplit) + " MiB/sec",
		// spd));
		// }
		SwingUtilities.invokeLater(new RunStatusUpdate("Kopiere " + copyAction.toString(), true, spd));
		FileUtils.copyFile(copyAction.getSource(), copyAction.getDestination());
		copied += copyAction.getSource().length();
		if (copyAction.isNew()) {
			db.add(new RelativeFile(copyAction.getRelativePath(), copyAction.getSource().length(),
					copyAction.getSource().lastModified(), FSSync.getChecksum(copyAction.getSource())));
			if (!copyAction.getDestination().getParentFile().exists()) {
				copyAction.getDestination().getParentFile().mkdirs();
			}
		} else {
			db.updateFile(new RelativeFile(copyAction.getRelativePath(), copyAction.getSource().length(),
					copyAction.getSource().lastModified(), FSSync.getChecksum(copyAction.getSource())));
		}
		try {
			setProgress((int) ((100.0 / updateSize) * copied));
		} catch (IllegalArgumentException e) {
			setProgress(100);
		}
		long tSplit = (System.currentTimeMillis() - tStart) / 1000;
		if (tSplit > 0) {
			SwingUtilities.invokeLater(new RunStatusTextUpdate(
					"Dateien Kopieren... " + ((copied / 1024 / 1024) / tSplit) + " MiB/sec", spd));
		}
		return copied;
	}

	private final void cancelSync(String message, File dbTmp, File dbDestination, boolean changed) {
		removePropertyChangeListener(this);
		SwingUtilities.invokeLater(new RunCancelled(message, spd));
		try {
			if (dbTmp != null && dbDestination != null) {
				if (changed)
					FileUtils.copyFile(dbTmp, dbDestination);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private final void cancelOp(String message, File dbTmp, File dbDestination, boolean changed) {
		SwingUtilities.invokeLater(new RunStatusUpdate(message, false, spd));
		try {
			if (dbTmp != null && dbDestination != null) {
				if (changed)
					FileUtils.copyFile(dbTmp, dbDestination);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// process() is not used because the output became disordered
	// @Override
	// protected void process(List<StatusMessage> chunks) {
	// for (StatusMessage msg : chunks) {
	// if (msg.getMessage() != null) {
	// if (msg.isVerbose()) {
	// spd.addStatusVerbose(msg.getMessage());
	// } else {
	// spd.addStatus(msg.getMessage());
	// }
	// }
	// if (msg.getText() != null) {
	// spd.setProcessStatusText(msg.getText());
	// }
	// }
	// }

	@Override
	public final void propertyChange(PropertyChangeEvent evt) {
		spd.setProgress(getProgress());
	}
}
