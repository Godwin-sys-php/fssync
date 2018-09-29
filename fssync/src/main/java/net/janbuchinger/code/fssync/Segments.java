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
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * This class is holding the Segments and provides utility functions.
 * <p>
 * It is intended for serialization with Gson.
 * 
 * @author Jan Buchinger
 *
 */
public class Segments {
	/**
	 * Segments singleton
	 */
	private static transient Segments SEGMENTS;

	/**
	 * Gets either the central Segments singleton or a newly created clone from the
	 * current <code>Segments</code> file.
	 * 
	 * clone is <code>true</code> is used for restoration dialog/process, for the
	 * <code>UIChangeWatcherThread</code> and the <code>TrayReminderThread</code>.
	 * 
	 * @param clone
	 *            <code>true</code> for a newly created <code>Segments</code> from
	 *            the current <code>Segments</code> file.
	 * 
	 * @return The requested <code>Segments</code>.
	 */
	public static Segments getSegments(boolean clone) {
		Segments s = null;
		if (SEGMENTS == null || clone) {
			Gson g = new Gson();
			// get the segments file
			File segmentsFile = FSSyncPaths.getSegmentsFile();
			// if not clone
			if (!clone) {
				// then initialize the singleton object
				try {
					// get Segments from JSON File
					SEGMENTS = g.fromJson(FileUtils.readFileToString(segmentsFile, Charset.defaultCharset()),
							Segments.class);
					// set the segments file for saving
					SEGMENTS.setSegmentsFile(segmentsFile);
				} catch (JsonSyntaxException | IOException e) {
					// JSON bad or file not found
					// initialize the singleton object with the segments file
					SEGMENTS = new Segments(segmentsFile);
				}
				s = SEGMENTS;
			} else {
				// initialize a clone
				try {
					// get current Segments from file.
					s = g.fromJson(FileUtils.readFileToString(segmentsFile, Charset.defaultCharset()),
							Segments.class);
					// do not set segments file for clone
				} catch (JsonSyntaxException | IOException e) {
					// JSON bad or file not found
					// initialize the clone with segments file is null
					s = new Segments(null);
				}
			}
			// update old configuration
			disableIgnoreModifiedWhenEqual(s);
		} else {
			// Segments is not null and clone is false
			s = SEGMENTS;
		}
		// return the requested segments
		return s;
	}

	/**
	 * search for out dated configuration change old option "ignore modification
	 * date when files equal" to elastic comparison.
	 * 
	 * @param s
	 *            The <code>Segments</code> to check.
	 */
	private static void disableIgnoreModifiedWhenEqual(Segments s) {
		// assume up to date
		boolean reconfigured = false;
		// loop through all Segments
		for (Segment segment : s.getData()) {
			// and operations
			for (Operation operation : segment.getOperations()) {
				// if the deprecated option is used
				if (operation.isIgnoreModifiedWhenEqual()) {
					// then disable it
					operation.setIgnoreModifiedWhenEqual(false);
					// and fall back on compare elastic
					operation.setCompareElastic(true);
					// indicate saving is needed
					reconfigured = true;
				}
			}
		}
		// save if necessary
		if (reconfigured) {
			s.save();
		}
	}

	/**
	 * the file representation of this class
	 */
	private transient File segmentsFile;
	/**
	 * the segments
	 */
	private ArrayList<Segment> segments;

	/**
	 * Construct a new, empty Segments object.
	 * 
	 * @param segmentsFile
	 *            The file where the segments are stored.
	 *            <p>
	 *            <code>null</code> if the <code>Segments</code> is a clone.
	 */
	private Segments(File segmentsFile) {
		// set the settings file
		this.segmentsFile = segmentsFile;
		// initialize empty list
		this.segments = new ArrayList<Segment>();
	}

