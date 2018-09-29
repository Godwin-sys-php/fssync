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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.NumberFormat;
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

import bug507401.DangerousPathChecker;
import net.janbuchinger.code.fssync.sync.OnlineDB;
import net.janbuchinger.code.fssync.sync.RecoverSystemDialog;
import net.janbuchinger.code.fssync.sync.RecoverSystemProcess;
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
	// private JCheckBox ckIgnoreModifiedWhenEqual;
	private JCheckBox ckElasticComparison;
	private JCheckBox ckAlwaysQuickSync;

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

	private JLabel lbRunningTimeAvgQuick;
	private JLabel lbRunningTimeQuick;
	private JLabel lbRunCountQuick;
	private JLabel lbRunningTimeAvgDeep;
	private JLabel lbRunningTimeDeep;
	private JLabel lbRunCountDeep;
	private JLabel lbRunningTimeSync;
	private JLabel lbRunningTimeAvgSync;
	private JLabel lbRunCountSync;
	private JLabel lbMiBCopiedAvg;
	private JLabel lbFilesCopiedTotal;
	private JButton btClearStats;

	public final static int CANCEL = 0;
	public final static int SAVE = 1;
	private int answer;
	private Operation operation;
	private Segments segments;

	public OperationEditorDialog(JDialog frm, Segments segments) {
		this(frm, null, segments);
	}

	public OperationEditorDialog(JFrame frm, Operation synchronisationOperation, Segments segments) {
		super(frm, "Operation", true);
		init(synchronisationOperation, segments, frm);
	}

	public OperationEditorDialog(JDialog frm, Operation synchronisationOperation, Segments segments) {
		super(frm, "Operation", true);
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

		// ckIgnoreModifiedWhenEqual = new JCheckBox(
		// "Änderungsdatum ignorieren wenn Prüfsumme und Länge gleichgeblieben");
		// ckIgnoreModifiedWhenEqual.addActionListener(this);
		ckElasticComparison = new JCheckBox("Elastischer Zeitvergleich (+/- 1 sek)");
		// ckElasticComparison.addActionListener(this);
		ckAlwaysQuickSync = new JCheckBox("Schnell Synchronisieren (Integritätsprüfung überspringen)");
		// ckAlwaysQuickSync.addActionListener(this);

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

		lbRunningTimeAvgQuick = new JLabel("---");
		lbRunningTimeQuick = new JLabel("---");
		lbRunCountQuick = new JLabel("---");
		lbRunningTimeAvgDeep = new JLabel("---");
		lbRunningTimeDeep = new JLabel("---");
		lbRunCountDeep = new JLabel("---");
		lbRunningTimeSync = new JLabel("---");
		lbRunningTimeAvgSync = new JLabel("---");
		lbRunCountSync = new JLabel("---");
		lbMiBCopiedAvg = new JLabel("---");
		lbFilesCopiedTotal = new JLabel("---");
		btClearStats = new JButton("Statistik Zurücksetzen");
		btClearStats.addActionListener(this);

		updateStatsLabels();

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
			// ckIgnoreModifiedWhenEqual.setSelected(operation.isIgnoreModifiedWhenEqual());
			ckElasticComparison.setSelected(operation.isCompareElastic());
			ckAlwaysQuickSync.setSelected(operation.isAlwaysQuickSync());
			// ckIgnoreModifiedWhenEqual.setEnabled(!ckAlwaysQuickSync.isSelected());

			int priority = operation.getPriorityOnConflict();
			if (priority == Operation.PRIORITY_NEW) {
				rbPrioNew.setSelected(true);
			} else if (priority == Operation.PRIORITY_OLD) {
				rbPrioOld.setSelected(true);
			} else if (priority == Operation.PRIORITY_TARGET) {
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
			if (intervalMode == Operation.INTERVAL_DAYS) {
				rbIntervalDays.setSelected(true);
			} else if (intervalMode == Operation.INTERVAL_HOURS) {
				rbIntervalHours.setSelected(true);
			} else if (intervalMode == Operation.INTERVAL_MINUTES) {
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
		pnOptions.add(ckElasticComparison, c);
		// c.gridy++;
		// pnOptions.add(ckIgnoreModifiedWhenEqual, c);
		c.gridy++;
		pnOptions.add(ckAlwaysQuickSync, c);
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
		pnTiming.add(new JLabel("Intervall"), c);
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

		JPanel pnStatistics = new JPanel(new GridBagLayout());
		c = UIFx.initGridBagConstraints();
		pnStatistics.add(new JLabel("Analyse Schnell Durchschnitt"), c);
		c.gridx++;
		c.weightx = 1;
		pnStatistics.add(lbRunningTimeAvgQuick, c);
		c.weightx = 0;
		c.gridx = 0;
		c.gridy++;
		pnStatistics.add(new JLabel("Analyse Schnell Gesamt"), c);
		c.gridx++;
		pnStatistics.add(lbRunningTimeQuick, c);
		c.gridx = 0;
		c.gridy++;
		pnStatistics.add(new JLabel("Anzahl Analysen Schnell"), c);
		c.gridx++;
		pnStatistics.add(lbRunCountQuick, c);
		c.gridx = 0;
		c.gridy++;
		pnStatistics.add(new JLabel("Analyse Genau Durchschnitt"), c);
		c.gridx++;
		pnStatistics.add(lbRunningTimeAvgDeep, c);
		c.gridx = 0;
		c.gridy++;
		pnStatistics.add(new JLabel("Analyse Genau Gesamt"), c);
		c.gridx++;
		pnStatistics.add(lbRunningTimeDeep, c);
		c.gridx = 0;
		c.gridy++;
		pnStatistics.add(new JLabel("Anzahl Analysen Genau"), c);
		c.gridx++;
		pnStatistics.add(lbRunCountDeep, c);
		c.gridx = 0;
		c.gridy++;
		pnStatistics.add(new JLabel("Synchronisation Durchschnitt"), c);
		c.gridx++;
		pnStatistics.add(lbRunningTimeAvgSync, c);
		c.gridx = 0;
		c.gridy++;
		pnStatistics.add(new JLabel("Synchronisation Gesamt"), c);
		c.gridx++;
		pnStatistics.add(lbRunningTimeSync, c);
		c.gridx = 0;
		c.gridy++;
		pnStatistics.add(new JLabel("Anzahl Synchronisationen"), c);
		c.gridx++;
		pnStatistics.add(lbRunCountSync, c);
		c.gridx = 0;
		c.gridy++;
		pnStatistics.add(new JLabel("MiB Kopiert Durchschnitt"), c);
		c.gridx++;
		pnStatistics.add(lbMiBCopiedAvg, c);
		c.gridx = 0;
		c.gridy++;
		pnStatistics.add(new JLabel("Insgesamt Kopierte Dateien"), c);
		c.gridx++;
		pnStatistics.add(lbFilesCopiedTotal, c);
		c.gridx++;
		c.gridheight = 2;
		pnStatistics.add(btClearStats, c);

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

		JPanel pnStatsStretch = new JPanel(new BorderLayout());
		pnStatsStretch.add(pnStatistics, BorderLayout.NORTH);
		pnStatsStretch.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

		JTabbedPane tpOperation = new JTabbedPane();
		tpOperation.setPreferredSize(new Dimension(525, 250));
		tpOperation.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
		tpOperation.setTabPlacement(JTabbedPane.TOP);
		tpOperation.add("Synchronisation", UIFx.initScrollPane(pnSyncStretch, 15));
		tpOperation.add("Ausnahmen", pnExclude);
		tpOperation.add("Optionen", UIFx.initScrollPane(pnFurtherStretch, 15));
		tpOperation.add("Timing", UIFx.initScrollPane(pnTimingStretch, 15));
		if (operation != null) {
			tpOperation.add("Statistik", UIFx.initScrollPane(pnStatsStretch, 15));
		}

		JPanel pnContent = new JPanel(new BorderLayout());
		pnContent.add(tpOperation, BorderLayout.CENTER);
		pnContent.add(pnButtons, BorderLayout.SOUTH);

		setContentPane(pnContent);
		UIFx.packAndCenter(this, frm);
	}

	private void updateStatsLabels() {
		if (operation != null) {
			NumberFormat nf = NumberFormat.getNumberInstance();
			nf.setMaximumFractionDigits(0);
			nf.setMinimumIntegerDigits(0);
			if (operation.getRunCountQuickAnalysis() > 0) {
				lbRunningTimeAvgQuick.setText(
						UIFx.formatMillisAsHoursMinutesSeconds(operation.getAverageAnalyseTimeQuick()));
				lbRunningTimeQuick.setText(
						UIFx.formatMillisAsHoursMinutesSeconds(operation.getRunningTimeQuickAnalysis()));
				lbRunCountQuick.setText(nf.format(operation.getRunCountQuickAnalysis()));

			}
			if (operation.getRunCountDeepAnalysis() > 0) {
				lbRunningTimeAvgDeep.setText(
						UIFx.formatMillisAsHoursMinutesSeconds(operation.getAverageAnalyseTimeDeep()));
				lbRunningTimeDeep.setText(
						UIFx.formatMillisAsHoursMinutesSeconds(operation.getRunningTimeDeepAnalysis()));
				lbRunCountDeep.setText(nf.format(operation.getRunCountDeepAnalysis()));

			}
			if (operation.getRunCountSynchronization() > 0) {
				lbRunningTimeSync
						.setText(UIFx.formatMillisAsHoursMinutesSeconds(operation.getRunningTimeDataCopy()));
				lbRunningTimeAvgSync
						.setText(UIFx.formatMillisAsHoursMinutesSeconds(operation.getAverageSyncTime()));

				lbRunCountSync.setText(nf.format(operation.getRunCountSynchronization()));
				lbFilesCopiedTotal.setText(nf.format(operation.getTotalFilesCopiedCount()));
				nf.setMaximumFractionDigits(2);
				nf.setMinimumFractionDigits(2);
				lbMiBCopiedAvg.setText(nf.format(operation.getTransferredMegaBytesAverage()));
			}
		}
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
			try {
				DangerousPathChecker dpc = new DangerousPathChecker();
				if (dpc.isDangerous(source)) {
					JOptionPane.showMessageDialog(this,
							"Dieser Pfad kann nicht als Quellverzeichnis genutzt werden.", "Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
			} catch (IllegalStateException | IllegalArgumentException e2) {}
			File target = new File(tfDestination.getPath());
			if (segments.createsCircularRelation(source, target, operation)) {
				JOptionPane.showMessageDialog(this, "Zirkulärbezug nicht erlaubt.", "Fehler",
						JOptionPane.ERROR_MESSAGE);
				return;
			} else if (!ckBidirectional.isSelected()
					&& segments.targetIsBidirectionalSource(target, operation)) {
				int answer = JOptionPane.showConfirmDialog(this,
						"Es wird davon abgeraten ein Quellverzeichnis einer bidirektionalen Operation "
								+ "als Zielverzeichnis einer unidirektionalen Operation zu verwenden, "
								+ "trotzdem fortfahren?",
						"Warnung", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				if (answer == JOptionPane.CANCEL_OPTION) {
					return;
				}

			}
			if (target.exists()) {
				boolean targetAlreadyInUse = false;
				for (Segment s : segments.getData()) {
					for (Operation o : s.getOperations()) {
						if (o.getTargetPath().equals(target.getPath())) {
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
					if (targetAlreadyInUse) {
						break;
					}
				}
				if (targetAlreadyInUse) {
					JOptionPane.showMessageDialog(this, "Zielverzeichnis ist bereits in Verwendung!", "Fehler",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
			} else { // target directory doesn't exist
				if (JOptionPane.showConfirmDialog(this, "Soll das Zielverzeichnis erstellt werden?",
						"Ziel existiert nicht", JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
					if (!target.mkdirs()) {
						JOptionPane.showMessageDialog(this, "Zielverzeichnis konnte nicht erstellt werden!",
								"Fehler", JOptionPane.ERROR_MESSAGE);
						return;
					}
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
				File[] targetFiles = target.listFiles();
				boolean spider = true;
				if (targetFiles != null) {
					if (targetFiles.length > 0) {
						int answer = JOptionPane.showConfirmDialog(this,
								"Es wurden Dateien im Zielordner aber keine Datenbank gefunden,"
										+ "\nEs wird jetzt das Zieldateisystem eingelesen um bereits "
										+ "bestehende Dateipaare zu finden."
										+ "\nDieser Prozess kann nicht abgebrochen werden.",
								"Warnung", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
						if (answer == JOptionPane.CANCEL_OPTION) {
							return;
						}
					} else {
						spider = false;
					}
				}
				try {
					OnlineDB.initNewDB(source, target);
					FSFx.hideWindowsFile(fsdb);
					if (spider) {
						RecoverSystemDialog rsd = new RecoverSystemDialog(this);
						RecoverSystemProcess rsp = new RecoverSystemProcess(source, target, rsd);
						rsp.execute();
						if (!rsd.isDone()) {
							rsd.setVisible(true);
						}
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			boolean manageVersions = ckVersionManagement.isSelected();
			Vector<String> exclude = lmExclude.getStrings();

			Iterator<String> iExc = exclude.iterator();
			while (iExc.hasNext()) {
				if (!new File(source, iExc.next()).exists()) {
					iExc.remove();
				}
			}

			boolean syncBidirectional = ckBidirectional.isSelected();
			// boolean ignoreModifiedWhenEqual = ckIgnoreModifiedWhenEqual.isSelected();
			boolean elasticComparison = ckElasticComparison.isSelected();
			boolean alwaysQuickSync = ckAlwaysQuickSync.isSelected();

			int priorityOnConflict = -1;
			if (rbPrioNew.isSelected()) {
				priorityOnConflict = Operation.PRIORITY_NEW;
			} else if (rbPrioOld.isSelected()) {
				priorityOnConflict = Operation.PRIORITY_OLD;
			} else if (rbPrioTarget.isSelected()) {
				priorityOnConflict = Operation.PRIORITY_TARGET;
			} else {
				priorityOnConflict = Operation.PRIORITY_SOURCE;
			}

			int interval = 0;
			try {
				interval = Integer.parseInt(tfInterval.getText());
			} catch (NumberFormatException e2) {
				JOptionPane.showMessageDialog(this, "Bitte Ganzzahl als Intervall eingeben", "Fehler",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			boolean remind = ckRemind.isSelected() && interval != 0 ? ckRemind.isSelected() : false;

			int intervalMode = Operation.INTERVAL_DAYS;
			if (rbIntervalHours.isSelected()) {
				intervalMode = Operation.INTERVAL_HOURS;
			} else if (rbIntervalMinutes.isSelected()) {
				intervalMode = Operation.INTERVAL_MINUTES;
			}

			if (operation == null) {
				operation = new Operation(source, target, manageVersions, exclude, syncBidirectional,
						elasticComparison, alwaysQuickSync, priorityOnConflict, interval, intervalMode,
						remind);
			} else {
				operation.setSource(source);
				operation.setTarget(target);

				operation.setExcludes(exclude);

				operation.setSyncBidirectional(syncBidirectional);
				operation.setPriorityOnConflict(priorityOnConflict);
				// operation.setIgnoreModifiedWhenEqual(ignoreModifiedWhenEqual);
				operation.setCompareElastic(elasticComparison);
				operation.setAlwaysQuickSync(alwaysQuickSync);
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
				if (JOptionPane.showConfirmDialog(this,
						lmExclude.get(jlExclude.getSelectedIndex()) + " wirklich Entfernen?",
						"Eintrag Löschen", JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
					lmExclude.remove(jlExclude.getSelectedIndex());
					jlExclude.setSelectedIndices(new int[0]);
				}
			}
		} else if (e.getSource() == btClearStats) {
			int answer = JOptionPane.showConfirmDialog(this,
					"Sollen die Statistiken wirklich zurückgesetzt werden?", "Frage",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (answer == JOptionPane.YES_OPTION) {
				operation.clearStats();
				updateStatsLabels();
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