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
package net.janbuchinger.code.fssync;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;

import org.apache.commons.io.FileUtils;

import net.janbuchinger.code.fssync.sync.RestorationProcess;
import net.janbuchinger.code.fssync.sync.SynchronizationProcess;
import net.janbuchinger.code.fssync.sync.ui.SynchronizationProcessDialog;
import net.janbuchinger.code.mishmash.FSFx;
import net.janbuchinger.code.mishmash.ui.UIFx;
import net.janbuchinger.code.mishmash.ui.dialog.InfoDialog;

/**
 * Main UI class of FSSync. It builds the UI frame and tray icon, handles user
 * input events and configures the synchronization work flow.
 * 
 * @author Jan Buchinger
 *
 */
public final class FSSyncUI
		implements WindowListener, ActionListener, MouseListener, UncaughtExceptionHandler {

	/**
	 * The main frame
	 */
	private final JFrame frm;
	/**
	 * Content pane
	 */
	private final JPanel pnOperationsOverview;
	/**
	 * Add segment menu item
	 */
	private final JMenuItem miAddSegment;
	/**
	 * Settings menu item
	 */
	private final JMenuItem miSettings;
	/**
	 * menu containing "add segment", "edit segment" and "edit settings"
	 */
	private final JMenu muEdit;
	/**
	 * Run All menu items
	 */
	private final JMenuItem miRunAll;
	private final JMenuItem miRunAllQuick;
	private final JMenuItem miRunAllDeep;
	/**
	 * Run Selected menu items
	 */
	private final JMenuItem miRunSelected;
	private final JMenuItem miRunSelectedQuick;
	private final JMenuItem miRunSelectedDeep;
	/**
	 * Run Due menu items
	 */
	private final JMenuItem miRunDue;
	private final JMenuItem miRunDueQuick;
	private final JMenuItem miRunDueDeep;
	/**
	 * menu containing the menu items miRun*Quick and miRun*Deep
	 */
	private final JMenu muRunOptions;
	/**
	 * Open restoration dialog
	 */
	private final JMenuItem miRestore;
	/**
	 * menu containing items to run a segment or selection
	 */
	private final JMenu muRun;
	/**
	 * ? menu items
	 */
	private final JMenuItem miAbout;
	private final JMenuItem miHelp;
	private final JMenuItem miLicense;
	private final JMenuItem miToDo;
	private final JMenuItem miChangeLog;
	/**
	 * Button to add the first segment, shown only if Segments are empty
	 */
	private final JButton btNewSegment;

	/**
	 * Menu for tray icon
	 */
	private final PopupMenu trayPopup;
	/**
	 * Tray icon menu item to exit the program
	 */
	private final MenuItem tiExit;
	/**
	 * Tray icon menu item to run all segments
	 */
	private final MenuItem tiRunAll;

	/**
	 * tray icon
	 */
	private final TrayIcon trayIcon;
	/**
	 * System tray
	 */
	private final SystemTray tray;

	/**
	 * reusable settings dialog
	 */
	private final SettingsDialog settingsDialog;

	/**
	 * The <code>Segments</code> singleton that stores all segments and operations
	 * in a JSON file.
	 */
	private final Segments segments;

	/**
	 * The <code>Settings</code> singleton that provides all the general settings
	 */
	private final Settings settings;

	/**
	 * URLs to the program documents
	 */
	private final URL helpURL;
	private final URL aboutURL;
	private final URL changeLogURL;
	private final URL toDoURL;
	private final URL licenseURL;

	/**
	 * The currently running <code>UIChangeWatcherThread</code> or null.
	 */
	private UIChangeWatcherThread uiChangeWatcher;

	/**
	 * The currently running <code>TrayReminderThread</code> or null.
	 */
	private TrayReminderThread trayReminder;

	/**
	 * indicator if an operation or segment is ran from tray, false by default.
	 * <p>
	 * set true it disables left click on the tray icon and the tray menu items.
	 */
	private boolean showFromTray;

	/**
	 * Date time format for logfiles.
	 */
	private final SimpleDateFormat sdfLog;

	/**
	 * Default uncaught exception handler.
	 */
	private final UncaughtExceptionHandler defaultUncaughtExceptionHandler;

	/**
	 * Constructs the <code>FSSyncUI</code> (main UI class). Used only by
	 * <code>RunFSSyncUI</code> class.
	 */
	public FSSyncUI() {
		// get the current default exception handler
		defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
		// Set this class as default uncaught exception handler
		Thread.setDefaultUncaughtExceptionHandler(this);
		// initialize the date time format for the log file name
		sdfLog = new SimpleDateFormat("yyyy-MM-dd-HHmmss");

		/*
		 * Current version
		 */
		String version = "0.8a";

		/*
		 * initialize the main data structures
		 */
		// the program settings
		settings = Settings.getSettings();

		// the segments containing the synchronization operations
		segments = Segments.getSegments(false);

		/*
		 * check the file system situation
		 */

		// get the documents directory (inside program directory)
		File docsDir = FSSyncPaths.getDocsDir();

		// get the version file containing the currently installed version
		File versionFile = FSSyncPaths.getVersionFile();
		// an empty array docsNames indicates no file updates
		String[] docsNames = new String[0];
		// set true when a version upgrade occurred and on installation
		boolean showChangelog = false;

		// push program documents during development
//		try {
//			FileUtils.writeStringToFile(versionFile, "0.7a", Charset.defaultCharset());
//		} catch (IOException e2) {
//			e2.printStackTrace();
//		}

		// set true if the version string is not recognized
		boolean isOldVersion = false;

		/*
		 * check installation version
		 */

		// if the version file does not exist
		if (!versionFile.exists()) {
			showChangelog = true;
			// push all document files
			docsNames = new String[] { "res/about.html", "res/help.html", "res/requestForeignFileHandling.png",
					"res/requestSourceForRestore.png", "res/requestRestoreMode.png", "res/settings.png",
					"res/gui.png", "res/fssyncLogo.png", "res/Apache2.0.txt",
					"res/NOTICE_Apache_Commons_Codec.txt", "res/CHANGELOG", "res/TODO", "res/LICENSE",
					"res/operation.png", "res/operationExceptions.png", "res/operationOptions.png",
					"res/operationTiming.png", "res/segment.png", "res/createMissingSourceDialog.png",
					"res/restoreDialogAvailable.png", "res/restoreDialogEmpty.png",
					"res/restoreDialogOutstanding.png", "res/operationStats.png" };
			try {
				FileUtils.writeStringToFile(versionFile, version, Charset.defaultCharset());
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else { // there is a version file
			try {
				// try reading the file to string
				String versionFileString = FileUtils.readFileToString(versionFile, Charset.defaultCharset())
						.trim();
				// version 0.7a is recognized
				if (versionFileString.equals("0.7a")) {
					showChangelog = true;
					// document files for upgrading 0.7a to 0.8a
					docsNames = new String[] { "res/about.html", "res/help.html", "res/fssyncLogo.png",
							"res/Apache2.0.txt", "res/NOTICE_Apache_Commons_Codec.txt", "res/CHANGELOG",
							"res/TODO", "res/LICENSE", "res/operation.png", "res/operationExceptions.png",
							"res/operationOptions.png", "res/operationTiming.png", "res/segment.png",
							"res/createMissingSourceDialog.png", "res/restoreDialogAvailable.png",
							"res/restoreDialogEmpty.png", "res/restoreDialogOutstanding.png",
							"res/requestSourceForRestore.png", "res/requestRestoreMode.png",
							"res/operationStats.png", "res/gui.png", "res/settings.png",
							"res/requestForeignFileHandling.png" };
					// delete old files
					File oldFileToDelete = new File(docsDir, "disk-128.png");
					if (oldFileToDelete.exists()) {
						oldFileToDelete.delete();
					}
					oldFileToDelete = new File(docsDir, "requestContinueRestore.png");
					if (oldFileToDelete.exists()) {
						oldFileToDelete.delete();
					}
					// try to write current version to version file
					try {
						FileUtils.writeStringToFile(versionFile, version, Charset.defaultCharset());
					} catch (IOException e) {
						e.printStackTrace();
					}
					// version 0.8a is recognized (current version)
				} else if (versionFileString.equals("0.8a")) {
					// // next version: upgrade to 0.9a?
					// showChangelog = true;
					// // enable new version dialog for next version
					// settings.setIgnoreNewVersion(false);
					// settings.write();
					// docsNames = new String[] { "res/about.html" };
					// // try to write current version to version file
					// try {
					// FileUtils.writeStringToFile(versionFile, version, Charset.defaultCharset());
					// } catch (IOException e) {
					// e.printStackTrace();
					// }

					// the version was not recognized
				} else if (versionFileString.length() > 0) {
					isOldVersion = true;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// push document files
		for (int i = 0; i < docsNames.length; i++) {
			FSFx.copyResourceFile(getClass(), docsNames[i],
					new File(docsDir, new File(docsNames[i]).getName()));
		}
		// try to initialize document URLs for info dialogs
		URL aboutURL = null;
		URL helpURL = null;
		URL changeLogURL = null;
		URL toDoURL = null;
		URL licenseURL = null;
		try {
			aboutURL = new File(docsDir, "about.html").toURI().toURL();
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		try {
			helpURL = new File(docsDir, "help.html").toURI().toURL();
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		try {
			toDoURL = new File(docsDir, "TODO").toURI().toURL();
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		try {
			changeLogURL = new File(docsDir, "CHANGELOG").toURI().toURL();
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		try {
			licenseURL = new File(docsDir, "LICENSE").toURI().toURL();
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		// set the resulting URLs to the final class fields
		this.aboutURL = aboutURL;
		this.helpURL = helpURL;
		this.changeLogURL = changeLogURL;
		this.toDoURL = toDoURL;
		this.licenseURL = licenseURL;

		/*
		 * Initialize UI components
		 */

		// initialize the main frame
		frm = new JFrame("FSSync " + version);
		// disable JFrame closing behavior
		frm.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		// let this class's window listener take over..
		frm.addWindowListener(this);

		// try initializing the UI icon
		BufferedImage icon = null;
		try {
			icon = ImageIO.read(getClass().getResource("res/fssyncLogo.png"));
			// set icon
			frm.setIconImage(icon);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// initialize settings dialog
		settingsDialog = new SettingsDialog(frm);

		/*
		 * initialize edit menu items
		 */
		miAddSegment = new JMenuItem("Segment hinzufügen...");
		miAddSegment.addActionListener(this);

		miSettings = new JMenuItem("Einstellungen...");
		miSettings.addActionListener(this);

		/*
		 * initialize run menu items
		 */
		miRunAll = new JMenuItem("Alle");
		miRunAll.addActionListener(this);

		miRunAllQuick = new JMenuItem("Alle (Schnell)");
		miRunAllQuick.addActionListener(this);

		miRunAllDeep = new JMenuItem("Alle (Mit Integritätsprüfung)");
		miRunAllDeep.addActionListener(this);

		miRunSelected = new JMenuItem("Ausgewählte");
		miRunSelected.addActionListener(this);

		miRunSelectedQuick = new JMenuItem("Ausgewählte (Schnell)");
		miRunSelectedQuick.addActionListener(this);

		miRunSelectedDeep = new JMenuItem("Ausgewählte (Mit Integritätsprüfung)");
		miRunSelectedDeep.addActionListener(this);

		miRunDue = new JMenuItem("Fällige");
		miRunDue.addActionListener(this);

		miRunDueQuick = new JMenuItem("Fällige (Schnell)");
		miRunDueQuick.addActionListener(this);

		miRunDueDeep = new JMenuItem("Fällige (Genau)");
		miRunDueDeep.addActionListener(this);

		miRestore = new JMenuItem("Wiederherstellen...");
		miRestore.addActionListener(this);

		/*
		 * initialize about menu items
		 */
		miAbout = new JMenuItem("About (Englisch)...");
		miAbout.addActionListener(this);

		miHelp = new JMenuItem("Hilfe...");
		miHelp.addActionListener(this);

		miChangeLog = new JMenuItem("Change Log (Englisch)...");
		miChangeLog.addActionListener(this);

		miLicense = new JMenuItem("License (Englisch)...");
		miLicense.addActionListener(this);

		miToDo = new JMenuItem("To Do (Englisch)...");
		miToDo.addActionListener(this);

		/*
		 * initialize menu edit (built dynamically in rebuildUserInterface())
		 */
		muEdit = new JMenu("Bearbeiten");

		/*
		 * initialize menu run (built dynamically in rebuildUserInterface())
		 */
		muRun = new JMenu("Ausführen");

		/*
		 * initialize and build run options sub menu
		 */
		muRunOptions = new JMenu("Optionen");
		muRunOptions.add(miRunAllQuick);
		muRunOptions.add(miRunAllDeep);
		muRunOptions.add(miRunSelectedQuick);
		muRunOptions.add(miRunSelectedDeep);
		muRunOptions.add(miRunDueQuick);
		muRunOptions.add(miRunDueDeep);

		/*
		 * initialize and build documents menu
		 */
		JMenu muQuestion = new JMenu("?");
		muQuestion.add(miHelp);
		muQuestion.add(miAbout);
		muQuestion.add(miChangeLog);
		muQuestion.add(miLicense);
		muQuestion.add(miToDo);

		/*
		 * initialize and build menu bar
		 */
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(muEdit);
		menuBar.add(muRun);
		menuBar.add(muQuestion);

		// set menu bar
		frm.setJMenuBar(menuBar);

		// initialize add first segment button
		btNewSegment = new JButton("Erstes Segment Anlegen...");
		btNewSegment.addActionListener(this);

		// initialize the dynamically built operations overview panel that represents
		// the frames content
		pnOperationsOverview = new JPanel(new GridBagLayout());
		// set an empty border to operations overview panel
		pnOperationsOverview.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		// content pane
		JPanel pnContent = new JPanel(new BorderLayout());
		// add operations overview panel to a scroll pane that is added to the content
		// pane
		pnContent.add(UIFx.initScrollPane(pnOperationsOverview, 15), BorderLayout.CENTER);
		// set frame content pane
		frm.setContentPane(pnContent);

		/*
		 * check system tray, initialize tray icon and menu if available
		 */
		if (SystemTray.isSupported()) {
			/*
			 * temporary variables
			 */
			TrayIcon trayIconTmp = null;
			MenuItem tiExitTmp = null;
			MenuItem tiRunAllTmp = null;
			PopupMenu trayPopupTmp = null;
			SystemTray trayTmp = null;

			// try loading the program logo and setting it as tray icon
			try {
				Image imgIcon = ImageIO.read(getClass().getResource("res/fssyncLogo.png"));
				int traySquare = new TrayIcon(imgIcon).getSize().width;
				trayIconTmp = new TrayIcon(
						imgIcon.getScaledInstance(traySquare, traySquare, Image.SCALE_SMOOTH));
				// this class is listening to click events onto the tray icon
				trayIconTmp.addMouseListener(this);
				/*
				 * initialize the general tray menu items
				 */
				tiExitTmp = new MenuItem("Beenden");
				tiExitTmp.addActionListener(this);
				tiRunAllTmp = new MenuItem("Alle Ausführen");
				tiRunAllTmp.addActionListener(this);
				// initialize and set the tray icons pop up menu
				trayPopupTmp = new PopupMenu();
				trayIconTmp.setPopupMenu(trayPopupTmp);
				// get the system tray
				trayTmp = SystemTray.getSystemTray();
			} catch (IOException e) {
				e.printStackTrace();
			}
			trayIcon = trayIconTmp;
			tiExit = tiExitTmp;
			tiRunAll = tiRunAllTmp;
			trayPopup = trayPopupTmp;
			tray = trayTmp;
		} else { // system tray is not supported
			tray = null;
			trayIcon = null;
			trayPopup = null;
			tiExit = null;
			tiRunAll = null;
		}

		// build the UI
		rebuildUserInterface();
		UIFx.center(frm);
		/*
		 * show either tray icon or frame
		 */
		if (settings.isStartToTray() && SystemTray.isSupported()) {
			try {
				tray.add(trayIcon);
				startTrayReminder();
			} catch (AWTException e) {
				frm.setVisible(true);
				startUIChangeWatcher();
			}
		} else {
			frm.setVisible(true);
			startUIChangeWatcher();
		}

		// if (settings.getFileBrowser().equals("")) {
		// int answer = JOptionPane.showConfirmDialog(frm,
		// "Soll nach einem Dateiexplorer gesucht werden? (Dabei öffnet sich vermutlich
		// ein Fenster)",
		// "Dateiexplorer fehlt", JOptionPane.YES_NO_OPTION,
		// JOptionPane.QUESTION_MESSAGE);
		// if (answer == JOptionPane.YES_OPTION) {
		// String cmd = settings.findFileBrowser();
		// if (cmd.length() == 0) {
		// JOptionPane.showMessageDialog(frm, "Es konnte kein Browser erkannt werden.",
		// "Kein Ergebnis", JOptionPane.ERROR_MESSAGE);
		// } else {
		// settings.setFileBrowser(cmd);
		// settings.write();
		// settingsDialog.updateFileBrowser(cmd);
		// }
		// }
		// }

		/*
		 * After the UI is shown it is possible to display dialogs
		 */
		// show change log after upgrade
		if (showChangelog) {
			// open frame in tray icon mode
			if (!frm.isVisible()) {
				openFromTray();
			}
			// show change log dialog
			InfoDialog id = new InfoDialog(frm, changeLogURL, "CHANGELOG");
			id.setVisible(true);
		}

		// display warning message if the version was not recognized
		if (isOldVersion) {
			// open frame in tray icon mode
			if (!frm.isVisible()) {
				openFromTray();
			}
			// warn user that the program executable is old
			JOptionPane.showMessageDialog(frm,
					"Achtung, Sie verwenden vermutlich eine alte Version des Programmes!!\n"
							+ "Bitte beenden Sie das Programm und führen Sie die aktuelle Datei aus!\n"
							+ "Die weitere Nutzung des Programmes könnte sonst zu unvorhergesehenen Problemen führen!",
					"Warnung", JOptionPane.WARNING_MESSAGE);
		}

		// if the dialog to inform about a new version is not suppressed
		if (!settings.isIgnoreNewVersion() && !isOldVersion) {
			// then run the check for new version thread
			CheckNewVersionThread rcfnv = new CheckNewVersionThread(version, this);
			Thread tCheckVersion = new Thread(rcfnv);
			tCheckVersion.start();
		}
	}

	/**
	 * Shows the main frame and removes the tray icon
	 */
	private void openFromTray() {
		// refresh the UI
		refresh();
		// start the UI change watcher thread
		startUIChangeWatcher();
		// show the main frame
		frm.setVisible(true);
		// stop the tray reminder thread
		stopTrayReminder();
		// remove the tray icon from the system tray
		tray.remove(trayIcon);
	}

	/**
	 * (re)build the UI and tray menu (only if system tray is supported)
	 */
	private final void rebuildUserInterface() {
		// first build tray icon menu
		if (SystemTray.isSupported()) {
			// temporary menu item containing a segment
			SegmentTrayMenuItem stmi;
			// temporary menu item containing an operation
			OperationTrayMenuItem otmi;
			// remove all tray menu items to build the menu again
			trayPopup.removeAll();
			// loop through all segments
			for (Segment s : segments.getData()) {
				// for each segment create a menu item
				stmi = new SegmentTrayMenuItem(s);
				stmi.addActionListener(this);
				// add segment menu item
				trayPopup.add(stmi);
				// loop through all operations
				for (Operation o : s.getOperations()) {
					// for each operation create a menu item
					otmi = new OperationTrayMenuItem(o);
					otmi.addActionListener(this);
					// add the operation menu item
					trayPopup.add(otmi);
				}
				// add separator between segments
				trayPopup.addSeparator();
			}
			// add general menu items
			trayPopup.add(tiRunAll);
			trayPopup.addSeparator();
			trayPopup.add(tiExit);
		}
		// clear the main UI panel
		pnOperationsOverview.removeAll();
		// clear the run menu
		muRun.removeAll();
		// add run menu items only if there is anything to be ran
		if (segments.size() > 0) {
			muRun.add(miRunAll);
			muRun.add(miRunSelected);
			muRun.add(miRunDue);
			muRun.add(muRunOptions);
			muRun.addSeparator();
		}
		// clear edit menu
		muEdit.removeAll();
		// add add segment menu item
		muEdit.add(miAddSegment);
		muEdit.addSeparator();

		// the column count for the segments to be laid out
		int cols = settings.getColumns();

		// current segment panel
		JPanel pnSegment;
		// current edit segment menu item
		SegmentMenuItem miSegment;
		// current run segment menu item
		RunSegmentMenuItem miRunSegment;
		// counter for segment position/id
		int ccSegId = 0;
		// current operation panel
		OperationPanel opPan;
		// counter for operation order
		int cc = 1;

		// initialize a new GridBagConstraints with default values
		GridBagConstraints c = UIFx.initGridBagConstraints();
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.BOTH;
		// if all operations of a segment are online, then the segment panel gets a
		// green border
		boolean allOpsOnline;
		// loop through all segments
		for (Segment seg : segments.getData()) {
			// Each segment gets...
			// ... a panel with a GridLayout with n operations rows and 1 column
			pnSegment = new JPanel(new GridLayout(seg.size(), 1));
			// ... a segment menu item to open the segment editor dialog
			miSegment = new SegmentMenuItem(seg);
			miSegment.addActionListener(this);
			// add the edit segment menu item
			muEdit.add(miSegment);
			// ... a run segment menu item to start synchronization of the segment
			miRunSegment = new RunSegmentMenuItem(seg, ccSegId++);
			miRunSegment.addActionListener(this);
			// add the run segment menu item
			muRun.add(miRunSegment);

			// assume all ops online
			allOpsOnline = true;
			// loop through all operations
			for (Operation op : seg.getOperations()) {
				// if the operation is not online
				if (!op.isOnline() && allOpsOnline) {
					// set all operations online false
					allOpsOnline = false;
				}
				// initialize the operation panel
				opPan = new OperationPanel(this, op, cc++, settings);
				// if the operation is due
				if (op.isDue()) {
					// the operation panel gets an orange border
					opPan.setBorder(new LineBorder(Color.orange, 2, true));
				}
				// add the operation panel to the segment panel
				pnSegment.add(opPan);
			} // end of operations loop

			// if all operations were online
			if (allOpsOnline) {
				// then the segment panel gets a titled border in green
				pnSegment.setBorder(BorderFactory.createTitledBorder(
						BorderFactory.createLineBorder(OperationPanel.online, 2, true), seg.getName()));
			} else {
				// if any operations were offline
				// then the segment panel gets a titled border in gray
				pnSegment.setBorder(BorderFactory.createTitledBorder(
						BorderFactory.createLineBorder(OperationPanel.offline, 2, true), seg.getName()));
			}
			// add the segment panel to the main UI panel
			pnOperationsOverview.add(pnSegment, c);
			// if the position+1 of the segment in the segments list is divisible by n
			// columns with 0 remainder
			if (ccSegId % cols == 0) {
				// then set the next segment on the next line
				c.gridy++;
				c.gridx = 0;
			} else {
				// set the next segment to the next column
				c.gridx++;
			}
		} // end of segments loop
			// if there were any segments
		if (segments.size() > 0) {
			// add a separator after the edit segment menu items
			muEdit.addSeparator();
			// add a separator after the run segment menu items
			muRun.addSeparator();
		} else {
			// if there were no segments then show a button to edit the first segment
			// panel to lay out the add segment button
			JPanel pnNewSegmentButtonPanel = new JPanel(new FlowLayout());
			// add the add first segment button
			pnNewSegmentButtonPanel.add(btNewSegment);
			// panel to lay out a text above the add first segment button
			JPanel pnIntro = new JPanel(new GridLayout(2, 1));
			// add label informing the user that there are no entries yet
			pnIntro.add(new JLabel("Es gibt noch keine Einträge"));
			// add panel with add first segment button
			pnIntro.add(pnNewSegmentButtonPanel);
			// add the introduction message panel
			pnOperationsOverview.add(pnIntro);
		}
		// add open settings dialog menu item
		muEdit.add(miSettings);
		// at the end of the run menu the restoration dialog can be opened
		muRun.add(miRestore);
		// repaint the UI
		pnOperationsOverview.repaint();
		// pack and center
		// UIFx.packAndCenter(frm);
		frm.pack();
	}

	/**
	 * Runs the specified <code>Operation</code> with the defined options.
	 * <p>
	 * public to provide <code>OperationPanel</code> access.
	 * 
	 * @param operation
	 *            The <code>Operation</code> to be ran.
	 */
	public final void runOperation(Operation operation) {
		runOperation(operation, operation.isAlwaysQuickSync());
	}

	/**
	 * Runs the specified <code>Operation</code> with the specified analysis option.
	 * <p>
	 * public to provide <code>OperationPanel</code> access.
	 * 
	 * @param operation
	 *            The <code>Operation</code> to be executed.
	 * @param quickSync
	 *            <code>true</code> for Quick synchronization and <code>false</code>
	 *            for Deep synchronization.
	 */
	public final void runOperation(Operation operation, boolean quickSync) {
		// the list of OperationArguments to run
		Vector<OperationArgument> opArgs = new Vector<>();
		// the operation with the specified analysis option
		opArgs.add(new OperationArgument(operation, quickSync));
		// run operation without batch title
		runOperations(opArgs, null);
	}

	/**
	 * Runs the specified segment with the operation defined analysis options.
	 * 
	 * @param segment
	 *            The segment to be executed.
	 */
	private void runSegment(Segment segment) {
		// the list of OperationArguments to run
		Vector<OperationArgument> operations = new Vector<>();
		// loop through the segments operations
		for (Operation op : segment.getOperations()) {
			// add the operation with the default analysis option
			operations.add(new OperationArgument(op));
		}
		// run the segments operations with the segments name as batch title
		runOperations(operations, segment.getName());
	}

	/**
	 * Runs the selected <code>Operation</code>s with the specified analysis option.
	 * <p>
	 * If both <code>forceQuick</code> and <code>forceIntegrityCheck</code> are
	 * <code>true</code> then <code>forceQuick</code> is selected.
	 * <p>
	 * If both <code>forceQuick</code> and <code>forceIntegrityCheck</code> are
	 * <code>false</code> then the option that is defined in the operation is
	 * selected.
	 * 
	 * @param forceQuick
	 *            <code>true</code> to select quick synchronization.
	 * 
	 * @param forceIntegrityCheck
	 *            <code>true</code> to select deep synchronization.
	 */
	private void runSelected(boolean forceQuick, boolean forceIntegrityCheck) {
		// the list of OperationArguments to run
		Vector<OperationArgument> operations = new Vector<>();
		// loop through all segments
		for (Segment seg : segments.getData()) {
			// loop through the segments operations
			for (Operation op : seg.getOperations()) {
				// if the operation is selected
				if (op.isSelected()) {
					// then add it accordingly
					if (forceQuick) {
						operations.add(new OperationArgument(op, true));
					} else if (forceIntegrityCheck) {
						operations.add(new OperationArgument(op, false));
					} else {
						operations.add(new OperationArgument(op));
					}
					// deselect the operation after adding it to the batch
					op.setSelected(false);
				}
			}
		}
		// if any operations were selected
		if (operations.size() > 0) {
			// then run the operations with "selected operations" as batch title
			runOperations(operations, "Ausgewählte Operationen");
		} else {
			// if there were no selected operations then show a warning dialog
			JOptionPane.showMessageDialog(frm, "Nichts Ausgewählt!", "Warnung", JOptionPane.WARNING_MESSAGE);
		}
	}

	/**
	 * Runs all due <code>Operation</code>s with the specified analysis option.
	 * <p>
	 * If both <code>forceQuick</code> and <code>forceIntegrityCheck</code> are
	 * <code>true</code> then <code>forceQuick</code> is selected.
	 * <p>
	 * If both <code>forceQuick</code> and <code>forceIntegrityCheck</code> are
	 * <code>false</code> then the option that is defined in the operation is
	 * selected.
	 * 
	 * @param forceQuick
	 *            <code>true</code> to select quick synchronization.
	 * 
	 * @param forceIntegrityCheck
	 *            <code>true</code> to select deep synchronization.
	 */
	private void runDue(boolean forceQuick, boolean forceIntegrityCheck) {
		// the list of OperationArguments to run
		Vector<OperationArgument> operations = new Vector<>();
		// loop through all segments
		for (Segment seg : segments.getData()) {
			// loop through the segments operations
			for (Operation op : seg.getOperations()) {
				// if the operation is due
				if (op.isDue()) {
					// then add it accordingly
					if (forceQuick) {
						operations.add(new OperationArgument(op, true));
					} else if (forceIntegrityCheck) {
						operations.add(new OperationArgument(op, false));
					} else {
						operations.add(new OperationArgument(op));
					}
				}
			}
		}
		// if any operations were due
		if (operations.size() > 0) {
			// run the operations wit "due operations" as batch title
			runOperations(operations, "Fällige Operationen");
		} else {
			// if there were no operations due then show a warning dialog
			JOptionPane.showMessageDialog(frm, "Nichts fällig!", "Warnung", JOptionPane.WARNING_MESSAGE);
		}
	}

	/**
	 * Runs all <code>Operation</code>s with the specified analysis option.
	 * <p>
	 * If both <code>forceQuick</code> and <code>forceIntegrityCheck</code> are
	 * <code>true</code> then <code>forceQuick</code> is selected.
	 * <p>
	 * If both <code>forceQuick</code> and <code>forceIntegrityCheck</code> are
	 * <code>false</code> then the option that is defined in the operation is
	 * selected.
	 * 
	 * @param forceQuick
	 *            <code>true</code> to select quick synchronization.
	 * 
	 * @param forceIntegrityCheck
	 *            <code>true</code> to select deep synchronization.
	 */
	private void runAll(boolean forceQuick, boolean forceIntegrityCheck) {
		// the list of OperationArguments to run
		Vector<OperationArgument> operations = new Vector<>();
		// loop through all segments
		for (Segment seg : segments.getData()) {
			// loop through the segments operations
			for (Operation op : seg.getOperations()) {
				// add the operation accordingly
				if (forceQuick) {
					operations.add(new OperationArgument(op, true));
				} else if (forceIntegrityCheck) {
					operations.add(new OperationArgument(op, false));

				} else {
					operations.add(new OperationArgument(op));
				}
			}
		}
		// run all operations with "All segments" as batch title
		runOperations(operations, "Alle Segmente");
	}

	/**
	 * Runs the specified operations. All run* methods call this method.
	 * 
	 * @param operations
	 *            The <code>OperationArguments</code> to run.
	 * 
	 * @param syncTitle
	 *            The title (if any) for the batch.
	 */
	private void runOperations(Vector<OperationArgument> operations, String syncTitle) {
		// stop the UI change watcher thread during synchronization
		stopUIChangeWatcher();
		// initialize the SynchronizationProcess first
		SynchronizationProcess sp = new SynchronizationProcess(operations, syncTitle);
		// initialize the SynchronizationProcessDialog second and pass it the
		// SynchronizationProcess object for cancelling
		SynchronizationProcessDialog spd = new SynchronizationProcessDialog("Synchronisation", frm, settings,
				sp);
		// then pass the SynchronizationProcessDialog to the SynchronizationProcess
		sp.setSynchronisationProcessDialog(spd);
		// execute the SynchronizationProcess
		sp.execute();
		// show the SynchronizationProcessDialog (modal)
		spd.setVisible(true);
		// after the synchronization
		// save the segments with the new statistics
		segments.save();
		// refresh the UI to remove due borders
		refresh();
		// start the UI change watcher again
		startUIChangeWatcher();
	}

	/**
	 * Restores the selected <code>Operation</code>s.
	 * 
	 * @param segmentsRestore
	 *            The reversed segments clone from the
	 *            <code>RestorationDialog</code> containing the selection.
	 */
	private void restoreSelected(Segments segmentsRestore) {
		// the list of operations to be restored
		Vector<Operation> operations = new Vector<Operation>();
		// loop through the segments clone
		for (Segment seg : segmentsRestore.getData()) {
			// loop through the segments operations
			for (Operation op : seg.getOperations()) {
				// if the operation is selected
				if (op.isSelected()) {
					// then add the operation to the list of operations to be restored
					operations.add(op);
				}
			}
		}
		// if there were no operations selected
		if (operations.size() == 0) {
			// show a warning message
			JOptionPane.showMessageDialog(frm, "Nichts zum Wiederherstellen ausgewählt!", "Warnung",
					JOptionPane.WARNING_MESSAGE);
		} else { // there are operations selected
			// stop the UI change watcher thread during synchronization
			stopUIChangeWatcher();
			// first initialize the restoration process
			RestorationProcess rp = new RestorationProcess(operations);
			// second initialize the SynchronizationProcessDialog and pass it the
			// RestorationProcess for canceling
			SynchronizationProcessDialog spd = new SynchronizationProcessDialog("Wiederherstellen", frm,
					settings, rp);
			// then pass the SynchronizationProcessDialog to the RestorationProcess
			rp.setSpd(spd);
			// execute the RestorationProcess
			rp.execute();
			// show the SynchronizationProcessDialog (modal)
			spd.setVisible(true);
			// after restoration refresh the UI
			refresh();
			// start the UI change watcher again
			startUIChangeWatcher();
		}
	}

	/**
	 * Gets the main frame. Used by <code>OperationPanel</code> to display the
	 * <code>OperationEditorDialog</code>.
	 * 
	 * @return the main frame.
	 */
	public JFrame getFrame() {
		return frm;
	}

	/**
	 * Refreshes the UI, used by <code>RunRefreshUI</code> executed by UI change
	 * watcher thread.
	 */
	public void refresh() {
		rebuildUserInterface();
	}

	/**
	 * starts the UI change watcher thread.
	 */
	public final void startUIChangeWatcher() {
		// stop if already running
		if (uiChangeWatcher != null) {
			stopUIChangeWatcher();
		}
		// reinitialize and start the UI change watcher thread
		uiChangeWatcher = new UIChangeWatcherThread(this);
		Thread t = new Thread(uiChangeWatcher);
		t.start();
	}

	/**
	 * Stops the UI change watcher thread if running.
	 */
	public final void stopUIChangeWatcher() {
		// if the UI change watcher thread is running
		if (uiChangeWatcher != null) {
			// then stop it
			uiChangeWatcher.stop();
			// and set the variable null to indicate no running thread
			uiChangeWatcher = null;
		}
	}

	/**
	 * Starts the due operations reminder thread (entering tray mode).
	 * <p>
	 * This is called when entering tray mode.
	 */
	public final void startTrayReminder() {
		// if the reminder thread is still running
		if (trayReminder != null) {
			// then terminate it
			stopTrayReminder();
		}
		// reinitialize and execute the reminder thread
		trayReminder = new TrayReminderThread(this);
		Thread t = new Thread(trayReminder);
		t.start();
	}

	/**
	 * Stops the due operations reminder (exiting tray mode)
	 */
	public final void stopTrayReminder() {
		// if the reminder thread is running
		if (trayReminder != null) {
			// then terminate it
			trayReminder.stop();
			trayReminder = null;
		}
	}

	/**
	 * Notifies the user that a new version of the program is released.
	 * <p>
	 * This method is called from the class <code>RunNotifyNewVersion</code>.
	 * 
	 * @param newVersion
	 *            The new version from the web server.
	 */
	public void notifyNewVersion(String newVersion) {
		// open frame if in tray mode
		if (!frm.isVisible()) {
			openFromTray();
		}
		// check box to disable checking for new version in the future
		JCheckBox ckIgnore = new JCheckBox("Nicht mehr anzeigen");
		// panel to lay out the message components
		JPanel pnMessage = new JPanel(new GridBagLayout());
		// initialize new GridBagConstraints
		GridBagConstraints c = UIFx.initGridBagConstraints();
		// initialize and add a NewVersionMessageComponent that has a link to the
		// download site
		pnMessage.add(new NewVersionMessageComponent(newVersion), c);
		// next line
		c.gridy++;
		// add the ignore check for new version check box
		pnMessage.add(ckIgnore, c);
		// show the message dialog (modal)
		JOptionPane.showMessageDialog(frm, pnMessage, "Neue Version", JOptionPane.WARNING_MESSAGE);
		// check if the user has selected the disable check for new version check box
		if (ckIgnore.isSelected()) {
			settings.setIgnoreNewVersion(true);
			settings.write();
		}
	}

	/**
	 * Reminds the user that an <code>Operation</code> is due by showing a message
	 * from the tray icon area.
	 * <p>
	 * This method is called from <code>RunRemindTray</code>.
	 * 
	 * @param o
	 *            A clone of the due <code>Operation</code> to notify about.
	 */
	public final void remind(Operation o) {
		// show tray area message
		trayIcon.displayMessage("Erinnerung", "Operation fällig: " + o.toString(), TrayIcon.MessageType.INFO);
		// indicates end of loops
		boolean breakk = false;
		// loop through all segments
		for (Segment s : segments.getData()) {
			// loop through the segments operations
			for (Operation o2 : s.getOperations()) {
				// if the current operation equals the operations clone
				if (o.equals(o2)) {
					// set the operation reminded
					o2.setReminded(true);
					// save segments
					segments.save();
					// exit loops
					breakk = true;
					break;
				}
			} // end of operations loop
			if (breakk) {
				break;
			}
		} // end of segments loop
	}

	/**
	 * Searches for any visible instances of modal <code>JDialog</code>s.
	 * 
	 * @return <code>true</code> if a modal <code>JDialog</code> is visible.
	 */
	private boolean isModalJDialogVisible() {
		// get all windows
		Window[] windows = Window.getWindows();
		// assume that there are no dialogs
		boolean isModalJDialogVisible = false;
		// be sure not to provoke a null pointer exception
		if (windows != null) {
			// loop through all windows
			for (Window w : windows) {
				// if the window is visible, an instance of JDialog and modal
				if (w.isVisible() && w instanceof JDialog && ((JDialog) w).isModal()) {
					// then set the return answer true
					isModalJDialogVisible = true;
					// exit loop
					break;
				}
			}
		}
		// return answer
		return isModalJDialogVisible;
	}

	/**
	 * handles action events from menu items (menu bar and tray icon) and "add first
	 * segment" button, the UI clicks are handled by <code>OperationPanel</code>.
	 * 
	 * @see OperationPanel
	 */
	@Override
	public final void actionPerformed(ActionEvent e) {
		if (e.getSource() == miAddSegment || e.getSource() == btNewSegment) {
			// segment editor dialog for new segment
			SegmentEditorDialog sed = new SegmentEditorDialog(frm, null, segments, this);
			// display dialog (modal)
			sed.setVisible(true);
			// evaluate user answer, if answer is ok button
			if (sed.getAnswer() == SegmentEditorDialog.OK) {
				// halt the UI change watcher thread
				stopUIChangeWatcher();
				// add the new segment to the segments
				segments.add(sed.getSegment());
				// sort the changed segments
				segments.sort();
				// save the segments to JSON
				segments.save();
				// rebuild UI to display the new segment
				rebuildUserInterface();
				// start UI change watcher with the new data
				startUIChangeWatcher();
			}
		} else if (e.getSource() instanceof SegmentMenuItem) {
			// stop the UI change watcher thread
			stopUIChangeWatcher();
			// get the segment to edit
			Segment s = ((SegmentMenuItem) e.getSource()).getSegment();
			// initialize a segment editor dialog
			SegmentEditorDialog sed = new SegmentEditorDialog(frm, s, segments, this);
			// show the segment editor dialog (modal)
			sed.setVisible(true);
			// if the user choice was the delete button
			if (sed.isDelete()) {
				// ask if the corresponding databases should be deleted too
				int answer = JOptionPane.showConfirmDialog(frm,
						"Sollen die Dateisystemindexdatenbanken auch gelöscht werden?", "Datenbanken",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				// if the databases should be deleted
				if (answer == JOptionPane.YES_OPTION) {
					// assume the segment online
					boolean segOnline = true;
					// loop through the segments operations
					for (Operation op : s.getOperations()) {
						// if any operation is not online
						if (!op.isOnline()) {
							// segment is not online
							segOnline = false;
							break;
						}
					}
					// if any of the segments operations are not online
					if (!segOnline) {
						// notify the user that cleanup is not possible before all devices are connected
						JOptionPane.showMessageDialog(frm, "Bitte zuerst alle Datenträger verbinden!",
								"Fehler", JOptionPane.ERROR_MESSAGE);
						// abort delete
						return;
					}
				}
				// remove the segment
				segments.remove(s);
			} else if (sed.hasChanges()) {
				// if the segment has changes
				segments.sort();
				// then save the segments
				segments.save();
			}
			// refresh the UI
			rebuildUserInterface();
			// start the UI change watcher thread again
			startUIChangeWatcher();
		} else if (e.getSource() == miSettings) {
			// save the current column count
			int cols = settings.getColumns();
			// display the settings dialog (modal)
			settingsDialog.display();
			// if the column count was changed
			if (cols != settings.getColumns()) {
				// then rebuild UI
				rebuildUserInterface();
			}
		} else if (e.getSource() == miRunAll) {
			// run all segments with the operation option
			runAll(false, false);
		} else if (e.getSource() == miRunAllQuick) {
			// run all segments quick
			runAll(true, false);
		} else if (e.getSource() == miRunAllDeep) {
			// run all segments deep
			runAll(false, true);
		} else if (e.getSource() == miRunSelected) {
			// run selected segments with the operation option
			runSelected(false, false);
		} else if (e.getSource() == miRunSelectedQuick) {
			// run selected segments quick
			runSelected(true, false);
		} else if (e.getSource() == miRunSelectedDeep) {
			// run selected segments deep
			runSelected(false, true);
		} else if (e.getSource() == miRunDue) {
			// run due segments with the operation option
			runDue(false, false);
		} else if (e.getSource() == miRunDueQuick) {
			// run due segments quick
			runDue(true, false);
		} else if (e.getSource() == miRunDueDeep) {
			// run due segments deep
			runDue(false, true);
		} else if (e.getSource() instanceof RunSegmentMenuItem) {
			// get the segment to run
			Segment s = ((RunSegmentMenuItem) e.getSource()).getSegment();
			// run the segment
			runSegment(s);
		} else if (e.getSource() == miRestore) {
			// initialize the restoration dialog
			RestorationSelectionDialog rsd = new RestorationSelectionDialog(frm);
			// stop the UI change watcher
			stopUIChangeWatcher();
			// show the restoration dialog (modal)
			rsd.setVisible(true);
			// if the user answer is positive
			if (rsd.getAnswer() == RestorationSelectionDialog.ANSWER_RESTORE) {
				// then restore the selected operations
				restoreSelected(rsd.getSegments());
			}
			// refresh the UI
			refresh();
			// start the UI change watcher thread again
			startUIChangeWatcher();
		} else if (e.getSource() == miHelp) {
			// if the help URL is not null
			if (helpURL != null) {
				// create info dialog for help document
				InfoDialog id = new InfoDialog(frm, helpURL, "Hilfe", 0.5, 0.7, null);
				// display dialog (modal)
				id.setVisible(true);
			}
		} else if (e.getSource() == miAbout) {
			// if the about URL is not null
			if (aboutURL != null) {
				// create info dialog for about document
				InfoDialog id = new InfoDialog(frm, aboutURL, "About", 0.35, 0.5, null);
				// display dialog (modal)
				id.setVisible(true);
			}
		} else if (e.getSource() == miLicense) {
			// if the license URL is not null
			if (licenseURL != null) {
				// create info dialog for license document
				InfoDialog id = new InfoDialog(frm, licenseURL, "LICENSE");
				// display dialog (modal)
				id.setVisible(true);
			}
		} else if (e.getSource() == miToDo) {
			// if the to do URL is not null
			if (toDoURL != null) {
				// create to do dialog for license document
				InfoDialog id = new InfoDialog(frm, toDoURL, "TODO");
				// display dialog (modal)
				id.setVisible(true);
			}
		} else if (e.getSource() == miChangeLog) {
			// if the change log URL is not null
			if (changeLogURL != null) {
				// create change log dialog for license document
				InfoDialog id = new InfoDialog(frm, changeLogURL, "CHANGELOG");
				// display dialog (modal)
				id.setVisible(true);
			}
		} else if (e.getSource() == tiExit && !showFromTray) {
			// exit from tray
			// remove tray icon from system tray
			tray.remove(trayIcon);
			// exit system
			System.exit(0);
		} else if (e.getSource() == tiRunAll && !showFromTray) {
			// run all from tray
			// disable tray icon left click and menu items
			showFromTray = true;
			// stop the tray reminder
			stopTrayReminder();
			// show the main frame
			frm.setVisible(true);
			// run all with default option
			runAll(false, false);
			// hide frame again after synchronization
			frm.setVisible(false);
			// start the tray reminder again
			startTrayReminder();
			// enable tray icon left click and menu items
			showFromTray = false;
		} else if (e.getSource() instanceof SegmentTrayMenuItem && !showFromTray) {
			// run segment from tray
			// get the segment to run
			Segment s = ((SegmentTrayMenuItem) e.getSource()).getSegment();
			// disable tray icon left click and menu items
			showFromTray = true;
			// stop the tray reminder
			stopTrayReminder();
			// show the main frame
			frm.setVisible(true);
			// run the selected segment
			runSegment(s);
			// hide the main frame after synchronization
			frm.setVisible(false);
			// start the tray reminder again
			startTrayReminder();
			// enable tray icon left click and menu items
			showFromTray = false;
		} else if (e.getSource() instanceof OperationTrayMenuItem && !showFromTray) {
			// get the Operation to run
			Operation o = ((OperationTrayMenuItem) e.getSource()).getOperation();
			// disable tray icon left click and menu items
			showFromTray = true;
			// stop the tray reminder
			stopTrayReminder();
			// show the main frame
			frm.setVisible(true);
			// run the selected operation
			runOperation(o);
			// hide the main frame after synchronization
			frm.setVisible(false);
			// start the tray reminder again
			startTrayReminder();
			// enable tray icon left click and menu items
			showFromTray = false;
		}
	}

	/**
	 * The user closes the main frame
	 */
	@Override
	public final void windowClosing(WindowEvent arg0) {
		// hide the main frame anyway
		frm.setVisible(false);
		// if neither the setting "close to tray" or SystemTray.isSupported()
		if (!settings.isCloseToTray() || !SystemTray.isSupported()) {
			// then terminate the system normally
			System.exit(0);
		} else { // setting "close to tray" is true and system tray is supported
			try {
				// stop the UI change watcher
				stopUIChangeWatcher();
				// add the tray icon to the system tray
				tray.add(trayIcon);
				// start tray reminder
				startTrayReminder();
			} catch (AWTException e) {
				// if adding the tray icon failed
				// then exit the system abnormally
				System.exit(1);
			}
		}
	}

	/**
	 * The user minimizes the main frame
	 */
	@Override
	public final void windowIconified(WindowEvent arg0) {
		// if setting "minimize to tray" is true and SystemTray.isSupported()
		if (!isModalJDialogVisible() && settings.isMinimizeToTray() && SystemTray.isSupported()) {
			try {
				// try adding the tray icon
				tray.add(trayIcon);
				// start the tray reminder thread
				startTrayReminder();
				// hide the main frame
				frm.setVisible(false);
				// stop the UI change watcher
				stopUIChangeWatcher();
				// restore the main frame for showing later
				frm.setExtendedState(Frame.NORMAL);
			} catch (AWTException e) {}
		}
	}

	/**
	 * The user left clicks on the tray icon; disabled when running a
	 * synchronization process from tray.
	 */
	@Override
	public void mouseClicked(MouseEvent e) {
		// if source component is trayIcon
		if (e.getSource() == trayIcon && !showFromTray) {
			// if clicked mouse button is left button
			if (e.getButton() == MouseEvent.BUTTON1) {
				// open the main frame and remove the tray icon
				openFromTray();
			}
		}
	}

	/**
	 * Catching uncaught exceptions and writing the stack trace to a file in the log
	 * files directory
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		if (sdfLog != null) {
			// the file to write to
			File logFile = new File(settings.getLogFilesDir(),
					"FSSync-Error-Log-" + sdfLog.format(System.currentTimeMillis()) + ".txt");
			// save the stack trace to file
			FSSync.saveErrorLog(logFile, e);
			// pass the Exception to the defaultUncaughtExceptionHandler
			if (defaultUncaughtExceptionHandler != null) {
				defaultUncaughtExceptionHandler.uncaughtException(t, e);
			} else {
				// do nothing
			}
		}
	}

	/**
	 * Unused window event
	 */
	@Override
	public final void windowActivated(WindowEvent arg0) {}

	/**
	 * Unused window event
	 */
	@Override
	public final void windowClosed(WindowEvent arg0) {}

	/**
	 * Unused window event
	 */
	@Override
	public final void windowDeactivated(WindowEvent arg0) {}

	/**
	 * Unused window event
	 */
	@Override
	public final void windowDeiconified(WindowEvent arg0) {}

	/**
	 * Unused window event
	 */
	@Override
	public final void windowOpened(WindowEvent arg0) {}

	/**
	 * Unused mouse event
	 */
	@Override
	public void mousePressed(MouseEvent e) {}

	/**
	 * Unused mouse event
	 */
	@Override
	public void mouseReleased(MouseEvent e) {}

	/**
	 * Unused mouse event
	 */
	@Override
	public void mouseEntered(MouseEvent e) {}

	/**
	 * Unused mouse event
	 */
	@Override
	public void mouseExited(MouseEvent e) {}
}
