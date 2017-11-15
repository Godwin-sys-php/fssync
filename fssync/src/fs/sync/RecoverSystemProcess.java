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
package fs.sync;

import java.io.File;
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
		File fseditdb = new File(source, ".fs.edit.db");
		int c = 1;
		while(fseditdb.exists()) {
			fseditdb = new File(source, ".fs.edit" + (c++) + ".db");
		}
		
		File fsdb = new File(target, ".fs.db");
		
		FileUtils.copyFile(fsdb, fseditdb);
		
		OnlineDB db = new OnlineDB(fseditdb);
		
		visitor.setDB(db);
		try {
			Files.walkFileTree(start, visitor);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		FileUtils.copyFile(fseditdb, fsdb);
		return null;
	}

	@Override
	protected void done() {
		super.done();
		rsd.setVisible(false);
	}
}
