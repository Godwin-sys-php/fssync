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
package net.janbuchinger.code.fssync.sync.ui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;

import net.janbuchinger.code.mishmash.ui.UIFx;

@SuppressWarnings("serial")
public class RestorationModePanel extends JPanel implements ActionListener {

	private final JRadioButton rbRestoreSoft, rbUndoChanges, rbRestoreAll;
	private final JCheckBox ckDeleteNew;
	public final static int MODE_SOFT = 1, MODE_ALL = 2, MODE_UNDO_CHANGES = 3;

	public RestorationModePanel(JDialog dialog) {
		Color green = Color.GREEN.darker();
		Color red = Color.RED.darker();

		ButtonGroup bg = new ButtonGroup();
		rbRestoreSoft = new JRadioButton("Sanft Wiederherstellen");
		rbRestoreSoft.setForeground(green);
		bg.add(rbRestoreSoft);
		rbUndoChanges = new JRadioButton("Änderungen rückgängig Machen");
		rbUndoChanges.setForeground(red);
		bg.add(rbUndoChanges);
		rbRestoreAll = new JRadioButton("Alle Wiederherstellen");
		rbRestoreAll.setForeground(red);
		bg.add(rbRestoreAll);
		JLabel lbRestoreSoft = new JLabel("Nur Dateien die im Quellordner fehlen Wiederherstellen");
		lbRestoreSoft.setForeground(green);
		JLabel lbRestoreAll = new JLabel(
				"Es werden alle Dateien aus dem Zielverzeichnis in das Quellverzeichnis kopiert");
		lbRestoreAll.setForeground(red);
		JLabel lbUndoChanges = new JLabel(
				"Alle geänderten Dateien mit ihrer alten Version aus dem Zielverzeichnis Überschreiben");
		lbUndoChanges.setForeground(red);

		JLabel lbTitle = new JLabel("Modus zum Wiederherstellen:");
		lbTitle.setForeground(Color.CYAN.darker());

		rbRestoreSoft.addActionListener(this);
		rbRestoreAll.addActionListener(this);
		rbUndoChanges.addActionListener(this);

		rbRestoreSoft.setSelected(true);

		ckDeleteNew = new JCheckBox("Neue Dateien Löschen");
		ckDeleteNew.setEnabled(false);
		ckDeleteNew.setForeground(red);

		setLayout(new GridBagLayout());
		GridBagConstraints c = UIFx.initGridBagConstraints();

		add(lbTitle, c);
		c.gridy++;
		add(rbRestoreSoft, c);
		c.gridy++;
		add(lbRestoreSoft, c);
		c.gridy++;
		add(new JSeparator(), c);
		c.gridy++;
		add(rbRestoreAll, c);
		c.gridy++;
		add(lbRestoreAll, c);
		c.gridy++;
		add(new JSeparator(), c);
		c.gridy++;
		add(rbUndoChanges, c);
		c.gridy++;
		add(lbUndoChanges, c);
		c.gridy++;
		add(ckDeleteNew, c);
	}

	public final boolean isDeleteNew() {
		return ckDeleteNew.isSelected();
	}

	public final int getMode() {
		int mode = -1;
		if (rbRestoreAll.isSelected()) {
			mode = MODE_ALL;
		} else if (rbRestoreSoft.isSelected()) {
			mode = MODE_SOFT;
		} else if (rbUndoChanges.isSelected()) {
			mode = MODE_UNDO_CHANGES;
		}
		return mode;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JRadioButton) {
			if (((JRadioButton) e.getSource()).isSelected()) {
				if (e.getSource() == rbRestoreSoft) {
					ckDeleteNew.setSelected(false);
					ckDeleteNew.setEnabled(false);
				} else if (e.getSource() == rbUndoChanges || e.getSource() == rbRestoreAll) {
					ckDeleteNew.setEnabled(true);
				}
			}
		}
	}
}
