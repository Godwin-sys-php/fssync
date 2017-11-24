package net.janbuchinger.code.fssync.fs;

import java.util.Iterator;
import java.util.Vector;

public class UIChangeWatcher implements Runnable {
	private final Segments segments;
	private final FSSyncUI ui;
	private final Vector<boolean[]> isOnline;
	private final Vector<boolean[]> isDue;
	private boolean running;

	public UIChangeWatcher(Segments segments, FSSyncUI ui) {
		this.segments = segments;
		this.ui = ui;
		isOnline = new Vector<boolean[]>();
		isDue = new Vector<boolean[]>();
		running = true;

		Iterator<Segment> iSeg = segments.iterator();
		Segment s;
		Iterator<Operation> iOp;
		Operation o;
		boolean[] b;
		boolean[] c;
		int i;

		while (iSeg.hasNext()) {
			s = iSeg.next();
			b = new boolean[s.size()];
			c = new boolean[s.size()];
			iOp = s.iterator();
			i = 0;
			while (iOp.hasNext()) {
				o = iOp.next();
				b[i] = o.isOnline();
				c[i++] = o.isDue();
			}
			isOnline.add(b);
			isDue.add(c);
		}
	}

	@Override
	public void run() {
		boolean wasChanged;

		Iterator<Segment> iSeg;
		Iterator<Operation> iOp;
		Operation o;
		Iterator<boolean[]> iIsOnline;
		Iterator<boolean[]> iIsDue;
		boolean[] b;
		boolean[] c;
		int i;

		while (running) {
			iSeg = segments.iterator();
			iIsOnline = isOnline.iterator();
			iIsDue = isDue.iterator();
			wasChanged = false;
			while (iSeg.hasNext()) {
				iOp = iSeg.next().iterator();
				b = iIsOnline.next();
				c = iIsDue.next();
				i = 0;
				while (iOp.hasNext()) {
					o = iOp.next();
					if (o.isOnline() != b[i] || o.isDue() != c[i]) {
						wasChanged = true;
						b[i] = o.isOnline();
						c[i] = o.isDue();
					}
					i++;
				}
			}
			
			if (wasChanged)
				ui.refresh();

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public final void stop() {
		running = false;
	}
}
