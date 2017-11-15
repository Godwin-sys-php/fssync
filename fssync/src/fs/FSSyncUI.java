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
package fs;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import mishmash.FSFx;
import mishmash.GC;
import mishmash.PropFx;
import mishmash.ui.UIFx;
import mishmash.ui.dialog.InfoDialog;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import fs.sync.SynchronisationProcess;
import fs.sync.RestorationProcess;
import fs.sync.ui.SynchronisationProcessDialog;

public final class FSSyncUI implements WindowListener, ActionListener {

	private final JFrame frm;
	private final JPanel pnOperationsOverview;
	private final JMenuItem miAddSegment;
	private final JMenuItem miSettings;
	private final JMenuItem miRunAll;
	private final JMenuItem miRefresh;
	private final JMenuItem miAbout;
	private final JMenuItem miHelp;
	private final JMenu muSegments;
	private final JMenu muRun;
	private final JMenu muRestore;

	private final SettingsDialog settingsDialog;

	private final Segments segments;

	// private final Vector<OperationPanel> operationPanels;

	private final Settings settings;

	// private final BufferedImage icon;

	private final URL helpURL;
	private final URL aboutURL;

	public FSSyncUI() {
		frm = new JFrame("FSSync 0.4a");
		frm.addWindowListener(this);
		frm.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		// JPopupMenu

		BufferedImage icon = null;
		try {
			icon = ImageIO.read(getClass().getResource("disk-128.png"));
			frm.setIconImage(icon);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		// this.icon = icon;

		File programDir = Paths.get(PropFx.userHome(), ".fssync").toFile();
		File docsDir = new File(programDir, "docs");

		if (!programDir.exists()) {
			programDir.mkdir();
			FSFx.hideWindowsFile(programDir);
		}

		if (!docsDir.exists()) {
			docsDir.mkdir();
			String[] names = new String[] { "about.html", "help.html", "requestContinueRestore.png",
					"requestForeignFileHandling.png", "requestSourceForRestore.png", "requestRestoreMode.png",
					"settings.png", "gui.png", "disk-128.png" };
			for (int i = 0; i < names.length; i++) {
				FSFx.copyResourceFile(getClass(), names[i], new File(docsDir, names[i]));
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

		miRunAll = new JMenuItem("Alle Ausf" + GC.ue() + "hren...");
		miRunAll.addActionListener(this);

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

		rebuildOverview();

		frm.setVisible(true);

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

	private final void rebuildOverview() {
		pnOperationsOverview.removeAll();
		muRestore.removeAll();
		muRun.removeAll();
		muRun.add(miRunAll);
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
				pnSegment.add(opPan);
			}
			if (allOpsOnline) {
				pnSegment.setBorder(BorderFactory.createTitledBorder(
						BorderFactory.createLineBorder(OperationPanel.online, 2, true), seg.getName()));
			} else {
				pnSegment.setBorder(BorderFactory.createTitledBorder(
						BorderFactory.createLineBorder(OperationPanel.offline, 2, true), seg.getName()));
			}

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
		SynchronisationProcessDialog spd = new SynchronisationProcessDialog("Synchronisation", frm, settings);
		SynchronisationProcess sp = new SynchronisationProcess(operations, syncTitle, settings, spd) {};
		sp.execute();
		spd.setVisible(true);
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
				segments.add(sed.getSegment());
				segments.sort();
				segments.save();
				rebuildOverview();
			}
		} else if (e.getSource() == miRefresh) {
			rebuildOverview();
		} else if (e.getSource() == miRunAll) {
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
		} else if (e.getSource() == miSettings) {
			int cols = settings.getColumns();
			settingsDialog.display();
			if (cols != settings.getColumns())
				rebuildOverview();
		} else if (e.getSource() == miHelp) {
			if (helpURL == null)
				return;
			String[] rules = new String[] { "ol.sync {margin: 10px; padding: 5px;}" };
			InfoDialog id = new InfoDialog(frm, helpURL, "Hilfe", 0.5, 0.7, rules);
			id.setVisible(true);
		} else if (e.getSource() == miAbout) {
			if (aboutURL == null)
				return;
			InfoDialog id = new InfoDialog(frm, aboutURL, "Über", 0.35, 0.5, null);
			id.pack();
			id.setVisible(true);
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
			if (sed.isDelete()) {
				segments.remove(s);
				rebuildOverview();
			} else if (sed.hasChanges()) {
				segments.sort();
				segments.save();
				rebuildOverview();
			}
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
		}
	}

	@Override
	public final void windowClosing(WindowEvent arg0) {
		System.exit(0);
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
	public final void windowIconified(WindowEvent arg0) {}

	@Override
	public final void windowOpened(WindowEvent arg0) {}
}
