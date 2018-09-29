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
package net.janbuchinger.code.fssync.sync.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class ProgressBarCountDownThread extends SwingWorker<Void, Void> implements PropertyChangeListener {

	private final SynchronizationProcessDialog spd;
	private final long timeToCountDown;
	
	private boolean paused;

	public ProgressBarCountDownThread(SynchronizationProcessDialog spd, long timeToCountDown) {
		this.spd = spd;
		this.timeToCountDown = timeToCountDown;
		paused = false;
		addPropertyChangeListener(this);
	}

	@Override
	protected Void doInBackground() throws Exception {

		long start = System.currentTimeMillis();
		int progress = 0;
		long timePassed;
		
		SwingUtilities.invokeLater(new RunSetDeterminate(true, spd));
		
		while (!isCancelled()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {}
			if(paused) {
				start += 500;
			} else {
				timePassed = System.currentTimeMillis() - start;
				progress = (int)((100.0 / (double)timeToCountDown) * (double)timePassed);
				if(progress >= 100) {
					setProgress(100);
					cancel(false);
				} else {
					setProgress(progress);
				}
			}
		}
		
		SwingUtilities.invokeLater(new RunSetDeterminate(false, spd));

		return null;
	}

	public void setPaused(boolean paused) {
		this.paused = paused;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		spd.setProgress(getProgress());
	}

}
