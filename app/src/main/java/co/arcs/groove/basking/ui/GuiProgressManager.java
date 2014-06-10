package co.arcs.groove.basking.ui;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.eventbus.Subscribe;

import co.arcs.groove.basking.R;
import co.arcs.groove.basking.ui.TricklingProgressAnimator.Listener;
import co.arcs.groove.basking.event.Events.BuildSyncPlanFinishedEvent;
import co.arcs.groove.basking.event.Events.BuildSyncPlanStartedEvent;
import co.arcs.groove.basking.event.Events.DownloadSongProgressChangedEvent;
import co.arcs.groove.basking.event.Events.DownloadSongStartedEvent;
import co.arcs.groove.basking.event.Events.DownloadSongsFinishedEvent;
import co.arcs.groove.basking.event.Events.DownloadSongsStartedEvent;
import co.arcs.groove.basking.event.Events.GeneratePlaylistsFinishedEvent;
import co.arcs.groove.basking.event.Events.GeneratePlaylistsStartedEvent;
import co.arcs.groove.basking.event.Events.GetSongsToSyncProgressChangedEvent;
import co.arcs.groove.basking.event.Events.GetSongsToSyncStartedEvent;
import co.arcs.groove.basking.event.Events.SyncProcessFinishedEvent;
import co.arcs.groove.basking.event.Events.SyncProcessFinishedWithErrorEvent;
import co.arcs.groove.basking.event.Events.SyncProcessStartedEvent;
import de.passsy.holocircularprogressbar.HoloCircularProgressBar;

public class GuiProgressManager {

    private final Resources res;
    private final HoloCircularProgressBar bar1;
    private final ProgressBar secondaryBar;
    private final Button button;
    private final TextView secondaryText;
    private final TricklingProgressAnimator<HoloCircularProgressBar> trickleAnimator;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean ignoreBusEvents;

    public GuiProgressManager(HoloCircularProgressBar bar1,
            ProgressBar secondaryBar,
            Button button,
            TextView secondaryText) {
        this.res = bar1.getResources();
        this.bar1 = bar1;
        this.secondaryBar = secondaryBar;
        this.button = button;
        this.secondaryText = secondaryText;
        this.trickleAnimator = new TricklingProgressAnimator<HoloCircularProgressBar>(HoloCircularProgressBar.class,
                bar1,
                "progress");
        trickleAnimator.setListener(trickleAnimatorListener);
    }

    private final Listener trickleAnimatorListener = new Listener() {
        @Override
        public void onAnimationCompleted(float progress) {
            if (progress == 1.0f) {
                bar1.animate().alpha(0.0f).start();
            }
        }
    };

    @Subscribe
    public void onEvent(SyncProcessStartedEvent e) {
        ignoreBusEvents = false;
        bar1.setProgress(0.0f);
        bar1.setProgressColor(bar1.getResources().getColor(R.color.progress_bar_color));
        bar1.animate().alpha(1.0f).start();
        trickleAnimator.setTrickleEnabled(true);
        trickleAnimator.start();
    }

    @Subscribe
    public void onEvent(GetSongsToSyncStartedEvent e) {
        if (!ignoreBusEvents) {
            button.setText(res.getString(R.string.status_retrieving_profile));
        }
    }

    @Subscribe
    public void onEvent(GetSongsToSyncProgressChangedEvent e) {
        if (!ignoreBusEvents) {
            if (e.getProgress() > 0) {
                trickleAnimator.increment();
            }
        }
    }

    @Subscribe
    public void onEvent(BuildSyncPlanStartedEvent e) {
        if (!ignoreBusEvents) {
            button.setText(res.getString(R.string.status_building_sync_plan));
        }
    }

    @Subscribe
    public void onEvent(BuildSyncPlanFinishedEvent e) {
        if (!ignoreBusEvents) {
            trickleAnimator.increment();
        }
    }

    @Subscribe
    public void onEvent(DownloadSongsStartedEvent e) {
        if (!ignoreBusEvents) {
            button.setText(res.getString(R.string.status_downloading));
            secondaryText.animate().alpha(1.0f).start();
            secondaryBar.animate().alpha(1.0f).start();
        }
    }

    @Subscribe
    public void onEvent(DownloadSongStartedEvent e) {
        if (!ignoreBusEvents) {
            secondaryText.setText(e.getSong().getArtistName() + " - " + e.getSong().getName());
        }
    }

    @Subscribe
    public void onEvent(DownloadSongProgressChangedEvent e) {
        if (!ignoreBusEvents) {
            secondaryBar.setProgress((int) (e.getFraction() * secondaryBar.getMax()));
        }
    }

    @Subscribe
    public void onEvent(DownloadSongsFinishedEvent e) {
        if (!ignoreBusEvents) {
            trickleAnimator.increment();
            secondaryText.animate().alpha(0.0f).start();
            secondaryBar.animate().alpha(0.0f).start();
        }
    }

    @Subscribe
    public void onEvent(GeneratePlaylistsStartedEvent e) {
        if (!ignoreBusEvents) {
            button.setText(res.getString(R.string.status_generating_playlists));
        }
    }

    @Subscribe
    public void onEvent(GeneratePlaylistsFinishedEvent e) {
        if (!ignoreBusEvents) {
            trickleAnimator.increment();
        }
    }

    @Subscribe
    public void onEvent(SyncProcessFinishedEvent e) {
        if (!ignoreBusEvents) {
            ignoreBusEvents = true;
            button.setText(res.getString(R.string.status_finished));
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    button.setText(res.getString(R.string.sync));
                }
            }, 2000);
            trickleAnimator.finish();
        }
    }

    @Subscribe
    public void onEvent(SyncProcessFinishedWithErrorEvent e) {
        if (!ignoreBusEvents) {
            ignoreBusEvents = true;
            button.setText(res.getString(R.string.sync));
            secondaryText.animate().alpha(0.0f).start();
            secondaryBar.animate().alpha(0.0f).start();
            trickleAnimator.setTrickleEnabled(false);
            ObjectAnimator animator = ObjectAnimator.ofInt(bar1,
                    "progressColor",
                    bar1.getResources().getColor(R.color.progress_bar_color_failed));
            animator.setEvaluator(new ArgbEvaluator());
            animator.start();
            trickleAnimator.setProgress(0.0f, true);
        }
    }
}
