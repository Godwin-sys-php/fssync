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
package net.janbuchinger.code.fssync.sync;

import java.awt.BorderLayout;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.janbuchinger.code.mishmash.ui.UIFx;

@SuppressWarnings("serial")
public class RecoverSystemDialog extends JDialog {
	private final JProgressBar progressBar;
	private boolean done;

	public RecoverSystemDialog(JDialog frm) {
		super(frm, "Datenbank aufbauen", true);

		progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);

		done = false;

		JPanel pnContent = new JPanel(new BorderLayout());
		pnContent.add(new JLabel("Dateisysteme werden verglichen..."), BorderLayout.CENTER);
		pnContent.add(progressBar, BorderLayout.SOUTH);

		setContentPane(pnContent);

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		UIFx.packAndCenter(this, frm);
	}

	@Override
	public void setVisible(boolean b) {
		if (!b) {
			done = true;
			setDefaultCloseOperation(HIDE_ON_CLOSE);
		}
		super.setVisible(b);
	}

	public boolean isDone() {
		return done;
	}
}
