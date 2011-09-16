package com.vaguehope.mikuru;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.vaguehope.mikuru.ExecHelper.LineProcessor;

public class MikuruActivity extends Activity {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		wireGui();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected TextView txtConsole;
	
	private void wireGui () {
		Button btnRunSync = (Button) findViewById(R.id.btnRunSync);
		btnRunSync.setOnClickListener(this.btnRunSync_onClick);
		
		this.txtConsole = (TextView) findViewById(R.id.txtConsole);
	}
	
	private OnClickListener btnRunSync_onClick = new OnClickListener() {
		@Override
		public void onClick (View v) {
			RsyncRun task = new RsyncRun();
			task.execute();
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected class RsyncRun extends AsyncTask<Void, String, Void> {
		
		@Override
		protected Void doInBackground (Void... params) {
			try {
				RsyncHelper.readyRsync(MikuruActivity.this);
				
				List<String> args = new LinkedList<String>();
				args.add("--help");
				
				ProcessBuilder pb = RsyncHelper.makeProcessBulder(MikuruActivity.this, args );
				ExecHelper.expectExec(pb, this.lineProc);
			} catch (IOException e) {
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
			MikuruActivity.this.txtConsole.append(values[0]);
			MikuruActivity.this.txtConsole.append("\n");
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}