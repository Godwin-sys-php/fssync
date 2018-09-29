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

import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 * This <code>Runnable</code> is used to inform the user that there is already
 * an instance of the program running and then exit the system.
 * 
 * It is passed to the EDT by the main method after detecting another instance
 * locking the lock file.
 * 
 * @author Jan Buchinger
 *
 */
public final class RunAlreadyRunningMessage implements Runnable {
	@Override
	public void run() {
		// create an option pane with the message and an OK button.
		JOptionPane optionPane = new JOptionPane("FSSync läuft bereits!", JOptionPane.ERROR_MESSAGE,
				JOptionPane.DEFAULT_OPTION);
		// create a JDialog to display option pane
		JDialog dialog = optionPane.createDialog("Fehler");
		// display the dialog (modal)
		dialog.setVisible(true);
		// exit the program with error
		System.exit(1);
	}

}
