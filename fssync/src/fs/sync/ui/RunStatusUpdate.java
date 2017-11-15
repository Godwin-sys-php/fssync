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

public final class RunStatusUpdate implements Runnable {

	private final String message;
	private final boolean verbose;
	private final SynchronisationProcessDialog spd;

	public RunStatusUpdate(String message, boolean verbose, SynchronisationProcessDialog spd) {
		this.message = message;
		this.verbose = verbose;
		this.spd = spd;
	}

	@Override
	public void run() {
		if (verbose) {
			spd.addStatusVerbose(message);
		} else {
			spd.addStatus(message);
		}
	}
}
