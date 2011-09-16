package com.vaguehope.mikuru;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ExecHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private ExecHelper () {}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected static void quiteExec (String[] args) throws IOException {
		ProcessBuilder procBld = new ProcessBuilder(args);
		procBld.redirectErrorStream(true);
		quiteExec(procBld);
	}
	
	protected static void quiteExec (ProcessBuilder procBld) throws IOException {
		if (!procBld.redirectErrorStream()) throw new IllegalArgumentException("procBld must have redirectErrorStream set.");
		Process proc = procBld.start();
		InputStream is = proc.getInputStream();
		try {
			while (is.skip(8192) > 0 || is.read() > 0) { // TODO clean this.
				// Eat input.
			}
			int exitCode = proc.waitFor();
			if (exitCode != 0) throw new IOException("Exec returned " + exitCode + ".");
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		finally {
			is.close();
			proc.destroy();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static interface LineProcessor {
		public boolean processLine (String line);
	}
	
	protected static void expectExec (String[] args, LineProcessor lineProc) throws IOException {
		ProcessBuilder procBld = new ProcessBuilder(args);
		procBld.redirectErrorStream(true);
		expectExec(procBld, lineProc);
	}
	
	protected static void expectExec (ProcessBuilder procBld, LineProcessor lineProc) throws IOException {
		if (!procBld.redirectErrorStream()) throw new IllegalArgumentException("procBld must have redirectErrorStream set.");
		final Process proc = procBld.start();
		final BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		try {
			String line;
			boolean abort = false;
			while ((line = reader.readLine()) != null) {
				abort = !lineProc.processLine(line);
				if (abort) break;
			}
			if (!abort && proc.waitFor() != 0) throw new IOException("Exec returned " + proc.waitFor() + ".");
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		finally {
			reader.close();
			proc.destroy();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
