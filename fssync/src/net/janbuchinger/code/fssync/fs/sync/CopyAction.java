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

public final class CopyAction {

	private final File source;
	private final File destination;
	private final String relativePath;
	private final boolean isNew;
	private final int direction;

	private CopyAction duplicate;
	private boolean isSelected;

	public final static int DIR_BACKUP = 0;
	public final static int DIR_RESTORE = 1;

	public CopyAction(File source, File destination, String relativePath, boolean isNew, int direction) {
		this.direction = direction;
		this.source = source;
		this.destination = destination;
		this.relativePath = relativePath;
		this.isNew = isNew;

		duplicate = null;
		isSelected = true;
	}

	public final int getDirection() {
		return direction;
	}

	public final String getRelativePath() {
		return relativePath;
	}

	public final File getSource() {
		return source;
	}

	public final File getDestination() {
		return destination;
	}

	public final boolean isNew() {
		return isNew;
	}

	public final boolean isSelected() {
		return isSelected;
	}

	public final void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	public final CopyAction getConflict() {
		return duplicate;
	}

	public final void setConflict(CopyAction duplicate) {
		this.duplicate = duplicate;
	}

	@Override
	public String toString() {
		return (isNew ? "neu" : "mod") + (direction == DIR_BACKUP ? " >> " : " << ") + relativePath;
	}
}
