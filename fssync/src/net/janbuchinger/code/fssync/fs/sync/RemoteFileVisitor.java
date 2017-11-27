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
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Vector;

import net.janbuchinger.code.fssync.fs.sync.ui.SynchronisationProcessDialog;
import net.janbuchinger.code.mishmash.FSFx;

public class RemoteFileVisitor implements FileVisitor<Path> {

	private final SynchronisationProcessDialog spd;
	private final File target;
	private final Vector<File> remoteFiles;
	private final Vector<File> emptyDirs;

	public RemoteFileVisitor(File target, Vector<File> remoteFiles, Vector<File> emptyDirs,
			SynchronisationProcessDialog spd) {
		this.spd = spd;
		this.target = target;
		this.remoteFiles = remoteFiles;
		this.emptyDirs = emptyDirs;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		if (spd.isCancelled())
			return FileVisitResult.TERMINATE;
		if (!FSFx.hasDirEntries(dir))
			emptyDirs.add(dir.toFile());
		return FileVisitResult.CONTINUE;
	}

	private String filename;

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		if (spd.isCancelled())
			return FileVisitResult.TERMINATE;
		if (file.getParent().toString().equals(target.getPath())) {
			filename = file.getFileName().toString();
			if (filename.startsWith(".fs.") && filename.endsWith(".db"))
				// System.err.println(filename);
				return FileVisitResult.CONTINUE;
		}
		remoteFiles.add(file.toFile());
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

}
