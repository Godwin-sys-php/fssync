/*
 * Copyright 2017-2018 Jan Buchinger
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
package net.janbuchinger.code.fssync;

import java.io.File;
import java.util.Vector;

import net.janbuchinger.code.fssync.sync.EditDBsFilenameFilter;

/**
 * Operation is a core class of FSSync. This class defines a synchronization
 * procedure with its source and target directory and has options and
 * statistics. This class is being serialized by Gson.
 * 
 * @author Jan Buchinger
 *
 */
public class Operation {
	/**
	 * Priority on Conflict: Select source
	 */
	public final static int PRIORITY_SOURCE = 0;
	/**
	 * Priority on Conflict: Select target
	 */
	public final static int PRIORITY_TARGET = 1;
	/**
	 * Priority on Conflict: Select new
	 */
	public final static int PRIORITY_NEW = 2;
	/**
	 * Priority on Conflict: Select old
	 */
	public final static int PRIORITY_OLD = 3;

	/**
	 * Key for interval value defined as days
	 */
	public static final transient int INTERVAL_DAYS = 0;
	/**
	 * Key for interval value defined as hours
	 */
	public static final transient int INTERVAL_HOURS = 1;
	/**
	 * Key for interval value defined as minutes
	 */
	public static final transient int INTERVAL_MINUTES = 2;

	/**
	 * singleton like file for performance optimization
	 */
	private transient File dbOriginal;
	/**
	 * Transient field isSelected for UI selection false until selected by user
	 */
	private transient boolean isSelected;
	/**
	 * The source directory of the operation
	 */
	private File source;
	/**
	 * The target directory of the operation
	 */
	private File target;
	/**
	 * Option manage Versions - currently not used
	 */
	private boolean manageVersions;
	/**
	 * The list of directories to exclude
	 */
	private Vector<String> exclude;
	/**
	 * Option synchronize bidirectional
	 */
	private boolean syncBidirectional;
	/**
	 * Option ignore modification date when checksum and length are not modified
	 */
	private boolean ignoreModifiedWhenEqual;
	/**
	 * Option elastic comparison: assume file in target as unchanged when the
	 * modification date does not abbreviate more than +/- 1 second.
	 */
	private boolean compareElastic;
	/**
	 * Option quick synchronization by default
	 */
	private boolean alwaysQuickSync;
	/**
	 * Priority on Conflict mode
	 * 
	 */
	private int priorityOnConflict;

	/**
	 * Timestamp of last synchronization, set when synchronization process finishes
	 * an operation.
	 */
	private long lastSynced;
	/**
	 * The value of the remind interval in days, hours or minutes
	 */
	private int interval;
	/**
	 * the indicator if <code>interval</code> is days, hours or minutes
	 */
	private int intervalMode;
	/**
	 * Option remind
	 */
	private boolean remind;
	/**
	 * indicator if already reminded, false by default
	 */
	private boolean reminded;

	/**
	 * Statistics: running time, 0 by default
	 */
	private long runningTimeQuickAnalysisAvg;
	private long runningTimeDeepAnalysisAvg;
	private long runningTimeSynchronizationAvg;
	/**
	 * Statistics: run count, 0 by default
	 */
	private int runCountQuickAnalysis;
	private int runCountDeepAnalysis;
	private int runCountSynchronization;
	/**
	 * Statistics: files, 0 by default
	 */
	private double transferredMiBAvg;
	private long totalFilesCopiedCount;

