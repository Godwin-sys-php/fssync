/*
 * Copyright 2018 Jan Buchinger
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

public class RunRemindTray implements Runnable {
	private final FSSyncUI ui;
	private final Operation o;
	
	public RunRemindTray(FSSyncUI ui, Operation o) {
		this.ui = ui;
		this.o = o;
	}
	
	@Override
	public void run() {
		ui.remind(o);
	}

}
