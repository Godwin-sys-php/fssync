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
package fs.sync.ui;

import java.util.Vector;

import fs.Operation;

public class GetRestoreSourceChoice implements Runnable {
	private final SynchronisationProcessDialog spd;
	private final Vector<Operation> duplicates;

	private int selection;

	public GetRestoreSourceChoice(SynchronisationProcessDialog spd, Vector<Operation> duplicates) {
		this.spd = spd;
		this.duplicates = duplicates;
		selection = -1;
	}

	@Override
	public void run() {
		selection = spd.requestSourceForRestore(duplicates);
	}

	public final int getSelection() {
		return selection;
	}
}
