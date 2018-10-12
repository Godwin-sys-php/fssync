
![alt text][logo]
# FSSync
FSSync is short for "Filesystems Synchronisator". The program is supposed to be a minimalistic data flow configurator to backup and archive files.
Yet it should be possible to map complex file system structures.

## Attention
**German language ahead... Since the primary/initial target group of users are german speakers with some lacking english skills. So all UI strings are hard coded in german at the moment. Internationalization stands on the to do list...**

## Getting Started
Just fetch the git repository and import the project path (.../fssync/fssync) as existing gradle project into eclipse. I hope that should do it.

### JDK Version
The target Java version is currently 1.8.

## Testing
There are no tests created yet.

## Deployment
When building the project an executable library file (.../fssync/fssync/build/libs/fssync-*version*.jar) is created containing all dependencies. This file can be used as stand alone runnable.
Also zip & tar archives are created (fssync/fssync/build/distributions/fssync.*version*.zip **or** .tar) these archives contain the runnable fssync.*version*.jar and its dependencies outside the archive.
Additionally batch and shell scripts are available to initialize the application.

There is no installer available yet.

## Concept and Names
The general concept is to organize synchronization operations in segments.

A segment contains 0 or more operations.

An operation consists of a source (also local) directory and a target (also destination or remote) directory. It can have excluded directories (source) and is configurable by options. 
Also statistics about the running time are stored inside the operation object.

The persistance strategy for the segments and operations and settings is creating pretty formatted Json dumps (Gson) of the Segments and Settings objects. 
These are stored as `sync.json` and `settings.json` in the program directory. The program directory is created when FSSync starts for the first time. It is called `.fssync` and is located in the user directory.
On windows systems the directory is also hidden. Other Files stored in the program directory are the file `lock.file` that is used to only allow one instance running and the directory `docs` that contains license, help and about documents.
Inside the `docs` directory there also is a file called `version` that contains the version string of the program release currently installed.

A SQLite database file in the root directory is created to index the directory contents. it is synchronized on creation and after the synchronization work has been done.
The database contains a meta data table and a file system table. The metadata table has one row containing a unique database ID, the number of times data was synchronized and the structural build version of the database.
This database is used to identify new files and files to delete. Furthermore a SHA384 checksum is stored to determine data integrity.
The index represents the state of the file system at the end of the last synchronization.

# Source Files
## Code
### package bug507401
Package containing a downloaded class that checks if a path points to a network drive under windows.

Commented | Class Name | Comment
--- | --- | ---
partially | DangerousPathChecker.java | Supplies static utility function isDangerous(File) to check if a path (in windows) points to a network resource.

### package net.janbuchinger.code.fssync
Package for all general program and UI classes.

