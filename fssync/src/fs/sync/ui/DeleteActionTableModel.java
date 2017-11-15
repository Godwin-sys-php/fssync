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

import java.text.SimpleDateFormat;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import mishmash.FSFx;
import mishmash.ui.UIFx;
import fs.sync.DeleteAction;
import fs.sync.OperationSummary;

@SuppressWarnings("serial")
public class DeleteActionTableModel extends AbstractTableModel {

	private final Vector<DeleteAction> data;
	private final OperationSummary operationSummary;
	private DeleteAction deleteAction;

	private final SimpleDateFormat df;

	public DeleteActionTableModel(Vector<DeleteAction> data, OperationSummary operationSummary) {
		this.data = data;
		this.operationSummary = operationSummary;
		df = UIFx.initDisplayDateTimeFormat();
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
		deleteAction = data.get(rowIndex);
		switch (columnIndex) {
		case 0:
			return deleteAction.isSelected();
		case 1:
			return deleteAction.toString();
		case 2:
			return FSFx.formatFileLength(deleteAction.getFile().length());
		case 3:
			return df.format(deleteAction.getFile().lastModified());
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
		deleteAction = data.get(rowIndex);
		switch (columnIndex) {
		case 0:
			if((Boolean) aValue){
				if(deleteAction.getLocation() == DeleteAction.del_destination){
					operationSummary.addRmDestination(deleteAction.getFile().length());
				} else {
					operationSummary.addRmSource(deleteAction.getFile().length());
				}
			} else {
				if(deleteAction.getLocation() == DeleteAction.del_destination){
					operationSummary.removeRmDestination(deleteAction.getFile().length());
				} else {
					operationSummary.removeRmSource(deleteAction.getFile().length());
				}
			}
			operationSummary.reCalc();
			deleteAction.setSelected((Boolean) aValue);
			fireTableDataChanged();
			break;
		}
	}

	public DeleteAction getRow(int row) {
		return data.get(row);
	}
}
