package co.arcs.groove.basking;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import co.arcs.groove.basking.pref.AppPreferences;
import co.arcs.groove.basking.pref.AppPreferences.Keys;

@ReportsCrashes(
        formKey = "",
        formUri = "https://arcs.cloudant.com/acra-basking/_design/acra-storage/_update/report",
        excludeMatchingSharedPreferencesKeys = {Keys.STR_USERNAME, Keys.STR_PASSWORD},
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        formUriBasicAuthLogin = "bosequiestanieropningeri",
        formUriBasicAuthPassword = "p2XDLB6jNdGrAfQSwhWb2MGl")
public class App extends android.app.Application {

    private static AppPreferences appPreferences;

    @Override
    public void onCreate() {
        super.onCreate();

        ACRA.init(this);
        ACRA.getErrorReporter().setEnabled(!BuildConfig.DEBUG);

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