	/**
	 * Constructs a new Operation. Only called by <code>OperationEditorDialog</code>
	 * when adding a new <code>Operation</code>.
	 * 
	 * @param source
	 *            The source directory
	 * @param target
	 *            The target directory
	 * @param manageVersions
	 *            Option manage versions
	 * @param exclude
	 *            list of directories to exclude
	 * @param syncBidirectional
	 *            Option bidirectional synchronization
	 * @param ignoreModifiedWhenEqual
	 *            Option ignore modification date when checksum and file length
	 *            match.
	 * @param compareElastic
	 *            Option elastic comparison.
	 * @param alwaysQuickSync
	 *            Option quick sync
	 * @param priorityOnConflict
	 *            Priority on conflict mode
	 * @param interval
	 *            The interval
	 * @param intervalMode
	 *            interval unit
	 * @param remind
	 *            option remind due
	 */
	public Operation(File source, File target, boolean manageVersions, Vector<String> exclude,
			boolean syncBidirectional, boolean compareElastic,
			boolean alwaysQuickSync, int priorityOnConflict, int interval, int intervalMode, boolean remind) {
		this.source = source;
		this.target = target;
		this.manageVersions = manageVersions;
		this.exclude = exclude;
		this.syncBidirectional = syncBidirectional;
		this.ignoreModifiedWhenEqual = false;
		this.compareElastic = compareElastic;
		this.alwaysQuickSync = alwaysQuickSync;
		this.priorityOnConflict = priorityOnConflict;
		this.lastSynced = 0;
		this.interval = interval;
		this.intervalMode = intervalMode;
		this.remind = remind;
	}

	/**
	 * Checks if the <code>Operation</code> is online.
	 * 
	 * @return true only when in the source directory files like .fs.edit.db or
	 *         .fs.edit1.db contains and in the target directory the file .fs.db
	 *         exist.
	 */
	public boolean isOnline() {
		return isSourceOnline() && isTargetOnline();
	}

