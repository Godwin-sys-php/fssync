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

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import net.janbuchinger.code.mishmash.FSFx;

public class Operation {
	private File source;
	private File target;
	private boolean manageVersions;
	private Vector<String> exclude;
	// private boolean excludeHidden;
	// private Vector<String> forceHidden;
	private boolean syncBidirectional;
	private boolean ignoreModifiedWhenEqual;
	private int priorityOnConflict;

	// public Operation(String source, String target, boolean manageVersions,
	// Vector<String> exclude,
	// /*boolean excludeHidden, Vector<String> forceHidden, */boolean
	// syncBidirectional, boolean ignoreModifiedWhenEqual) {
	// this(new File(source), new File(target), manageVersions, exclude,
	// /*excludeHidden, forceHidden,*/
	// syncBidirectional, ignoreModifiedWhenEqual);
	// }

	public Operation(File source, File target, boolean manageVersions, Vector<String> exclude,
			boolean syncBidirectional, boolean ignoreModifiedWhenEqual, int priorityOnConflict) {
		this.source = source;
		this.target = target;
		this.manageVersions = manageVersions;
		this.exclude = exclude;
		// this.excludeHidden = excludeHidden;
		// this.forceHidden = forceHidden;
		this.syncBidirectional = syncBidirectional;
		this.ignoreModifiedWhenEqual = ignoreModifiedWhenEqual;
		this.priorityOnConflict = priorityOnConflict;
	}

	public boolean isOnline() {
		return isSourceOnline() && isTargetOnline();
	}

	public boolean isSourceOnline() {
		try {
			return source.exists() && FSFx.hasDirEntries(source.toPath());
		} catch (IOException e) {
			return false;
		}
	}

	public File getDbOriginal() {
		return new File(target, ".fs.db");
	}

	public boolean isTargetOnline() {
		return getDbOriginal().exists();
	}

	public final File getSource() {
		return source;
	}

	public final void setSource(File source) {
		this.source = source;
	}

	public String getSourcePath() {
		return source.getPath();
	}

	public final File getTarget() {
		return target;
	}

	public final void setTarget(File target) {
		this.target = target;
	}

	public String getRemotePath() {
		return target.getPath();
	}

	public final boolean isManageVersions() {
		return manageVersions;
	}

	public final void setManageVersions(boolean manageVersions) {
		this.manageVersions = manageVersions;
	}

	public final Vector<String> getExcludes() {
		return exclude;
	}

	public final void setExclude(Vector<String> exclude) {
		this.exclude = exclude;
	}

	public final boolean isSyncBidirectional() {
		return syncBidirectional;
	}

	public final void setSyncBidirectional(boolean syncBidirectional) {
		this.syncBidirectional = syncBidirectional;
	}

	public boolean isCheckFilesAfterSync() {
		return true;
	}

	public boolean isCheckFilesBeforeSync() {
		return true;
	}

	public final boolean isIgnoreModifiedWhenEqual() {
		return ignoreModifiedWhenEqual;
	}

	public final void setIgnoreModifiedWhenEqual(boolean ignoreModifiedWhenEqual) {
		this.ignoreModifiedWhenEqual = ignoreModifiedWhenEqual;
	}

	public final int getPriorityOnConflict() {
		return priorityOnConflict;
	}

	public final void setPriorityOnConflict(int priorityOnConflict) {
		this.priorityOnConflict = priorityOnConflict;
	}

	// public final boolean isExcludeHidden() {
	// return excludeHidden;
	// }
	//
	// public final void setExcludeHidden(boolean excludeHidden) {
	// this.excludeHidden = excludeHidden;
	// }
	//
	// public final Vector<String> getForceHidden() {
	// return forceHidden;
	// }
	//
	// public final void setForceHidden(Vector<String> forceHidden) {
	// this.forceHidden = forceHidden;
	// }

	@Override
	public String toString() {
		return source.getPath() + (syncBidirectional ? " <> " : " >> ") + target.getPath();
	}
}