Commented | Class Name | Comment
--- | --- | ---
:x: | ArrowPanel.java | Panel drawing an arrow for an `OperationPanel`.
:x: | ArrowRestorePanel.java | Panel drawing an arrow for an `RestoreOperationPanel`.
:heavy_check_mark: | CheckNewVersionThread.java | Thread checking for new version at program start (if connection is possible).
:heavy_check_mark: | FSSync.java | Starting point class containing the `main(String[])` method and general utility methods. UI is launched only if the file lock was obtained.
:heavy_check_mark: | FSSyncPaths.java | Class serving the file system paths relevant for program execution and data persistence.
:heavy_check_mark: | FSSyncUI.java | Main user interface class, here are all menu item selections, tray icon clics and window events handled.
:x: | NewVersionMessageComponent.java | Content of the new version info dialog.
:x: | NumberPanel.java | Panel drawing a circle around a number for `OperationPanel` and RestoreOperationPanel.
:heavy_check_mark: | Operation.java | The data structure for an `Operation`.
:x: | OperationArgument.java | Helper class to communicate the analysis mode for the `Operation` (quick or deep) to the `SynchronizationProcess`.
:x: | OperationCheckBox.java | Checkbox for `OperationPanel` and `RestoreOperationPanel` to select an `Operation` for synchronization.
:x: | OperationEditorDialog.java | `JDialog` containing the UI to edit an `Operation`.
:x: | OperationPanel.java | Panel for the main UI, here are the UIs clicks handled.
:x: | OperationsListModel.java | The `ListModel` to handle the operations inside the `SegmentEditorDialog`.
:x: | OperationTrayMenuItem.java | `MenuItem` for tray menu.
:heavy_check_mark: | ReleaseApplicationLockThread.java | Thread to be executen on application exit releasing the file lock to prevent any further instances to start.
:x: | RestorationSelectionDialog.java | The Dialog that manages all restoration and repair tasks.
:x: | RestoreOperationPanel.java | A version of `OperationPanel` with a different colour and an arrow pointing to left instead of right or both directions.
:heavy_check_mark: | RunAlreadyRunningMessage.java | `Runnable` for the EDT, shows a parentless dialog warning that the program is already running and exits after ok was pushed.
:x: | RunFSSyncUI.java | `Runnable` to start the main UI on the EDT.
:x: | RunNotifyNewVersion.java | `Runnable` that is initialized off the EDT in `CheckNewVersionThread`, it tells the main frame to display the new version detected message.
:x: | RunRefreshUI.java | `Runnable` for `UIChangeWatcherThread` to communicate to the main frame to refresh.
:x: | RunRemindTray.java | `Runnable` for `TrayReminderThread` to communicate to the tray icon to display the reminder for synchronization.
:x: | RunSegmentMenuItem.java | Menu item to run a `Segment`.
:heavy_check_mark: | Segment.java | The `Segment` class manages 0 or more `Operation`s and has a name.
:x: | SegmentEditorDialog.java | The dialog to edit a `Segment`.
:x: | SegmentMenuItem.java | Menu item to start the `SegmentEditorDialog`.
:heavy_check_mark: | Segments.java | The main data structure represented by `sync.json`. All segments and operations are stored here.
:x: | SegmentTrayMenuItem.java | Menu item to synchronize a `Segment` via tray menu.
:x: | Settings.java | The `Settings` data structure represented by `settings.json`.
:x: | SettingsDialog.java | The dialog to edit the `Settings`.
:heavy_check_mark: | TrayReminderThread.java | Thread to check for any due operations to remind about.
:heavy_check_mark: | UIChangeWatcherThread.java | Thread to check if any operation paths have come online or are gone offline. On changes `RunRefreshUI` is executed.

### package net.janbuchinger.code.fssync.sync
Package for all classes (executed off the EDT) around the SynchronizationProcess, RestorationProcess and RecoverSystemProcess.

Commented | Class Name | Comment
--- | --- | ---
:x: | CopyAction.java | A copy action containing a files source, target and relative path and the direction of the copy action.
:x: | DeleteAction.java | A delete action containing the path of the file to delete and the location of the file to delete.
:x: | EditDBsFilenameFilter.java | A `FilenameFilter` to fetch all editable file system index databases in a directory.
:x: | LocalFileVisitor.java | The `FileVisitor` to list all files in the source directory.
:heavy_check_mark: | OnlineDB.java | The database class.
:x: | OperationSummary.java | A data structure to summarize all copy and delete actions of an `Operation`.
:x: | ProgressBarCountDownThread.java | A Thread that counts down from a specified time and updates the progress bar in the `SynchronizationProcessDialog`.
:x: | RecoverSystemProcess.java | Thread to build a new database by searching for already existing file pairs.
:x: | RecoverSystemVisitor.java | `FileVisitor` to list all files in the target directory to recover already existing file pairs.
:x: | RelativeFile.java | A file as stored in the database.
:x: | RemoteFileVisitor.java | `FileVisitor` to list the contents of the target file system.
:heavy_check_mark: | RestorationProcess.java | The reverse synchronization process for operation restoration.
:x: | SynchronizationCancelledException.java | `Exception` to signal that the cancel button was pressed.
:heavy_check_mark: | SynchronizationProcess.java | The `SynchronizationProcess`.

### package net.janbuchinger.code.fssync.sync.ui
Package for all classes related to the SynchronizationProcess, RestorationProcess and RecoverSystemProcess classes that are executed on the EDT.

