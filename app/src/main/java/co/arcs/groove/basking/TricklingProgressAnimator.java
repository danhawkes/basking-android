package co.arcs.groove.basking;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Property;
import android.view.View;

public class TricklingProgressAnimator<T extends View> {

    public interface Listener {
        void onAnimationCompleted(float progress);
    }

    private static final int TRICKLE_MSG = 1;

    private long trickleInterval = 800L;
    private float trickleDelta = 0.02f;
    private float trickleMax = 0.97f;
    private float tricklePower = 0.5f;

    private float incrementDelta = 0.1f;
    private float incrementPower = 0.5f;
    private float initialIncrementDelta = 0.05f;

    private final T target;
    private final Property<T, Float> progressProperty;
    private boolean trickleEnabled = true;
    private ObjectAnimator currentAnimator;
    private float progress;
    private Listener listener;

    public TricklingProgressAnimator(Class<T> viewType, T target, String propertyName) {
        this.target = target;
        this.progressProperty = Property.of(viewType, Float.class, propertyName);
    }

    private final Handler handler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            if (trickleEnabled) {
                if (addTrickleProgress()) {
                    handler.sendEmptyMessageDelayed(TRICKLE_MSG, trickleInterval);
                } else {
                    stopTrickling();
                }
            }
        }
    };

    private static float decay(float value, float progress, float power) {
        return value * (1.0f - (float) Math.pow(progress, power));
    }

    /**
     * Adds some 'trickle' progress.
     *
     * @return True, if progress could be added, else false. If false is returned, trickle progress
     * cannot be added until the total progress is (re_set to a lower level.
     */
    private boolean addTrickleProgress() {
        float delta = decay(trickleDelta, progress, tricklePower) * (float) Math.random();
        float newProgress = Math.min(delta + progress, trickleMax);
        animateToProgress(newProgress, false);
        return (newProgress < trickleMax);
    }

    private void addProgress() {
        float delta = decay(incrementDelta, progress, incrementPower);
        animateToProgress(progress + delta, true);
    }

    private void animateToProgress(final float progress, boolean notifyOnCompletion) {
        this.progress = progress;
        if (currentAnimator != null) {
            currentAnimator.cancel();
        }
        currentAnimator = ObjectAnimator.ofFloat(target, progressProperty, progress);
        currentAnimator.setFloatValues(progress);
        if (notifyOnCompletion) {
            currentAnimator.addListener(new AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (listener != null) {
                        listener.onAnimationCompleted(progress);
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
        }
        currentAnimator.start();
    }

    private void startTrickling() {
        handler.sendEmptyMessageDelayed(TRICKLE_MSG, trickleInterval);
    }

    private void stopTrickling() {
        handler.removeMessages(TRICKLE_MSG);
    }

    /**
     * Resets the progress to zero, then performs the default start increment.
     */
    public void start() {
        progressProperty.set(target, 0.0f);
        animateToProgress(initialIncrementDelta, false);
        if (trickleEnabled && !handler.hasMessages(TRICKLE_MSG)) {
            startTrickling();
        } else if (!trickleEnabled && handler.hasMessages(TRICKLE_MSG)) {
            stopTrickling();
        }
    }

    public void increment() {
        addProgress();
    }

    public void finish() {
        stopTrickling();
        animateToProgress(1.0f, true);
    }

    ///

    /**
     * @see #setTrickleEnabled(boolean)
     */
    public boolean getTrickleEnabled() {
        return trickleEnabled;
    }

    /**
     * Configures whether the animator should simulate progress by incrementing a small amount at a
     * regular interval.
     *
     * @param enabled
     */
    public void setTrickleEnabled(boolean enabled) {
        if (trickleEnabled && !enabled) {
            stopTrickling();
        } else if (!trickleEnabled && enabled) {
            startTrickling();
        }
        trickleEnabled = enabled;
    }

    /**
     * @see #setProgress(float, boolean)
     */
    public float getProgress() {
        return progress;
    }

    /**
     * Sets the current progress.
     *
     * <p>Progress is represented as a value between 0 and 1, where 1 indicates the task is
     * complete. Values outside these bounds are constrained.</p>
     */
    public void setProgress(float progress, boolean animate) {
        if (animate) {
            animateToProgress(progress, true);
        } else {
            this.progress = progress;
            progressProperty.set(target, progress);
        }
    }

    public long getTrickleInterval() {
        return trickleInterval;
    }

    /**
     * Sets the interval between trickle events that simulate progress. Only relevant if trickle is
     * enabled.
     *
     * @param trickleInterval
     *         Interval in milliseconds.
     */
    public void setTrickleInterval(long trickleInterval) {
        this.trickleInterval = trickleInterval;
    }

    public float getTrickleDelta() {
        return trickleDelta;
    }

    /**
     * Sets the progress to use as a base for the trickle progress calculation. Note that actual
     * progress added by trickle events will depend on this value, the current progress, and also
     * the trickle power (see {@link #setTrickleDecayPower(float)}).
     */
    public void setTrickleDelta(float trickleDelta) {
        this.trickleDelta = trickleDelta;
    }

    public float getTrickleMax() {
        return trickleMax;
    }

    /**
     * Sets the level of progress beyond which the 'trickle' effect is inhibited.
     */
    public void setTrickleMax(float trickleMax) {
        this.trickleMax = trickleMax;
    }

    public float getTrickleDecayPower() {
        return tricklePower;
    }

    /**
     * Sets the style of the 'decay' effect applied to the trickle progress. The decay effect
     * reduces the effect of trickle progress as total progress nears 1.0. A value of 0.5 simulates
     * decreasing exponential decay; 2.0 simulates increasing decay. A value of 1.0 simulates linear
     * decay.
     *
     * @param power
     */
    public void setTrickleDecayPower(float power) {
        this.tricklePower = power;
    }

    public float getIncrementDelta() {
        return incrementDelta;
    }

    public void setIncrementDelta(float incrementDelta) {
        this.incrementDelta = incrementDelta;
    }

    public float getIncrementDecayPower() {
        return incrementPower;
    }

    public void setIncrementDecayPower(float incrementPower) {
        this.incrementPower = incrementPower;
    }

    public Listener getListener() {
        return listener;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }
}
