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

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.vaguehope.mikuru.ExecHelper.CancelCaller;
import com.vaguehope.mikuru.ExecHelper.LineProcessor;

public class RsyncTask extends AsyncTask<String, String, Integer> implements LineProcessor, CancelCaller {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Context context;
	private final Appender appender;
	private final AtomicBoolean runningTracker;
	private final Runnable onFinish;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public RsyncTask (Context context, Appender appender, AtomicBoolean runningTracker, Runnable onFinish) {
		this.context = context;
		this.appender = appender;
		this.runningTracker = runningTracker;
		this.onFinish = onFinish;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void cancel () {
		if (RsyncTask.this.cancelCaller == null) throw new IllegalStateException("Can not cancel without cancelCaller.");
		RsyncTask.this.cancelCaller.run();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected Runnable cancelCaller;
	
	@Override
	protected void onPreExecute () {
		if (!this.runningTracker.compareAndSet(false, true)) {
			this.cancel(true);
			Toast.makeText(this.context, "Task alrady running.\n", Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	public void setCancelCallerRunnable (Runnable r) {
		this.cancelCaller = r;
	}
	
	@Override
	protected Integer doInBackground (String... params) {
		if (isCancelled()) return null;
		
		publishProgress("params:");
		for (String param : params) publishProgress(param);
		publishProgress(""); // Force empty line.
		
		try {
			RsyncHelper.readyRsync(this.context);
			ProcessBuilder pb = RsyncHelper.makeProcessBulder(this.context, Arrays.asList(params));
			int code = ExecHelper.expectExec(pb, this, this);
			return Integer.valueOf(code);
		}
		catch (IOException e) {
			publishProgress(Log.getStackTraceString(e));
			return null;
		}
	}
	
	@Override
	public boolean processLine (String line) {
		publishProgress(new String[] { line });
		return true;
	}
	
	@Override
	protected void onProgressUpdate (String... values) {
		if (values == null || values.length < 1) return;
		for (String line : values) {
			this.appender.append(line, "\n");
		}
	}
	
	@Override
	protected void onCancelled () {
		this.appender.append("cancelled.");
		cleanUp();
	}
	
	@Override
	protected void onPostExecute (Integer result) {
		if (result == null) {
			this.appender.append("failed.");
		}
		else if (result.intValue() == Integer.MIN_VALUE) {
			this.appender.append("aborted.");
		}
		else {
			this.appender.append("exit code ", result.toString(), ".", "\n");
		}
		
		cleanUp();
	}
	
	private void cleanUp () {
		this.runningTracker.set(false);
		if (this.onFinish != null) this.onFinish.run();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