	/**
	 * writes this Segments object to a Json file.
	 */
	public final void save() {
		// abort saving cloned Segments
		if (segmentsFile == null) {
			return;
		}
		// set up Gson for pretty printing
		GsonBuilder gb = new GsonBuilder();
		gb.setPrettyPrinting();
		Gson g = gb.create();
		// create json String
		String j = g.toJson(this);
		try {
			// write json String to file
			FileUtils.writeStringToFile(segmentsFile, j, Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Removes the specified Segment Object (if found) and saves Segments to file.
	 * 
	 * @param o
	 *            The Segment object to be removed.
	 * @return true if the list contained the specified Segment.
	 */
	public boolean remove(Object o) {
		// if o is not a Segment object
		if (!(o instanceof Segment)) {
			// then abort
			return false;
		} else {
			// try to remove the segment
			boolean b = segments.remove(o);
			// if remove was successful
			if (b) {
				// then save the segments to file
				save();
			}
			// return true for success
			return b;
		}
	}

	/**
	 * Sorts the segments and operations.
	 * 
	 * @see Segment#sort() Segment.sort() for alternative comments.
	 */
	public final void sort() {
		// first sort the operations within the segments
		for (Segment segment : segments) {
			segment.sort();
		}
		// temp variables for comparison
		Segment segA, segB;
		// the list size for loops
		int size = segments.size();
		// indicator that an element has been moved from j to i
		boolean stepBack = false;
		// fail safe counter for circular relations
		int counter = 0;
		// sort loop
		for (int i = 0; i < size; i++) {
			// if an element has been inserted at i in the previous round, decrement i again
			if (stepBack) {
				i--;
				stepBack = false;
			}
			// outer loop element
			segA = segments.get(i);
			// search if an element after i should be put before segA (at i)
			for (int j = i + 1; j < size; j++) {
				// inner loop element
				segB = segments.get(j);
				// if segment segB has an operation that writes to segA then put segB before
				// segA
				if (segA.compareTo(segB) > 0) {
					segments.remove(j);
					segments.add(i, segB);
					// indicate that the list has changed
					if (!stepBack) {
						stepBack = true;
					}
				}
			}
			counter++;
			// fail safe, should not be possible to happen (anymore).
			if (counter > 300) {
				System.err.println("Circular relation between different segments");
				break;
			}
		}
	}

	/**
	 * Checks if the new configuration creates a circular relation.
	 * 
	 * @param source
	 *            The new source directory.
	 * @param target
	 *            The new target directory.
	 * @param operation
	 *            The operation that is being edited, null if new operation.
	 * @return Returns true if target path starts with source path or if the target
	 *         path leads to the source path again.
	 */
	public final boolean createsCircularRelation(File source, File target, Operation operation) {
		// list of target files
		Vector<File> nextTargets = new Vector<>();
		// initialize targets with the new Operation target
		nextTargets.add(target);
		// tmp list of next targets is being built while the current next targets is
		// being queried
		Vector<File> nextTargetsTmp;
		// finished is false until there are no more targets
		boolean finished = false;
		// loop until finished
		while (!finished) {
			// if a target file points to the source file return true
			for (File targetCompare : nextTargets) {
				if (targetCompare.getPath().startsWith(source.getPath())) {
					// circular relation identified
					return true;
				}
			}
			// initialize next targets for this round
			nextTargetsTmp = new Vector<>();
			// loop through all Segments and Operations
			for (Segment segment : segments) {
				for (Operation opCompare : segment.getOperations()) {
					// exclude current Operation
					if (opCompare != operation) {
						// loop through targets from previous round
						for (File nextTarget : nextTargets) {
							// if the current operation source starts with a target path
							if (opCompare.getSource().getPath().startsWith(nextTarget.getPath())) {
								// then add the current operation target to nextTargetsTmp
								nextTargetsTmp.add(opCompare.getTarget());
							}
						}
					}
				}
			}
			// nextTargets is being set for the next round
			nextTargets = nextTargetsTmp;
			// if there are no more targets then end the loop
			if (nextTargets.size() == 0) {
				finished = true;
			}
		}
		// there is no circular relation
		return false;
	}

	/**
	 * Checks if the target directory for an unidirectional operation points to or
	 * inside the source directory of another bidirectional operation.
	 * 
	 * @param target
	 *            the target directory to be checked.
	 * 
	 * @param operation
	 *            the currently edited operation or null if the operation is new
	 * 
	 * @return true only when the given target directory points to or inside the
	 *         source directory of another bidirectional operation.
	 */
	public final boolean targetIsBidirectionalSource(File target, Operation operation) {
		// loop through all segments and operations
		for (Segment segment : segments) {
			for (Operation opTmp : segment.getOperations()) {
				// do not compare with self
				if (opTmp != operation) {
					// if the other operation is bidirectional and the target path to be checked
					// points to or inside the other operations source path
					if (opTmp.isSyncBidirectional() && target.getPath().startsWith(opTmp.getSourcePath())) {
						// then return true
						return true;
					}
				}
			}
		}
		// target does not point to or inside another bidirectional operations source
		// directory
		return false;
	}

	/**
	 * Gets the number of available Segments.
	 * 
	 * @return The Segments list size.
	 */
	public int size() {
		return segments.size();
	}

	/**
	 * Gets the Segment at the specified location.
	 * 
	 * @param i
	 *            The index of the requested Segment.
	 * @return the requested Segment or null if the i is not a valid index.
	 */
	public Segment get(int i) {
		try {
			return segments.get(i);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	/**
	 * Add a new Segment.
	 * 
	 * @param segment
	 *            the Segment to be added.
	 */
	public void add(Segment segment) {
		segments.add(segment);
	}

	/**
	 * Sets the file where this object is stored as JSON.
	 * 
	 * @param segmentsFile
	 *            The location of "sync.json".
	 */
	public final void setSegmentsFile(File segmentsFile) {
		this.segmentsFile = segmentsFile;
	}

	/**
	 * Used by restoration dialog to restore in reverse order.
	 */
	public void reverse() {
		for (Segment segment : segments) {
			Collections.reverse(segment.getOperations());
		}
		Collections.reverse(segments);
	}

	/**
	 * sets all operations deselected
	 */
	public final void selectNone() {
		for (Segment segment : segments) {
			segment.selectNone();
		}
	}

	/**
	 * Gets the actual list of segments.
	 * 
	 * @return the actual list of segments.
	 */
	public final ArrayList<Segment> getData() {
		return segments;
	}
}
