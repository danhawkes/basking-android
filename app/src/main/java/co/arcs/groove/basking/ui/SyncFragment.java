package co.arcs.groove.basking.ui;

import android.app.Fragment;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.eventbus.EventBus;

import javax.inject.Inject;

import co.arcs.groove.basking.App;
import co.arcs.groove.basking.BaskingSyncService;
import co.arcs.groove.basking.BaskingSyncService.SyncBinder;
import co.arcs.groove.basking.R;
import co.arcs.groove.basking.pref.AppPreferences;
import de.passsy.holocircularprogressbar.HoloCircularProgressBar;

public class SyncFragment extends Fragment {

    @Inject AppPreferences appPreferences;
    private Button primaryTextButton;
    private SyncBinder serviceBinder;
    private GuiProgressManager guiProgressManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = new Intent(getActivity(), BaskingSyncService.class);

        ((App) getActivity().getApplication()).inject(this);

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
        return inflater.inflate(R.layout.fragment_sync, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        primaryTextButton = (Button) view.findViewById(R.id.primaryTextButton);
        primaryTextButton.setOnClickListener(syncButtonOnClickListener);
        HoloCircularProgressBar bar1 = (HoloCircularProgressBar) view.findViewById(R.id.primaryBar);
        ProgressBar bar2 = (ProgressBar) view.findViewById(R.id.secondaryBar);
        TextView secondaryText = (TextView) view.findViewById(R.id.secondaryText);
        guiProgressManager = new GuiProgressManager(bar1, bar2, primaryTextButton, secondaryText);
    }

    @Override
    public void onStart() {
        super.onStart();
        appPreferences.getPrefs()
                .registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        primaryTextButton.setEnabled(canSync());
    }

    @Override
    public void onStop() {
        super.onStop();
        appPreferences.getPrefs()
                .unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (serviceBinder != null) {
            serviceBinder.getSyncEventBus().unregister(this);
        }
        getActivity().unbindService(serviceConnection);
    }

    private final OnSharedPreferenceChangeListener preferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            primaryTextButton.setEnabled(canSync());
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            SyncFragment.this.serviceBinder = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SyncFragment.this.serviceBinder = (SyncBinder) service;
            EventBus bus = serviceBinder.getSyncEventBus();
            bus.register(SyncFragment.this);
            bus.register(guiProgressManager);
        }
    };

    private OnClickListener syncButtonOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            getActivity().startService(BaskingSyncService.newStartIntent(getActivity()));
        }
    };

    private boolean canSync() {
        boolean syncOngoing = (serviceBinder != null) && (serviceBinder.isSyncOngoing());
        boolean haveCredentials = appPreferences.hasLoginCredentials();
        return !syncOngoing && haveCredentials;
    }
}
