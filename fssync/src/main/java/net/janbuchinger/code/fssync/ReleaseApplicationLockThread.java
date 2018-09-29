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

import java.io.RandomAccessFile;
import java.nio.channels.Channel;
import java.nio.channels.FileLock;

/**
 * This <code>Thread</code> is initialized on the main thread after successfully
 * locking the lock file and before launching the UI. It is then passed to
 * <code>Runtime.getRuntime().addShutdownHook(Thread)</code>.
 * 
 * @author Jan Buchinger
 *
 */
public final class ReleaseApplicationLockThread extends Thread {
	private final FileLock lock;
	private final Channel channel;
	private final RandomAccessFile raf;

	public ReleaseApplicationLockThread(FileLock lock, Channel channel, RandomAccessFile raf) {
		this.lock = lock;
		this.channel = channel;
		this.raf = raf;
	}

	@Override
	public void run() {
		try {
			lock.release();
			channel.close();
			raf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
