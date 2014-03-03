package co.arcs.groove.basking.pref;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import co.arcs.android.fileselector.FileSelectorActivity;

public class PreferenceActivity extends Activity implements SyncDirPreference.Listener {

	private static final int REQUEST_SYNC_DIR = 1;
	private SyncDirPreference syncDirPreference;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		if (savedInstanceState == null) {
			PreferenceFragment fragment = new PreferenceFragment();
			getFragmentManager().beginTransaction().add(android.R.id.content, fragment, null)
					.commit();
		}
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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_SYNC_DIR && resultCode == Activity.RESULT_OK) {
			File file = (File) data.getSerializableExtra(FileSelectorActivity.EXTRA_PICKED_FILE);
			syncDirPreference.onDirectorySelected(file);
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public void onClickSyncDirPreference(SyncDirPreference preference) {
		this.syncDirPreference = preference;
		Intent i = new Intent(this, FileSelectorActivity.class);
		i.putExtra(FileSelectorActivity.EXTRA_STR_SELECTION_TYPE,
				FileSelectorActivity.TYPE_DIRECTORY);
		startActivityForResult(i, REQUEST_SYNC_DIR);
	}
}
