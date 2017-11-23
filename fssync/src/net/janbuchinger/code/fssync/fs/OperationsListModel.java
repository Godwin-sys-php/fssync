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

import javax.swing.DefaultListModel;

@SuppressWarnings("serial")
public class OperationsListModel extends DefaultListModel<Operation> {
	private final Segment operations;
	private boolean hasChanges;

	public OperationsListModel(Segment s) {
		operations = s;
		hasChanges = false;
	}

	@Override
	public int getSize() {
		return operations.size();
	}

	@Override
	public Operation get(int index) {
		return operations.get(index);
	}

	@Override
	public Operation elementAt(int index) {
		return get(index);
	}

	@Override
	public Operation getElementAt(int index) {
		return get(index);
	}

	@Override
	public void addElement(Operation element) {
		operations.add(element);
		fireContentsChanged(this, 0, getSize() - 1);
		hasChanges = true;
	}

	@Override
	public Operation remove(int index) {
		Operation x = operations.remove(index);
		fireContentsChanged(this, 0, getSize() - 1);
		hasChanges = true;
		return x;
	}

	public Segment getSynchronisationOperations() {
		return operations;
	}

	public boolean hasChanges() {
		return hasChanges;
	}

	public void refresh() {
		fireContentsChanged(this, 0, getSize() - 1);
	}

	@Override
	public void setElementAt(Operation element, int index) {
		set(index, element);
	}
}
