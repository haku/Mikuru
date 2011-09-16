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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class RsyncServiceClient {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Context context;
	protected RsyncService boundRsyncService;
	protected Runnable onReady;
	protected final Runnable onDisconnect;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public RsyncServiceClient (Context context, Runnable onReady, Runnable onDisconnect) {
		this.context = context;
		this.onReady = onReady;
		this.onDisconnect = onDisconnect;
		bindService();
	}
	
	public void dispose () {
		unbindService();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void clearReadyListener () {
		this.onReady = null;
	}
	
	public boolean isReady () {
		return this.boundRsyncService != null;
	}
	
	public void rebind () {
		if (!isReady()) bindService();
	}
	
	public RsyncService getService () {
		return this.boundRsyncService;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private ServiceConnection serviceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			RsyncServiceClient.this.boundRsyncService = ((RsyncServiceImpl.LocalBinder)service).getService();
			if (RsyncServiceClient.this.onReady != null) RsyncServiceClient.this.onReady.run();
		}
		
		@Override
		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			RsyncServiceClient.this.boundRsyncService = null;
			if (RsyncServiceClient.this.onDisconnect != null) RsyncServiceClient.this.onDisconnect.run();
		}
		
	};
	
	private void bindService () {
		Intent intent = new Intent(this.context, RsyncServiceImpl.class);
		boolean bound = this.context.bindService(intent, this.serviceConnection, 0);
		if (!bound) this.boundRsyncService = null;
	}
	
	private void unbindService () {
		if (this.boundRsyncService != null) {
			this.context.unbindService(this.serviceConnection);
		}
		this.boundRsyncService = null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -	
}
