package co.arcs.groove.basking;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class MainFragment extends Fragment {

	private Button syncButton;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.fragment_main, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		syncButton = (Button) view.findViewById(R.id.sync_button);
		syncButton.setOnClickListener(syncButtonOnClickListener);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		syncButton.setEnabled(App.getPreferenceUtils().hasLoginCredentials());
	}

	private OnClickListener syncButtonOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Context context = getActivity().getApplicationContext();

			Intent startIntent = new Intent(getActivity(), BaskingSyncService.class);
			startIntent
					.putExtra(BaskingSyncService.EXTRA_COMMAND, BaskingSyncService.COMMAND_START);

			context.startService(startIntent);
		}
	};
}