Commented | Class Name | Comment
--- | --- | ---
:x: | CopyActionTableCellRenderer.java | `TableCellRenderer` to display a `CopyAction` in the `OperationSummaryDialog`.
:heavy_check_mark: | CopyActionTableModel.java | The `TableModel` for the `CopyAction` `JTable` in the `OperationSummaryDialog`.
:x: | DeleteActionTableCellRenderer.java | The `TableCellRenderer` to display a `DeleteAction` in the `OperationSummaryDialog`.
:x: | DeleteActionTableModel.java | The `TableModel` for the `DeleteAction` `JTable` in the `OperationSummaryDialog`.
:x: | DisplayFile.java | A File that returns `File.getPath()` in the custom `toString()` method.
:x: | FileTableModel.java | **deprecated** A `TableModel` to display a generic list of files.
:x: | GetContinueRestore.java | **deprecated** `Runnable` to obtain a deceision of the user whether to continue when lost files are found.
:x: | GetForeignFileHandling.java | `Runnable` to get the users deceision how to handle unexpected changes in the target file system.
:x: | GetRestorationMode.java | `Runnable` to get the users deceision which restoration mode for the restoration batch to use.
:x: | GetRestoreSourceChoice.java | `Runnable` to get the user deceision which source to use when more than one sources are available to restore.
:x: | GetRetryOnOutOfMemory.java | `Runnable` to get the users deceision whether to retry when out of disk space.
:x: | GetSummaryApproval.java | `Runnable` to display the `OperationSummaryDialog`.
:x: | OperationSummaryDialog.java | The operation summary dialog to let the user review the changes that will be synchronized.
:x: | OverviewPanel.java | The overview panel inside the `OperationSummaryDialog` summarizing the changes in numbers.
:x: | RecoverSystemDialog.java | Dialog containing only a progress bar.
:x: | RestorationModePanel.java | Message panel for the restoration mode selection dialog.
:x: | RunAbortCountDown.java | `Runnable` to cancel the current progress bar countdown in `SynchronizationProcessDialog` and set the progress bar indeterminate.
:x: | RunCancelled.java | `Runnable` to set the `SynchronizationProcessDialog` cancelled and closeable.
:x: | RunFinished.java | `Runnable` to set the `SynchronizationProcessDialog` finished and closeable.
:x: | RunPauseCountDown.java | `Runnable` to pause or resume the current progress bar countdown.
:x: | RunSetDeterminate.java | `Runnable` to set the progress bar in the `SynchronizationProcessDialog` in/determinate.
:x: | RunStartCountDown.java | `Runnable` to start a new progress bar count down.
:x: | RunStatusMessageUpdate.java | `Runnable` to pass the current list of status messages to the `SynchronizationProcessDialog`.
:x: | RunStatusTextUpdate.java | `Runnable` to update the status label text.
:x: | StatusMessage.java | The data object representing a status message.
:x: | SynchronizationProcessDialog.java | The progress and status dialog for `SynchronizationProcess` and `RestorationProcess`.

## Resources
### package net.janbuchinger.code.fssync.res
Resouce Name | Comment
--- | ---
about.html | The content for the about dialog.
Apache2.0.txt | The license file to be opened with the current systems txt editor.
CHANGELOG | The change log (for dialog).
createMissingSourceDialog.png | Image showing the dialog for searching missing source directories.
fssyncLogo.png | The program logo for the about dialog.
gui.png | Image showing the main UI.
help.html | The content for the help dialog.
LICENSE | The license (for dialog)
NOTICE_Apache_Commons_Codec.txt | Notice of the commons-codec library to be shown in the current systems txt editor.
operation.png | Image showing the basic operation configuration.
operationExceptions.png | Image showing the exceptions tab of the operation editor.
operationOptions.png | Image showing the options tab of the operations editor.
operationStats.png | Image showing the operation stats tab of the operation editor.
operationTiming.png | Image showing the timing and reminding tab of the operation editor.
requestForeignFileHandling.png | Image showing the dialog to handle unexpected changes in the target file system.
requestRestoreMode.png | Image showing the restoration mode selection dialog.
requestSourceForRestore.png | Image showing the selection dialog if there are multiple sources to restore one target.
restoreDialogAvailable.png | Image showing the Restoration handling dialog with all available operations that are ready to restore selected.
restoreDialogEmpty.png | Image showing the Restoration dialog with no outstanding operations available.
restoreDialogOutstanding.png | Image showing the Restoration dialog with outstanding operations available.
segment.png | Image showing the segment editor dialog.
settings.png | Image showing the settings dialog.
TODO | To do list (for dialog)

# Acknowledgments
* Apache Foundation
  * **commons-io** is used to read, write and copy files. (Apache 2.0)
  * **commons-codec** is used to create checksums. (Apache 2.0)
* Google
  * **Gson** is used to create JSON dumps of classes. (Apache 2.0)
* org.xerial
  * **sqlite-jdbc** is used to manage the file system index databases. (Apache 2.0)
* Aaron Digulla: DangerousPathChecker.java (public domain)
* And many, many tanks for the many, many hints from the stackoverflow.com community!

[logo]: fssync/src/main/resources/net/janbuchinger/code/fssync/res/fssyncLogo.png "FSSync Logo"
