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
package net.janbuchinger.code.fssync;

import java.util.ArrayList;
import java.util.Iterator;

public class Segment implements Comparable<Segment> {
	private String segmentName;
	private ArrayList<Operation> operations;

	public Segment(String name) {
		this.segmentName = name;
		this.operations = new ArrayList<Operation>();
	}

	public final String getName() {
		return segmentName;
	}

	public final void setName(String name) {
		this.segmentName = name;
	}

	public final Iterator<Operation> iterator() {
		return operations.iterator();
	}

	@Override
	public int compareTo(Segment s) {
		Iterator<Operation> iThis, iOther;
		iThis = iterator();
		Operation sOp, sOpOther;
		String sOpSrc, sOpTrg, sOpOtSrc, sOpOtTrg;
		while (iThis.hasNext()) {
			sOp = iThis.next();
			sOpSrc = sOp.getSource().getPath();
			sOpTrg = sOp.getTarget().getPath();
			iOther = s.iterator();
			while (iOther.hasNext()) {
				sOpOther = iOther.next();
				sOpOtSrc = sOpOther.getSource().getPath();
				sOpOtTrg = sOpOther.getTarget().getPath();
				if (sOpOtTrg.startsWith(sOpSrc)) {
					return 1;
				} else if (sOpOtSrc.startsWith(sOpTrg)) {
					return -1;
				}
			}
		}
		return 0;
	}

	@Override
	public synchronized String toString() {
		return segmentName;
	}

	public int size() {
		return operations.size();
	}

	public Operation get(int index) {
		return operations.get(index);
	}

	public Operation remove(int index) {
		return operations.remove(index);
	}

	public Operation set(int index, Operation element) {
		return operations.set(index, element);
	}

	public void add(Operation element) {
		operations.add(element);
	}

	public final ArrayList<Operation> getOperations() {
		return operations;
	}
}
