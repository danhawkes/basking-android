package co.arcs.groove.basking;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.CancellationException;

import javax.annotation.Nullable;

import co.arcs.android.util.MainThreadExecutorService;
import co.arcs.groove.basking.pref.AppPreferences.Keys;
import co.arcs.groove.basking.task.SyncTask.Outcome;
import co.arcs.groove.basking.ui.NotificationSyncProgressController;

public class BaskingSyncService extends Service {

    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";

    private static final String TAG = BaskingSyncService.class.getSimpleName();

    /**
     * A sync is expected to finish within half an hour. More than that, and it's assumed to have
     * gone wrong and its wakelock should be released.
     */
    private static final long WAKELOCK_TIMEOUT = 1000 * 60 * 30;

    private SyncService syncService;
    private final SyncBinder binder = new SyncBinder();
    private NotificationSyncProgressController notificationManager;
    private ListenableFuture<Outcome> syncOutcomeFuture;
    private WakeLock wakeLock;
    private WifiLock wifiLock;

    @Override
    public void onCreate() {
        super.onCreate();
        this.notificationManager = new NotificationSyncProgressController(this);
        this.syncService = new SyncService(new AsyncEventBus(MainThreadExecutorService.get()));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean syncRunning = false;
        if (intent != null) {
            String command = intent.getAction();
            if (ACTION_START.equals(command)) {
                syncRunning = startSync();
            } else if (ACTION_STOP.equals(command)) {
                stopSync();
            }
        }
        // Make sure service is stopped if there's no longer a sync running
        if (!syncRunning) {
            stopSelf();
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel any running sync
        stopSync();
        super.onDestroy();
    }

    /**
     * Starts the sync process.
     *
     * @return True, if the sync was started, or was already running. False is returned if it was
     * not possible to start the sync.
     */
    private boolean startSync() {

        if (syncOutcomeFuture != null && !syncOutcomeFuture.isDone()) {
            Log.d(TAG, "Sync start skipped, as one is already running");
            return true;
        }

        // Load config
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Config config = new Config();
        config.username = prefs.getString(Keys.STR_USERNAME, null);
        config.password = prefs.getString(Keys.STR_PASSWORD, null);
        config.syncDir = new File(prefs.getString(Keys.STR_SYNC_DIR, null));

        // Bail out if invalid
        if (config.username == null || config.password == null || config.syncDir == null) {
            Log.d(TAG, "Sync start skipped, as missing account details");
            return false;
        }

        // Set up wake/wifi locks, then start sync
        moveToForeground();
        acquireWakeLock();
        acquireWifiLock();
        syncOutcomeFuture = syncService.start(config);

        notificationManager.startNotifying(syncService.getEventBus());

        Futures.addCallback(syncOutcomeFuture,
                syncOutcomeCallback,
                MainThreadExecutorService.get());

        Log.d(TAG, "Sync started");
        return true;
    }

    private void stopSync() {
        if ((syncOutcomeFuture != null) && !syncOutcomeFuture.isDone()) {
            syncOutcomeFuture.cancel(true);
            // Other cleanup will happen in sync outcome listener
        }
    }

    private void moveToForeground() {
        Notification notification = notificationManager.newOngoingNotification(getApplicationContext());
        startForeground(NotificationSyncProgressController.NOTIFICATION_ID_ONGOING, notification);
    }

    private void moveToBackground() {
        stopForeground(true);
    }

    private void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            this.wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        if (!wakeLock.isHeld()) {
            wakeLock.acquire(WAKELOCK_TIMEOUT);
        }
    }

    private void releaseWakeLock() {
        if ((wakeLock != null) && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private void acquireWifiLock() {
        if (wifiLock == null) {
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            this.wifiLock = wifiManager.createWifiLock(getPackageName());
        }
        if (!wifiLock.isHeld()) {
            wifiLock.acquire();
        }
    }

    private void releaseWifiLock() {
        if ((wifiLock != null) && wifiLock.isHeld()) {
            wifiLock.release();
        }
    }

    private FutureCallback<Outcome> syncOutcomeCallback = new FutureCallback<Outcome>() {

        @Override
        public void onSuccess(Outcome arg0) {
            Log.d(TAG, "Sync finished successfully");
            moveToBackground();
            releaseWakeLock();
            releaseWifiLock();
            stopSelf();
        }

        @Override
        public void onFailure(Throwable arg0) {
            if (arg0 instanceof CancellationException) {
                Log.d(TAG, "Sync cancelled");
            } else {
                Log.d(TAG, "Sync finished with error", arg0);
            }
            moveToBackground();
            releaseWakeLock();
            releaseWifiLock();
            stopSelf();
        }
    };

    public class SyncBinder extends android.os.Binder {

        public boolean isSyncOngoing() {
            return syncOutcomeFuture != null && !syncOutcomeFuture.isDone();
        }

        @Nullable
        public EventBus getSyncEventBus() {
            return syncService.getEventBus();
        }
    }

    public static Intent newStartIntent(Context context) {
        Intent intent = new Intent(context, BaskingSyncService.class);
        intent.setAction(ACTION_START);
        return intent;
    }

    public static Intent newStopIntent(Context context) {
        Intent intent = new Intent(context, BaskingSyncService.class);
        intent.setAction(ACTION_STOP);
        return intent;
    }
}
