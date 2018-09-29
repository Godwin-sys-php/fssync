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

import java.io.File;

import net.janbuchinger.code.mishmash.FSFx;

/**
 * This class contains all the file system paths relevant to run the program.
 *
 * The paths are stored in <code>File</code>s that are served in singleton
 * style.
 * 
 * @author Jan Buchinger
 *
 */
public class FSSyncPaths {
	/**
	 * the program directory called ".fssync" inside the user.home containing all
	 * the relevant data to run the program.
	 * <p>
	 * this directory is hidden under windows too.
	 * 
	 * @see FSFx#hideWindowsFile(File)
	 */
	private static File programDir;
	/**
	 * The settings file "settings.json" inside the program directory
	 */
	private static File settingsFile;
	/**
	 * The <code>Segments</code> JSON file system representation
	 */
	private static File segmentsFile;
	/**
	 * The documents directory "docs" inside the program directory
	 */
	private static File docsDir;
	/**
	 * The version file "version" inside the documents directory
	 */
	private static File versionFile;
	/**
	 * The file "lock.file" inside the program directory that is used to be locked
	 * by the only running instance of the program so no further running instances
	 * will be allowed
	 */
	private static File lockFile;

	/**
	 * Gets the program directory singleton <code>File</code>
	 * 
	 * @return The program directory singleton <code>File</code>
	 */
	public static File getProgramDir() {
		if (programDir == null) {
			programDir = new File(System.getProperty("user.home"), ".fssync");
			// create the hidden program directory if it doesn't exist yet
			if (!programDir.exists()) {
				if (programDir.mkdir()) {
					// hide the program directory under windows
					FSFx.hideWindowsFile(programDir);
				} else {
					// TODO exit system?
				}
			}
		}
		return programDir;
	}

	/**
	 * Gets the settings singleton <code>File</code>
	 * 
	 * @return The settings singleton <code>File</code>
	 */
	public static File getSettingsFile() {
		if (settingsFile == null) {
			settingsFile = new File(getProgramDir(), "settings.json");
		}
		return settingsFile;
	}

	/**
	 * Gets the segments singleton <code>File</code>
	 * 
	 * @return The segments singleton <code>File</code>
	 */
	public static File getSegmentsFile() {
		if (segmentsFile == null) {
			segmentsFile = new File(getProgramDir(), "sync.json");
		}
		return segmentsFile;
	}

	/**
	 * Gets the documents directory singleton <code>File</code>
	 * 
	 * @return The documents directory singleton <code>File</code>
	 */
	public static File getDocsDir() {
		if (docsDir == null) {
			docsDir = new File(getProgramDir(), "docs");
			if (!docsDir.exists()) {
				docsDir.mkdir();
			}
		}
		return docsDir;
	}

	/**
	 * Gets the currently installed version singleton <code>File</code>
	 * 
	 * @return The currently installed version singleton <code>File</code>
	 */
	public static File getVersionFile() {
		if (versionFile == null) {
			versionFile = new File(getDocsDir(), "version");
		}
		return versionFile;
	}

	/**
	 * Gets the lock singleton <code>File</code>
	 * 
	 * @return The lock singleton <code>File</code>
	 */
	public static File getLockFile() {
		if (lockFile == null) {
			lockFile = new File(getProgramDir(), "lock.file");
		}
		return lockFile;
	}
}
