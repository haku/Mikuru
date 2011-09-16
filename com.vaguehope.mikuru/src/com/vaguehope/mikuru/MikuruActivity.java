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

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

public class MikuruActivity extends Activity {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected RsyncServiceClient rsyncServiceClient = null;
	protected ConsoleAppender consoleAppender;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Life cycle events.
	
	@Override
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		wireGui();
	}
	
	@Override
	protected void onResume () {
		super.onResume();
		bindService();
	}
	
	@Override
	protected void onPause () {
		unbindService();
		super.onPause();
	}
	
	@Override
	protected void onDestroy () {
		disposeService();
		super.onDestroy();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI setup and helpers.
	
	private Button btnRunSync;
	private Button btnCancelSync;
	
	private void wireGui () {
		this.btnRunSync = (Button) findViewById(R.id.btnRunSync);
		this.btnRunSync.setOnClickListener(this.btnRunSync_onClick);
		
		this.btnCancelSync = (Button) findViewById(R.id.btnCancelSync);
		this.btnCancelSync.setOnClickListener(this.btnCancelSync_onClick);
		setServiceBtnsEnabled(false);
		
		Button btnClearLog = (Button) findViewById(R.id.btnClearLog);
		btnClearLog.setOnClickListener(this.btnClearLog_onClick);
		
		TextView tv = (TextView) findViewById(R.id.txtConsole);
		ScrollView sv = (ScrollView) findViewById(R.id.svConsole);
		this.consoleAppender = new ConsoleAppender(tv, sv);
	}
	
	private void setServiceBtnsEnabled (boolean enabled) {
		this.btnCancelSync.setEnabled(enabled);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Binding.
	
	/**
	 * May be called multiple times in a row.
	 */
	private void bindService () {
		if (this.rsyncServiceClient == null) {
			this.rsyncServiceClient = new RsyncServiceClient(getApplicationContext(), new Runnable() {
				@Override
				public void run() {
					/* this convoluted method is because the service connection won't finish
					 * until this thread processes messages again (i.e., after it exits this thread).
					 * if we try to talk to the DB service before then, it will NPE.
					 */
					postBind();
				}
			}, new Runnable() {
				@Override
				public void run () {
					postUnbind();
				}
			});
		}
		this.rsyncServiceClient.rebind();
		if (this.rsyncServiceClient.isReady()) {
			// because we stop listening in onPause(), we must resume if the user comes back.
			postBind();
		}
	}
	
	private void unbindService () {
		// We might be pausing before the callback has come.
		if (this.rsyncServiceClient.isReady()) {
			preUnbind();
			postUnbind();
		}
		else {
			// If we have not even had the callback yet, cancel it.
			this.rsyncServiceClient.clearReadyListener();
		}
	}
	
	private void disposeService () {
		this.rsyncServiceClient.dispose();
		this.rsyncServiceClient = null;
	}
	
	/**
	 * May be called multiple times in a row.
	 */
	protected void postBind () {
		this.rsyncServiceClient.getService().addAppender(this.consoleAppender);
		setServiceBtnsEnabled(true);
	}
	
	protected void preUnbind () {
		this.rsyncServiceClient.getService().removeAppender(this.consoleAppender);
	}
	
	protected void postUnbind () {
		setServiceBtnsEnabled(false);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private OnClickListener btnRunSync_onClick = new OnClickListener() {
		@Override
		public void onClick (View v) {
			startRsync();
		}
	};
	
	private OnClickListener btnCancelSync_onClick = new OnClickListener() {
		@Override
		public void onClick (View v) {
			cancelRsync();
		}
	};
	
	private OnClickListener btnClearLog_onClick = new OnClickListener() {
		@Override
		public void onClick (View v) {
			MikuruActivity.this.consoleAppender.clear();
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Action calls.
	
	protected void startRsync () {
		File configFile = new File(C.CONFIG_FILE_PATH);
		if (!configFile.exists()) {
			MikuruActivity.this.consoleAppender.append("File not found: " + configFile.getAbsolutePath());
			return;
		}
		
		this.startService(new Intent(this, RsyncServiceImpl.class));
		bindService();
	}
	
	protected void cancelRsync () {
		if (this.rsyncServiceClient == null) throw new IllegalStateException("Service is not bound.");
		this.rsyncServiceClient.getService().cancelRun();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}