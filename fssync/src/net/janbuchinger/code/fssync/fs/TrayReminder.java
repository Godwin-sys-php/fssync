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

import java.awt.TrayIcon;
import java.util.Iterator;

public class TrayReminder implements Runnable {
	private final TrayIcon trayIcon;
	private final Segments segments;
	private boolean running;

	public TrayReminder(Segments segments, TrayIcon trayIcon) {
		this.segments = segments;
		this.trayIcon = trayIcon;
		running = true;
	}

	@Override
	public void run() {
		Iterator<Segment> iSeg;
		Iterator<Operation> iOp;
		Operation o;
		while (running) {
			iSeg = segments.iterator();
			while (iSeg.hasNext()) {
				iOp = iSeg.next().iterator();
				while (iOp.hasNext()) {
					o = iOp.next();
					if (o.isDue() && !o.isReminded()) {
							trayIcon.displayMessage("Erinnerung", o.toString() + " ist fällig",
									TrayIcon.MessageType.WARNING);
							o.setReminded(true);
							segments.save();
					}
				}
			}
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public final void stop() {
		running = false;
	}
}
