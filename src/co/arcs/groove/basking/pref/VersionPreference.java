package co.arcs.groove.basking.pref;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.preference.Preference;
import android.util.AttributeSet;

public class VersionPreference extends Preference {

	public VersionPreference(Context context) {
		super(context);
	}

	public VersionPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public VersionPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public CharSequence getSummary() {
		try {
			Context context = getContext();
			PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(),
					0);
			return "v" + info.versionName;
		} catch (NameNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void onClick() {
		super.onClick();
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse("https://github.com/danhawkes/basking-android"));
		getContext().startActivity(i);
	}
}
