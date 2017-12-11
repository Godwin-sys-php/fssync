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

import net.janbuchinger.code.fssync.sync.OperationSummary;

public class GetSummaryApproval implements Runnable {

	private final SynchronisationProcessDialog spd;
	private boolean isApproved;
	private final boolean isBiDirectional;
	private final OperationSummary operationSummary;
	private final int priorityOnConflict;

	public GetSummaryApproval(SynchronisationProcessDialog spd, OperationSummary operationSummary,
			boolean isBiDirectional, int priorityOnConflict) {
		this.spd = spd;
		this.isBiDirectional = isBiDirectional;
		this.operationSummary = operationSummary;
		this.priorityOnConflict = priorityOnConflict;
		isApproved = false;
	}

	@Override
	public void run() {
		isApproved = spd.approveSummary(operationSummary, isBiDirectional, priorityOnConflict);
	}

	public final boolean isApproved() {
		return isApproved;
	}
}
