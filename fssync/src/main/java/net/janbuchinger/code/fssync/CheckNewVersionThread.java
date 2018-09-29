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

import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import javax.swing.SwingUtilities;

import org.apache.commons.io.IOUtils;

/**
 * This <code>Runnable</code> is executed after the main UI is built and shown
 * and the setting to ignore the new version dialog is false.
 * 
 * @author Jan Buchinger
 *
 */
public class CheckNewVersionThread implements Runnable {
	/**
	 * The current version string
	 */
	private final String version;
	/**
	 * The main UI class
	 */
	private final FSSyncUI ui;

	/**
	 * Constructs a new <code>CheckNewVersionThread</code>
	 * 
	 * @param version
	 *            The program executable version
	 * 
	 * @param ui
	 *            The main UI object.
	 */
	public CheckNewVersionThread(String version, FSSyncUI ui) {
		this.version = version;
		this.ui = ui;
	}

	/**
	 * Checks if a new version of FSSync is available. If yes a dialog is shown to
	 * inform the user.
	 */
	@Override
	public void run() {
		try {
			try {
				// the URL where the last published version is stored as text
				URL url = new URL("https://code.janbuchinger.net/fssync/currentVersion");

				// try to read the version file from the web server.
				String currentVersion = IOUtils.toString(url.openStream(), Charset.forName("UTF-8")).trim();
				// if a string was read,
				if (currentVersion != null) {
					// if the read string length is longer than zero
					if (currentVersion.length() > 0) {
						// and if the current binary version does not equal the online version
						if (!currentVersion.equals(version)) {
							// then notify the user about the new version
							SwingUtilities.invokeLater(new RunNotifyNewVersion(ui, currentVersion));
						}
					}
				}
			} catch (UnknownHostException e) {
				// if there is no Internet connection...
				System.err.println(
						"could not retrieve current version from the web server; ip could not be resolved");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
