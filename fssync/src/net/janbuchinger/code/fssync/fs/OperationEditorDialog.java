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
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import net.janbuchinger.code.fssync.fs.sync.OnlineDB;
import net.janbuchinger.code.fssync.fs.sync.RecoverSystemDialog;
import net.janbuchinger.code.fssync.fs.sync.RecoverSystemProcess;
import net.janbuchinger.code.fssync.fs.sync.ui.CopyActionTableModel;
import net.janbuchinger.code.mishmash.FSFx;
import net.janbuchinger.code.mishmash.ui.UIFx;
import net.janbuchinger.code.mishmash.ui.dialog.DialogEscapeHook;
import net.janbuchinger.code.mishmash.ui.dialog.dirChooser.DirChooserDialog;
import net.janbuchinger.code.mishmash.ui.models.StringListModel;
import net.janbuchinger.code.mishmash.ui.userInput.FolderPathTextField;

@SuppressWarnings("serial")
public class OperationEditorDialog extends JDialog implements ActionListener {
	private JButton btOk, btCancel, btAddExcl, btRemExcl;
	private FolderPathTextField tfSource;
	private FolderPathTextField tfDestination;
	private JList<String> jlExclude;
	private StringListModel lmExclude;
	private JCheckBox ckVersionManagement;
	private JCheckBox ckBidirectional;
	private JCheckBox ckIgnoreModifiedWhenEqual;

	private JRadioButton rbPrioSource;
	private JRadioButton rbPrioTarget;
	private JRadioButton rbPrioNew;
	private JRadioButton rbPrioOld;

	private JTextField tfLastSynced;
	private JTextField tfInterval;
	private JCheckBox ckRemind;

	private JRadioButton rbIntervalDays;
	private JRadioButton rbIntervalHours;
	private JRadioButton rbIntervalMinutes;

	public final static int CANCEL = 0;
	public final static int SAVE = 1;
	private int answer;
	private Operation operation;
	private Segments segments;

	public OperationEditorDialog(JDialog frm, Segments segments) {
		this(frm, null, segments);
	}

	public OperationEditorDialog(JFrame frm, Operation synchronisationOperation, Segments segments) {
		super(frm, "Synchronisation", true);
		init(synchronisationOperation, segments, frm);
	}

	public OperationEditorDialog(JDialog frm, Operation synchronisationOperation, Segments segments) {
		super(frm, "Synchronisation", true);
		init(synchronisationOperation, segments, frm);
	}

