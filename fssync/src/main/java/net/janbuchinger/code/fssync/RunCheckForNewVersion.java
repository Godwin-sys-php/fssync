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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import javax.swing.SwingUtilities;

import org.apache.commons.io.IOUtils;

public class RunCheckForNewVersion implements Runnable {
	private final String version;
	private final FSSyncUI ui;

	public RunCheckForNewVersion(String version, FSSyncUI ui) {
		this.version = version;
		this.ui = ui;
	}

	@Override
	public void run() {
		try {
			URL url = new URL("http://code.janbuchinger.net/fssync/currentVersion");
			String currentVersion = IOUtils.toString(url.openStream(), Charset.forName("UTF-8"));
			if (!currentVersion.equals(version)) {
				SwingUtilities.invokeLater(new RunNotifyNewVersion(ui, currentVersion));
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
