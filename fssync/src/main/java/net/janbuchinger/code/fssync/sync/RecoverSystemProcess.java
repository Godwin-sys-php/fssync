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
package net.janbuchinger.code.fssync.sync;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.SwingWorker;

import org.apache.commons.io.FileUtils;

public class RecoverSystemProcess extends SwingWorker<Void, Void> {

	private final RecoverSystemVisitor visitor;
	private final Path start;
	private final RecoverSystemDialog rsd;
	private final File source;
	private final File target;

	public RecoverSystemProcess(File source, File target, RecoverSystemDialog rsd) {
		visitor = new RecoverSystemVisitor(source, target);
		start = target.toPath();
		this.rsd = rsd;
		this.source = source;
		this.target = target;
	}

	@Override
	protected Void doInBackground() throws Exception {
//		File[] editDbs = source.listFiles(new EditDBsFilenameFilter());
		File fsdb = new File(target, ".fs.db");
		
		if(!fsdb.exists()) {
			throw new FileNotFoundException("Database file not found.");
		}
		
		File dbEditFile = OnlineDB.getEditableDBFile(source, fsdb);
		if(dbEditFile == null) {
			dbEditFile = OnlineDB.nextEditableDBFile(source);
			FileUtils.copyFile(fsdb, dbEditFile);
		}

		OnlineDB db = new OnlineDB(dbEditFile);

		visitor.setDB(db);
		try {
			Files.walkFileTree(start, visitor);
		} catch (IOException e) {
			e.printStackTrace();
		}

		FileUtils.copyFile(dbEditFile, fsdb);
		return null;
	}

	@Override
	protected void done() {
		super.done();
		rsd.setVisible(false);
	}
}
