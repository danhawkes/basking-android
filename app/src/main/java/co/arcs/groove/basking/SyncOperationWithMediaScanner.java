package co.arcs.groove.basking;

import android.content.Context;

import com.google.common.base.Function;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;

import javax.annotation.Nullable;

import co.arcs.groove.basking.event.TaskEvent;
import co.arcs.groove.basking.task.SyncTask.Outcome;
import co.arcs.groove.basking.task.Task;

import static com.google.common.util.concurrent.Futures.transform;

/**
 * Subclass of {@link co.arcs.groove.basking.SyncOperation} that adds an additional step in which
 * the Android media scanner is run. Note that this means {@link co.arcs.groove.basking.event.Events.SyncProcessFinishedEvent}
 * and {@link co.arcs.groove.basking.event.Events.SyncProcessFinishedWithErrorEvent} are no longer
 * relevant as further work occurs after the main operation has finished.
 */
public class SyncOperationWithMediaScanner extends SyncOperation {

    public static class MediaScannerTask implements Task<Void> {

        @Override
        public Void call() throws Exception {
            return null;
        }
    }

    public static class MediaScannerStartedEvent extends TaskEvent<MediaScannerTask> {
        public MediaScannerStartedEvent(MediaScannerTask task) {
            super(task);
        }
    }

    public static class MediaScannerFinishedEvent extends TaskEvent<MediaScannerTask> {
        public MediaScannerFinishedEvent(MediaScannerTask task) {
            super(task);
        }
    }

    private final Context context;
    private final Config config;

    public SyncOperationWithMediaScanner(Config config, Context context, EventBus bus) {
        super(config, bus);
        this.context = context;
        this.config = config;
    }

    @Override
    protected ListenableFuture<Outcome> startFuture() {
        final MediaScannerTask task = new MediaScannerTask();

        return transform(super.startFuture(), new AsyncFunction<Outcome, Outcome>() {
            @Override
            public ListenableFuture<Outcome> apply(final Outcome outcome) throws Exception {

                poster.post(new MediaScannerStartedEvent(task));

                return transform(new MediaScanner(context, config.syncDir).scan(),
                        new Function<Void, Outcome>() {
                            @Nullable
                            @Override
                            public Outcome apply(@Nullable Void input) {

                                poster.post(new MediaScannerFinishedEvent(task));

                                return outcome;
                            }
                        });
            }
        });
    }
}
