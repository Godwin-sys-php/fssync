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

import java.util.Vector;

import javax.swing.DefaultListModel;

@SuppressWarnings("serial")
public class StringListModel extends DefaultListModel<String> {
	private Vector<String> files;
	
	public StringListModel() {
		files = new Vector<String>();
	}
	
	@Override
	public int getSize() {
		return files.size();
	}
	
	@Override
	public String get(int index) {
		return files.get(index);
	}
	
	@Override
	public String elementAt(int index) {
		return get(index);
	}
	
	@Override
	public String getElementAt(int index) {
		return get(index);
	}
	
	@Override
	public void addElement(String element) {
		files.add(element);
		fireContentsChanged(this, 0, getSize()-1);
	}
	
	@Override
	public String remove(int index) {
		String x = files.remove(index);
		fireContentsChanged(this, 0, getSize()-1);
		return x;
	}
	
	public Vector<String> getStrings() {
		return files;
	}

	public void setList(Vector<String> exclude) {
		this.files = exclude;
		fireContentsChanged(this, 0, getSize()-1);
	}
}
