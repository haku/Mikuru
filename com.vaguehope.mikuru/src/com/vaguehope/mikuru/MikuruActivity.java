package com.vaguehope.mikuru;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
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
	
	protected TextView txtConsole;
	
	private void wireGui () {
		Button btnRunSync = (Button) findViewById(R.id.btnRunSync);
		btnRunSync.setOnClickListener(this.btnRunSync_onClick);
		
		Button btnClearLog = (Button) findViewById(R.id.btnClearLog);
		btnClearLog.setOnClickListener(this.btnClearLog_onClick);
		
		this.txtConsole = (TextView) findViewById(R.id.txtConsole);
	}
	
	private OnClickListener btnRunSync_onClick = new OnClickListener() {
		@Override
		public void onClick (View v) {
			RsyncTask task = new RsyncTask(MikuruActivity.this, MikuruActivity.this.txtConsole, v);
			task.execute("--help");
		}
	};
	
	private OnClickListener btnClearLog_onClick = new OnClickListener() {
		@Override
		public void onClick (View v) {
			MikuruActivity.this.txtConsole.setText("");
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}