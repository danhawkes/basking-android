package co.arcs.groove.basking;

import java.io.File;

import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
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
		PreferenceManager.setDefaultValues(this, R.xml.settings, true);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.edit()
				.putString(
						PreferenceKeys.SYNC_DIR,
						Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
								+ File.separator + "Grooveshark").apply();
	}
	
	
	public static PreferenceUtils getPreferenceUtils() {
		return preferenceUtils;
	}
}
