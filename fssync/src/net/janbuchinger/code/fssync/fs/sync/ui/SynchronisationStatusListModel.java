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

import java.util.Iterator;
import java.util.Vector;

import javax.swing.AbstractListModel;

@SuppressWarnings("serial")
public class SynchronisationStatusListModel extends AbstractListModel<String> {

	private final Vector<String> data;

	public SynchronisationStatusListModel() {
		data = new Vector<String>();
	}

	@Override
	public int getSize() {
		// TODO Auto-generated method stub
		return data.size();
	}

	@Override
	public String getElementAt(int index) {
		return data.get(index);
	}

	public final void addStatus(String status) {
		data.add(status);
		fireContentsChanged(this, 0, getSize() - 1);
	}

	public final Iterator<String> iterator(){
		return data.iterator();
	}
}
