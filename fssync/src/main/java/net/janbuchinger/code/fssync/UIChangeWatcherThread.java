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

import java.util.Iterator;
import java.util.Vector;

import javax.swing.SwingUtilities;

/**
 * The <code>UIChangeWatcherThread</code> class is a background process that checks
 * for relevant changes in the availability of operations directories, databases
 * or due states. It is being terminated (and restarted) every time when the
 * Segments or Operations could have been changed, during a synchronization
 * process and as long the the main frame is being minimized to tray.
 * 
 * @author Jan Buchinger
 *
 */
public class UIChangeWatcherThread implements Runnable {
	/**
	 * a clone of the current <code>Segments</code>
	 */
	private final Segments segments;
	/**
	 * the main user interface object
	 */
	private final FSSyncUI ui;
	/**
	 * list of states corresponding the segments list
	 */
	private final Vector<boolean[][]> isOnline;
	/**
	 * list of states corresponding the segments list
	 */
	private final Vector<boolean[]> isDue;
	/**
	 * true as long as the process is supposed to be running
	 */
	private boolean running;

	/**
	 * Constructs a new UIChangeWatcher.
	 * 
	 * @param ui
	 *            The main user interface object to send the refresh command.
	 */
	public UIChangeWatcherThread(FSSyncUI ui) {
		// set the user interface class field
		this.ui = ui;
		// get a clone of the current segments
		this.segments = Segments.getSegments(true);
		// initialize online state list
		isOnline = new Vector<boolean[][]>();
		// initialize operation due state list
		isDue = new Vector<boolean[]>();
		// enable process loop
		running = true;

		// the online states of a segment
		boolean[][] online;
		// the operation is due states of a segment
		boolean[] due;
		// counter
		int i;

		// loop through all segments
		for (Segment s : segments.getData()) {
			// initialize state arrays for the segment
			online = new boolean[s.size()][4];
			due = new boolean[s.size()];
			i = 0;
			// loop through segment operations
			for (Operation o : s.getOperations()) {
				// set the current states
				// is the source database available?
				online[i][0] = o.isSourceOnline();
				// does the source directory exist?
				online[i][1] = o.getSource().exists();
				// is the original / target database available?
				online[i][2] = o.isTargetOnline();
				// does the target directory exist?
				online[i][3] = o.getTarget().exists();
				// is the operation due for synchronization?
				due[i++] = o.isDue();
			}
			// add the segments online states
			isOnline.add(online);
			// add the segments due states
			isDue.add(due);
		}
	}

	@Override
	public void run() {
		// false by default, set true if changes are being registered
		boolean hasChanged;
		
		// iterator running parallel to the segments loop
		Iterator<boolean[][]> iIsOnline;
		// iterator running parallel to the segments loop
		Iterator<boolean[]> iIsDue;
		// the current segments online states
		boolean[][] online;
		// the current segments operation due states
		boolean[] due;
		// counter
		int i;

		// source database availability changed
		boolean srcOnlineChanged;
		// source directory availability changed
		boolean srcDirOnlineChanged;
		// target database availability changed
		boolean dstOnlineChanged;
		// target directory availability changed
		boolean dstDirOnlineChanged;
		// operation due state changed
		boolean opIsDueChanged;
		
		// source database availability
		boolean srcOnline;
		// source directory availability
		boolean srcDirOnline;
		// target database availability
		boolean dstOnline;
		// target directory availability 
		boolean dstDirOnline;
		// operation due state
		boolean opIsDue;

		// loop until running is set false
		while (running) {
			// initialize online states iterator
			iIsOnline = isOnline.iterator();
			// initialize operation due states iterator
			iIsDue = isDue.iterator();
			// assume Segments states unchanged
			hasChanged = false;
			// loop through Segments
			for (Segment segment : segments.getData()) {
				// get next array from isOnline list
				online = iIsOnline.next();
				// get next array from isDue list
				due = iIsDue.next();
				// set counter to zero
				i = 0;
				// loop through the segments operations
				for (Operation o : segment.getOperations()) {
					// is the source database available?
					srcOnline = o.isSourceOnline();
					// if srcOnline then srcDirOnline
					if(srcOnline) {
						srcDirOnline = true;
					} else {
						// if !srcOnline is then the source directory available?
						srcDirOnline = o.getSource().exists();
					}
					// is the target database available?
					dstOnline = o.isTargetOnline();
					// if dstOnline then dstDirOnline too
					if(dstOnline) {
						dstDirOnline = true;
					} else {
						// if !dstOnline is then the target directory available?
						dstDirOnline = o.getTarget().exists();
					}
					// is the operation due?
					opIsDue = o.isDue();
					
					// has the source database online state changed?
					srcOnlineChanged = srcOnline != online[i][0];
					// has the source directory availability state changed?
					srcDirOnlineChanged = srcDirOnline != online[i][1];
					// has the target database online state changed?
					dstOnlineChanged = dstOnline != online[i][2];
					// has the target directory availability state changed?
					dstDirOnlineChanged = dstDirOnline != online[i][3];
					// has the operation due state changed?
					opIsDueChanged = opIsDue != due[i];
					
					// if anything has changed
					if (srcOnlineChanged || srcDirOnlineChanged || dstOnlineChanged || dstDirOnlineChanged
							|| opIsDueChanged) {
						// set changed flag true
						if(!hasChanged) {
							hasChanged = true;
						}
						// apply the new value(s)
						online[i][0] = srcOnline;
						online[i][1] = srcDirOnline;
						online[i][2] = dstOnline;
						online[i][3] = dstDirOnline;
						due[i] = opIsDue;
					}
					i++;
				} // end of operations loop
			} // end of segments loop

			// if anything has changed
			if (hasChanged) {
				// then send user interface refresh command
				SwingUtilities.invokeLater(new RunRefreshUI(ui));
			}

			// sleep for 750 milliseconds
			try {
				Thread.sleep(750);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} // end of process loop
	}

	/**
	 * exits the process loop
	 */
	public final void stop() {
		// set process loop condition false
		running = false;
	}
}
