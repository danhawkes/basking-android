package co.arcs.groove.basking;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class BackgroundSyncScheduler {

    public static void reschedule(Context context, boolean enabled) {

        Intent serviceIntent = new Intent(context, BaskingSyncService.class);
        serviceIntent.putExtra(BaskingSyncService.EXTRA_COMMAND, BaskingSyncService.COMMAND_START);

        PendingIntent pendingIntent = PendingIntent.getService(context,
                0,
                serviceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

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
