/*
 * Copyright 2011 Fae Hutter
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
		public void processLine (String line, Runnable canceller);
	}
	
	public static interface CancelCaller {
		public void setCancelCallerRunnable (Runnable r);
	}
	
	protected static void expectExec (String[] args, LineProcessor lineProc, CancelCaller cancelCaller) throws IOException {
		ProcessBuilder procBld = new ProcessBuilder(args);
		procBld.redirectErrorStream(true);
		expectExec(procBld, lineProc, cancelCaller);
	}
	
	protected static int expectExec (ProcessBuilder procBld, LineProcessor lineProc, CancelCaller cancelCaller) throws IOException {
		if (!procBld.redirectErrorStream()) throw new IllegalArgumentException("procBld must have redirectErrorStream set.");
		
		final Process proc = procBld.start();
		
		final Runnable canceller = new Runnable() {
			@Override
			public void run () {
				proc.destroy();
			}
		};
		if (cancelCaller != null) cancelCaller.setCancelCallerRunnable(canceller);
		
		final BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				lineProc.processLine(line, canceller);
			}
			return proc.waitFor();
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
