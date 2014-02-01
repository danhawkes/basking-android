package co.arcs.groove.basking;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import co.arcs.groove.basking.R;

public class SettingsFragment extends PreferenceFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.settings);
	} 
}
