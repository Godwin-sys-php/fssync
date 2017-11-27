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

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import net.janbuchinger.code.fssync.fs.sync.CopyAction;

@SuppressWarnings("serial")
public class CopyActionTableCellRenderer extends DefaultTableCellRenderer {
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column) {
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		if (column == 2 || column == 3)
			setHorizontalAlignment(JLabel.RIGHT);
		else
			setHorizontalAlignment(JLabel.LEFT);

		CopyAction ca = ((CopyActionTableModel) table.getModel()).getRow(row);

		setBackground(table.getBackground());

		CopyAction conflict = ca.getConflict();

		if (ca.isSelected() && conflict == null) {
			setForeground(Color.green.darker());
		} else if (ca.isSelected() && conflict != null) {
			setForeground(Color.green.darker());
			setBackground(Color.yellow);
		} else if (!ca.isSelected() && conflict != null) {
			if (conflict.isSelected()) {
				setForeground(Color.yellow);
				setBackground(Color.red.darker());
			} else {
				setForeground(Color.white);
				setBackground(Color.gray);
			}
		} else {
			setForeground(Color.gray);
		}
		return this;
	}
}
