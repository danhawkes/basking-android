package co.arcs.groove.basking.pref;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceUtils {

    private final SharedPreferences prefs;

    public PreferenceUtils(Context context) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public boolean hasLoginCredentials() {
        return prefs.getString(PreferenceKeys.USERNAME, null) != null;
    }

    public boolean backgroundSyncEnabled() {
        return prefs.getBoolean(PreferenceKeys.BACKGROUND_SYNC, false);
    }
}
