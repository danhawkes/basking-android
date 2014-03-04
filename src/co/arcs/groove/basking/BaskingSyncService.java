package co.arcs.groove.basking;

import java.io.File;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import co.arcs.android.util.MainThreadExecutorService;
import co.arcs.groove.basking.pref.PreferenceKeys;
import co.arcs.groove.basking.task.SyncTask.Outcome;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class BaskingSyncService extends Service {

	static final int COMMAND_START = 1;
	static final int COMMAND_STOP = 2;
	static final String EXTRA_COMMAND = "commmand";
	private static final long WAKELOCK_TIMEOUT = 1000 * 60 * 60;

	private final SyncBinder binder = new SyncBinder();
	private BaskingNotificationManager notificationManager;
	private co.arcs.groove.basking.SyncService service;
	private ListenableFuture<Outcome> syncOutcomeFuture;
	private WakeLock wakeLock;

	@Override
	public void onCreate() {
		super.onCreate();
		this.notificationManager = new BaskingNotificationManager(this);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int command = intent.getIntExtra(EXTRA_COMMAND, -1);
		if (command == COMMAND_START) {
			startSync();
		} else if (command == COMMAND_STOP) {
			stopSync();
		}
		return Service.START_NOT_STICKY;
	}

	private void startSync() {

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Config config = new Config();
		config.username = prefs.getString(PreferenceKeys.USERNAME, null);
		config.password = prefs.getString(PreferenceKeys.PASSWORD, null);
		config.syncDir = new File(prefs.getString(PreferenceKeys.SYNC_DIR, null));
		service = new co.arcs.groove.basking.SyncService(config);
		service.getEventBus().register(notificationManager);
		syncOutcomeFuture = service.start();
		notificationManager.setSyncFuture(syncOutcomeFuture);
		Futures.addCallback(syncOutcomeFuture, syncOutcomeCallback, MainThreadExecutorService.get());
		
		moveToForeground();
		acquireWakelock();
	}

	private void stopSync() {
		syncOutcomeFuture.cancel(true);
	}

	private void moveToForeground() {
		Notification notification = notificationManager.newOngoingNotification(
				getApplicationContext()).build();
		startForeground(BaskingNotificationManager.NOTIFICATION_ID_ONGOING, notification);
	}

	private void moveToBackground() {
		stopForeground(true);
	}

	private void acquireWakelock() {
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, EXTRA_COMMAND);
		wakeLock.acquire(WAKELOCK_TIMEOUT);
	}

	private void releaseWakelock() {
		wakeLock.release();
	}

	private FutureCallback<Outcome> syncOutcomeCallback = new FutureCallback<Outcome>() {

		@Override
		public void onSuccess(Outcome arg0) {
			moveToBackground();
			releaseWakelock();
		}

		@Override
		public void onFailure(Throwable arg0) {
			moveToBackground();
			releaseWakelock();
		}
	};

	public class SyncBinder extends android.os.Binder {

		public void start() {
			startSync();
		}
	}

	static Intent newStartIntent(Context context) {
		Intent intent = new Intent(context, BaskingSyncService.class);
		intent.putExtra(EXTRA_COMMAND, COMMAND_START);
		return intent;
	}

	static Intent newStopIntent(Context context) {
		Intent intent = new Intent(context, BaskingSyncService.class);
		intent.putExtra(EXTRA_COMMAND, COMMAND_STOP);
		return intent;
	}
}
