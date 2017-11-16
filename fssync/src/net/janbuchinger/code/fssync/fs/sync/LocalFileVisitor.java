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
import java.util.Iterator;
import java.util.Vector;

import net.janbuchinger.code.fssync.fs.sync.ui.SynchronisationProcessDialog;


public class LocalFileVisitor implements FileVisitor<Path> {

	private final Vector<File> localFiles;
	private final Vector<String> excludes;

	private final SynchronisationProcessDialog spd;

	private final int localBasePathNameCount;

	private final File source;

	public LocalFileVisitor(File source, Vector<File> localFiles, Vector<String> excludes,
			SynchronisationProcessDialog spd) {
		this.excludes = new Vector<String>();
		this.excludes.addAll(excludes);
		this.localFiles = localFiles;
		this.spd = spd;
		localBasePathNameCount = source.toPath().getNameCount();
		this.source = source;
	}

	private Iterator<String> iExcludes;

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		if (spd.isCancelled())
			return FileVisitResult.TERMINATE;
		if (dir.toString().equals(source.getPath()))
			return FileVisitResult.CONTINUE;
		iExcludes = excludes.iterator();
		int dirNameCount = dir.getNameCount();
		while (iExcludes.hasNext()) {
			String exclude = iExcludes.next();
			try {
				if (exclude.equals(dir.subpath(localBasePathNameCount, dirNameCount).toString())) {
					iExcludes.remove();
					return FileVisitResult.SKIP_SUBTREE;
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		return FileVisitResult.CONTINUE;
	}

	private String filename;

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//		System.out.println(attrs.fileKey());
		if (spd.isCancelled())
			return FileVisitResult.TERMINATE;
		if (file.getParent().toString().equals(source.getPath())) {
			filename = file.getFileName().toString();
			if (filename.startsWith(".fs.") && filename.endsWith(".db"))
				// System.err.println(filename);
				return FileVisitResult.CONTINUE;
		}
		localFiles.add(new File(file.toString()));
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
