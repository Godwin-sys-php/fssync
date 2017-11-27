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
