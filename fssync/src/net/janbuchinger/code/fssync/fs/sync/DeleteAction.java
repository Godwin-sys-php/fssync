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
package net.janbuchinger.code.fssync.fs.sync;

import java.io.File;

public class DeleteAction {
	public final static int del_destination = 0;
	public final static int del_source = 1;

	private final File f;
	private final int location;

	private boolean isSelected;

	private final String relativePath;

	private final boolean modeRestore;

	public DeleteAction(File f, String relativePath, int location, boolean modeRestore) {
		this.location = location;
		this.f = f;
		this.relativePath = relativePath;
		this.modeRestore = modeRestore;

		isSelected = true;
	}

	public final File getFile() {
		return f;
	}

	public final int getLocation() {
		return location;
	}

	public final boolean isSelected() {
		return isSelected;
	}

	public final void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	@Override
	public String toString() {
		String s = (!isSelected && !modeRestore ? "kopieren" : "löschen");
		if (modeRestore && !isSelected)
			s += " >< ";
		else
			s += (isSelected ? (location == del_destination ? " >> x " : " x << ")
					: (location == del_destination ? " << " : " >> "));
		s += relativePath;
		return s;
	}

	public String getRelativePath() {
		return relativePath;
	}
}
