package co.arcs.groove.basking;

import java.io.File;

import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import co.arcs.groove.basking.pref.PreferenceKeys;

public class App extends android.app.Application {

	@Override
	public void onCreate() {
		super.onCreate();
		initialisePreferences();
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
}
