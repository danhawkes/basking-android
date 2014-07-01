package co.arcs.groove.basking.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import co.arcs.groove.basking.BaskingSyncService;
import co.arcs.groove.basking.R;
import co.arcs.groove.basking.event.Events.BuildSyncPlanProgressChangedEvent;
import co.arcs.groove.basking.event.Events.BuildSyncPlanStartedEvent;
import co.arcs.groove.basking.event.Events.DeleteFilesStartedEvent;
import co.arcs.groove.basking.event.Events.DownloadSongProgressChangedEvent;
import co.arcs.groove.basking.event.Events.DownloadSongStartedEvent;
import co.arcs.groove.basking.event.Events.GeneratePlaylistsProgressChangedEvent;
import co.arcs.groove.basking.event.Events.GeneratePlaylistsStartedEvent;
import co.arcs.groove.basking.event.Events.GetSongsToSyncProgressChangedEvent;
import co.arcs.groove.basking.event.Events.GetSongsToSyncStartedEvent;
import co.arcs.groove.basking.event.Events.SyncProcessFinishedEvent;
import co.arcs.groove.basking.event.Events.SyncProcessFinishedWithErrorEvent;
import co.arcs.groove.basking.task.SyncTask.Outcome;
import co.arcs.groove.thresher.GroovesharkException.InvalidCredentialsException;
import co.arcs.groove.thresher.GroovesharkException.RateLimitedException;
import co.arcs.groove.thresher.GroovesharkException.ServerErrorException;

/**
 * Watches events on the sync event bus and updates 'in-progress' and 'finished' notifications.
 *
 * <p>Some implementation notes:</p>
 *
 * <p>The 'in-progress' notification is supplied to the sync service for use as its foreground
 * notification. The sync service dismisses the notification at the end of the sync process when it
 * returns to the background state.</p>
 *
 * <p>It turns out building notifications is very slow (Notification.Builder#build() takes ~60ms),
 * and showing them is even slower (due to a load of IPC). Trying to update the notification at full
 * speed maxes out the CPU, so instead updates from the event bus are batched into updates that get
 * published at a set interval.
 */
public class NotificationSyncProgressController {

    public static final int NOTIFICATION_ID_ONGOING = 2387544;
    private static final int NOTIFICATION_ID_FINISHED = 8764332;
    public static final int NOTIFICATION_PI_REQUESTCODE_VIEW = 4590867;
    public static final int NOTIFICATION_PI_REQUESTCODE_CANCEL = 3289253;
    private static final int MSG_UPDATE_ONGOING = 1;
    private static final int MSG_UPDATE_FINISHED = 2;
    private static final int NOTIFICATION_UPDATE_INTERVAL = 100;

    private final NotificationValueHolder ongoing = new NotificationValueHolder(
            NOTIFICATION_ID_ONGOING);
    private final NotificationValueHolder finished = new NotificationValueHolder(
            NOTIFICATION_ID_FINISHED);

    private final Context context;
    private final NotificationManager notMan;

    private final PendingIntent viewPendingIntent;
    private final PendingIntent cancelPendingIntent;

    private EventBus syncServiceEventBus;
    private boolean handleOngoingEvents;

    public NotificationSyncProgressController(Context context) {
        this.context = context;
        this.notMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.viewPendingIntent = PendingIntent.getActivity(context,
                NOTIFICATION_PI_REQUESTCODE_VIEW,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        this.cancelPendingIntent = PendingIntent.getService(context,
                NOTIFICATION_PI_REQUESTCODE_CANCEL,
                BaskingSyncService.newStopIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT);

        ongoing.iconId = android.R.drawable.stat_notify_sync;
        ongoing.viewPendingIntent = viewPendingIntent;
        ongoing.cancelPendingIntent = cancelPendingIntent;
        ongoing.title = context.getString(R.string.notif_ongoing_title);
        ongoing.subtitle = null;
        ongoing.ticker = ongoing.title;
        ongoing.showProgress = true;
        ongoing.progressPercent = 0;
        ongoing.hasCancelButton = true;
        ongoing.ongoing = true;
    }

    /**
     * Configure the notification manager to observe a new sync operation.
     *
     * <p>Note: Any previously supplied sync future must have finished before calling this method
     * with a new future.</p>
     *
     * @param syncEventBus
     *         Bus on which sync progress events will be delivered.
     */
    public void startNotifying(EventBus syncEventBus) {

        // Remove any finished notification from a previous sync
        notMan.cancel(finished.id);

        // Observe progress events on the bus.
        this.syncServiceEventBus = syncEventBus;
        startObservingEvents(syncEventBus);
    }

    /**
     * Generate a new 'ongoing' notification for use by the {@link BaskingSyncService} when going
     * into the foreground state.
     */
    public Notification newOngoingNotification(Context context) {
        return newNotification(context, ongoing);
    }

    private Notification newNotification(Context context, NotificationValueHolder holder) {

        Notification.Builder builder = new Notification.Builder(context);
        builder.setSmallIcon(holder.iconId);
        builder.setContentTitle(holder.title);
        if (holder.subtitle != null) {
            builder.setContentText(holder.subtitle);
        }
        builder.setTicker(holder.ticker);
        if (holder.viewPendingIntent != null) {
            builder.setContentIntent(holder.viewPendingIntent);
            if (holder.dismissOnClick) {
                builder.setAutoCancel(true);
            }
        }
        if (holder.hasCancelButton) {
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel,
                    context.getString(android.R.string.cancel),
                    holder.cancelPendingIntent);
        }
        if (holder.showProgress) {
            builder.setProgress(100, holder.progressPercent, false);
        }
        builder.setOngoing(holder.ongoing);
        return builder.build();
    }

