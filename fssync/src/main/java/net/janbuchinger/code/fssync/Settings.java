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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import net.janbuchinger.code.mishmash.PropFx;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Settings {
	private static transient Settings SETTINGS;
	public static Settings getSettings() {
		if(SETTINGS == null) {
			File settingsFile = FSSyncPaths.getSettingsFile();
			Gson g = new Gson();
			try {
				SETTINGS = g.fromJson(FileUtils.readFileToString(settingsFile, Charset.defaultCharset()), Settings.class);
				SETTINGS.setSettingsFile(settingsFile);
			} catch (IOException e) {
				SETTINGS = new Settings(settingsFile);
			}
		}
		return SETTINGS;
	}
	
	private transient File settingsFile;

	private boolean alwaysSaveLog;
	private int columns;
	private String logFilesDir;
	private boolean showSummary;
	private boolean verbose;
	private String fileBrowser;
	private boolean startToTray;
	private boolean closeToTray;
	private boolean minimizeToTray;
	private boolean ignoreNewVersion;

	private Settings(File settingsFile) {
		super();
		this.settingsFile = settingsFile;
		alwaysSaveLog = false;
		columns = 1;
		logFilesDir = PropFx.userHome();
		showSummary = true;
		verbose = false;

		String os = PropFx.osName().toLowerCase();
		fileBrowser = os.startsWith("win") ? "explorer" : os.startsWith("mac") ? "finder" : "";

		startToTray = false;
		closeToTray = false;
		minimizeToTray = false;
		
		ignoreNewVersion = false;
	}

	public final void write() {
		GsonBuilder gb = new GsonBuilder();
		gb.setPrettyPrinting();
		Gson g = gb.create();

		String j = g.toJson(this);
		try {
			FileUtils.writeStringToFile(settingsFile, j, Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public final String findFileBrowser() {

		try {
			Runtime.getRuntime().exec("thunar");
			return "thunar";
		} catch (IOException e) {}
		try {
			Runtime.getRuntime().exec("caja");
			return "caja";
		} catch (IOException e) {}
		try {
			Runtime.getRuntime().exec("nautilus");
			return "nautilus";
		} catch (IOException e) {}
		try {
			Runtime.getRuntime().exec("conqueror");
			return "conqueror";
		} catch (IOException e) {}
		try {
			Runtime.getRuntime().exec("xdg-open");
			return "xdg-open";
		} catch (IOException e) {}
		return "";
	}

	public final boolean isAlwaysSaveLog() {
		return alwaysSaveLog;
	}

	public final void setAlwaysSaveLog(boolean alwaysSaveLog) {
		this.alwaysSaveLog = alwaysSaveLog;
	}

	public final int getColumns() {
		return columns;
	}

	public final void setColumns(int columns) {
		this.columns = columns;
	}

	public final String getLogFilesDir() {
		return logFilesDir;
	}

	public final void setLogFilesDir(String logFilesDir) {
		this.logFilesDir = logFilesDir;
	}

	public final boolean isShowSummary() {
		return showSummary;
	}

	public final void setShowSummary(boolean showSummary) {
		this.showSummary = showSummary;
	}

	public final boolean isVerbose() {
		return verbose;
	}

	public final void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public final String getUserProgramDir() {
		return settingsFile.getParent();
	}

	public final void setSettingsFile(File settingsFile) {
		this.settingsFile = settingsFile;
	}

	public final String getFileBrowser() {
		return fileBrowser;
	}

	public final void setFileBrowser(String fileBrowser) {
		this.fileBrowser = fileBrowser;
	}

	public final boolean isStartToTray() {
		return startToTray;
	}

	public final void setStartToTray(boolean startToTray) {
		this.startToTray = startToTray;
	}

	public final boolean isCloseToTray() {
		return closeToTray;
	}

	public final void setCloseToTray(boolean closeToTray) {
		this.closeToTray = closeToTray;
	}

	public final boolean isMinimizeToTray() {
		return minimizeToTray;
	}

	public final void setMinimizeToTray(boolean minimizeToTray) {
		this.minimizeToTray = minimizeToTray;
	}

	public boolean isIgnoreNewVersion() {
		return ignoreNewVersion;
	}

	public void setIgnoreNewVersion(boolean ignoreNewVersion) {
		this.ignoreNewVersion = ignoreNewVersion;
	}
}
