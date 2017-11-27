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

import javax.swing.JMenuItem;

@SuppressWarnings("serial")
public class RunSegmentMenuItem extends JMenuItem {

	private final Segment segment;
	private final int segmentId;

	public RunSegmentMenuItem(Segment segment, int segmentId) {
		super(segment.getName());
		this.segmentId = segmentId;
		this.segment = segment;
	}

	public Segment getSegment() {
		return segment;
	}

	public int getSegmentId() {
		return segmentId;
	}
}
