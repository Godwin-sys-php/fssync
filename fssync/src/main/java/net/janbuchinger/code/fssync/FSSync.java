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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;

import javax.swing.SwingUtilities;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

/**
 * This is the programs main class. In the main method a file lock is obtained
 * before launching the UI. If the file lock is held by another instance, the
 * program will show an error message and then terminate.
 * 
 * @author Jan Buchinger
 *
 */
public final class FSSync {
	public static void main(String[] args) {
		try {
			// get the lock file
			File lockFile = FSSyncPaths.getLockFile();
			// create a RandomAccessFile from the lock file
			RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
			// get the FileChannel from the RandomAccessFile
			FileChannel channel = raf.getChannel();
			// try to obtain a file lock
			FileLock lock = channel.tryLock();
			// if the lock could not be obtained
			if (lock == null) {
				try {
					// close the channel
					channel.close();
					// close the RandomAccessFile
					raf.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				// show message that the program is already running.
				SwingUtilities.invokeLater(new RunAlreadyRunningMessage());
			} else { // the file lock was successfully obtained
				// add the shutdown hook to release the file lock and close the channel and the
				// RandomAccessFile before exiting
				Runtime.getRuntime().addShutdownHook(new ReleaseApplicationLockThread(lock, channel, raf));
				// after registering the shutdown hook the UI is launched
				SwingUtilities.invokeLater(new RunFSSyncUI());
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Utility function to create a SHA384 file checksum in hex format.
	 * 
	 * @param file
	 *            the file to create the checksum from.
	 * 
	 * @return the SHA384 file checksum in hex format or <code>null</code> if an
	 *         <code>IOException</code> occurs.
	 * 
	 * @see DigestUtils#sha384Hex(InputStream) DigestUtils.sha384Hex(InputStream)
	 *      from apache commons codec
	 */
	public static String createSHA384Hex(File file) {
		try (InputStream data = new FileInputStream(file)) {
			return DigestUtils.sha384Hex(data);
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Saves the stack trace of an Exception to a file.
	 * 
	 * @param logFile
	 *            The file to be written to.
	 * @param e
	 *            The Exeption to get the stack trace from.
	 * 
	 * @return <code>true</code> if the file was written.
	 */
	public static boolean saveErrorLog(File logFile, Throwable e) {
		boolean written = false;
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		try {
			FileUtils.writeStringToFile(logFile, sw.toString(), Charset.defaultCharset());
			written = true;
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		pw.close();
		try {
			sw.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return written;
	}
}