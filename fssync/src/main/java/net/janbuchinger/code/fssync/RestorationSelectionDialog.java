/*
 * Copyright 2018 Jan Buchinger
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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.janbuchinger.code.mishmash.ui.UIFx;

@SuppressWarnings("serial")
public class RestorationSelectionDialog extends JDialog implements ActionListener {
	private final Segments segments;

	private final JFrame frm;

	private final JRadioButton rbOutstanding;
	private final JRadioButton rbAvailable;
	private final JPanel pnDisplay;
	private final JButton btRestore;
	private final JButton btCancel;
	private final JMenuItem miCreateSourceDirectories;
	private final JMenuItem miRepairDatabaseNames;

	private int answer;
	public final static int ANSWER_CANCEL = 0;
	public final static int ANSWER_RESTORE = 1;

	public RestorationSelectionDialog(JFrame frm) {
		super(frm, "Wiederherstellung", true);
		this.frm = frm;
		this.answer = ANSWER_CANCEL;

		miCreateSourceDirectories = new JMenuItem("Erzeuge fehlende Quellverzeichnisse...");
		miCreateSourceDirectories.addActionListener(this);
		miRepairDatabaseNames = new JMenuItem("Korrigiere Datenbanknamen nach manuellem Kopieren...");
		miRepairDatabaseNames.addActionListener(this);
		JMenu muExtras = new JMenu("Extras");
		muExtras.add(miCreateSourceDirectories);
		// muExtras.add(miRepairDatabaseNames);
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(muExtras);
		setJMenuBar(menuBar);

		segments = Segments.getSegments(true);
		segments.reverse();
		JPanel pnSelectorButtons = new JPanel(new GridLayout(1, 2));
		ButtonGroup bg = new ButtonGroup();
		rbOutstanding = new JRadioButton("Ausständige");
		rbOutstanding.setSelected(true);
		rbOutstanding.addActionListener(this);
		bg.add(rbOutstanding);
		rbAvailable = new JRadioButton("Verfügbare");
		rbAvailable.addActionListener(this);
		bg.add(rbAvailable);
		pnSelectorButtons.add(rbOutstanding);
		pnSelectorButtons.add(rbAvailable);
		btRestore = new JButton("Wiederherstellen");
		btRestore.addActionListener(this);
		btCancel = new JButton("Abbrechen");
		btCancel.addActionListener(this);
		JPanel pnButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		pnButtons.add(btCancel);
		pnButtons.add(btRestore);
		pnDisplay = new JPanel(new GridBagLayout());
		JPanel pnBorder = new JPanel(new BorderLayout());
		pnBorder.add(pnSelectorButtons, BorderLayout.NORTH);
		pnBorder.add(UIFx.initScrollPane(pnDisplay, 15), BorderLayout.CENTER);
		pnBorder.add(pnButtons, BorderLayout.SOUTH);
		setContentPane(pnBorder);
		updateUI();
	}

	private void updateUI() {
		pnDisplay.removeAll();
		GridBagConstraints c = UIFx.initGridBagConstraints();

		boolean filterOutstanding = rbOutstanding.isSelected();
		boolean srcOnline, dstOnline, srcDirExists;
		JPanel pnSegment;
		int n = 1;
		GridBagConstraints c1;
		for (Segment segment : segments.getData()) {
			pnSegment = new JPanel(new GridBagLayout());
			c1 = UIFx.initGridBagConstraints();
			c1.weightx = 1.0;
			for (Operation op : segment.getOperations()) {
				op.setSelected(false);
				srcOnline = op.isSourceOnline();
				dstOnline = op.isTargetOnline();
				srcDirExists = op.getSource().exists();
				if (filterOutstanding && srcDirExists && !srcOnline && dstOnline) {
					pnSegment.add(new RestoreOperationPanel(op, n++), c1);
					c1.gridy++;
				} else if (!filterOutstanding && srcOnline && dstOnline) {
					pnSegment.add(new RestoreOperationPanel(op, n++), c1);
					c1.gridy++;
				} else {}
			}
			if (c1.gridy > 0) {
				pnSegment.setBorder(BorderFactory.createTitledBorder(
						BorderFactory.createLineBorder(RestoreOperationPanel.colRestore, 2, true),
						segment.getName()));
				pnDisplay.add(pnSegment, c);
				c.gridy++;
			}
		}

		UIFx.packAndCenter(this, frm);

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btRestore) {
			answer = ANSWER_RESTORE;
			setVisible(false);
		} else if (e.getSource() == btCancel) {
			setVisible(false);
		} else if (e.getSource() == miCreateSourceDirectories) {
			createMissingSourceDirectories();
			updateUI();
		} else if (e.getSource() == miCreateSourceDirectories) {
			repairDatabaseNames();
			updateUI();
		} else if (e.getSource() == rbOutstanding || e.getSource() == rbAvailable) {
			updateUI();
		}
	}

	private void createMissingSourceDirectories() {
		JPanel message = new JPanel(new GridBagLayout());
		GridBagConstraints c = UIFx.initGridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		Vector<JComponent> uiComponents = new Vector<>();
		JCheckBox ck;
		File file;
		boolean errors;
		Vector<String> dirsToCreate;
		Iterator<String> iDirs;
		JLabel lb;
		for (Segment seg : segments.getData()) {
			dirsToCreate = new Vector<>();
			for (Operation op : seg.getOperations()) {
				if (!op.getSource().exists()) {
					dirsToCreate.add(op.getSourcePath());
				}
			}
			if (dirsToCreate.size() > 0) {
				iDirs = dirsToCreate.iterator();
				while (iDirs.hasNext()) {
					uiComponents.add(new JCheckBox(iDirs.next()));
				}
				lb = new JLabel(seg.getName());
				uiComponents.add(lb);
			}
		}

		if (uiComponents.size() == 0) {
			JOptionPane.showMessageDialog(this, "Es wurden keine fehlenden Verzeichnisse gefunden.",
					"Information", JOptionPane.INFORMATION_MESSAGE);
		} else {
			Collections.reverse(uiComponents);
			for (JComponent comp : uiComponents) {
				message.add(comp, c);
				c.gridy++;
			}
			int answer = JOptionPane.showConfirmDialog(this, message, "Verzeichnisse Erzeugen",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (answer == JOptionPane.OK_OPTION) {
				errors = false;
				for (JComponent comp : uiComponents) {
					if (comp instanceof JCheckBox) {
						ck = (JCheckBox) comp;
						if (ck.isSelected()) {
							file = new File(ck.getText());
							if (!file.mkdirs()) {
								errors = true;
							}
						}
					}
				}

				if (errors) {
					JOptionPane.showMessageDialog(this,
							"Es sind Fehler beim Erzeugen der Verzeichnisse aufgetreten!", "Fehler",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}

	private void repairDatabaseNames() {

	}

	private void updateUserName() {

	}

	private void restoreConfiguration() {

	}

	private void searchOrphanedDatabases() {

	}

	public int getAnswer() {
		return answer;
	}

	public Segments getSegments() {
		return segments;
	}
}
