package co.arcs.groove.basking;

import android.app.Fragment;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.common.eventbus.Subscribe;

import co.arcs.groove.basking.BaskingSyncService.SyncBinder;
import co.arcs.groove.basking.event.impl.SyncEvent;

public class MainFragment extends Fragment {

    private Button syncButton;
    private BaskingSyncService.SyncBinder serviceBinder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = new Intent(getActivity(), BaskingSyncService.class);

        boolean bound = getActivity().bindService(i, serviceConnection, Service.BIND_AUTO_CREATE);
        if (!bound) {
            throw new RuntimeException("Failed to bind to sync service, cannot continue");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
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

        boolean syncOngoing = (serviceBinder != null) && (serviceBinder.isSyncOngoing());
        boolean hasLoginCredentials = App.getAppPreferences().hasLoginCredentials();
        syncButton.setEnabled(!syncOngoing && hasLoginCredentials);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (serviceBinder != null) {
            serviceBinder.getSyncEventBus().unregister(this);
        }
        getActivity().unbindService(serviceConnection);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            MainFragment.this.serviceBinder = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MainFragment.this.serviceBinder = (SyncBinder) service;
            serviceBinder.getSyncEventBus().register(MainFragment.this);
        }
    };

    private OnClickListener syncButtonOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            syncButton.setEnabled(false);
            getActivity().startService(BaskingSyncService.newStartIntent(getActivity()));
        }
    };

    @Subscribe
    public void onEvent(SyncEvent.Finished e) {
        syncButton.setEnabled(true);
    }

    @Subscribe
    public void onEvent(SyncEvent.FinishedWithError e) {
        syncButton.setEnabled(true);
    }
}
