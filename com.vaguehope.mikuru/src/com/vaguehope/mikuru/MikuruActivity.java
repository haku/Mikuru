package com.vaguehope.mikuru;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

public class MikuruActivity extends Activity {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected static final String CONFIG_FILE_PATH = "/sdcard/mikuru.conf";
	
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
		Button btn;
		
		btn = (Button) findViewById(R.id.btnRunSync);
		btn.setOnClickListener(this.btnRunSync_onClick);
		
		btn = (Button) findViewById(R.id.btnCancelSync);
		btn.setEnabled(false);
		
		btn = (Button) findViewById(R.id.btnClearLog);
		btn.setOnClickListener(this.btnClearLog_onClick);
		
		TextView tv = (TextView) findViewById(R.id.txtConsole);
		ScrollView sv = (ScrollView) findViewById(R.id.svConsole);
		this.consoleAppender = new ConsoleAppender(tv, sv);
	}
	
	private OnClickListener btnRunSync_onClick = new OnClickListener() {
		@Override
		public void onClick (View v) {
			File configFile = new File(CONFIG_FILE_PATH);
			if (!configFile.exists()) {
				MikuruActivity.this.consoleAppender.append("File not found: " + configFile.getAbsolutePath());
				return;
			}
			
			List<String> config;
			try {
				config = FileHelper.fileToList(configFile);
			}
			catch (IOException e) {
				MikuruActivity.this.consoleAppender.append(Log.getStackTraceString(e));
				return;
			}
			
			RsyncHelper.writePasswordFile(MikuruActivity.this, config.get(0));
			
			List<String> args = new LinkedList<String>();
			args.add("--password-file=" + RsyncHelper.getPasswordFilePath(MikuruActivity.this));
			args.addAll(config.subList(1, config.size()));
			
			RsyncTask task = new RsyncTask(MikuruActivity.this, MikuruActivity.this.consoleAppender, v, findViewById(R.id.btnCancelSync));
			task.execute(args.toArray(new String[]{}));
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