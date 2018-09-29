/*
 * Copyright 2017-2018 Jan Buchinger
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

import javax.swing.SwingUtilities;

/**
 * The <code>TrayReminderThread</code> class is running when the tray icon is
 * visible. It notifies the user interface about operations that just became due
 * so that a message in the tray icon area can be shown.
 * 
 * @author Jan Buchinger
 *
 */
public class TrayReminderThread implements Runnable {
	/**
	 * The main user interface object
	 */
	private final FSSyncUI ui;
	/**
	 * A clone of the current segments
	 */
	private final Segments segments;
	/**
	 * false indicates if the process should be terminated
	 */
	private boolean running;

	/**
	 * Constructs a new <code>TrayReminderThread</code>.
	 * 
	 * @param ui
	 *            The main user interface object
	 */
	public TrayReminderThread(FSSyncUI ui) {
		// set main user interface field
		this.ui = ui;
		// obtain segments clone
		this.segments = Segments.getSegments(true);
		// set running true
		running = true;
	}

	/**
	 * The tray reminder thread
	 */
	@Override
	public void run() {
		// process loop
		while (running) {
			// loop through all segments
			for (Segment s : segments.getData()) {
				// and all operations
				for (Operation o : s.getOperations()) {
					// to check if any operation just became due
					if (o.isDue() && o.isRemind() && !o.isReminded()) {
						// notify user interface
						SwingUtilities.invokeLater(new RunRemindTray(ui, o));
						// exclude current operation from reminding
						o.setReminded(true);
					}
				}
			}
			// sleep 5 seconds
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} // end of process loop
	}

	/**
	 * exits the process loop to terminate the process
	 */
	public final void stop() {
		running = false;
	}
}
