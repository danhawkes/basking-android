package co.arcs.groove.basking.pref;

import android.os.Bundle;
import co.arcs.groove.basking.R;

public class PreferenceFragment extends android.preference.PreferenceFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.settings);
	}
}
