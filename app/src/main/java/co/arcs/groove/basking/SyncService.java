package co.arcs.groove.basking;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import co.arcs.groove.basking.ui.NotificationSyncProgressController;

/**
 * A foreground service that ensures the app can continue running while a sync operation is
 * ongoing.
 *
 * <p>Note: The service itself doesn't do anything other than start and stop in response to
 * commands; the sync operation is handled in {@link SyncManager}.</p>
 */
public class SyncService extends Service {

    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";

    private NotificationSyncProgressController notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        this.notificationManager = new NotificationSyncProgressController(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new RuntimeException("This service cannot be bound");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String command = intent.getAction();
            if (ACTION_START.equals(command)) {
                moveToForeground();
            } else if (ACTION_STOP.equals(command)) {
                moveToBackground();
                stopSelf();
            }
        } else {
            stopSelf();
        }
        return Service.START_NOT_STICKY;
    }

    private void moveToForeground() {
        Notification notification = notificationManager.newOngoingNotification(getApplicationContext());
        startForeground(NotificationSyncProgressController.NOTIFICATION_ID_ONGOING, notification);
    }

    private void moveToBackground() {
        stopForeground(true);
    }

    public static Intent newStartIntent(Context context) {
        Intent intent = new Intent(context, SyncService.class);
        intent.setAction(ACTION_START);
        return intent;
    }

    public static Intent newStopIntent(Context context) {
        Intent intent = new Intent(context, SyncService.class);
        intent.setAction(ACTION_STOP);
        return intent;
    }
}
