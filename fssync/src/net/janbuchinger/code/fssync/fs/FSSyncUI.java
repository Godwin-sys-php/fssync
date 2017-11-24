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
package net.janbuchinger.code.fssync.fs;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;

import net.janbuchinger.code.fssync.fs.sync.RestorationProcess;
import net.janbuchinger.code.fssync.fs.sync.SynchronisationProcess;
import net.janbuchinger.code.fssync.fs.sync.ui.SynchronisationProcessDialog;
import net.janbuchinger.code.mishmash.FSFx;
import net.janbuchinger.code.mishmash.GC;
import net.janbuchinger.code.mishmash.PropFx;
import net.janbuchinger.code.mishmash.ui.UIFx;
import net.janbuchinger.code.mishmash.ui.dialog.InfoDialog;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public final class FSSyncUI implements WindowListener, ActionListener, MouseListener {

	private final JFrame frm;
	private final JPanel pnOperationsOverview;
	private final JMenuItem miAddSegment;
	private final JMenuItem miSettings;
	private final JMenuItem miRunAll;
	private final JMenuItem miRunSelected;
	private final JMenuItem miRefresh;
	private final JMenuItem miAbout;
	private final JMenuItem miHelp;
	private final JMenu muSegments;
	private final JMenu muRun;
	private final JMenu muRestore;

	private final PopupMenu trayPopup;
	private final MenuItem tiExit;
	private final MenuItem tiRunAll;

	private final TrayIcon trayIcon;
	private final SystemTray tray;

	private final SettingsDialog settingsDialog;

	private final Segments segments;

	private final Settings settings;

	private final URL helpURL;
	private final URL aboutURL;

	private long click;

	private UIChangeWatcher uiChangeWatcher;
	
	private TrayReminder trayReminder;

	public FSSyncUI() {
		click = 0;

		frm = new JFrame("FSSync 0.5a");
		frm.addWindowListener(this);
		frm.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		BufferedImage icon = null;
		try {
			icon = ImageIO.read(getClass().getResource("res/disk-128.png"));
			frm.setIconImage(icon);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		File programDir = Paths.get(PropFx.userHome(), ".fssync").toFile();
		File docsDir = new File(programDir, "docs");

		if (!programDir.exists()) {
			programDir.mkdir();
			FSFx.hideWindowsFile(programDir);
		}

		if (!docsDir.exists()) {
			docsDir.mkdir();
			String[] names = new String[] { "res/about.html", "res/help.html",
					"res/requestContinueRestore.png", "res/requestForeignFileHandling.png",
					"res/requestSourceForRestore.png", "res/requestRestoreMode.png", "res/settings.png",
					"res/gui.png", "res/disk-128.png" };
			for (int i = 0; i < names.length; i++) {
				FSFx.copyResourceFile(getClass(), names[i], new File(docsDir, new File(names[i]).getName()));
			}
		}

		URL aboutURL = null, helpURL = null;
		try {
			aboutURL = new File(docsDir, "about.html").toURI().toURL();
			helpURL = new File(docsDir, "help.html").toURI().toURL();
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		this.aboutURL = aboutURL;
		this.helpURL = helpURL;

		settingsDialog = new SettingsDialog(frm);

		settings = settingsDialog.getSettings();

		Gson g = new Gson();
		File segmentsFile = new File(settings.getUserProgramDir(), "sync.json");
		Segments s = null;
		try {
			s = g.fromJson(FileUtils.readFileToString(segmentsFile, Charset.defaultCharset()), Segments.class);
			s.setSegmentsFile(segmentsFile);
		} catch (JsonSyntaxException | IOException e) {
			s = new Segments(segmentsFile);
		}
		segments = s;

		miAddSegment = new JMenuItem("Segment hinzuf" + GC.ue() + "gen...");
		miAddSegment.addActionListener(this);

		miSettings = new JMenuItem("Einstellungen...");
		miSettings.addActionListener(this);

		miRunAll = new JMenuItem("Alle");
		miRunAll.addActionListener(this);

		miRunSelected = new JMenuItem("Ausgewählte");
		miRunSelected.addActionListener(this);

		miRefresh = new JMenuItem("Aktualisieren");
		miRefresh.addActionListener(this);

		miAbout = new JMenuItem("Über...");
		miAbout.addActionListener(this);

		miHelp = new JMenuItem("Hilfe...");
		miHelp.addActionListener(this);

		muSegments = new JMenu("Segmente");

		muRun = new JMenu("Ausf" + GC.ue() + "hren");

		muRestore = new JMenu("Wiederherstellen");

		JMenu muEdit = new JMenu("Bearbeiten");
		muEdit.add(miSettings);

		JMenu muQuestion = new JMenu("?");
		muQuestion.add(miHelp);
		muQuestion.add(miAbout);

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(muEdit);
		menuBar.add(muSegments);
		menuBar.add(muRun);
		menuBar.add(muQuestion);

		frm.setJMenuBar(menuBar);

		pnOperationsOverview = new JPanel();
		pnOperationsOverview.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		JPanel pnContent = new JPanel(new BorderLayout());

		pnContent.add(UIFx.initScrollPane(pnOperationsOverview, 15), BorderLayout.CENTER);
		frm.setContentPane(pnContent);

		if (SystemTray.isSupported()) {
			Image imgIcon = null;
			try {
				imgIcon = ImageIO.read(getClass().getResource("res/disk-128.png"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			int traySquare = new TrayIcon(imgIcon).getSize().width;
			trayIcon = new TrayIcon(imgIcon.getScaledInstance(traySquare, traySquare, Image.SCALE_SMOOTH));
			trayIcon.addMouseListener(this);
			tray = SystemTray.getSystemTray();
			trayPopup = new PopupMenu();
			tiExit = new MenuItem("Schliessen");
			tiExit.addActionListener(this);
			tiRunAll = new MenuItem("Alle Ausführen");
			tiRunAll.addActionListener(this);
			trayIcon.setPopupMenu(trayPopup);
		} else {
			tray = null;
			trayIcon = null;
			trayPopup = null;
			tiExit = null;
			tiRunAll = null;
		}
		rebuildOverview();

		if (settings.isStartToTray() && SystemTray.isSupported()) {
			try {
				tray.add(trayIcon);
//				trayIcon.displayMessage("Caption", "Text", TrayIcon.MessageType.WARNING);
				startTrayReminder();
			} catch (AWTException e) {
				frm.setVisible(true);
				startUIChangeWatcher();
			}
		} else {
			frm.setVisible(true);
			startUIChangeWatcher();
		}

		if (settings.getFileBrowser().equals("")) {
			int answer = JOptionPane
					.showConfirmDialog(
							frm,
							"Linux: Soll nach einem Dateiexplorer gesucht werden? (Dabei öffnet sich vermutlich ein Fenster)",
							"Dateiexplorer fehlt", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (answer == JOptionPane.YES_OPTION) {
				String cmd = settings.findFileBrowser();
				if (cmd.length() == 0) {
					JOptionPane.showMessageDialog(frm, "Es konnte kein Browser erkannt werden.",
							"Kein Ergebnis", JOptionPane.ERROR_MESSAGE);
				} else {
					settings.setFileBrowser(cmd);
					settings.write();
					settingsDialog.updateFileBrowser(cmd);
				}
			}
		}
	}

	private void rebuildTrayMenu() {
		// Menu muRun = new Menu("Ausführen");
		Iterator<Segment> iSeg;
		Segment s;
		SegmentTrayMenuItem stmi;
		Iterator<Operation> iOp;
		Operation o;
		OperationTrayMenuItem otmi;
		iSeg = segments.iterator();

		trayPopup.removeAll();
		while (iSeg.hasNext()) {
			s = iSeg.next();
			stmi = new SegmentTrayMenuItem(s);
			stmi.addActionListener(this);
			trayPopup.add(stmi);
			iOp = s.iterator();
			while (iOp.hasNext()) {
				o = iOp.next();
				otmi = new OperationTrayMenuItem(o);
				otmi.addActionListener(this);
				trayPopup.add(otmi);
			}
			trayPopup.addSeparator();
		}

		trayPopup.add(tiRunAll);
		trayPopup.addSeparator();
		trayPopup.add(tiExit);
	}

	private final void rebuildOverview() {
		if (SystemTray.isSupported())
			rebuildTrayMenu();

		pnOperationsOverview.removeAll();
		muRestore.removeAll();
		muRun.removeAll();
		muRun.add(miRunAll);
		muRun.add(miRunSelected);
		muSegments.removeAll();
		muSegments.add(miRefresh);
		muSegments.add(miAddSegment);

		Iterator<Segment> iSeg = segments.iterator();
		Segment seg;

		Iterator<Operation> iOp;
		Operation op;

		int cols = settings.getColumns();

		pnOperationsOverview.setLayout(new GridBagLayout());
		JPanel pnSegment;
		SegmentMenuItem miSegment;
		RunSegmentMenuItem miRunSegment;
		RestoreSegmentMenuItem miRestoreSegment;
		int ccSegId = 0;
		int cc = 1;
		int ccSeg = 1;
		OperationPanel opPan = null;

		GridBagConstraints c = UIFx.initGridBagConstraints();
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.BOTH;
		boolean allOpsOnline;
		while (iSeg.hasNext()) {
			seg = iSeg.next();
			iOp = seg.iterator();

			pnSegment = new JPanel(new GridLayout(seg.size(), 1));
			miSegment = new SegmentMenuItem(seg);
			miSegment.addActionListener(this);
			muSegments.add(miSegment);
			miRunSegment = new RunSegmentMenuItem(seg, ccSegId++);
			miRunSegment.addActionListener(this);
			muRun.add(miRunSegment);
			miRestoreSegment = new RestoreSegmentMenuItem(seg, ccSegId - 1);
			miRestoreSegment.addActionListener(this);
			muRestore.add(miRestoreSegment);
			allOpsOnline = true;

			while (iOp.hasNext()) {
				op = iOp.next();
				if (!op.isOnline()) {
					allOpsOnline = false;
				}
				opPan = new OperationPanel(this, op, cc++, settings);

				if (op.isDue()) {
					opPan.setBorder(new LineBorder(Color.orange, 2, true));
					// opPan.setBackground(Color.gray.brighter());
				}

				pnSegment.add(opPan);
			}
			if (allOpsOnline) {
				pnSegment.setBorder(BorderFactory.createTitledBorder(
						BorderFactory.createLineBorder(OperationPanel.online, 2, true), seg.getName()));
			} else {
				pnSegment.setBorder(BorderFactory.createTitledBorder(
						BorderFactory.createLineBorder(OperationPanel.offline, 2, true), seg.getName()));
			}

			// JPanel pnSegmentPush = new JPanel(new BorderLayout());
			// pnSegmentPush.add(pnSegment, BorderLayout.NORTH);

			pnOperationsOverview.add(pnSegment, c);
			if ((ccSeg++) % cols == 0) {
				c.gridy++;
				c.gridx = 0;
			} else {
				c.gridx++;
			}
		}

		muRun.add(muRestore);
		pnOperationsOverview.repaint();

		UIFx.packAndCenter(frm);
	}

	public final void runOperations(Vector<Operation> operations, String syncTitle) {
		boolean restartChangeWatcher = false;
		if(uiChangeWatcher != null){
			restartChangeWatcher = true;
			stopUIChangeWatcher();
		}		
		SynchronisationProcessDialog spd = new SynchronisationProcessDialog("Synchronisation", frm, settings);
		SynchronisationProcess sp = new SynchronisationProcess(operations, syncTitle, settings, spd) {};
		sp.execute();
		spd.setVisible(true);
		segments.save();
		refresh();
		if(restartChangeWatcher)
			startUIChangeWatcher();
	}

	public Segments getSegments() {
		return segments;
	}

	public JFrame getFrame() {
		return frm;
	}

	public void refresh() {
		rebuildOverview();
	}

	public final void startUIChangeWatcher() {
		if (uiChangeWatcher == null) {
			uiChangeWatcher = new UIChangeWatcher(segments, this);
			Thread t = new Thread(uiChangeWatcher);
			t.start();
		}
	}

	public final void stopUIChangeWatcher() {
		if (uiChangeWatcher != null) {
			uiChangeWatcher.stop();
			uiChangeWatcher = null;
		}
	}

	public final void startTrayReminder() {
		if (trayReminder == null) {
			trayReminder = new TrayReminder(segments, trayIcon);
			Thread t = new Thread(trayReminder);
			t.start();
		}
	}

	public final void stopTrayReminder() {
		if (trayReminder != null) {
			trayReminder.stop();
			trayReminder = null;
		}
	}

	@Override
	public final void actionPerformed(ActionEvent e) {
		if (e.getSource() == miAddSegment) {
			// String[] segNames = new String[segments.size()];
			// for (int i = 0; i < segNames.length; i++) {
			// segNames[i] = segments.get(i).getName();
			// }
			SegmentEditorDialog sed = new SegmentEditorDialog(frm, null, segments);
			sed.setVisible(true);
			if (sed.getAnswer() == SegmentEditorDialog.OK) {
				stopUIChangeWatcher();
				segments.add(sed.getSegment());
				segments.sort();
				segments.save();
				startUIChangeWatcher();
				rebuildOverview();
			}
		} else if (e.getSource() == miRefresh) {
			refresh();
		} else if (e.getSource() == miRunAll) {
			runAll();
		} else if (e.getSource() == miRunSelected) {
			Vector<Operation> operations = new Vector<Operation>();
			Iterator<Segment> iSeg = segments.iterator();
			Iterator<Operation> iOp;
			Operation op;
			while (iSeg.hasNext()) {
				iOp = iSeg.next().iterator();
				while (iOp.hasNext()) {
					op = iOp.next();
					if (op.isSelected())
						operations.add(op);
				}
			}
			runOperations(operations, "Ausgewählte Operationen");
		} else if (e.getSource() == miSettings) {
			int cols = settings.getColumns();
			settingsDialog.display();
			if (cols != settings.getColumns())
				rebuildOverview();
		} else if (e.getSource() == miHelp) {
			if (helpURL == null)
				return;
			String[] rules = new String[] {};
			InfoDialog id = new InfoDialog(frm, helpURL, "Hilfe", 0.5, 0.7, rules);
			id.setVisible(true);
		} else if (e.getSource() == miAbout) {
			if (aboutURL == null)
				return;
			InfoDialog id = new InfoDialog(frm, aboutURL, "Über", 0.35, 0.5, null);
			id.pack();
			id.setVisible(true);
		} else if (e.getSource() == tiExit) {
			tray.remove(trayIcon);
			System.exit(0);
		} else if (e.getSource() == tiRunAll) {
			frm.setVisible(true);
			runAll();
			frm.setVisible(false);
		} else if (e.getSource() instanceof SegmentMenuItem) {
			Segment s = ((SegmentMenuItem) e.getSource()).getSegment();
			String[] segNames = new String[segments.size() - 1];
			int i1 = 0;
			for (int i = 0; i < segments.size(); i++) {
				if (segments.get(i) != s) {
					segNames[i1++] = segments.get(i).getName();
				}
			}
			SegmentEditorDialog sed = new SegmentEditorDialog(frm, s, segments);
			sed.setVisible(true);
			stopUIChangeWatcher();
			if (sed.isDelete()) {
				segments.remove(s);
				rebuildOverview();
			} else if (sed.hasChanges()) {
				segments.sort();
				segments.save();
				rebuildOverview();
			}
			startUIChangeWatcher();
		} else if (e.getSource() instanceof RunSegmentMenuItem) {
			RunSegmentMenuItem rsmi = (RunSegmentMenuItem) e.getSource();
			Vector<Operation> operations = new Vector<Operation>();
			Iterator<Operation> iOp = rsmi.getSegment().iterator();
			while (iOp.hasNext()) {
				operations.add(iOp.next());
			}
			runOperations(operations, rsmi.getSegment().getName());
		} else if (e.getSource() instanceof RestoreSegmentMenuItem) {
			RestoreSegmentMenuItem rsmi = (RestoreSegmentMenuItem) e.getSource();
			Vector<Operation> operations = new Vector<Operation>();
			operations.addAll(rsmi.getSegment().getOperations());
			SynchronisationProcessDialog spd = new SynchronisationProcessDialog("Wiederherstellen", frm,
					settings);
			RestorationProcess rp = new RestorationProcess(operations, rsmi.getSegment().getName(), spd);
			rp.execute();
			spd.setVisible(true);
		} else if (e.getSource() instanceof SegmentTrayMenuItem) {
			SegmentTrayMenuItem stmi = (SegmentTrayMenuItem) e.getSource();
			Vector<Operation> operations = new Vector<Operation>();
			Iterator<Operation> iOp = stmi.getSegment().iterator();
			while (iOp.hasNext()) {
				operations.add(iOp.next());
			}
			frm.setVisible(true);
			runOperations(operations, stmi.getSegment().getName());
			frm.setVisible(false);
		} else if (e.getSource() instanceof OperationTrayMenuItem) {
			OperationTrayMenuItem otmi = (OperationTrayMenuItem) e.getSource();
			Vector<Operation> ops = new Vector<Operation>();
			ops.add(otmi.getOperation());
			frm.setVisible(true);
			runOperations(ops, null);
			frm.setVisible(false);
		}
	}

	private void runAll() {
		Vector<Operation> operations = new Vector<Operation>();
		Iterator<Segment> iSeg = segments.iterator();
		Iterator<Operation> iOp;
		while (iSeg.hasNext()) {
			iOp = iSeg.next().iterator();
			while (iOp.hasNext()) {
				operations.add(iOp.next());
			}
		}
		runOperations(operations, "Alle Segmente");
	}

	@Override
	public final void windowClosing(WindowEvent arg0) {
		if (!settings.isCloseToTray() || !SystemTray.isSupported()) {
			frm.setVisible(false);
			System.exit(0);
		} else {
			try {
				tray.add(trayIcon);
				startTrayReminder();
				frm.setVisible(false);
				stopUIChangeWatcher();
			} catch (AWTException e) {}
		}
	}

	@Override
	public final void windowActivated(WindowEvent arg0) {}

	@Override
	public final void windowClosed(WindowEvent arg0) {}

	@Override
	public final void windowDeactivated(WindowEvent arg0) {}

	@Override
	public final void windowDeiconified(WindowEvent arg0) {}

	@Override
	public final void windowIconified(WindowEvent arg0) {
		if (settings.isMinimizeToTray() && SystemTray.isSupported()) {
			try {
				tray.add(trayIcon);
				startTrayReminder();
				frm.setVisible(false);
				stopUIChangeWatcher();
				frm.setExtendedState(Frame.NORMAL);
			} catch (AWTException e) {}
		}
	}

	@Override
	public final void windowOpened(WindowEvent arg0) {}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getSource() == trayIcon) {
			if (System.currentTimeMillis() - click < 500) {
				refresh();
				frm.setVisible(true);
				startUIChangeWatcher();
				tray.remove(trayIcon);
				stopTrayReminder();
			}
			click = System.currentTimeMillis();
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}
}
