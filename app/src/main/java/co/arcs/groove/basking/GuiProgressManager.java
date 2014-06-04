package co.arcs.groove.basking;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.widget.TextView;

import com.google.common.eventbus.Subscribe;

import co.arcs.groove.basking.TricklingProgressAnimator.Listener;
import co.arcs.groove.basking.event.Events.BuildSyncPlanFinishedEvent;
import co.arcs.groove.basking.event.Events.DownloadSongsFinishedEvent;
import co.arcs.groove.basking.event.Events.GeneratePlaylistsFinishedEvent;
import co.arcs.groove.basking.event.Events.GetSongsToSyncProgressChangedEvent;
import co.arcs.groove.basking.event.Events.SyncProcessFinishedEvent;
import co.arcs.groove.basking.event.Events.SyncProcessFinishedWithErrorEvent;
import co.arcs.groove.basking.event.Events.SyncProcessStartedEvent;
import co.arcs.groove.basking.event.TaskEvent.TaskStartedEvent;
import de.passsy.holocircularprogressbar.HoloCircularProgressBar;

public class GuiProgressManager {

    private final HoloCircularProgressBar bar1;
    private final TextView textView;
    private final TricklingProgressAnimator trickleAnimator;
    private boolean ignoreBusEvents;

    public GuiProgressManager(HoloCircularProgressBar bar1, TextView textView) {
        this.bar1 = bar1;
        this.textView = textView;
        this.trickleAnimator = new TricklingProgressAnimator(HoloCircularProgressBar.class,
                bar1,
                "progress");
        trickleAnimator.setListener(trickleAnimatorListener);
    }

    private Listener trickleAnimatorListener = new Listener() {
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
    public void onEvent(TaskStartedEvent e) {
        if (!ignoreBusEvents) {
            textView.setText(e.getTask().getClass().getSimpleName());
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
    public void onEvent(BuildSyncPlanFinishedEvent e) {
        if (!ignoreBusEvents) {
            trickleAnimator.increment();
        }
    }

    @Subscribe
    public void onEvent(DownloadSongsFinishedEvent e) {
        if (!ignoreBusEvents) {
            trickleAnimator.increment();
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
            trickleAnimator.finish();
        }
    }

    @Subscribe
    public void onEvent(SyncProcessFinishedWithErrorEvent e) {
        if (!ignoreBusEvents) {
            ignoreBusEvents = true;
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
