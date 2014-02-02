package co.arcs.groove.basking.pref;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

public class PreferenceActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		PreferenceFragment fragment = new PreferenceFragment();
		getFragmentManager().beginTransaction().add(android.R.id.content, fragment, null).commit();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
	        NavUtils.navigateUpFromSameTask(this);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
}
