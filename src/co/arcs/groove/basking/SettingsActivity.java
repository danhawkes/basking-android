package co.arcs.groove.basking;

import android.app.Activity;
import android.os.Bundle;

public class SettingsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SettingsFragment fragment = new SettingsFragment();
		getFragmentManager().beginTransaction().add(android.R.id.content, fragment, null).commit();
	}
}
