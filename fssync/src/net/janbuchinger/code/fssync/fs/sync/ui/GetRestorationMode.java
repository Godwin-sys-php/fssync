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

import javax.swing.JDialog;
import javax.swing.JOptionPane;

public class GetRestorationMode implements Runnable {
	private final JDialog dialog;

	private int answer;
	private int mode;
	private boolean deleteNew;

	public GetRestorationMode(JDialog dialog) {
		this.dialog = dialog;
	}

	@Override
	public void run() {
		RestorationModePanel rmp = new RestorationModePanel(dialog);
		answer = JOptionPane.showConfirmDialog(dialog, rmp, "Wiederherstellungsmodus",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		deleteNew = rmp.isDeleteNew();
		mode = rmp.getMode();
	}

	public int getAnswer() {
		return answer;
	}

	public int getMode() {
		return mode;
	}

	public boolean isDeleteNew() {
		return deleteNew;
	}
}
