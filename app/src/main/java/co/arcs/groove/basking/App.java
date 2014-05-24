package co.arcs.groove.basking;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;

import co.arcs.groove.basking.pref.PreferenceKeys;
import co.arcs.groove.basking.pref.PreferenceUtils;

public class App extends android.app.Application {

    static PreferenceUtils preferenceUtils;

    @Override
    public void onCreate() {
        super.onCreate();
        initialisePreferences();
        App.preferenceUtils = new PreferenceUtils(this);
    }

    private void initialisePreferences() {

        // Set default values
        PreferenceManager.setDefaultValues(this, R.xml.settings, true);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.contains(PreferenceKeys.SYNC_DIR)) {
            File syncDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) + File.separator + "Grooveshark");
            prefs.edit().putString(PreferenceKeys.SYNC_DIR, syncDir.getAbsolutePath()).apply();
        }

        prefs.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

                if (key.equals(PreferenceKeys.BACKGROUND_SYNC)) {
                    boolean enabled = sharedPreferences.getBoolean(key, false);
                    rescheduleAlarm(App.this, enabled);
                }
            }
        });
    }

    public static PreferenceUtils getPreferenceUtils() {
        return preferenceUtils;
    }

    static void rescheduleAlarm(Context context, boolean enabled) {

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
