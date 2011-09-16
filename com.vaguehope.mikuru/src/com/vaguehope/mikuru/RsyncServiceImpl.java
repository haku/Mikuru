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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class RsyncServiceImpl extends Service implements RsyncService, Appender {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void onCreate () {
		super.onCreate();
		
		addPersistantNotification();
	}
	
	@Override
	public void onStart (Intent intent, int startId) {
		serviceBegan(startId);
		startWork(startId);
	}
	
	@Override
	public void onDestroy () {
		removePersistantNotification();
		releaseWakeLock("onDestroy");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Service binding.
	
	public class LocalBinder extends Binder {
		public RsyncService getService () {
			return RsyncServiceImpl.this;
		}
	}
	
	private final IBinder binder = new LocalBinder();
	
	@Override
	public IBinder onBind (Intent intent) {
		return this.binder;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	RsyncService and Appender
	
	private final Set<Appender> appenders = new LinkedHashSet<Appender>();
	
	@Override
	public void cancelRun () {
		RsyncTask task = this.taskRef.get();
		if (task == null) throw new IllegalStateException("Can not cancel without task reference.");
		task.cancel();
	}
	
	@Override
	public void addAppender (Appender appender) {
		this.appenders.add(appender);
	}
	
	@Override
	public void removeAppender (Appender appender) {
		this.appenders.remove(appender);
	}
	
	@Override
	public void append (String... msg) {
		for (Appender appender : this.appenders) {
			appender.append(msg);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actual work.
	
	private final AtomicBoolean runningTracker = new AtomicBoolean(false); // This is a sort of lock RsyncTask to use.
	protected final AtomicReference<RsyncTask> taskRef = new AtomicReference<RsyncTask>();
	
	private void startWork (final int startId) {
		File configFile = new File(C.CONFIG_FILE_PATH);
		List<String> config;
		try {
			config = FileHelper.fileToList(configFile);
			RsyncHelper.writePasswordFile(this, config.get(0));
		}
		catch (IOException e) {
			this.append(Log.getStackTraceString(e));
			finishService(RsyncServiceImpl.this, startId);
			return;
		}
		
		List<String> args = new LinkedList<String>();
		args.add("--password-file=" + RsyncHelper.getPasswordFilePath(this));
		args.addAll(config.subList(1, config.size()));
		
		final Runnable postRun = new Runnable() {
			@Override
			public void run () {
				RsyncServiceImpl.this.taskRef.set(null);
				finishService(RsyncServiceImpl.this, startId);
			}
		};
		
		RsyncTask task = new RsyncTask(this, this, this.runningTracker, postRun);
		if (this.taskRef.compareAndSet(null, task)) {
			task.execute(args.toArray(new String[]{}));
		}
		else {
			Toast.makeText(this, "Already running.", Toast.LENGTH_LONG).show();
			finishService(RsyncServiceImpl.this, startId);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Persistent notification.
	
	private NotificationManager notificationManager;
	private Notification persistantNotification;
	
	private NotificationManager getNotificationManager () {
		if (this.notificationManager == null) {
			this.notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		}
		return this.notificationManager;
	}
	
	private void addPersistantNotification () {
		if (this.persistantNotification == null) {
			this.persistantNotification = new Notification(R.drawable.service_notification, getText(R.string.service_running), System.currentTimeMillis());
			this.persistantNotification.flags |= Notification.FLAG_ONGOING_EVENT;
		}
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MikuruActivity.class), 0);
		this.persistantNotification.setLatestEventInfo(this, getText(R.string.service_label), getText(R.string.service_running), contentIntent);
		getNotificationManager().notify(R.string.service_running, this.persistantNotification);
	}
	
	private void removePersistantNotification () {
		getNotificationManager().cancel(R.string.service_running);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Wake locks.
	
	private final static Lock startLock = new ReentrantLock();
	private static PowerManager.WakeLock wakeLock;
	private static final Set<Integer> serviceStarts = new HashSet<Integer>();
	
	public static void beginService (Context context, Intent intent) {
		startLock.lock();
		try {
			if (wakeLock == null) {
				PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
				wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, C.TAG);
				wakeLock.setReferenceCounted(false);
			}
			if (!wakeLock.isHeld()) {
				wakeLock.acquire();
				Log.i(C.TAG, "Wakelock acquired.");
			}
			context.startService(intent);
		}
		finally {
			startLock.unlock();
		}
	}
	
	protected static void serviceBegan (int startId) {
		Log.i(C.TAG, "serviceBegan startId=" + startId);
		serviceStarts.add(Integer.valueOf(startId));
	}
	
	protected static void finishService (Service service, int startId) {
		startLock.lock();
		try {
			serviceStarts.remove(Integer.valueOf(startId));
			if (wakeLock != null && serviceStarts.size() == 0) {
				service.stopSelf();
				releaseWakeLock("startid=" + startId);
			}
		}
		finally {
			startLock.unlock();
		}
	}
	
	protected static void releaseWakeLock (String who) {
		if (wakeLock.isHeld()) {
			wakeLock.release();
			Log.i(C.TAG, "Wakelock released by "+who+".");
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
