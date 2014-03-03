package co.arcs.groove.basking;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import co.arcs.groove.basking.R;
import co.arcs.groove.basking.pref.PreferenceActivity;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState == null) {
			Fragment fragment = new MainFragment();
			getFragmentManager().beginTransaction().add(android.R.id.content, fragment).commit();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_settings) {
			startActivity(new Intent(this, PreferenceActivity.class));
			return true;
		}
		return false;
	}
}
