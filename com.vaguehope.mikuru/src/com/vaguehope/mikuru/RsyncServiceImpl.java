package com.vaguehope.mikuru;

import java.io.File;
import java.io.IOException;
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

public class RsyncServiceImpl extends Service implements RsyncService, Appender {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void onCreate () {
		super.onCreate();
		
		addPersistantNotification();
	}
	
	@Override
	public void onStart (Intent intent, int startId) {
		startWork();
	}
	
	@Override
	public void onDestroy () {
		removePersistantNotification();
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
//	RsyncService
	
	protected final AtomicBoolean running = new AtomicBoolean(false); // Is the service running?
	protected final Set<Appender> appenders = new LinkedHashSet<Appender>();
	
	@Override
	public boolean isRunning () {
		return this.running.get();
	}
	
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Appender
	
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
	
	private void startWork () {
		if (!this.running.compareAndSet(false, true)) {
			throw new IllegalStateException("Can not start a second run.");
		}
		
		File configFile = new File(C.CONFIG_FILE_PATH);
		List<String> config;
		try {
			config = FileHelper.fileToList(configFile);
		}
		catch (IOException e) {
			this.append(Log.getStackTraceString(e));
			return;
		}
		
		RsyncHelper.writePasswordFile(this, config.get(0));
		
		List<String> args = new LinkedList<String>();
		args.add("--password-file=" + RsyncHelper.getPasswordFilePath(this));
		args.addAll(config.subList(1, config.size()));
		
		RsyncTask task = new RsyncTask(this, this, this.runningTracker, this.postRunSelf);
		if (this.taskRef.compareAndSet(null, task)) {
			task.execute(args.toArray(new String[]{}));
		}
		else {
			throw new IllegalStateException("taskRef already set.");
		}
	}
	
	private final Runnable postRunSelf = new Runnable() {
		@Override
		public void run () {
			RsyncServiceImpl.this.running.set(false);
			RsyncServiceImpl.this.taskRef.set(null);
			stopSelf();
		}
	};
	
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
	private static PowerManager.WakeLock mStartingService;
	
	public static void beginStartingService (Context context, Intent intent) {
		 Log.i(C.TAG, "beginStartingService() intent=" + intent.toString());
		
		startLock.lock();
		try {
			if (mStartingService == null) {
				PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
				mStartingService = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, C.TAG);
				mStartingService.setReferenceCounted(false);
			}
			mStartingService.acquire();
			context.startService(intent);
		}
		finally {
			startLock.unlock();
		}
	}
	
	public static void finishStartingService (Service service, int startId) {
		Log.i(C.TAG, "finishStartingService() startId=" + startId);
		
		startLock.lock();
		try {
			if (mStartingService != null) {
				if (service.stopSelfResult(startId)) {
					mStartingService.release();
				}
			}
		}
		finally {
			startLock.unlock();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
