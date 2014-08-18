package co.arcs.groove.basking.ui;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import butterknife.ButterKnife;
import butterknife.InjectView;
import co.arcs.groove.basking.R;
import co.arcs.groove.basking.SyncOperation;
import co.arcs.groove.basking.SyncOperationWithMediaScanner.MediaScannerStartedEvent;
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
import co.arcs.groove.basking.event.Events.SyncProcessStartedEvent;
import co.arcs.groove.basking.event.TaskEvent;
import co.arcs.groove.basking.task.SyncTask.Outcome;
import co.arcs.groove.basking.ui.TricklingProgressAnimator.Listener;
import de.passsy.holocircularprogressbar.HoloCircularProgressBar;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.observables.ConnectableObservable;

public class CircleSyncProgressController {

    private final Resources resources;
    private final TricklingProgressAnimator<HoloCircularProgressBar> trickleAnimator;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private OperationHandler operationHandler;

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

    public void startDisplayingOperation(SyncOperation operation) {
        stopDisplayingOperation();
        operationHandler = new OperationHandler(operation);
    }

    public void stopDisplayingOperation() {
        if (operationHandler != null) {
            operationHandler.disable();
        }
    }

    private static final String TAG = "cs";

    private class OperationHandler implements FutureCallback<Outcome> {

        private final Subscription cancelSubscription;
        private boolean enabled = true;

        private OperationHandler(SyncOperation operation) {

            Observable buffered = operation.getObservable()
                    .buffer(500, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread());

            ConnectableObservable published = buffered.publish();
            cancelSubscription = published.connect();

            published.subscribe(new Subscriber<List<TaskEvent>>() {
                @Override
                public void onCompleted() {
                    Log.d(TAG, "onFinish");
                }

                @Override
                public void onError(Throwable t) {
                    Log.d(TAG, "onError" + t.getMessage());
                }

                @Override
                public void onNext(List<TaskEvent> taskEvents) {
                    if (taskEvents.size() > 0) {
                        Log.d(TAG, "onNext");
                        for (TaskEvent taskEvent : taskEvents) {
                            Log.d(TAG, "  " + taskEvent.getClass().getSimpleName());
                        }
                    }
                }
            });
        }

        public void disable() {
            cancelSubscription.unsubscribe();
            enabled = false;
        }

        @Override
        public void onSuccess(@Nullable Outcome result) {
            if (enabled) {
                disable();
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

        @Override
        public void onFailure(Throwable t) {
            if (enabled) {
                disable();
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

        @Subscribe
        public void onEvent(SyncProcessStartedEvent e) {
            button.setEnabled(false);
            primaryBar.setProgress(0.0f);
            primaryBar.setProgressColor(primaryBar.getResources()
                    .getColor(R.color.progress_bar_color));
            primaryBar.animate().alpha(1.0f).start();
            trickleAnimator.setTrickleEnabled(true);
            trickleAnimator.start();
        }

        @Subscribe
        public void onEvent(GetSongsToSyncStartedEvent e) {
            if (enabled) {
                primaryText.setText(resources.getString(R.string.status_retrieving_profile));
                primaryText.animate().alpha(1.0f).start();
            }
        }

        @Subscribe
        public void onEvent(GetSongsToSyncProgressChangedEvent e) {
            if (enabled) {
                if (e.getProgress() > 0) {
                    trickleAnimator.increment();
                }
            }
        }

        @Subscribe
        public void onEvent(BuildSyncPlanStartedEvent e) {
            if (enabled) {
                primaryText.setText(resources.getString(R.string.status_building_sync_plan));
            }
        }

        @Subscribe
        public void onEvent(BuildSyncPlanFinishedEvent e) {
            if (enabled) {
                trickleAnimator.increment();
            }
        }

        @Subscribe
        public void onEvent(DownloadSongsStartedEvent e) {
            if (enabled) {
                primaryText.setText(resources.getString(R.string.status_downloading));
                animateSecondaryIn();
            }
        }

        @Subscribe
        public void onEvent(DownloadSongStartedEvent e) {
            if (enabled) {
                secondaryText.setText(e.getSong().getArtistName() + " - " + e.getSong().getName());
                secondaryBar.setProgress(0);
            }
        }

        @Subscribe
        public void onEvent(DownloadSongProgressChangedEvent e) {
            if (enabled) {
                secondaryBar.setProgress((int) (e.getFraction() * secondaryBar.getMax()));
            }
        }

        @Subscribe
        public void onEvent(DownloadSongsFinishedEvent e) {
            if (enabled) {
                trickleAnimator.increment();
                animateSecondaryOut();
            }
        }

        @Subscribe
        public void onEvent(GeneratePlaylistsStartedEvent e) {
            if (enabled) {
                primaryText.setText(resources.getString(R.string.status_generating_playlists));
            }
        }

        @Subscribe
        public void onEvent(GeneratePlaylistsFinishedEvent e) {
            if (enabled) {
                trickleAnimator.increment();
            }
        }

        @Subscribe
        public void onEvent(MediaScannerStartedEvent e) {
            if (enabled) {
                primaryText.setText(resources.getString(R.string.status_updating_media_library));
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
}
