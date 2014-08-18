package co.arcs.groove.basking.ui;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import co.arcs.groove.basking.App;
import co.arcs.groove.basking.R;
import co.arcs.groove.basking.SyncManager;
import co.arcs.groove.basking.SyncOperation;
import co.arcs.groove.basking.pref.AppPreferences;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.functions.Action1;

public class SyncFragment extends Fragment {

    @Inject AppPreferences appPreferences;
    @Inject SyncManager syncManager;
    @InjectView(R.id.syncButton) Button primaryTextButton;

    private CircleSyncProgressController circleSyncProgressController;
    private Subscription syncOperationSubscriber;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((App) getActivity().getApplication()).inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sync, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.inject(this, view);
        primaryTextButton.setOnClickListener(syncButtonOnClickListener);
        circleSyncProgressController = new CircleSyncProgressController(view);

        syncOperationSubscriber = AndroidObservable.bindFragment(this,
                syncManager.getOperationObservable()).subscribe(new Action1<SyncOperation>() {
            @Override
            public void call(SyncOperation operation) {
                Log.d("syncfrag", "new op = " + operation);
                if (operation != null) {
                    circleSyncProgressController.startDisplayingOperation(operation);
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        appPreferences.getPrefs()
                .registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        appPreferences.getPrefs()
                .unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    public void onDestroy() {
        if (syncOperationSubscriber != null) {
            syncOperationSubscriber.unsubscribe();
        }
        super.onDestroy();
    }

    private final OnSharedPreferenceChangeListener preferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            primaryTextButton.setEnabled(canSync());
        }
    };

    private OnClickListener syncButtonOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (canSync()) {
                syncManager.startSync();
            }
        }
    };

    private boolean canSync() {
        boolean syncOngoing = (syncManager.getOperationObservable().toBlocking().first() != null);
        return !syncOngoing && appPreferences.hasLoginCredentials();
    }
}