	private final void init(Operation operation, Segments segments, Window frm) {
		this.segments = segments;
		this.operation = operation;

		new DialogEscapeHook(this);

		answer = CANCEL;

		int top = 10, left = 30, bottom = 0, right = 3;
		JLabel lbSync = new JLabel("Synchronisation");
		lbSync.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
		JLabel lbExceptions = new JLabel("Ausnahmen (nicht Synchronisieren)");
		lbExceptions.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
		JLabel lbVersions = new JLabel("Versionen");
		lbVersions.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
		tfSource = new FolderPathTextField(this);
		tfDestination = new FolderPathTextField(this);

		lmExclude = new StringListModel();
		jlExclude = new JList<String>(lmExclude);

		ckVersionManagement = new JCheckBox("Dateiversionen Speichern");

		ckBidirectional = new JCheckBox("Bidirektional Synchronisieren");

		ckIgnoreModifiedWhenEqual = new JCheckBox(
				"Änderungsdatum ignorieren wenn Prüfsumme und Länge gleichgeblieben");

		ButtonGroup bg = new ButtonGroup();

		rbPrioSource = new JRadioButton("Quelle");
		bg.add(rbPrioSource);
		rbPrioTarget = new JRadioButton("Ziel");
		bg.add(rbPrioTarget);
		rbPrioNew = new JRadioButton("Neu");
		bg.add(rbPrioNew);
		rbPrioOld = new JRadioButton("Alt");
		bg.add(rbPrioOld);

		JPanel pnPriorityOnConflict = new JPanel(new GridLayout(1, 4));
		pnPriorityOnConflict.add(rbPrioSource);
		pnPriorityOnConflict.add(rbPrioTarget);
		pnPriorityOnConflict.add(rbPrioNew);
		pnPriorityOnConflict.add(rbPrioOld);

		tfLastSynced = new JTextField();
		tfLastSynced.setEditable(false);
		tfLastSynced.setText("---");
		tfInterval = new JTextField();
		tfInterval.setText("0");
		ckRemind = new JCheckBox("Erinnern");

		bg = new ButtonGroup();
		rbIntervalDays = new JRadioButton("Tage");
		bg.add(rbIntervalDays);
		rbIntervalHours = new JRadioButton("Stunden");
		bg.add(rbIntervalHours);
		rbIntervalMinutes = new JRadioButton("Minuten");
		bg.add(rbIntervalMinutes);

		JPanel pnIntervalMode = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		pnIntervalMode.add(rbIntervalDays);
		pnIntervalMode.add(rbIntervalHours);
		pnIntervalMode.add(rbIntervalMinutes);

		btAddExcl = new JButton("+");
		btAddExcl.addActionListener(this);
		btRemExcl = new JButton("-");
		btRemExcl.addActionListener(this);
		JPanel pnExclButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		pnExclButtons.add(btAddExcl);
		pnExclButtons.add(btRemExcl);

		btOk = new JButton("Speichern");
		btOk.addActionListener(this);
		btCancel = new JButton("Abbrechen");
		btCancel.addActionListener(this);
		JPanel pnButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		pnButtons.add(btCancel);
		pnButtons.add(btOk);

		if (operation != null) {
			tfSource.setPath(operation.getSource().getPath());
			tfDestination.setPath(operation.getTarget().getPath());
			lmExclude.setStrings(operation.getExcludes());
			ckVersionManagement.setSelected(operation.isManageVersions());
			ckBidirectional.setSelected(operation.isSyncBidirectional());
			ckIgnoreModifiedWhenEqual.setSelected(operation.isIgnoreModifiedWhenEqual());

			int priority = operation.getPriorityOnConflict();
			if (priority == CopyActionTableModel.sel_new) {
				rbPrioNew.setSelected(true);
			} else if (priority == CopyActionTableModel.sel_old) {
				rbPrioOld.setSelected(true);
			} else if (priority == CopyActionTableModel.sel_destination) {
				rbPrioTarget.setSelected(true);
			} else {
				rbPrioSource.setSelected(true);
			}
			if (operation.getLastSynced() > 0) {
				tfLastSynced.setText(UIFx.initDisplayDateTimeFormat().format(operation.getLastSynced()));
			}
			tfInterval.setText(operation.getInterval() + "");
			ckRemind.setSelected(operation.isRemind());
			int intervalMode = operation.getIntervalMode();
			if (intervalMode == Operation.MD_DAYS) {
				rbIntervalDays.setSelected(true);
			} else if (intervalMode == Operation.MD_HOURS) {
				rbIntervalHours.setSelected(true);
			} else if (intervalMode == Operation.MD_MINUTES) {
				rbIntervalMinutes.setSelected(true);
			}
		} else {
			rbPrioSource.setSelected(true);
			rbIntervalDays.setSelected(true);
		}

		GridBagConstraints c = UIFx.initGridBagConstraints();
		c.weightx = 1;
		JPanel pnSync = new JPanel(new GridBagLayout());

		pnSync.add(new JLabel("Quelle"), c);
		c.gridy++;
		pnSync.add(tfSource, c);
		c.gridy++;
		pnSync.add(new JLabel("Ziel"), c);
		c.gridy++;
		pnSync.add(tfDestination, c);
		c.gridy++;

		c = UIFx.initGridBagConstraints();
		c.weightx = 1;
		c.weighty = 1;
		JPanel pnExclude = new JPanel(new GridBagLayout());

		pnExclude.add(UIFx.initScrollPane(jlExclude, 15), c);
		c.weighty = 0;
		c.gridy++;
		pnExclude.add(pnExclButtons, c);

		c = UIFx.initGridBagConstraints();
		c.weightx = 1;
		JPanel pnOptions = new JPanel(new GridBagLayout());

		// pnOptions.add(ckVersionManagement, c);
		// c.gridy++;
		pnOptions.add(ckBidirectional, c);
		c.gridy++;
		pnOptions.add(new JLabel("Priorität bei Konflikt"), c);
		c.gridy++;
		pnOptions.add(pnPriorityOnConflict, c);
		c.gridy++;
		pnOptions.add(ckIgnoreModifiedWhenEqual, c);
		c.gridy++;

		c = UIFx.initGridBagConstraints();
		JPanel pnTiming = new JPanel(new GridBagLayout());

		c.weightx = 0;
		pnTiming.add(new JLabel("Letzte Synchronisierung "), c);
		c.gridx++;
		c.weightx = 1;
		c.gridwidth = 2;
		pnTiming.add(tfLastSynced, c);
		c.weightx = 0;
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		pnTiming.add(new JLabel("Intervall (Tage)"), c);
		c.weightx = 1;
		c.gridx++;
		pnTiming.add(tfInterval, c);
		c.gridx++;
		c.weightx = 0;
		pnTiming.add(pnIntervalMode, c);
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 3;
		pnTiming.add(ckRemind, c);

		JPanel pnSyncStretch = new JPanel(new BorderLayout());
		pnSyncStretch.add(pnSync, BorderLayout.NORTH);
		pnSyncStretch.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

		pnExclude.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

		JPanel pnFurtherStretch = new JPanel(new BorderLayout());
		pnFurtherStretch.add(pnOptions, BorderLayout.NORTH);
		pnFurtherStretch.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

		JPanel pnTimingStretch = new JPanel(new BorderLayout());
		pnTimingStretch.add(pnTiming, BorderLayout.NORTH);
		pnTimingStretch.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

		JTabbedPane tpOperation = new JTabbedPane();
		tpOperation.add("Synchronisation", UIFx.initScrollPane(pnSyncStretch, 15));
		tpOperation.add("Ausnahmen", pnExclude);
		tpOperation.add("Optionen", UIFx.initScrollPane(pnFurtherStretch, 15));
		tpOperation.add("Timing", UIFx.initScrollPane(pnTimingStretch, 15));

		JPanel pnContent = new JPanel(new BorderLayout());
		pnContent.add(tpOperation, BorderLayout.CENTER);
		pnContent.add(pnButtons, BorderLayout.SOUTH);

		setContentPane(pnContent);
		UIFx.packAndCenter(this, frm);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btOk) {
			File source = new File(tfSource.getPath());

			if (!source.exists()) {
				JOptionPane.showMessageDialog(this, "Bitte Quelle Wählen.", "Fehler",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (!source.isDirectory()) {
				JOptionPane.showMessageDialog(this, "Bitte Verzeichnis als Quelle Wählen.", "Fehler",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			File target = new File(tfDestination.getPath());

			Iterator<Segment> iSeg = segments.iterator();
			Iterator<Operation> iOp;
			Operation o;

			boolean targetAlreadyInUse = false;
			while (iSeg.hasNext()) {
				iOp = iSeg.next().iterator();
				while (iOp.hasNext()) {
					o = iOp.next();
					if (o.getRemotePath().equals(target.getPath())) {
						if (operation != null) {
							if (o != operation) {
								targetAlreadyInUse = true;
								break;
							}
						} else {
							targetAlreadyInUse = true;
							break;
						}
					}
				}
				if (targetAlreadyInUse)
					break;
			}
			if (targetAlreadyInUse) {
				JOptionPane.showMessageDialog(this, "Zielverzeichnis ist bereits in Verwendung!", "Fehler",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (!target.exists()) {
				if (JOptionPane.showConfirmDialog(this, "Soll das Zielverzeichnis erstellt werden?",
						"Ziel existiert nicht", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
					target.mkdirs();
				} else {
					return;
				}
			}
			if (!target.isDirectory()) {
				JOptionPane.showMessageDialog(this, "Bitte Verzeichnis als Ziel Wählen.", "Fehler",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			File fsdb = new File(target, ".fs.db");
			if (!fsdb.exists()) {
				try {
					OnlineDB.initNewDB(source, target);
					if (JOptionPane.showConfirmDialog(this,
							"Soll das Zielverzeichnis eingelesen und mit dem Quellsystem verglichen werden?",
							"Neue Datenbankdatei Aufbauen", JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
						RecoverSystemDialog rsd = new RecoverSystemDialog(this);
						RecoverSystemProcess rsp = new RecoverSystemProcess(source, target, rsd);
						rsp.execute();
						if (!rsd.isDone())
							rsd.setVisible(true);
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				FSFx.hideWindowsFile(fsdb);
			}
			boolean manageVersions = ckVersionManagement.isSelected();
			Vector<String> exclude = lmExclude.getStrings();

			Iterator<String> iExc = exclude.iterator();
			while (iExc.hasNext()) {
				if (!new File(source, iExc.next()).exists())
					iExc.remove();
			}

			boolean syncBidirectional = ckBidirectional.isSelected();
			boolean ignoreModifiedWhenEqual = ckIgnoreModifiedWhenEqual.isSelected();
			int priorityOnConflict = -1;
			if (rbPrioNew.isSelected()) {
				priorityOnConflict = CopyActionTableModel.sel_new;
			} else if (rbPrioOld.isSelected()) {
				priorityOnConflict = CopyActionTableModel.sel_old;
			} else if (rbPrioTarget.isSelected()) {
				priorityOnConflict = CopyActionTableModel.sel_destination;
			} else {
				priorityOnConflict = CopyActionTableModel.sel_source;
			}

			long lastSynced = operation != null ? operation.getLastSynced() : 0;
			int interval = 0;
			try {
				interval = Integer.parseInt(tfInterval.getText());
			} catch (NumberFormatException e2) {
				JOptionPane.showMessageDialog(this, "Bitte Ganzzahl als Intervall eingeben", "Fehler",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			boolean remind = ckRemind.isSelected() && interval != 0 ? ckRemind.isSelected() : false;
			boolean reminded = operation != null ? operation.isReminded() : false;

			int intervalMode = Operation.MD_DAYS;
			if (rbIntervalHours.isSelected()) {
				intervalMode = Operation.MD_HOURS;
			} else if (rbIntervalMinutes.isSelected()) {
				intervalMode = Operation.MD_MINUTES;
			}

			if (operation == null) {
				operation = new Operation(source, target, manageVersions, exclude, syncBidirectional,
						ignoreModifiedWhenEqual, priorityOnConflict, lastSynced, interval, intervalMode,
						remind, reminded);
			} else {
				operation.setSource(source);
				operation.setTarget(target);

				operation.setExcludes(exclude);

				operation.setSyncBidirectional(syncBidirectional);
				operation.setPriorityOnConflict(priorityOnConflict);
				operation.setIgnoreModifiedWhenEqual(ignoreModifiedWhenEqual);
				operation.setManageVersions(manageVersions);

				operation.setInterval(interval);
				operation.setIntervalMode(intervalMode);
				operation.setRemind(remind);
			}

			answer = SAVE;
			setVisible(false);
		} else if (e.getSource() == btCancel) {
			setVisible(false);
		} else if (e.getSource() == btAddExcl) {
			File source = new File(tfSource.getPath());
			if (!source.exists()) {
				JOptionPane.showMessageDialog(this, "Bitte erst Quelle Wählen", "Fehler",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			DirChooserDialog dcd = new DirChooserDialog(this, source, "");
			dcd.setVisible(true);
			if (dcd.getAnswer() == DirChooserDialog.OK) {
				Iterator<String> iStr = lmExclude.getStrings().iterator();
				String s, sAdd = dcd.getRelativePath();
				while (iStr.hasNext()) {
					s = iStr.next();
					if (s.equals(sAdd)) {
						JOptionPane.showMessageDialog(this, "Verzeichnis bereits in der Liste.", "Fehler",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				lmExclude.addElement(sAdd);
			}
		} else if (e.getSource() == btRemExcl) {
			if (jlExclude.getSelectedIndex() > -1) {
				if (JOptionPane.showConfirmDialog(this, lmExclude.get(jlExclude.getSelectedIndex())
						+ " wirklich Entfernen?", "Eintrag Löschen", JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
					lmExclude.remove(jlExclude.getSelectedIndex());
					jlExclude.setSelectedIndices(new int[0]);
				}
			}
		}
	}

	public final int getAnswer() {
		return answer;
	}

	public final Operation getOperation() {
		return operation;
	}
}