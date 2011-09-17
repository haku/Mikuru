/*
 * Copyright 2011 Alex Hutter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.vaguehope.mikuru;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;

public class RsyncHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String RSYNC_BINARY_NAME = "rsync";
	private static final String RSYNC_BINARY_PATH = "rsync";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private RsyncHelper () {}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static String getAppDataPath (Context context) {
		// FIXME is there a specific API for this?
		return "/data/data/" + context.getApplicationInfo().packageName;
	}
	
	private static String getRsyncBinaryPath (Context context) {
		return getAppDataPath(context) + "/" + RSYNC_BINARY_NAME;
	}
	
	public static ProcessBuilder makeProcessBulder (Context context, List<String> args) {
		List<String> procArgs = new LinkedList<String>();
		procArgs.add(getRsyncBinaryPath(context));
		procArgs.addAll(args);
		ProcessBuilder procBld = new ProcessBuilder(procArgs);
		procBld.redirectErrorStream(true);
		return procBld;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final AtomicBoolean rsyncReady = new AtomicBoolean(false);
	
	static public void readyRsync (Context context) throws IOException {
		final String targetPath = getRsyncBinaryPath(context);
		
		if (rsyncReady.get()) return;
		if (new File(targetPath).exists()) { // TODO check file permissions and MD5 as well.
			rsyncReady.set(true);
			return;
		}
		
		InputStream in = context.getAssets().open(RSYNC_BINARY_PATH);
		try {
			FileOutputStream out = new FileOutputStream(targetPath);
			try {
				int read;
				byte[] buffer = new byte[8192];
				while ((read = in.read(buffer)) > 0) {
					out.write(buffer, 0, read);
				}
			}
			finally {
				out.close();
			}
		}
		finally {
			in.close();
		}
		
		ExecHelper.quiteExec(new String[] { "/system/bin/chmod", "744", targetPath } );
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String PASSWORD_FILE_NAME = "rsyncpass";
	
	static public void writePasswordFile (Context context, String password) throws IOException {
		File file = new File(getPasswordFilePath(context));
		if (password != null && !password.equals(FileHelper.fileToString(file)) ) {
			FileHelper.stringToFile(file, password);
			ExecHelper.quiteExec(new String[] { "/system/bin/chmod", "600", file.getAbsolutePath() } );
		}
		else if (password == null && file.exists()) {
			file.delete();
		}
	}
	
	static public String getPasswordFilePath (Context context) {
		return getAppDataPath(context) + "/" + PASSWORD_FILE_NAME;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
