package co.arcs.groove.basking;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import javax.inject.Inject;

import co.arcs.groove.basking.inject.AppModule.ApplicationContext;
import co.arcs.groove.basking.pref.AppPreferences;
import co.arcs.groove.basking.pref.AppPreferences.Keys;

public class BackgroundSyncScheduler {

    @Inject AppPreferences appPreferences;
    @Inject @ApplicationContext Context context;

    /**
     * Starts the scheduler. This will schedule or cancel pending sync operations according to
     * changes in the app preferences.
     */
    public void init() {
        appPreferences.getPrefs()
                .registerOnSharedPreferenceChangeListener(onPreferenceChangedListener);
        updateSchedule();
    }

    private void updateSchedule() {

        PendingIntent pendingIntent = PendingIntent.getService(context,
                0,
                BaskingSyncService.newStartIntent(context),
                0);

        AlarmManager alarmMan = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (appPreferences.backgroundSyncEnabled()) {
            long interval = AlarmManager.INTERVAL_DAY;
            alarmMan.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    interval,
                    interval,
                    pendingIntent);
        } else {
            alarmMan.cancel(pendingIntent);
        }
    }

    private final OnSharedPreferenceChangeListener onPreferenceChangedListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(Keys.BOOL_BACKGROUND_SYNC)) {
                updateSchedule();
            }
        }
    };
}
