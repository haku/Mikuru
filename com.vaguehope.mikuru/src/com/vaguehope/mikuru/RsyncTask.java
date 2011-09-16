package com.vaguehope.mikuru;

import java.io.IOException;
import java.util.Arrays;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.vaguehope.mikuru.ExecHelper.CancelCaller;
import com.vaguehope.mikuru.ExecHelper.LineProcessor;

public class RsyncTask extends AsyncTask<String, String, Integer> implements LineProcessor, CancelCaller {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Context context;
	private final ConsoleAppender consoleAppender;
	private final View startButton;
	private final View cancelButton;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public RsyncTask (Context context, ConsoleAppender consoleAppender) {
		this.context = context;
		this.consoleAppender = consoleAppender;
		this.startButton = null;
		this.cancelButton = null;
	}
	
	public RsyncTask (Context context, ConsoleAppender consoleAppender, View startButton, View cancelButton) {
		this.context = context;
		this.consoleAppender = consoleAppender;
		this.startButton = startButton;
		this.cancelButton = cancelButton;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected Runnable cancelCaller;
	
	@Override
	protected void onPreExecute () {
		if (this.startButton != null) {
			if (this.startButton.isEnabled()) {
				this.startButton.setEnabled(false);
				this.cancelButton.setOnClickListener(this.cancelButton_onClick);
				this.cancelButton.setEnabled(true);
			}
			else {
				this.cancel(true);
				Toast.makeText(this.context, "Task alrady running.\n", Toast.LENGTH_LONG).show();
			}
		}
	}
	
	private OnClickListener cancelButton_onClick = new OnClickListener() {
		@Override
		public void onClick (View v) {
			if (RsyncTask.this.cancelCaller != null) {
				RsyncTask.this.cancelCaller.run();
			}
		}
	};
	
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
			this.consoleAppender.append(line, "\n");
		}
	}
	
	@Override
	protected void onPostExecute (Integer result) {
		if (result == null) {
			this.consoleAppender.append("failed.");
		}
		else if (result.intValue() == Integer.MIN_VALUE) {
			this.consoleAppender.append("aborted.");
		}
		else {
			this.consoleAppender.append("exit code ", result.toString(), ".", "\n");
		}
		
		if (this.startButton != null) this.startButton.setEnabled(true);
		if (this.cancelButton != null) this.cancelButton.setEnabled(false);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
