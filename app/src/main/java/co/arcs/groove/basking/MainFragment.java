package co.arcs.groove.basking;

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
import android.widget.TextView;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import co.arcs.groove.basking.BaskingSyncService.SyncBinder;
import co.arcs.groove.basking.event.Events.SyncProcessFinishedEvent;
import co.arcs.groove.basking.event.Events.SyncProcessFinishedWithErrorEvent;
import de.passsy.holocircularprogressbar.HoloCircularProgressBar;

public class MainFragment extends Fragment {

    private Button syncButton;
    private BaskingSyncService.SyncBinder serviceBinder;
    private HoloCircularProgressBar bar1;
    private TextView textView;
    private GuiProgressManager guiProgressManager;

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
        bar1 = (HoloCircularProgressBar) view.findViewById(R.id.bar1);
        textView = (TextView) view.findViewById(R.id.text);
        guiProgressManager = new GuiProgressManager(bar1, textView);
    }

    @Override
    public void onStart() {
        super.onStart();
        App.getAppPreferences()
                .getPrefs()
                .registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        syncButton.setEnabled(canSync());
    }

    @Override
    public void onStop() {
        super.onStop();
        App.getAppPreferences()
                .getPrefs()
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
            syncButton.setEnabled(canSync());
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            MainFragment.this.serviceBinder = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MainFragment.this.serviceBinder = (SyncBinder) service;
            EventBus bus = serviceBinder.getSyncEventBus();
            bus.register(MainFragment.this);
            bus.register(guiProgressManager);
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
    public void onEvent(SyncProcessFinishedEvent e) {
        syncButton.setEnabled(true);
    }

    @Subscribe
    public void onEvent(SyncProcessFinishedWithErrorEvent e) {
        syncButton.setEnabled(true);
    }

    private boolean canSync() {
        boolean syncOngoing = (serviceBinder != null) && (serviceBinder.isSyncOngoing());
        boolean haveCredentials = App.getAppPreferences().hasLoginCredentials();
        return !syncOngoing && haveCredentials;
    }
}
