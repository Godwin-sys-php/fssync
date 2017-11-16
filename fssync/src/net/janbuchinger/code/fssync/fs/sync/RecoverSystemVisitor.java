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
package net.janbuchinger.code.fssync.fs.sync;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import net.janbuchinger.code.fssync.fs.FSSync;


public class RecoverSystemVisitor implements FileVisitor<Path> {

	private final File source;
	private final File target;

	private final int targetBaseNameCount;

	private OnlineDB db;

	public RecoverSystemVisitor(File source, File target) {
		this.source = source;
		this.target = target;
		targetBaseNameCount = Paths.get(target.getPath()).getNameCount();
		db = null;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	private File fileInSource;
	private File fileInDestination;
	private Path relativePath;
	// private long lastModified;
	private String checksum;

	// private boolean isRootDir;

	String filename;

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		if (file.getParent().toString().equals(target.getPath())) {
			filename = file.getFileName().toString();
			if (filename.startsWith(".fs.") && filename.endsWith(".db"))
				// System.err.println(filename);
				return FileVisitResult.CONTINUE;
		}
		relativePath = file.subpath(targetBaseNameCount, file.getNameCount());
		fileInSource = new File(source, relativePath.toString());
		if (fileInSource.exists()) {
			fileInDestination = new File(file.toString());
			try {
				checksum = FSSync.getChecksum(fileInDestination);
				db.add(new RelativeFile(relativePath.toString(), fileInDestination.length(), attrs
						.lastModifiedTime().toMillis(), checksum));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		System.err.println("Error: " + file.toString() + ", " + exc.getMessage());
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	public void setDB(OnlineDB db) {
		this.db = db;
	}

}
