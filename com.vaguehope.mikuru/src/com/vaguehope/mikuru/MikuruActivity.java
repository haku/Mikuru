package com.vaguehope.mikuru;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

public class MikuruActivity extends Activity {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		wireGui();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected ConsoleAppender consoleAppender;
	
	private void wireGui () {
		Button btnRunSync = (Button) findViewById(R.id.btnRunSync);
		btnRunSync.setOnClickListener(this.btnRunSync_onClick);
		
		Button btnClearLog = (Button) findViewById(R.id.btnClearLog);
		btnClearLog.setOnClickListener(this.btnClearLog_onClick);
		
		TextView tv = (TextView) findViewById(R.id.txtConsole);
		ScrollView sv = (ScrollView) findViewById(R.id.svConsole);
		this.consoleAppender = new ConsoleAppender(tv, sv);
	}
	
	private OnClickListener btnRunSync_onClick = new OnClickListener() {
		@Override
		public void onClick (View v) {
			RsyncHelper.writePasswordFile(MikuruActivity.this, "passw0rd");
			RsyncTask task = new RsyncTask(MikuruActivity.this, MikuruActivity.this.consoleAppender, v);
			task.execute(
					"--help"
					);
		}
	};
	
	private OnClickListener btnClearLog_onClick = new OnClickListener() {
		@Override
		public void onClick (View v) {
			MikuruActivity.this.consoleAppender.clear();
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}