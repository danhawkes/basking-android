package co.arcs.groove.basking.pref;

import java.io.File;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

public class SyncDirPreference extends Preference {

	public interface Listener {

		void onClickSyncDirPreference(SyncDirPreference preference);
	}

	public SyncDirPreference(Context context) {
		super(context);
	}

	public SyncDirPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SyncDirPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onClick() {
		super.onClick();
		Context context = getContext();
		if (context instanceof Listener) {
			((Listener) context).onClickSyncDirPreference(this);
		}
	}

	@Override
	protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
		super.onAttachedToHierarchy(preferenceManager);
		String path = getPersistedString(null);
		if (path != null) {
			setSummary(path);
		}
	}

	/**
	 * Callback for when the user has selected a new sync directory.
	 */
	public void onDirectorySelected(File directory) {
		String path = directory.getAbsolutePath();
		persistString(path);
		setSummary(path);
	}
}
