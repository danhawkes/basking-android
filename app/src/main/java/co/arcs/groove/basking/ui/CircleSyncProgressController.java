package co.arcs.groove.basking.ui;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.eventbus.Subscribe;

import butterknife.ButterKnife;
import butterknife.InjectView;
import co.arcs.groove.basking.R;
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
import co.arcs.groove.basking.ui.TricklingProgressAnimator.Listener;
import de.passsy.holocircularprogressbar.HoloCircularProgressBar;

public class CircleSyncProgressController {

    private final Resources resources;
    private final TricklingProgressAnimator<HoloCircularProgressBar> trickleAnimator;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean ignoreBusEvents;

    @InjectView(R.id.syncButton) Button button;
    @InjectView(R.id.primaryBar) HoloCircularProgressBar primaryBar;
    @InjectView(R.id.primaryText) FadingTextView primaryText;
    @InjectView(R.id.secondaryBar) ProgressBar secondaryBar;
    @InjectView(R.id.secondaryText) TextView secondaryText;
    private final int downloadingSongPrimaryTextOffset;
    private final int downloadingSongSecondaryTextOffset;
    private final int downloadingSongSecondaryProgressOffset;
    private final int animationLongDuration;
    private final Interpolator animationInterpolator;

    public CircleSyncProgressController(View viewRoot) {
        ButterKnife.inject(this, viewRoot);
        this.resources = primaryBar.getResources();
        this.trickleAnimator = new TricklingProgressAnimator<HoloCircularProgressBar>(
                HoloCircularProgressBar.class,
                primaryBar,
                "progress");
        trickleAnimator.setListener(trickleAnimatorListener);
        this.downloadingSongPrimaryTextOffset = resources.getDimensionPixelSize(R.dimen.sync_circle_downloadingsong_offset_primarytext);
        this.downloadingSongSecondaryTextOffset = resources.getDimensionPixelSize(R.dimen.sync_circle_downloadingsong_offset_secondaryText);
        this.downloadingSongSecondaryProgressOffset = resources.getDimensionPixelSize(R.dimen.sync_circle_downloadingsong_offset_secondaryProgress);
        this.animationLongDuration = resources.getInteger(android.R.integer.config_longAnimTime);
        this.animationInterpolator = new AccelerateDecelerateInterpolator();
    }

    private final Listener trickleAnimatorListener = new Listener() {
        @Override
        public void onAnimationCompleted(float progress) {
            if (progress == 1.0f) {
                primaryBar.animate().alpha(0.0f).start();
            }
        }
    };

    @Subscribe
    public void onEvent(SyncProcessStartedEvent e) {
        ignoreBusEvents = false;
        button.setEnabled(false);
        primaryBar.setProgress(0.0f);
        primaryBar.setProgressColor(primaryBar.getResources().getColor(R.color.progress_bar_color));
        primaryBar.animate().alpha(1.0f).start();
        trickleAnimator.setTrickleEnabled(true);
        trickleAnimator.start();
    }

    @Subscribe
    public void onEvent(GetSongsToSyncStartedEvent e) {
        if (!ignoreBusEvents) {
            primaryText.setText(resources.getString(R.string.status_retrieving_profile));
            primaryText.animate().alpha(1.0f).start();
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
            primaryText.setText(resources.getString(R.string.status_building_sync_plan));
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
            primaryText.setText(resources.getString(R.string.status_downloading));
            animateSecondaryIn();
        }
    }

    @Subscribe
    public void onEvent(DownloadSongStartedEvent e) {
        if (!ignoreBusEvents) {
            secondaryText.setText(e.getSong().getArtistName() + " - " + e.getSong().getName());
            secondaryBar.setProgress(0);
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
            animateSecondaryOut();
        }
    }

    @Subscribe
    public void onEvent(GeneratePlaylistsStartedEvent e) {
        if (!ignoreBusEvents) {
            primaryText.setText(resources.getString(R.string.status_generating_playlists));
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
            primaryText.setText(resources.getString(R.string.status_finished));
            button.setEnabled(true);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    primaryText.setText(R.string.sync);
                }
            }, 2000);
            trickleAnimator.finish();
        }
    }

    @Subscribe
    public void onEvent(SyncProcessFinishedWithErrorEvent e) {
        if (!ignoreBusEvents) {
            ignoreBusEvents = true;
            animateSecondaryOut();
            button.setEnabled(true);
            primaryText.setText(R.string.sync);
            primaryBar.animate().alpha(0.0f).start();
            trickleAnimator.setTrickleEnabled(false);


            // Failure rewind animation
            ObjectAnimator animator = ObjectAnimator.ofInt(primaryBar,
                    "progressColor",
                    primaryBar.getResources().getColor(R.color.progress_bar_color_failed));
            animator.setEvaluator(new ArgbEvaluator());
            animator.start();
            trickleAnimator.setProgress(0.0f, true);
        }
    }

    private void animateSecondaryIn() {
        primaryText.animate()
                .translationY(downloadingSongPrimaryTextOffset)
                .setInterpolator(animationInterpolator)
                .setDuration(animationLongDuration)
                .start();
        secondaryText.animate()
                .alpha(1.0f)
                .translationY(downloadingSongSecondaryTextOffset)
                .setInterpolator(animationInterpolator)
                .setDuration(animationLongDuration)
                .start();
        secondaryBar.animate()
                .alpha(1.0f)
                .translationY(downloadingSongSecondaryProgressOffset)
                .setInterpolator(animationInterpolator)
                .setDuration(animationLongDuration)
                .start();
    }

    private void animateSecondaryOut() {
        primaryText.animate()
                .translationY(0)
                .setInterpolator(animationInterpolator)
                .setDuration(animationLongDuration)
                .start();
        secondaryText.animate()
                .alpha(0.0f)
                .translationY(0)
                .setInterpolator(animationInterpolator)
                .setDuration(animationLongDuration)
                .start();
        secondaryBar.animate()
                .alpha(0.0f)
                .translationY(0)
                .setInterpolator(animationInterpolator)
                .setDuration(animationLongDuration)
                .start();
    }
}