    void invalidateOngoingNotification() {
        // Only send another message if not already 'invalidated'
        if (!handler.hasMessages(MSG_UPDATE_ONGOING)) {
            handler.sendEmptyMessageDelayed(MSG_UPDATE_ONGOING, NOTIFICATION_UPDATE_INTERVAL);
        }
    }

    void invalidateFinishedNotification() {
        if (!handler.hasMessages(MSG_UPDATE_FINISHED)) {
            handler.sendEmptyMessageDelayed(MSG_UPDATE_FINISHED, NOTIFICATION_UPDATE_INTERVAL);
        }
    }

    private final Handler handler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_UPDATE_ONGOING) {
                if (handleOngoingEvents) {
                    notMan.notify(ongoing.id, newNotification(context, ongoing));
                }
            } else if (msg.what == MSG_UPDATE_FINISHED) {
                notMan.notify(finished.id, newNotification(context, finished));
            }
        }
    };

    // Query API for use information

    @Subscribe
    public void onEvent(GetSongsToSyncStartedEvent e) {
        if (handleOngoingEvents) {
            ongoing.subtitle = context.getString(R.string.status_retrieving_profile);
            invalidateOngoingNotification();
        }
    }

    @Subscribe
    public void onEvent(GetSongsToSyncProgressChangedEvent e) {
        if (handleOngoingEvents) {
            ongoing.progressPercent = (int) e.getPercentage();
            invalidateOngoingNotification();
        }
    }

    // Build sync plan

    @Subscribe
    public void onEvent(BuildSyncPlanStartedEvent e) {
        if (handleOngoingEvents) {
            ongoing.progressPercent = 0;
            ongoing.subtitle = context.getString(R.string.status_building_sync_plan);
            invalidateOngoingNotification();
        }
    }

    @Subscribe
    public void onEvent(BuildSyncPlanProgressChangedEvent e) {
        if (handleOngoingEvents) {
            ongoing.progressPercent = (int) e.getPercentage();
            invalidateOngoingNotification();
        }
    }

    // Delete files

    @Subscribe
    public void onEvent(DeleteFilesStartedEvent e) {
        if (handleOngoingEvents) {
            ongoing.progressPercent = 0;
            ongoing.subtitle = context.getString(R.string.status_deleting_items);
            invalidateOngoingNotification();
        }
    }

    // Download song

    @Subscribe
    public void onEvent(DownloadSongStartedEvent e) {
        if (handleOngoingEvents) {
            ongoing.progressPercent = 0;
            ongoing.subtitle = context.getString(R.string.status_downloading_song,
                    e.getSong().getArtistName(),
                    e.getSong().getName());
            invalidateOngoingNotification();
        }
    }

    @Subscribe
    public void onEvent(DownloadSongProgressChangedEvent e) {
        if (handleOngoingEvents) {
            ongoing.progressPercent = (int) e.getPercentage();
            ongoing.subtitle = context.getString(R.string.status_downloading_song,
                    e.getSong().getArtistName(),
                    e.getSong().getName());
            invalidateOngoingNotification();
        }
    }

    // Generate playlist

    @Subscribe
    public void onEvent(GeneratePlaylistsStartedEvent e) {
        if (handleOngoingEvents) {
            ongoing.subtitle = context.getString(R.string.status_generating_playlists);
            ongoing.progressPercent = 0;
            invalidateOngoingNotification();
        }
    }

    @Subscribe
    public void onEvent(GeneratePlaylistsProgressChangedEvent e) {
        if (handleOngoingEvents) {
            ongoing.progressPercent = (int) e.getPercentage();
            invalidateOngoingNotification();
        }
    }

    // Outcome

    @Subscribe
    public void onEvent(SyncProcessFinishedEvent e) {

        stopHandlingOngoingEvents();

        Outcome outcome = e.getOutcome();
        Resources res = context.getResources();
        finished.viewPendingIntent = viewPendingIntent;
        finished.dismissOnClick = true;
        finished.ticker = context.getString(R.string.notif_finished_success_ticker);
        if (outcome.getDownloaded() > 0 && outcome.getDeleted() > 0) {
            finished.subtitle = res.getQuantityString(R.plurals.notif_finished_success_subtitle_added_and_removed,
                    outcome.getDownloaded(),
                    outcome.getDownloaded(),
                    outcome.getDeleted());
        } else if (outcome.getDownloaded() > 0) {
            finished.subtitle = res.getQuantityString(R.plurals.notif_finished_success_subtitle_added,
                    outcome.getDownloaded(),
                    outcome.getDownloaded());
        } else if (outcome.getDeleted() > 0) {
            finished.subtitle = res.getQuantityString(R.plurals.notif_finished_success_subtitle_removed,
                    outcome.getDeleted(),
                    outcome.getDeleted());
        } else {
            finished.subtitle = context.getString(R.string.notif_finished_success_subtitle_unchanged);
        }
        finished.title = context.getString(R.string.notif_finished_success_title);
        finished.showProgress = false;
        finished.hasCancelButton = false;
        finished.ongoing = false;
        finished.iconId = android.R.drawable.stat_notify_sync_noanim;
        invalidateFinishedNotification();
    }

    @Subscribe
    public void onEvent(SyncProcessFinishedWithErrorEvent event) {

        stopHandlingOngoingEvents();

        Throwable t = getFirstNonExecutionException(event.getException());
        if ((t instanceof InterruptedException) || (t instanceof CancellationException)) {
            // Finish silently
        } else {
            finished.viewPendingIntent = viewPendingIntent;
            finished.dismissOnClick = true;
            finished.ticker = context.getString(R.string.notif_finished_failure_ticker);
            if (t instanceof IOException) {
                finished.subtitle = context.getString(R.string.notif_finished_failure_subtitle_noconnection);
            } else if (t instanceof InvalidCredentialsException) {
                finished.subtitle = context.getString(R.string.notif_finished_failure_subtitle_invalidcredentials);
            } else if (t instanceof RateLimitedException || t instanceof ServerErrorException) {
                finished.subtitle = context.getString(R.string.notif_finished_failure_subtitle_serverissue);
            } else {
                finished.subtitle = context.getString(R.string.notif_finished_failure_subtitle_unknown);
            }
            finished.title = context.getString(R.string.notif_finished_failure_title);
            finished.showProgress = false;
            finished.hasCancelButton = false;
            finished.ongoing = false;
            finished.iconId = R.drawable.stat_notify_sync_error;
            invalidateFinishedNotification();
        }
    }

    private void startObservingEvents(EventBus syncEventBus) {
        Log.d("BaskingNotificationManager", "Registering on bus: " + this.toString());
        this.handleOngoingEvents = true;
        syncEventBus.register(this);
    }

    private void stopHandlingOngoingEvents() {
        Log.d("BaskingNotificationManager", "Unregistering from bus: " + this.toString());
        if (syncServiceEventBus != null) {
            // We may receive events for a short period after calling
            // unregister() as an async event bus is being used. To work around,
            // set a flag and check it before handling events.
            if (handleOngoingEvents) {
                this.handleOngoingEvents = false;
                syncServiceEventBus.unregister(this);
            }
        }
    }

    /**
     * Returns the first non-ExecutionException in the given throwable's causal chain. In the case
     * that there is no non-ExecutionException to return (i.e. the root exception is an execution
     * exception), the root exception will be returned.
     */
    private static Throwable getFirstNonExecutionException(Throwable t) {
        Throwable cause = t.getCause();
        if (t == cause) {
            return t;
        } else if (t instanceof ExecutionException) {
            return getFirstNonExecutionException(cause);
        } else {
            return t;
        }
    }

    private static class NotificationValueHolder {

        public NotificationValueHolder(int id) {
            this.id = id;
        }

        final int id;
        int iconId;
        PendingIntent viewPendingIntent;
        PendingIntent cancelPendingIntent;
        String title;
        String subtitle;
        String ticker;
        boolean showProgress;
        int progressPercent;
        boolean hasCancelButton;
        boolean dismissOnClick;
        boolean ongoing;
    }
}
