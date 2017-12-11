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
package net.janbuchinger.code.fssync.sync.ui;

public class GetRetryOnOutOfMemory implements Runnable {
	private final SynchronisationProcessDialog spd;
	private final String storage;
	private final long updateSize;
	private boolean retry;

	public GetRetryOnOutOfMemory(SynchronisationProcessDialog spd, String storage, long updateSize) {
		this.spd = spd;
		this.storage = storage;
		this.updateSize = updateSize;

		retry = true;
	}

	@Override
	public void run() {
		retry = spd.retryOnOutOfMemoryWarning(storage, updateSize);
	}

	public final boolean isRetry() {
		return retry;
	}
}
