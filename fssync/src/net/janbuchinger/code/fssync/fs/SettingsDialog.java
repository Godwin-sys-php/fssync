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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemTray;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.janbuchinger.code.mishmash.ui.dialog.EscapeListener;
import net.janbuchinger.code.mishmash.ui.userInput.FolderPathTextField;
import net.janbuchinger.code.mishmash.ui.userInput.JTextFieldWithPopUp;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;

@SuppressWarnings("serial")
public class SettingsDialog extends JDialog implements ActionListener, EscapeListener {

	private final JButton btOk, btCancel;

	private final JFrame frm;

	private final File settingsFile;

	// private final File programDir;

	private final Settings settings;

	private final JComboBox<Integer> cbColumns;

	private final JCheckBox ckVerbose;

	private final FolderPathTextField tfLogFileDir;

	private final JCheckBox ckAlwaysSaveLog;

	private final JCheckBox ckShowSummary;

	private final JTextFieldWithPopUp tfFileBrowser;
	
	private final JCheckBox ckStartToTray;
	private final JCheckBox ckCloseToTray;
	private final JCheckBox ckMinimizeToTray;

	public SettingsDialog(JFrame frm) {
		super(frm, "Einstellungen", true);
		this.frm = frm;

		settingsFile = new File(new File(System.getProperty("user.home"), ".fssync"), "settings.json");

		// File programDir = settingsFile.getParentFile();

		Gson g = new Gson();

		Settings s;
		try {
			s = g.fromJson(FileUtils.readFileToString(settingsFile, Charset.defaultCharset()), Settings.class);
			s.setSettingsFile(settingsFile);
		} catch (IOException e) {
			s = new Settings(settingsFile);
		}

		settings = s;

		Integer[] cols = new Integer[10];
		for (int i = 0; i < cols.length; i++) {
			cols[i] = new Integer(i + 1);
		}
		cbColumns = new JComboBox<Integer>(cols);
		cbColumns.setSelectedIndex(s.getColumns() - 1);

		ckVerbose = new JCheckBox("Viele Informationen während der Synchronisation Anzeigen");
		ckVerbose.setSelected(s.isVerbose());

		tfLogFileDir = new FolderPathTextField(this);
		tfLogFileDir.setPath(s.getLogFilesDir());

		ckAlwaysSaveLog = new JCheckBox("Log immer Speichern");
		ckAlwaysSaveLog.setSelected(s.isAlwaysSaveLog());

		ckShowSummary = new JCheckBox("Zusammenfassung vor Änderung zeigen");
		ckShowSummary.setSelected(s.isShowSummary());

		tfFileBrowser = new JTextFieldWithPopUp();
		tfFileBrowser.setText(s.getFileBrowser());
		
		ckStartToTray = new JCheckBox("Als Tray Icon Starten");
		ckStartToTray.setSelected(s.isStartToTray());

		ckCloseToTray = new JCheckBox("Ins Tray Schliessen");
		ckCloseToTray.setSelected(s.isCloseToTray());

		ckMinimizeToTray = new JCheckBox("Ins Tray Minimieren");
		ckMinimizeToTray.setSelected(s.isMinimizeToTray());
		
		if(!SystemTray.isSupported()){
			ckStartToTray.setEnabled(false);
			ckCloseToTray.setEnabled(false);
			ckMinimizeToTray.setEnabled(false);
		}
		
		GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 2, 2);

		JPanel pnControls = new JPanel(new GridBagLayout());
		pnControls.add(new JLabel("Segmente in ? Spalten Anzeigen"), c);
		c.gridx++;
		pnControls.add(cbColumns, c);
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 2;
		pnControls.add(ckVerbose, c);
		c.gridy++;
		pnControls.add(new JLabel("Ordner für Logdateien"), c);
		c.gridy++;
		pnControls.add(tfLogFileDir, c);
		c.gridy++;
		pnControls.add(ckAlwaysSaveLog, c);
		c.gridy++;
		pnControls.add(ckShowSummary, c);
		c.gridy++;
		c.gridwidth = 1;
		pnControls.add(new JLabel("Dateibrowser"), c);
		c.gridx++;
		pnControls.add(tfFileBrowser, c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		pnControls.add(ckStartToTray, c);
		c.gridx++;
		pnControls.add(ckCloseToTray, c);
		c.gridy++;
		pnControls.add(ckMinimizeToTray, c);

		btOk = new JButton("Speichern");
		btOk.addActionListener(this);
		btCancel = new JButton("Abbrechen");
		btCancel.addActionListener(this);

		JPanel pnButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		pnButtons.add(btCancel);
		pnButtons.add(btOk);

		JPanel pnContent = new JPanel(new BorderLayout());
		pnContent.add(pnControls, BorderLayout.CENTER);
		pnContent.add(pnButtons, BorderLayout.SOUTH);
		setContentPane(pnContent);
		pack();
	}

	public final void display() {
		setLocation((frm.getX() + (frm.getWidth() / 2)) - (getWidth() / 2),
				(frm.getY() + (frm.getHeight() / 2)) - (getHeight() / 2));
		setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btOk) {
			settings.setColumns(((Integer) cbColumns.getSelectedItem()).intValue());
			settings.setVerbose(ckVerbose.isSelected());
			File dir = new File(tfLogFileDir.getPath());
			if (!dir.isDirectory()) {
				JOptionPane.showMessageDialog(this, "Pfad muss zu einem Verzeichnis führen", "Fehler",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (!dir.exists()) {
				int answer = JOptionPane.showConfirmDialog(this,
						"Soll das Verzeichnis für Logdateien erstellt werden?", "Fehler",
						JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
				if (answer == JOptionPane.YES_OPTION) {
					dir.mkdirs();
				} else {
					return;
				}
			}
			settings.setLogFilesDir(tfLogFileDir.getPath());
			settings.setAlwaysSaveLog(ckAlwaysSaveLog.isSelected());
			settings.setShowSummary(ckShowSummary.isSelected());
			settings.setFileBrowser(tfFileBrowser.getText());
			settings.setStartToTray(ckStartToTray.isSelected());
			settings.setCloseToTray(ckCloseToTray.isSelected());
			settings.setMinimizeToTray(ckMinimizeToTray.isSelected());
			settings.write();
			setVisible(false);
		} else if (e.getSource() == btCancel) {
			cbColumns.setSelectedIndex(settings.getColumns() - 1);
			ckVerbose.setSelected(settings.isVerbose());
			tfLogFileDir.setPath(settings.getLogFilesDir());
			ckAlwaysSaveLog.setSelected(settings.isAlwaysSaveLog());
			ckShowSummary.setSelected(settings.isShowSummary());
			tfFileBrowser.setText(settings.getFileBrowser());
			ckStartToTray.setSelected(settings.isStartToTray());
			ckCloseToTray.setSelected(settings.isCloseToTray());
			ckMinimizeToTray.setSelected(settings.isMinimizeToTray());
			setVisible(false);
		}
	}

	@Override
	public final void escaping() {
		actionPerformed(new ActionEvent(btCancel, 0, ""));
	}

	public Settings getSettings() {
		return settings;
	}

	public void updateFileBrowser(String cmd) {
		tfFileBrowser.setText(cmd);
	}
}
