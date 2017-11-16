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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import mishmash.FSFx;
import mishmash.ui.UIFx;

@SuppressWarnings("serial")
public class FileTableModel extends AbstractTableModel {

	private final Vector<File> data;
	private final SimpleDateFormat df;
	private File f;

	public FileTableModel(Vector<File> data) {
		this.data = data;
		f = null;
		df = UIFx.initDisplayDateTimeFormat();
	}

	@Override
	public int getRowCount() {
		return data.size();
	}

	@Override
	public String getColumnName(int column) {
		switch (column) {
		case 0:
			return "Name";
		case 1:
			return "Pfad";
		case 2:
			return "Grösse";
		case 3:
			return "Geändert";
		}
		return null;
	}

	@Override
	public int getColumnCount() {
		return 4;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		f = data.get(rowIndex);
		switch (columnIndex) {
		case 0:
			return f.getName();
		case 1:
			return f.getParent();
		case 2:
			return FSFx.formatFileLength(f.length());
		case 3:
			return df.format(f.lastModified());
		}
		return null;
	}

	public final File getRow(int id) {
		return data.get(id);
	}

}
