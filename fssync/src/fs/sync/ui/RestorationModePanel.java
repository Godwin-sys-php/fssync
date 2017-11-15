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
package fs.sync.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;

import mishmash.ui.UIFx;

@SuppressWarnings("serial")
public class RestorationModePanel extends JPanel {

	private final JRadioButton rbRestoreDamaged, rbRestoreModified, rbRestoreAll;
	private final JCheckBox ckDeleteNew;
	public final static int RESTORE_ALL = 0, RESTORE_MODIFIED = 1, RESTORE_DAMAGED = 2;

	public RestorationModePanel(JDialog dialog) {
		ButtonGroup bg = new ButtonGroup();
		rbRestoreDamaged = new JRadioButton("Beschädigte Wiederherstellen");
		bg.add(rbRestoreDamaged);
		rbRestoreModified = new JRadioButton("Geänderte Wiederherstellen");
		bg.add(rbRestoreModified);
		rbRestoreAll = new JRadioButton("Alle Wiederherstellen");
		bg.add(rbRestoreAll);

		rbRestoreModified.setSelected(true);

		ckDeleteNew = new JCheckBox("Neue Dateien Löschen");

		setLayout(new GridBagLayout());
		GridBagConstraints c = UIFx.initGridBagConstraints();

		add(new JLabel("Modus zum Wiederherstellen:"), c);
		c.gridy++;
		add(rbRestoreDamaged, c);
		c.gridy++;
		add(rbRestoreModified, c);
		c.gridy++;
		add(rbRestoreAll, c);
		c.gridy++;
		add(new JSeparator(), c);
		c.gridy++;
		add(ckDeleteNew, c);
	}

	public final boolean isDeleteNew() {
		return ckDeleteNew.isSelected();
	}

	public final int getMode() {
		int mode = -1;
		if (rbRestoreAll.isSelected()) {
			mode = RESTORE_ALL;
		} else if (rbRestoreDamaged.isSelected()) {
			mode = RESTORE_DAMAGED;
		} else if (rbRestoreModified.isSelected()) {
			mode = RESTORE_MODIFIED;
		}
		return mode;
	}
}
