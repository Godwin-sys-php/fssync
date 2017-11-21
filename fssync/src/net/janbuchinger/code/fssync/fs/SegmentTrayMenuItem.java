package net.janbuchinger.code.fssync.fs;
import java.awt.MenuItem;


@SuppressWarnings("serial")
public class SegmentTrayMenuItem extends MenuItem {
	private final Segment segment;

	public SegmentTrayMenuItem(Segment segment) {
		super(segment.getName());
		this.segment = segment;
	}

	public final Segment getSegment() {
		return segment;
	}
}
