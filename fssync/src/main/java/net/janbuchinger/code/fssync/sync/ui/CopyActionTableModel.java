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

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import net.janbuchinger.code.fssync.Operation;
import net.janbuchinger.code.fssync.sync.CopyAction;
import net.janbuchinger.code.fssync.sync.OperationSummary;
import net.janbuchinger.code.fssync.sync.SynchronizationCancelledException;
import net.janbuchinger.code.mishmash.FSFx;
import net.janbuchinger.code.mishmash.ui.UIFx;

/**
 * The table model for the <code>OperationSummaryDialog</code>s files to copy
 * table.
 * 
 * @author Jan Buchinger
 *
 */
@SuppressWarnings("serial")
public class CopyActionTableModel extends AbstractTableModel {
	/**
	 * Selection: Select none
	 */
	public final static int sel_none = 4;
	/**
	 * Selection: Select all
	 */
	public final static int sel_all = 5;
	/**
	 * Selection: Select Direction source
	 */
	public final static int sel_dir_source = 6;
	/**
	 * Selection: Select Direction target
	 */
	public final static int sel_dir_target = 7;

	/**
	 * The data of the model contains <code>CopyAction</code>s.
	 */
	private final Vector<CopyAction> data;
	/**
	 * The <code>OperationSummary</code> for updating values upon de/selection and
	 * de/selection
	 */
	private final OperationSummary operationSummary;
	/**
	 * multi purpose CopyAction
	 */
	private CopyAction copyAction;
	/**
	 * The currently selected conflict priority
	 */
	private int selectCurrent;
	/**
	 * <code>SimpleDateFormat</code> for formatting the file modification date
	 */
	private final SimpleDateFormat df;

	public CopyActionTableModel(Vector<CopyAction> data, OperationSummary operationSummary) {
		this.data = data;
		this.operationSummary = operationSummary;
		df = UIFx.initDisplayDateTimeFormat();
		selectCurrent = Operation.PRIORITY_SOURCE;
	}

	@Override
	public int getRowCount() {
		return data.size();
	}

	@Override
	public int getColumnCount() {
		return 4;
	}

	@Override
	public String getColumnName(int column) {
		switch (column) {
		case 0:
			return "";
		case 1:
			return "Aktion";
		case 2:
			return "Grösse";
		case 3:
			return "Geändert";
		default:
			return "";
		}
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case 0:
			return Boolean.class;
		default:
			return String.class;
		}
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		copyAction = data.get(rowIndex);
		switch (columnIndex) {
		case 0:
			return copyAction.isSelected();
		case 1:
			return copyAction.toString();
		case 2:
			return FSFx.formatFileLength(copyAction.getSource().length());
		case 3:
			return df.format(copyAction.getSource().lastModified());
		default:
			break;
		}
		return null;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		switch (columnIndex) {
		case 0:
			return true;
		default:
			return false;
		}
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		copyAction = data.get(rowIndex);
		switch (columnIndex) {
		case 0:
			boolean select = (Boolean) aValue;
			if (select) {
				copyAction.setSelected(true);
				calc(true, copyAction);
				CopyAction conflict = copyAction.getConflict();
				if (conflict != null) {
					if (conflict.isSelected()) {
						conflict.setSelected(false);
						calc(false, conflict);
					}
				}
			} else {
				copyAction.setSelected(false);
				calc(false, copyAction);
			}
			fireTableDataChanged();
			break;
		}
	}

	private final void calc(boolean b, CopyAction copyAction) {
		if (b) {
			if (copyAction.getDirection() == CopyAction.DIR_BACKUP) {
				if (copyAction.isNew()) {
					operationSummary.addDestinationNew(copyAction.getSource().length());
				} else {
					operationSummary.addDestinationModified(copyAction.getSource().length(),
							copyAction.getDestination().length());
				}
			} else {
				if (copyAction.isNew()) {
					operationSummary.addSourceNew(copyAction.getSource().length());
				} else {
					operationSummary.addSourceModified(copyAction.getSource().length(),
							copyAction.getDestination().length());
				}
			}
		} else {
			if (copyAction.getDirection() == CopyAction.DIR_BACKUP) {
				if (copyAction.isNew()) {
					operationSummary.removeDestinationNew(copyAction.getSource().length());
				} else {
					operationSummary.removeDestinationModified(copyAction.getSource().length(),
							copyAction.getDestination().length());
				}
			} else {
				if (copyAction.isNew()) {
					operationSummary.removeSourceNew(copyAction.getSource().length());
				} else {
					operationSummary.removeSourceModified(copyAction.getSource().length(),
							copyAction.getDestination().length());
				}
			}
		}
		operationSummary.reCalc();
	}

	public CopyAction getRow(int row) {
		return data.get(row);
	}

	private final void refresh() {
		try {
			operationSummary.reCalcAll();
		} catch (SynchronizationCancelledException e) {
			e.printStackTrace();
		}
		fireTableDataChanged();
	}

	public final void select(int select) {
		if (select == sel_all) {
			Iterator<CopyAction> iData = data.iterator();
			while (iData.hasNext())
				iData.next().setSelected(true);
			select = selectCurrent;
			if (!operationSummary.hasConflicts()) {
				refresh();
				return;
			}
		} else if (select == sel_none) {
			Iterator<CopyAction> iData = data.iterator();
			while (iData.hasNext())
				iData.next().setSelected(false);
			refresh();
			return;
		} else if (select == sel_dir_source) {
			Iterator<CopyAction> iData = data.iterator();
			CopyAction copyAction;
			while (iData.hasNext()) {
				copyAction = iData.next();
				if (copyAction.getDirection() == CopyAction.DIR_BACKUP)
					copyAction.setSelected(true);
				else
					copyAction.setSelected(false);
			}
			refresh();
			return;
		} else if (select == sel_dir_target) {
			Iterator<CopyAction> iData = data.iterator();
			CopyAction copyAction;
			while (iData.hasNext()) {
				copyAction = iData.next();
				if (copyAction.getDirection() == CopyAction.DIR_RESTORE)
					copyAction.setSelected(true);
				else
					copyAction.setSelected(false);
			}
			refresh();
			return;
		}

		CopyAction copyActionA;
		CopyAction copyActionB;

		boolean selectA;

		for(Vector<CopyAction> conflict : operationSummary.getCopyActionsDuplicates()) {
			copyActionA = conflict.elementAt(0);
			copyActionB = conflict.elementAt(1);

			switch (select) {
			case Operation.PRIORITY_SOURCE:
				selectA = copyActionA.getDirection() == CopyAction.DIR_BACKUP;
				break;
			case Operation.PRIORITY_TARGET:
				selectA = copyActionA.getDirection() == CopyAction.DIR_RESTORE;
				break;
			case Operation.PRIORITY_OLD:
				selectA = copyActionA.getSource().lastModified() < copyActionA.getDestination().lastModified();
				break;
			case Operation.PRIORITY_NEW:
			default:
				selectA = copyActionA.getSource().lastModified() > copyActionA.getDestination().lastModified();
				break;
			}
			copyActionA.setSelected(selectA);
			copyActionB.setSelected(!selectA);
		}
		selectCurrent = select;
		refresh();
	}

}
