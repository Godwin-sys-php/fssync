# Attention
**German language ahead... Since the primary/initial target group of users are german speakers with some lacking english skills. So all UI strings are hard coded in german. Internationalization stands on the to do list...**
![alt text][logo]
# FSSync
FSSync is short for "Filesystems Synchronisator". The program is supposed to be a minimalistic data flow configurator to backup and archive files.
Yet it should be possible to map complex file system structures.

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

An operation consists of a source directory and a target (or destination) directory. It can have excluded directories (source) and is configurable by options. 
Also statistics about the running time are stored inside the operation object.

The persistance strategy for the segments and operations and settings is creating pretty formatted Json dumps (Gson) of the Segments and Settings objects. 
These are stored as ´sync.json´ and ´settings.json´ in the program directory. The program directory is created when FSSync starts for the first time. It is called ´.fssync´ and is located in the user directory.
On windows systems the directory is also hidden. Other Files stored in the program directory are the file ´lock.file´ that is used to only allow one instance running and the directory ´docs´ that contains license, help and about documents.
Inside the ´docs´ directory there also is a file called ´version´ that contains the version string of the program release currently installed.

A SQLite database file in the root directory is created to index the directory contents. it is synchronized on creation and after the synchronization work has been done.
The database contains a meta data table and a file system table. The metadata table has one row containing a unique database ID, the number of times data was synchronized and the structural build version of the database.
This database is used to identify new files and files to delete. Furthermore a SHA384 checksum is stored to determine data integrity.
The index represents the state of the file system at the end of the last synchronization.

# Source Files
## Code
### package bug507401
Commented | class name | comment
--- | --- | ---
 | DangerousPathChecker.java | 

### package net.janbuchinger.code.fssync
Commented | class name | comment
--- | --- | ---
:x: | ArrowPanel.java | Panel drawing an arrow for an OperationPanel.
:x: | ArrowRestorePanel.java | Panel drawing an arrow for an RestoreOperationPanel.
:heavy_check_mark: | CheckNewVersionThread.java | Thread checking for new version at program start (if connection is possible).
:heavy_check_mark: | FSSync.java | Starting point class containing the main(String[]) method and general utility methods. UI is launched only if the file lock was obtained.
:heavy_check_mark: | FSSyncPaths.java | Class serving the file system paths relevant for program execution and data persistence.
:heavy_check_mark: | FSSyncUI.java | Main user interface class, here are all menu item selections, tray icon clics and window events handled.
:x: | NewVersionMessageComponent.java | Content of the new version info dialog.
:x: | NumberPanel.java | Panel drawing a circle around a number for OperationPanel and RestoreOperationPanel.
:heavy_check_mark: | Operation.java | The data structure for an Operation.
:x: | OperationArgument.java | Helper class to communicate the analysis mode for the Operation (quick or deep) to the SynchronizationProcess.
:x: | OperationCheckBox.java | Checkbox for OperationPanel and RestoreOperationPanel to select an Operation for synchronization.
:x: | OperationEditorDialog.java | JDialog containing the UI to edit an Operation.
:x: | OperationPanel.java | Panel for the main UI, here are the UIs clicks handled.
:x: | OperationsListModel.java | The ListModel to handle the operations inside the SegmentEditorDialog.
:x: | OperationTrayMenuItem.java | MenuItem for tray menu.
:heavy_check_mark: | ReleaseApplicationLockThread.java | Thread to be executen on application exit releasing the file lock to prevent any further instances to start.
:x: | RestorationSelectionDialog.java | The Dialog that manages all restoration and repair tasks.
:x: | RestoreOperationPanel.java | A version of Operation panel with a different colour and an arrow pointing to left instead of right or both directions.
:heavy_check_mark: | RunAlreadyRunningMessage.java | Runnable for the EDT, shows a parentless dialog warning that the program is already running and exits after ok was pushed.
:x: | RunFSSyncUI.java | Runnable to start the main UI on the EDT.
:x: | RunNotifyNewVersion.java | Runnable that is initialized off the EDT in CheckNewVersion, it tells the main frame to display the new version detected message.
:x: | RunRefreshUI.java | Runnable for UIChangeWatcherThread to communicate to the main frame to refresh.
:x: | RunRemindTray.java | Runnable for TrayReminderThread to communicate to the tray icon to display the reminder for synchronization.
:x: | RunSegmentMenuItem.java | Menu item to run a Segment.
:heavy_check_mark: | Segment.java | The Segment class manages 0 or more Operations and has a name.
:x: | SegmentEditorDialog.java | The dialog to edit a Segment.
:x: | SegmentMenuItem.java | Menu item to start the SegmentEditorDialog.
:heavy_check_mark: | Segments.java | The main data structure represented by ´sync.json´. All segments and operations are held here.
:x: | SegmentTrayMenuItem.java | Menu item to synchronize a segment via tray menu.
:x: | Settings.java | The Settings data structure represented by ´settings.json´.
:x: | SettingsDialog.java | The dialog to edit the Settings.
:heavy_check_mark: | TrayReminderThread.java | Thread to check for any due operations to remind about.
:heavy_check_mark: | UIChangeWatcherThread.java | Thread to check if any operation paths have come online or are gone offline. On changes RunRefreshUI is executed.

### package net.janbuchinger.code.fssync.sync
Commented | class name | comment
--- | --- | ---
 | CopyAction.java | 
 | DeleteAction.java | 
 | EditDBsFilenameFilter.java | 
 | LocalFileVisitor.java | 
 | OnlineDB.java | 
 | OperationSummary.java | 
 | RecoverSystemDialog.java | 
 | RecoverSystemProcess.java | 
 | RecoverSystemVisitor.java | 
 | RelativeFile.java | 
 | RemoteFileVisitor.java | 
 | RestorationProcess.java | 
 | SynchronizationCancelledException.java | 
 | SynchronizationProcess.java | 

### package net.janbuchinger.code.fssync.sync.ui
Commented | class name | comment
--- | --- | ---
 | CopyActionTableCellRenderer.java | 
 | CopyActionTableModel.java | 
 | DeleteActionTableCellRenderer.java | 
 | DeleteActionTableModel.java | 
 | DisplayFile.java | 
 | FileTableModel.java | 
 | GetContinueRestore.java | 
 | GetForeignFileHandling.java | 
 | GetRestorationMode.java | 
 | GetRestoreSourceChoice.java | 
 | GetRetryOnOutOfMemory.java | 
 | GetSummaryApproval.java | 
 | OperationSummaryDialog.java | 
 | OverviewPanel.java | 
 | ProgressBarCountDownThread.java | 
 | RestorationModePanel.java | 
 | RunAbortCountDown.java | 
 | RunCancelled.java | 
 | RunFinished.java | 
 | RunPauseCountDown.java | 
 | RunSetDeterminate.java | 
 | RunStartCountDown.java | 
 | RunStatusMessageUpdate.java | 
 | RunStatusTextUpdate.java | 
 | StatusMessage.java | 
 | SynchronizationProcessDialog.java | 

## Resources
### package net.janbuchinger.code.fssync.res
Resouce name | comment
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
