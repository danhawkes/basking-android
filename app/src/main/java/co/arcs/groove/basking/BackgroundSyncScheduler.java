package co.arcs.groove.basking;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;

public class BackgroundSyncScheduler {

    public static void reschedule(Context context, boolean enabled) {

        PendingIntent pendingIntent = PendingIntent.getService(context,
                0,
                BaskingSyncService.newStartIntent(context),
                0);

        AlarmManager alarmMan = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (enabled) {
            long interval = AlarmManager.INTERVAL_HALF_DAY;
            alarmMan.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    interval,
                    interval,
                    pendingIntent);
        } else {
            alarmMan.cancel(pendingIntent);
        }
    }
}