	/**
	 * Checks if the source directory contains files like .fs.edit.db or
	 * .fs.edit1.db
	 * 
	 * @return true only if there are files found
	 */
	public boolean isSourceOnline() {
		File[] fx = source.listFiles(new EditDBsFilenameFilter());
		if (fx != null) {
			if (fx.length > 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Creates a file to the "original" db file (.fs.db) in the target directory.
	 * 
	 * @return The original database file.
	 */
	public File getDbOriginal() {
		if (dbOriginal == null) {
			dbOriginal = new File(target, ".fs.db");
		}
		return dbOriginal;
	}

	/**
	 * Checks if the target data base file exists.
	 * 
	 * @return <code>true</code> if the target data base file exists.
	 */
	public boolean isTargetOnline() {
		return getDbOriginal().exists();
	}

	/**
	 * Gets the source directory file.
	 * 
	 * @return The source directory file.
	 */
	public final File getSource() {
		return source;
	}

	/**
	 * Sets the source directory file.
	 * 
	 * @param source
	 *            The new source directory file.
	 */
	public final void setSource(File source) {
		this.source = source;
	}

	/**
	 * Gets the path <code>String</code> to the source directory.
	 * 
	 * @return The path <code>String</code> to the source directory.
	 */
	public String getSourcePath() {
		return source.getPath();
	}

	/**
	 * Gets the target directory file.
	 * 
	 * @return The target directory file.
	 */
	public final File getTarget() {
		return target;
	}

	/**
	 * Sets the target directory file.
	 * 
	 * @param target
	 *            the target directory file.
	 */
	public final void setTarget(File target) {
		this.target = target;
	}

	/**
	 * Gets the target directory path <code>String</code>.
	 * 
	 * @return the target directory path <code>String</code>.
	 */
	public String getTargetPath() {
		return target.getPath();
	}

	/**
	 * Gets the <code>Operation</code> option "manage versions".
	 * 
	 * @return <code>true</code> if "manage versions" is on.
	 */
	public final boolean isManageVersions() {
		return manageVersions;
	}

	/**
	 * Sets the <code>Operation</code> option "manage versions".
	 * 
	 * @param manageVersions
	 *            <code>true</code> to turn "manage versions" on.
	 */
	public final void setManageVersions(boolean manageVersions) {
		this.manageVersions = manageVersions;
	}

	/**
	 * Gets the list of directories to exclude from synchronization.
	 * 
	 * @return the list of directories to exclude from synchronization.
	 */
	public final Vector<String> getExcludes() {
		return exclude;
	}

	/**
	 * Sets the list of directories to exclude from synchronization.
	 * 
	 * @param exclude
	 *            the list of directories to exclude from synchronization.
	 */
	public final void setExcludes(Vector<String> exclude) {
		this.exclude = exclude;
	}

	/**
	 * Gets the <code>Operation</code> option "synchronize bidirectional".
	 * 
	 * @return <code>true</code> if "synchronize bidirectional" is on.
	 */
	public final boolean isSyncBidirectional() {
		return syncBidirectional;
	}

	/**
	 * Sets the <code>Operation</code> option "synchronize bidirectional".
	 * 
	 * @param syncBidirectional
	 *            <code>true</code> to turn "synchronize bidirectional" on.
	 */
	public final void setSyncBidirectional(boolean syncBidirectional) {
		this.syncBidirectional = syncBidirectional;
	}

	/**
	 * Gets the deprecated option "ignore modified when equal"
	 * <p>
	 * This method is only used after loading the <code>Segments</code> to
	 * automatically turn it off and turn on "elastic comparison" instead.
	 * 
	 * @return <code>true</code> if "ignore modified when equal" is on.
	 */
	public final boolean isIgnoreModifiedWhenEqual() {
		return ignoreModifiedWhenEqual;
	}

	/**
	 * Sets the <code>Operation</code> option "ignore modified when equal".
	 * <p>
	 * This method is only used after loading the <code>Segments</code> to
	 * automatically turn it off and turn on "elastic comparison" instead.
	 * 
	 * @param ignoreModifiedWhenEqual
	 *            <code>true</code> to turn on "ignore modified when equal".
	 */
	public final void setIgnoreModifiedWhenEqual(boolean ignoreModifiedWhenEqual) {
		this.ignoreModifiedWhenEqual = ignoreModifiedWhenEqual;
	}

	/**
	 * Gets the <code>Operation</code> option "compare elastic".
	 * 
	 * @return <code>true</code> if "compare elastic" is turned on.
	 */
	public boolean isCompareElastic() {
		return compareElastic;
	}

	/**
	 * Sets the <code>Operation</code> option "compare elastic".
	 * 
	 * @param compareElastic
	 *            <code>true</code> to turn "compare elastic" on.
	 */
	public void setCompareElastic(boolean compareElastic) {
		this.compareElastic = compareElastic;
	}

	/**
	 * Gets the <code>Operation</code> option "quick sync".
	 * 
	 * @return <code>true</code> if "quick sync" is on.
	 */
	public boolean isAlwaysQuickSync() {
		return alwaysQuickSync;
	}

	/**
	 * Sets the <code>Operation</code> option "quick sync".
	 * 
	 * @param alwaysQuickSync
	 *            <code>true</code> to turn on "quick sync".
	 */
	public void setAlwaysQuickSync(boolean alwaysQuickSync) {
		this.alwaysQuickSync = alwaysQuickSync;
	}

	/**
	 * Gets the priority on conflict.
	 * 
	 * @return The priority on conflict.
	 */
	public final int getPriorityOnConflict() {
		return priorityOnConflict;
	}

	/**
	 * Sets the priority on conflict.
	 * 
	 * @param priorityOnConflict
	 *            The priority on conflict.
	 */
	public final void setPriorityOnConflict(int priorityOnConflict) {
		this.priorityOnConflict = priorityOnConflict;
	}

	/**
	 * Gets if the <code>Operation</code> is selected.
	 * 
	 * @return <code>true</code> if the <code>Operation</code> was selected by the
	 *         user.
	 */
	public final boolean isSelected() {
		return isSelected;
	}

	/**
	 * Sets the <code>Operation</code> selected.
	 * 
	 * @param isSelected
	 *            <code>true</code> to select the <code>Operation</code>.
	 */
	public final void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	/**
	 * Gets the time when this <code>Operation</code> was previously synchronized.
	 * 
	 * @return
	 */
	public final long getLastSynced() {
		return lastSynced;
	}

	/**
	 * Sets the previously synchronized time.
	 * 
	 * @param lastSynced
	 *            The time when the <code>Operation</code> was synchronized.
	 */
	public final void setLastSynced(long lastSynced) {
		this.lastSynced = lastSynced;
	}

	/**
	 * Gets the interval in which the <code>Operation</code> should be synchronized.
	 * 
	 * @return The interval in which the <code>Operation</code> is supposed to be
	 *         synchronized.
	 * 
	 * @see Operation#getIntervalMode()
	 */
	public final int getInterval() {
		return interval;
	}

	/**
	 * Sets the interval in which this <code>Operation</code> is supposed to be
	 * synchronized.
	 * 
	 * @param interval
	 *            The interval in which this <code>Operation</code> is supposed to
	 *            be synchronized.
	 * 
	 * @see Operation#getIntervalMode()
	 */
	public final void setInterval(int interval) {
		this.interval = interval;
	}

	/**
	 * Gets the interval mode in which the value of the interval should be
	 * interpreted.
	 * 
	 * @return The interval mode in which the interval should be interpreted
	 * 
	 * @see Operation#INTERVAL_DAYS
	 * @see Operation#INTERVAL_HOURS
	 * @see Operation#INTERVAL_MINUTES
	 */
	public final int getIntervalMode() {
		return intervalMode;
	}

	/**
	 * Sets the interval mode in which the interval should be interpreted.
	 * 
	 * @param intervalMode
	 *            the interval mode in which the interval should be interpreted.
	 * 
	 * @see Operation#INTERVAL_DAYS
	 * @see Operation#INTERVAL_HOURS
	 * @see Operation#INTERVAL_MINUTES
	 */
	public final void setIntervalMode(int intervalMode) {
		this.intervalMode = intervalMode;
	}

	/**
	 * Gets the interval in milliseconds.
	 * 
	 * @return the interval in milliseconds.
	 */
	private long getIntervalMillis() {
		if (intervalMode == INTERVAL_HOURS) {
			return interval * 60 * 60 * 1000;
		} else if (intervalMode == INTERVAL_MINUTES) {
			return interval * 60 * 1000;
		} else {
			// INTERVAL_DAYS by default
			return interval * 24 * 60 * 60 * 1000;
		}
	}

	/**
	 * Gets if the remind option is turned on.
	 * 
	 * @return <code>true</code> if the remind option is turned on.
	 */
	public final boolean isRemind() {
		return remind;
	}

	/**
	 * Sets the remind option.
	 * 
	 * @param remind
	 *            <code>true</code> to turn remind on.
	 */
	public final void setRemind(boolean remind) {
		this.remind = remind;
	}

	/**
	 * Gets if the due <code>Operation</code> was reminded already.
	 * 
	 * @return <code>true</code> if the user was already notified about the due
	 *         <code>Operation</code>.
	 */
	public final boolean isReminded() {
		return reminded;
	}

	/**
	 * Sets the <code>Operation</code> reminded.
	 * 
	 * @param reminded
	 *            <code>true</code> if the user was reminded about the due
	 *            <code>Operation</code>.
	 */
	public final void setReminded(boolean reminded) {
		this.reminded = reminded;
	}

	/**
	 * Gets if the operation is due to be synchronized.
	 * 
	 * @return <code>false</code> if the interval is zero or if the
	 *         <code>Operation</code> is not due.
	 *         <p>
	 *         <code>true</code> if the interval is greater than zero and the
	 *         <code>Operation</code> was never synchronized or is due.
	 */
	public boolean isDue() {
		if (interval == 0) {
			return false;
		} else if (lastSynced == 0) {
			return true;
		} else if ((lastSynced + getIntervalMillis()) < System.currentTimeMillis()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Registers a quick analysis to the statistics.
	 * 
	 * @param duration
	 *            The duration of the quick analysis.
	 */
	public final void registerQuickAnalysis(long duration) {
		runCountQuickAnalysis++;
		if (runCountQuickAnalysis == 1) {
			this.runningTimeQuickAnalysisAvg = duration;
		} else {
			this.runningTimeQuickAnalysisAvg = ((runningTimeQuickAnalysisAvg * (runCountQuickAnalysis - 1))
					+ duration) / runCountQuickAnalysis;
		}
	}

	/**
	 * Gets the total running time of quick analysis.
	 * 
	 * @return the total running time of quick analysis.
	 */
	public long getRunningTimeQuickAnalysis() {
		return runningTimeQuickAnalysisAvg * runCountQuickAnalysis;
	}

	/**
	 * Gets the number of times a quick analysis was done on this
	 * <code>Operation</code>.
	 * 
	 * @return the number of times a quick analysis was done on this
	 *         <code>Operation</code>.
	 */
	public int getRunCountQuickAnalysis() {
		return runCountQuickAnalysis;
	}

	/**
	 * Registers a deep analysis to the statistics.
	 * 
	 * @param duration
	 *            The duration of the deep analysis.
	 */
	public final void registerDeepAnalysis(long duration) {
		runCountDeepAnalysis++;
		if (runCountDeepAnalysis == 1) {
			this.runningTimeDeepAnalysisAvg = duration;
		} else {
			this.runningTimeDeepAnalysisAvg = ((runningTimeDeepAnalysisAvg * (runCountDeepAnalysis - 1))
					+ duration) / runCountDeepAnalysis;
		}
	}

	/**
	 * Gets the total running time of deep analysis.
	 * 
	 * @return the total running time of deep analysis.
	 */
	public long getRunningTimeDeepAnalysis() {
		return runningTimeDeepAnalysisAvg * runCountDeepAnalysis;
	}

	/**
	 * Gets the number of times deep analysis was performed on this
	 * <code>Operation</code>.
	 * 
	 * @return the number of times deep analysis was performed on this
	 *         <code>Operation</code>.
	 */
	public int getRunCountDeepAnalysis() {
		return runCountDeepAnalysis;
	}

	/**
	 * Registers a synchronization in the statistics.
	 * 
	 * @param duration
	 *            The duration of the synchronization.
	 * @param bytes
	 *            The total bytes transferred.
	 * @param nFiles
	 *            The number of files transferred.
	 */
	public final void registerSynchronisation(long duration, long bytes, int nFiles) {
		double mib = (double) bytes / 1024.0 / 1024.0;
		runCountSynchronization++;
		if (runCountSynchronization == 1) {
			this.runningTimeSynchronizationAvg = duration;
			this.transferredMiBAvg = mib;
		} else {
			this.runningTimeSynchronizationAvg = ((runningTimeSynchronizationAvg
					* (runCountSynchronization - 1)) + duration) / runCountSynchronization;
			this.transferredMiBAvg = ((transferredMiBAvg * (runCountSynchronization - 1)) + mib)
					/ runCountSynchronization;
		}
		this.totalFilesCopiedCount += nFiles;
	}

	/**
	 * Gets the number of times a synchronization was performed.
	 * 
	 * @return the number of times a synchronization was performed.
	 */
	public int getRunCountSynchronization() {
		return runCountSynchronization;
	}

	/**
	 * Gets the total synchronization running time.
	 * 
	 * @return the total synchronization running time.
	 */
	public long getRunningTimeDataCopy() {
		return runningTimeSynchronizationAvg * runCountSynchronization;
	}

	/**
	 * Gets the average MiB transferred.
	 * 
	 * @return the average MiB transferred.
	 */
	public double getTransferredMegaBytesAverage() {
		return transferredMiBAvg;
	}

	/**
	 * Gets the total files copied count.
	 * 
	 * @return the total files copied count.
	 */
	public long getTotalFilesCopiedCount() {
		return totalFilesCopiedCount;
	}

	/**
	 * Gets the average analyze time for quick analysis.
	 * 
	 * @return the average analyze time for quick analysis or 0 if no quick analysis
	 *         has been registered yet.
	 */
	public long getAverageAnalyseTimeQuick() {
		return runningTimeQuickAnalysisAvg;
	}

	/**
	 * Gets the average analyze time for deep analysis.
	 * 
	 * @return the average analyze time for deep analysis or 0 if no deep analysis
	 *         has been registered yet.
	 */
	public long getAverageAnalyseTimeDeep() {
		return runningTimeDeepAnalysisAvg;
	}

	/**
	 * Gets the average synchronization time relative to the update size.
	 * 
	 * @param updateSize
	 *            The total bytes to be transferred.
	 * 
	 * @return the average synchronization time relative to the update size or 0 if
	 *         no synchronization has been performed yet.
	 */
	public long getAverageSyncTime(long updateSize) {
		if (transferredMiBAvg > 0.0) {
			return (long) ((runningTimeSynchronizationAvg / transferredMiBAvg)
					* (updateSize / 1024.0 / 1024.0));
		} else {
			return 0;
		}
	}

	/**
	 * Gets the average synchronization time.
	 * 
	 * @return the average synchronization time or 0 if no synchronization has been
	 *         performed yet.
	 */
	public long getAverageSyncTime() {
		return runningTimeSynchronizationAvg;
	}

	/**
	 * sets all statistical values to 0.
	 */
	public final void clearStats() {
		runningTimeQuickAnalysisAvg = 0;
		runningTimeDeepAnalysisAvg = 0;
		runningTimeSynchronizationAvg = 0;
		runCountQuickAnalysis = 0;
		runCountDeepAnalysis = 0;
		runCountSynchronization = 0;
		transferredMiBAvg = 0.0;
		totalFilesCopiedCount = 0;
	}

	/**
	 * Compares this <code>Operation</code>s relation to another
	 * <code>Operation</code>.
	 * 
	 * @param other
	 *            The <code>Operation</code> for comparison.
	 * 
	 * @return 0 if the <code>Operation</code>s have no relation.
	 *         <p>
	 *         1 if the specified <code>Operation</code> precedes this
	 *         <code>Operation</code>.
	 */
	public int compareTo(Operation other) {
		// this operations source path
		String thisOpSourcePath = getSourcePath();
		// the other operations source path
		String otherOpSourcePath = other.getSourcePath();
		// the other operations target path
		String otherOpTargetPath = other.getTargetPath();
		// if other target starts with this source
		if (otherOpTargetPath.startsWith(thisOpSourcePath)) {
			// the other operation precedes this operation
			return 1;
			// if the other operation synchronizes bidirectional, this operation does not
			// synchronize bidirectional and the other source path starts with this
			// operations source path
		} else if (other.isSyncBidirectional() && !syncBidirectional
				&& otherOpSourcePath.startsWith(thisOpSourcePath)) {
			// then also the other Operation precedes this operation
			return 1;
		} else {
			// the operations have no relation
			return 0;
		}
	}

	/**
	 * Determines if this <code>Operation</code> is equal to another
	 * <code>Operation</code>.
	 * 
	 * @param o
	 *            The other <code>Operation</code>.
	 * @return <code>true</code> if both source and target directory of the two
	 *         operations are equal. <code>false</code> otherwise.
	 */
	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		} else if (o instanceof Operation) {
			Operation op2 = (Operation) o;
			return op2.getSourcePath().equals(getSourcePath()) && op2.getTargetPath().equals(getTargetPath());
		} else {
			return false;
		}
	}

	/**
	 * Gets the string representation of this <code>Operation</code> with the source
	 * directory on the left hand, the synchronization operator in the middle and
	 * the target directory on the right.
	 * <p>
	 * example:
	 * <p>
	 * /home/user >> /media/user/backup/user
	 */
	@Override
	public String toString() {
		return source.getPath() + (syncBidirectional ? " <> " : " >> ") + target.getPath();
	}
}
