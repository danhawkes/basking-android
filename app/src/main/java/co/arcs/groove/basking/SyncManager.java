package co.arcs.groove.basking;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import java.io.File;
import java.util.concurrent.CancellationException;

import co.arcs.android.util.MainThreadExecutorService;
import co.arcs.groove.basking.pref.AppPreferences.Keys;
import co.arcs.groove.basking.task.SyncTask.Outcome;
import co.arcs.groove.basking.ui.NotificationSyncProgressController;
import rx.Observable;
import rx.subjects.BehaviorSubject;

public class SyncManager {

    /**
     * A sync is expected to finish within half an hour. More than that, and it's assumed to have
     * gone wrong and its wake locks should be released.
     */
    private static final long WAKELOCK_TIMEOUT = 1000 * 60 * 30;
    private static final String TAG = SyncManager.class.getSimpleName();

    private final Context context;
    private BehaviorSubject<SyncOperation> operationSubject = BehaviorSubject.create((SyncOperation) null);
    private NotificationSyncProgressController notificationManager;
    private WakeLock wakeLock;
    private WifiLock wifiLock;
    private SyncOperationWithMediaScanner operation;

    public SyncManager(Context context) {
        this.context = context;
        this.notificationManager = new NotificationSyncProgressController(context);
    }

    public Observable<SyncOperation> getOperationObservable() {
        return operationSubject;
    }

    /**
     * Starts the sync process.
     *
     * @return True, if the sync was started, or was already running. False is returned if it was
     * not possible to start the sync.
     */
    public boolean startSync() {

        if (operation != null && !operation.getFuture().isDone()) {
            Log.d(TAG, "Sync start skipped, as one is already running");
            return true;
        }

        // Load config
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final Config config = new Config();
        config.username = prefs.getString(Keys.STR_USERNAME, null);
        config.password = prefs.getString(Keys.STR_PASSWORD, null);
        config.syncDir = new File(prefs.getString(Keys.STR_SYNC_DIR, null));

        // Bail out if invalid
        if (config.username == null || config.password == null) {
            Log.d(TAG, "Sync start skipped, as missing account details");
            return false;
        }

        startService();

        // Set up wake/wifi locks and start sync
        acquireWakeLock();
        acquireWifiLock();
        operation = new SyncOperationWithMediaScanner(config,
                context,
                new AsyncEventBus(MainThreadExecutorService.get()));

        operation.start();

        operationSubject.onNext(operation);

        notificationManager.startNotifying(operation.getEventBus());

        Futures.addCallback(operation.getFuture(),
                syncOutcomeCallback,
                MainThreadExecutorService.get());

        Log.d(TAG, "Sync started");
        return true;
    }

    public void stopSync() {
        if ((operation != null) && !operation.getFuture().isDone()) {
            operation.getFuture().cancel(true);
            // Other cleanup will happen in sync outcome listener
        }
    }

    private FutureCallback<Outcome> syncOutcomeCallback = new FutureCallback<Outcome>() {

        @Override
        public void onSuccess(Outcome arg0) {
            Log.d(TAG, "Sync finished successfully");
            shutdown();
        }

        @Override
        public void onFailure(Throwable arg0) {
            if (arg0 instanceof CancellationException) {
                Log.d(TAG, "Sync cancelled");
            } else {
                Log.d(TAG, "Sync finished with error", arg0);
            }
            shutdown();
        }

        private void shutdown() {
            releaseWakeLock();
            releaseWifiLock();
            stopService();
        }
    };

    private void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
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
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            this.wifiLock = wifiManager.createWifiLock(context.getPackageName());
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

    private void startService() {
        context.startService(SyncService.newStartIntent(context));
    }

    private void stopService() {
        context.startService(SyncService.newStopIntent(context));
    }
}
