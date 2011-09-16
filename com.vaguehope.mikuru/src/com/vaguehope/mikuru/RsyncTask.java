package com.vaguehope.mikuru;

import java.io.IOException;
import java.util.Arrays;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Toast;

import com.vaguehope.mikuru.ExecHelper.LineProcessor;

public class RsyncTask extends AsyncTask<String, String, Integer> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Context context;
	private final View button;
	private final ConsoleAppender consoleAppender;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public RsyncTask (Context context, ConsoleAppender consoleAppender) {
		this.context = context;
		this.consoleAppender = consoleAppender;
		this.button = null;
	}
	
	public RsyncTask (Context context, ConsoleAppender consoleAppender, View button) {
		this.context = context;
		this.consoleAppender = consoleAppender;
		this.button = button;
	}
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected void onPreExecute () {
		if (this.button != null) {
			if (this.button.isEnabled()) {
				this.button.setEnabled(false);
			}
			else {
				this.cancel(true);
				Toast.makeText(this.context, "Task alrady running.\n", Toast.LENGTH_LONG).show();
			}
		}
	}
	
	@Override
	protected Integer doInBackground (String... params) {
		if (isCancelled()) return null;
		
		try {
			RsyncHelper.readyRsync(this.context);
			ProcessBuilder pb = RsyncHelper.makeProcessBulder(this.context, Arrays.asList(params));
			int code = ExecHelper.expectExec(pb, this.lineProc);
			return Integer.valueOf(code);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected final LineProcessor lineProc = new LineProcessor() {
		@Override
		@SuppressWarnings("synthetic-access")
		public boolean processLine (String line) {
			publishProgress(new String[] { line });
			return true;
		}
	};
	
	@Override
	protected void onProgressUpdate (String... values) {
		if (values == null || values.length < 1) return;
		for (String line : values) {
			this.consoleAppender.append(line, "\n");
		}
	}
	
	@Override
	protected void onPostExecute (Integer result) {
		this.consoleAppender.append("exit code ", result == null ? null : result.toString(), ".", "\n");
		if (this.button != null) this.button.setEnabled(true);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
