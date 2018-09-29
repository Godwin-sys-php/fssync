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

import java.util.ArrayList;

public class Segment {
	/**
	 * This Segments name
	 */
	private String segmentName;
	/**
	 * The list of operations contained in this segment
	 */
	private ArrayList<Operation> operations;

	/**
	 * Constructs a new Segment with an empty list of operations.
	 * 
	 * @param name
	 *            The new Segments name
	 */
	public Segment(String name) {
		this.segmentName = name;
		this.operations = new ArrayList<Operation>();
	}

	/**
	 * Gets the name of the segment.
	 * 
	 * @return the name of this segment.
	 */
	public final String getName() {
		return segmentName;
	}

	/**
	 * Sets the name for this segment.
	 * 
	 * @param name
	 *            The name to be set.
	 */
	public final void setName(String name) {
		this.segmentName = name;
	}

	/**
	 * Gets the operations list size.
	 * 
	 * @return The number of <code>Operation</code>s contained in this
	 *         <code>Segment</code>.
	 */
	public int size() {
		return operations.size();
	}

	/**
	 * Gets the <code>Operation</code> corresponding to the specified index.
	 * 
	 * @param index
	 *            The index of the requested Operation.
	 * 
	 * @return The <code>Operation</code> corresponding to the specified index.
	 */
	public Operation get(int index) {
		return operations.get(index);
	}

	/**
	 * Removes the <code>Operation</code> underlying the specified index.
	 * 
	 * @param index
	 *            The position of the <code>Operation</code> to be removed.
	 * 
	 * @return The <code>Operation</code> that was removed from the list.
	 */
	public Operation remove(int index) {
		return operations.remove(index);
	}

	/**
	 * Sets the specified <code>Operation</code> at the specified index replacing
	 * the <code>Operation</code> currently at that position.
	 * 
	 * @param index
	 *            The position of the <code>Operation</code> to be set.
	 * 
	 * @param element
	 *            The <code>Operation</code> to be set.
	 * 
	 * @return The <code>Operation</code> that was replaced.
	 */
	public Operation set(int index, Operation element) {
		return operations.set(index, element);
	}

	/**
	 * Adds a new <code>Operation</code> to this <code>Segment</code>s list of
	 * <code>Operation</code>s.
	 * 
	 * @param element
	 *            The <code>Operation</code> to be added.
	 */
	public void add(Operation element) {
		operations.add(element);
	}

	/**
	 * Gets this <code>Segment</code>s list of <code>Operation</code>s.
	 * 
	 * @return This <code>Segment</code>s list of <code>Operation</code>s.
	 */
	public final ArrayList<Operation> getOperations() {
		return operations;
	}

	/**
	 * Deselects all <code>Operation</code>s.
	 */
	public void selectNone() {
		for (Operation op : operations) {
			op.setSelected(false);
		}
	}

	/**
	 * Sorts the operations contained in this <code>Segment</code>.
	 */
	public final void sort() {
		// the operations to compare in different loops
		Operation op1, op2;
		// if an element was moved in front of another element
		// the index of the outer loop is decremented by 1
		boolean stepBack = false;
		// the operations list size
		int size = operations.size();
		// fail save counter for circular relation
		int counter = 0;
		// outer / main loop
		for (int i = 0; i < size; i++) {
			// if any element/s was/were moved to in front of the current element
			// then decrement the main loop counter by 1
			if (stepBack) {
				i--;
				stepBack = false;
			}
			// get the current operation to be inspected
			op1 = operations.get(i);
			// start the inner loop from the outer loop index + 1
			for (int j = i + 1; j < size; j++) {
				// the second operation
				op2 = operations.get(j);
				// if the second operation should precede the current operation
				if (op1.compareTo(op2) > 0) {
					// then move the second operation in front of the current operation
					operations.remove(j);
					operations.add(i, op2);
					// take a step back in the next outer loop
					if (!stepBack) {
						stepBack = true;
					}
				}
			}
			// fail save to exit on circular relation
			counter++;
			if (counter > 300) {
				System.err.println("Zirkulärbezug im Segment " + segmentName);
				break;
			}
		}
	}

	/**
	 * Compare this <code>Segment</code>s <code>Operation</code>s to another
	 * <code>Segment</code>s <code>Operation</code>s.
	 * 
	 * @param s
	 *            The <code>Segment</code> to compare with this
	 *            <code>Segment</code>.
	 * @return If any <code>Operation</code> of the specified <code>Segment</code> s
	 *         precedes any <code>Operation</code> of this <code>Segment</code> then
	 *         1 is returned, else 0.
	 */
	public int compareTo(Segment s) {
		// fail save to not compare with self
		if (this == s) {
			return 0;
		}
		// temporary return value of Operation.compareTo(Operation)
		int compareOp;
		// loop through this Segments Operations
		for (Operation o1 : operations) {
			// loop through the other Segments Operations
			for (Operation o2 : s.getOperations()) {
				// compare Operations
				compareOp = o1.compareTo(o2);
				// if the return value of the operation comparison is not 0 then it is 1,
				// indicating that o2 precedes o1
				if (compareOp != 0) {
					// return 1
					return compareOp;
				}
			} // end of other Segments operations loop
		} // end of this Segments operations loop
			// if no preceding operations were found, return 0
		return 0;
	}

	/**
	 * The <code>String</code> representation of a <code>Segment</code> is its name.
	 */
	@Override
	public String toString() {
		return segmentName;
	}
}
