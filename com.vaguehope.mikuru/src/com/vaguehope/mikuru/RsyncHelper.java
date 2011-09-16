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
	
	private static String getRsyncBinaryPath (Context context) {
		// FIXME is there a specific API for this?
		return "/data/data/" + context.getApplicationInfo().packageName + "/" + RSYNC_BINARY_NAME;
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
		if (new File(targetPath).exists()) { // TODO check file permissions as well.
			rsyncReady.set(true);
			return;
		}
		
		InputStream in = context.getAssets().open(RSYNC_BINARY_PATH);
		try {
			FileOutputStream out = new FileOutputStream(targetPath);
			try {
				int read;
				byte[] buffer = new byte[4096];
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
}
