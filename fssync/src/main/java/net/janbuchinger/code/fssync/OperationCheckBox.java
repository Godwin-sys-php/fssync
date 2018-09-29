/*
 * Copyright 2018 Jan Buchinger
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

@SuppressWarnings("serial")
public class OperationCheckBox extends JCheckBox implements ActionListener {
	private final Operation operation;

	public OperationCheckBox(Operation operation) {
		super("", operation.isSelected());
		this.operation = operation;
		addActionListener(this);
		
	}

	@Override
	public void setSelected(boolean b) {
		super.setSelected(b);
		operation.setSelected(b);
	}

	@Override
	public void setEnabled(boolean b) {
		if (!b) {
			setSelected(b);
		}
		super.setEnabled(b);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this) {
			operation.setSelected(isSelected());
		}
	}
}
