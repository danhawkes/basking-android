package co.arcs.groove.basking.pref;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;

import co.arcs.groove.basking.R;

public class AppPreferences {

    private final Context context;
    private final SharedPreferences prefs;

    public AppPreferences(Context context) {
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    /**
     * Initialises the app's preferences with default values. Does nothing if already initialised.
     */
    public void initialiseDefaults() {

        PreferenceManager.setDefaultValues(context, R.xml.settings, true);

        // Set up a default sync directory. Can't otherwise do this in XML.
        if (!prefs.contains(Keys.STR_SYNC_DIR)) {
            File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            File syncDir = new File(musicDir, "Grooveshark");
            prefs.edit().putString(Keys.STR_SYNC_DIR, syncDir.getAbsolutePath()).apply();
        }
    }

    public boolean hasLoginCredentials() {
        return prefs.getString(Keys.STR_USERNAME, null) != null;
    }

    public boolean backgroundSyncEnabled() {
        return prefs.getBoolean(Keys.BOOL_BACKGROUND_SYNC, false);
    }

    public SharedPreferences getPrefs() {
        return prefs;
    }

    public static class Keys {

        public static final String STR_SYNC_DIR = "pref_sync_dir";
        public static final String STR_USERNAME = "pref_username";
        public static final String STR_PASSWORD = "pref_password";
        public static final String BOOL_BACKGROUND_SYNC = "pref_background_sync";
    }
}
