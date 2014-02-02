package co.arcs.groove.basking.pref;

import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

public class SyncDirPreference extends EditTextPreference {

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
	protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
		super.onAttachedToHierarchy(preferenceManager);
		String path = getPersistedString(null);
		if (path != null) {
			setSummary(path);
		}
	}
}
