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
	public static final transient int MD_DAYS = 0;
	public static final transient int MD_HOURS = 1;
	public static final transient int MD_MINUTES = 2;

	private transient boolean isSelected;
	private File source;
	private File target;
	private boolean manageVersions;
	private Vector<String> exclude;
	// private boolean excludeHidden;
	// private Vector<String> forceHidden;
	private boolean syncBidirectional;
	private boolean ignoreModifiedWhenEqual;
	private int priorityOnConflict;

	private long lastSynced;
	private int interval;
	private int intervalMode;
	private boolean remind;
	private boolean reminded;

	// public Operation(String source, String target, boolean manageVersions,
	// Vector<String> exclude,
	// /*boolean excludeHidden, Vector<String> forceHidden, */boolean
	// syncBidirectional, boolean ignoreModifiedWhenEqual) {
	// this(new File(source), new File(target), manageVersions, exclude,
	// /*excludeHidden, forceHidden,*/
	// syncBidirectional, ignoreModifiedWhenEqual);
	// }

	public Operation(File source, File target, boolean manageVersions, Vector<String> exclude,
			boolean syncBidirectional, boolean ignoreModifiedWhenEqual, int priorityOnConflict,
			long lastSynced, int interval, int intervalMode, boolean remind, boolean reminded) {
		this.source = source;
		this.target = target;
		this.manageVersions = manageVersions;
		this.exclude = exclude;
		// this.excludeHidden = excludeHidden;
		// this.forceHidden = forceHidden;
		this.syncBidirectional = syncBidirectional;
		this.ignoreModifiedWhenEqual = ignoreModifiedWhenEqual;
		this.priorityOnConflict = priorityOnConflict;

		this.lastSynced = lastSynced;
		this.interval = interval;
		this.intervalMode = intervalMode;
		this.remind = remind;
		this.reminded = reminded;
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

	public final void setExcludes(Vector<String> exclude) {
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

	public final boolean isSelected() {
		return isSelected;
	}

	public final void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	public final long getLastSynced() {
		return lastSynced;
	}

	public final void setLastSynced(long lastSynced) {
		this.lastSynced = lastSynced;
	}

	public final int getInterval() {
		return interval;
	}

	public final void setInterval(int interval) {
		this.interval = interval;
	}

	public final int getIntervalMode() {
		return intervalMode;
	}

	public final void setIntervalMode(int intervalMode) {
		this.intervalMode = intervalMode;
	}

	private long getIntervalMillis() {
		if (intervalMode == MD_HOURS)
			return interval * 60 * 60 * 1000;
		else if (intervalMode == MD_MINUTES)
			return interval * 60 * 1000;
		else
			return interval * 24 * 60 * 60 * 1000;
	}

	public final boolean isRemind() {
		return remind;
	}

	public final void setRemind(boolean remind) {
		this.remind = remind;
	}

	public final boolean isReminded() {
		return reminded;
	}

	public final void setReminded(boolean reminded) {
		this.reminded = reminded;
	}

	public boolean isDue() {
		if (interval == 0)
			return false;
		else if (!remind)
			return false;
		else if (lastSynced == 0)
			return true;
		else if ((lastSynced + getIntervalMillis()) < System.currentTimeMillis())
			return true;
		else
			return false;
	}

	@Override
	public String toString() {
		return source.getPath() + (syncBidirectional ? " <> " : " >> ") + target.getPath();
	}
}
