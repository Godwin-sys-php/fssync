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
package fs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Segments {

	private transient File segmentsFile;
	private ArrayList<Segment> segments;

	public Segments(File segmentsFile) {
		this.segmentsFile = segmentsFile;
		this.segments = new ArrayList<Segment>();
	}

	private final void write() {
		GsonBuilder gb = new GsonBuilder();
		gb.setPrettyPrinting();
		Gson g = gb.create();
		
		String j = g.toJson(this);
		try {
			FileUtils.writeStringToFile(segmentsFile, j, Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean remove(Object o) {
		boolean b = segments.remove(o);
		write();
		return b;
	}

	public final void save() {
		write();
	}

	public final void sort() {
		Segment segA, segB;

		int size = segments.size();
		int compare;
		Vector<Segment> vs = new Vector<>(segments);
		boolean wasBackStepped = false;
		for (int i = 0; i < size; i++) {
			if (wasBackStepped) {
				i--;
				wasBackStepped = false;
			}
			segA = vs.get(i);
			for (int j = i + 1; j < size; j++) {
				if (i != j) {
					segB = vs.get(j);
					compare = segA.compareTo(segB);
					if (compare > 0) {
						vs.remove(j);
						vs.insertElementAt(segB, i);
						if (!wasBackStepped) {
							wasBackStepped = true;
						}
					} else {

					}
				}
			}
		}
		segments.removeAll(segments);
		segments.addAll(vs);
	}

	public Iterator<Segment> iterator() {
		return segments.iterator();
	}

	public int size() {
		return segments.size();
	}

	public Segment get(int i) {
		return segments.get(i);
	}

	public void add(Segment segment) {
		segments.add(segment);
	}

	public final void setSegmentsFile(File segmentsFile) {
		this.segmentsFile = segmentsFile;
	}
}
