package com.vaguehope.mikuru;

import java.io.IOException;
import java.util.Arrays;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.TextView;

import com.vaguehope.mikuru.ExecHelper.LineProcessor;

public class RsyncTask extends AsyncTask<String, String, Void> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Context context;
	private final TextView txtConsole;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public RsyncTask (Context context, TextView txtConsole) {
		this.context = context;
		this.txtConsole = txtConsole;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected Void doInBackground (String... params) {
		try {
			RsyncHelper.readyRsync(this.context);
			ProcessBuilder pb = RsyncHelper.makeProcessBulder(this.context, Arrays.asList(params));
			ExecHelper.expectExec(pb, this.lineProc);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		return null;
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
			this.txtConsole.append(line);
			this.txtConsole.append("\n");
		}
	}
	
	@Override
	protected void onPostExecute (Void result) {
		this.txtConsole.append("exec ended.");
		this.txtConsole.append("\n");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
