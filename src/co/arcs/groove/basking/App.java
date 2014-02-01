package co.arcs.groove.basking;

import java.io.File;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import co.arcs.groove.basking.R;

public class App extends android.app.Application {

	@Override
	public void onCreate() {
		super.onCreate();
		initialiseSettings();
	}

	private void initialiseSettings() {
		PreferenceManager.setDefaultValues(this, R.xml.settings, true);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (!prefs.contains(SettingsKeys.SYNC_DIR)) {
			prefs.edit().putString(
					SettingsKeys.SYNC_DIR,
					Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
							+ File.separator + "Grooveshark").apply();
		}
	}
}
