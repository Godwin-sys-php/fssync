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
package net.janbuchinger.code.fssync.fs.sync.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;

import net.janbuchinger.code.fssync.fs.sync.OperationSummary;
import net.janbuchinger.code.mishmash.ui.UIFx;

@SuppressWarnings("serial")
public final class OperationSummaryDialog extends JDialog implements ActionListener, ChangeListener {

	private final JButton btOk, btCancel, btPrioSource, btPrioDestination, btPrioNew, btPrioOld, btSelectAll,
			btSelectNone, btSelectSource, btSelectDestination;
	private final JTable tbCopy, tbDelete;
	private final CopyActionTableModel tmCopy;
	private final DeleteActionTableModel tmDelete;
	private final JTable tbCorruptSource, tbCorruptDestination, tbLost;
	private final FileTableModel tmCorruptSource, tmCorruptDestination, tmLost;

	private final JTabbedPane tpActions;

	private boolean approved;

	// private final OperationSummary operationSummary;

	private final OverviewPanel overviewPanel;

	// private boolean isBiDirectional;

	public OperationSummaryDialog(JDialog parent, OperationSummary operationSummary, boolean isBiDirectional) {
		super(parent, "Zusammenfassung", true);

		// this.isBiDirectional = isBiDirectional;
		// this.operationSummary = operationSummary;

		approved = false;

		tmCopy = new CopyActionTableModel(operationSummary.getCopyActions(), operationSummary);
		tbCopy = new JTable(tmCopy);
		tbCopy.setDefaultRenderer(String.class, new CopyActionTableCellRenderer());

		btPrioSource = new JButton("Quelle");
		btPrioSource.addActionListener(this);

		btPrioDestination = new JButton("Ziel");
		btPrioDestination.addActionListener(this);

		btPrioNew = new JButton("Neu");
		btPrioNew.addActionListener(this);

		btPrioOld = new JButton("Alt");
		btPrioOld.addActionListener(this);

		JToolBar toolBarCopy = new JToolBar();
		toolBarCopy.setFloatable(false);
		toolBarCopy.add(new JLabel("Priorität" + " "));
		toolBarCopy.add(btPrioSource);
		toolBarCopy.add(btPrioDestination);
		toolBarCopy.add(btPrioNew);
		toolBarCopy.add(btPrioOld);

		btSelectAll = new JButton("Alle");
		btSelectAll.addActionListener(this);

		btSelectNone = new JButton("Keine");
		btSelectNone.addActionListener(this);

		btSelectSource = new JButton(">>");
		btSelectSource.addActionListener(this);

		btSelectDestination = new JButton("<<");
		btSelectDestination.addActionListener(this);

		JToolBar toolBarSelect = new JToolBar();
		toolBarSelect.setFloatable(false);
		toolBarSelect.add(new JLabel("Auswählen" + " "));
		toolBarSelect.add(btSelectAll);
		toolBarSelect.add(btSelectNone);
		if(isBiDirectional && !operationSummary.isRestore()) {
			toolBarSelect.add(btSelectSource);
			toolBarSelect.add(btSelectDestination);
		}

		JPanel pnTbCopyA = new JPanel(new BorderLayout());
		pnTbCopyA.add(toolBarSelect, BorderLayout.NORTH);

		JPanel pnTbCopyB = new JPanel(new BorderLayout());
		if (operationSummary.getCopyActionsDuplicates().size() > 0)
			pnTbCopyB.add(toolBarCopy, BorderLayout.NORTH);
		pnTbCopyB.add(UIFx.initScrollPane(tbCopy, 15), BorderLayout.CENTER);

		pnTbCopyA.add(pnTbCopyB, BorderLayout.CENTER);

		tmDelete = new DeleteActionTableModel(operationSummary.getDeleteActions(), operationSummary);
		tbDelete = new JTable(tmDelete);
		tbDelete.setDefaultRenderer(String.class, new DeleteActionTableCellRenderer(operationSummary.isRestore()));

		tmCorruptSource = new FileTableModel(operationSummary.getCorruptFilesSource());
		tbCorruptSource = new JTable(tmCorruptSource);

		tmCorruptDestination = new FileTableModel(operationSummary.getCorruptFilesDestination());
		tbCorruptDestination = new JTable(tmCorruptDestination);

		tmLost = new FileTableModel(operationSummary.getLostFiles());
		tbLost = new JTable(tmLost);

		tpActions = new JTabbedPane();
		tpActions.addChangeListener(this);

		overviewPanel = new OverviewPanel(operationSummary, isBiDirectional);

		tpActions.add("Übersicht", UIFx.initScrollPane(overviewPanel, 15));
		if (operationSummary.getCorruptFilesSource().size() > 0)
			tpActions.add("Korrupt (Quelle)", UIFx.initScrollPane(tbCorruptSource, 15));
		if (operationSummary.getCorruptFilesSource().size() > 0)
			tpActions.add("Korrupt (Ziel)", UIFx.initScrollPane(tbCorruptDestination, 15));
		if (operationSummary.getLostFiles().size() > 0)
			tpActions.add("Verloren", UIFx.initScrollPane(tbLost, 15));
		if (operationSummary.getCopyActions().size() > 0)
			tpActions.add("Kopieren", pnTbCopyA);
		if (operationSummary.getDeleteActions().size() > 0)
			tpActions.add("Löschen", UIFx.initScrollPane(tbDelete, 15));

		btOk = new JButton("Ok");
		btOk.addActionListener(this);
		btCancel = new JButton("Abbrechen");
		btCancel.addActionListener(this);
		JPanel pnButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		pnButtons.add(btCancel);
		pnButtons.add(btOk);

		JPanel pnContent = new JPanel(new BorderLayout());

		pnContent.add(tpActions, BorderLayout.CENTER);
		pnContent.add(pnButtons, BorderLayout.SOUTH);

		setContentPane(pnContent);

		UIFx.sizeAndCenter(this, parent, 0.7, 0.8);

		tbCopy.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tbCopy.getColumnModel().getColumn(0).setPreferredWidth(1);
		tbCopy.getColumnModel().getColumn(1).setPreferredWidth((int) (getWidth() * 0.7));
		tbCopy.getColumnModel().getColumn(2).setPreferredWidth(100);
		tbCopy.getColumnModel().getColumn(3).setPreferredWidth(150);

		tbDelete.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tbDelete.getColumnModel().getColumn(0).setPreferredWidth(1);
		tbDelete.getColumnModel().getColumn(1).setPreferredWidth((int) (getWidth() * 0.7));
		tbDelete.getColumnModel().getColumn(2).setPreferredWidth(100);
		tbDelete.getColumnModel().getColumn(3).setPreferredWidth(150);

		tbCorruptSource.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tbCorruptSource.getColumnModel().getColumn(0).setPreferredWidth(250);
		tbCorruptSource.getColumnModel().getColumn(1).setPreferredWidth((int) (getWidth() * 0.5));
		tbCorruptSource.getColumnModel().getColumn(2).setPreferredWidth(100);
		tbCorruptSource.getColumnModel().getColumn(3).setPreferredWidth(150);

		DefaultTableCellRenderer cr = new DefaultTableCellRenderer();
		cr.setHorizontalAlignment(JLabel.RIGHT);

		tbCorruptSource.getColumnModel().getColumn(2).setCellRenderer(cr);
		tbCorruptSource.getColumnModel().getColumn(3).setCellRenderer(cr);

		tbCorruptDestination.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tbCorruptDestination.getColumnModel().getColumn(0).setPreferredWidth(250);
		tbCorruptDestination.getColumnModel().getColumn(1).setPreferredWidth((int) (getWidth() * 0.5));
		tbCorruptDestination.getColumnModel().getColumn(2).setPreferredWidth(100);
		tbCorruptDestination.getColumnModel().getColumn(3).setPreferredWidth(150);

		tbCorruptDestination.getColumnModel().getColumn(2).setCellRenderer(cr);
		tbCorruptDestination.getColumnModel().getColumn(3).setCellRenderer(cr);

		tbLost.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tbLost.getColumnModel().getColumn(0).setPreferredWidth(250);
		tbLost.getColumnModel().getColumn(1).setPreferredWidth((int) (getWidth() * 0.5));
		tbLost.getColumnModel().getColumn(2).setPreferredWidth(100);
		tbLost.getColumnModel().getColumn(3).setPreferredWidth(150);

		tbLost.getColumnModel().getColumn(2).setCellRenderer(cr);
		tbLost.getColumnModel().getColumn(3).setCellRenderer(cr);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btOk) {
			approved = true;
			setVisible(false);
		} else if (e.getSource() == btCancel) {
			setVisible(false);
		} else if (e.getSource() == btSelectAll) {
			tmCopy.select(CopyActionTableModel.sel_all);
		} else if (e.getSource() == btSelectNone) {
			tmCopy.select(CopyActionTableModel.sel_none);
		} else if (e.getSource() == btSelectSource) {
			tmCopy.select(CopyActionTableModel.sel_dir_source);
		} else if (e.getSource() == btSelectDestination) {
			tmCopy.select(CopyActionTableModel.sel_dir_destination);
		} else if (e.getSource() == btPrioSource) {
			tmCopy.select(CopyActionTableModel.sel_source);
		} else if (e.getSource() == btPrioDestination) {
			tmCopy.select(CopyActionTableModel.sel_destination);
		} else if (e.getSource() == btPrioNew) {
			tmCopy.select(CopyActionTableModel.sel_new);
		} else if (e.getSource() == btPrioOld) {
			tmCopy.select(CopyActionTableModel.sel_old);
		}
	}

	public boolean isApproved() {
		return approved;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (tpActions.getSelectedIndex() == 0) {
			overviewPanel.refresh();
		}
	}

	public CopyActionTableModel getModel() {
		return tmCopy;
	}
}
