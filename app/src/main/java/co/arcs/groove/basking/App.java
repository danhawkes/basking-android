package co.arcs.groove.basking;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import co.arcs.groove.basking.pref.AppPreferences;
import co.arcs.groove.basking.pref.AppPreferences.Keys;

public class App extends android.app.Application {

    static AppPreferences appPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        App.appPreferences = new AppPreferences(this);
        appPreferences.initialiseDefaults();

        appPreferences.getPrefs()
                .registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {

                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                            String key) {

                        if (key.equals(Keys.BOOL_BACKGROUND_SYNC)) {
                            boolean enabled = sharedPreferences.getBoolean(key, false);
                            BackgroundSyncScheduler.reschedule(App.this, enabled);
                        }
                    }
                });

        if (appPreferences.backgroundSyncEnabled()) {
            BackgroundSyncScheduler.reschedule(this, true);
        }
    }

    public static AppPreferences getAppPreferences() {
        return appPreferences;
    }
}
