package co.arcs.groove.basking;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;

import com.beust.jcommander.internal.Maps;
import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CancellationException;

import co.arcs.groove.basking.event.impl.BuildSyncPlanEvent;
import co.arcs.groove.basking.event.impl.DeleteFileEvent;
import co.arcs.groove.basking.event.impl.DownloadSongEvent;
import co.arcs.groove.basking.event.impl.GeneratePlaylistsEvent;
import co.arcs.groove.basking.event.impl.GetSongsToSyncEvent;
import co.arcs.groove.basking.event.impl.SyncEvent;
import co.arcs.groove.basking.task.SyncTask.Outcome;
import co.arcs.groove.thresher.GroovesharkException.InvalidCredentialsException;
import co.arcs.groove.thresher.GroovesharkException.RateLimitedException;
import co.arcs.groove.thresher.GroovesharkException.ServerErrorException;
import co.arcs.groove.thresher.Song;

public class BaskingNotificationManager {

    static final int NOTIFICATION_ID_ONGOING = 1;
    static final int NOTIFICATION_ID_FINISHED = 2;

    private final NotificationValueHolder ongoing = new NotificationValueHolder(
            NOTIFICATION_ID_ONGOING);
    private final NotificationValueHolder finished = new NotificationValueHolder(
            NOTIFICATION_ID_FINISHED);

    private final Context context;
    private final NotificationManager notMan;

    private final PendingIntent viewPendingIntent;
    private final PendingIntent cancelPendingIntent;

    private EventBus syncServiceEventBus;
    private boolean observeEvents;

    public BaskingNotificationManager(Context context) {
        this.context = context;
        this.notMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.viewPendingIntent = PendingIntent.getActivity(context,
                0,
                new Intent(context, MainActivity.class),
                Intent.FLAG_ACTIVITY_NEW_TASK);
        this.cancelPendingIntent = PendingIntent.getService(context,
                0,
                BaskingSyncService.newStopIntent(context),
                0);

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
     * <p>
     * Note: Any previously supplied sync future must have finished before
     * calling this method with a new future.
     * </p>
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
     * Generate a new 'ongoing' notification for use by the
     * {@link BaskingSyncService} when going into the foreground state.
     */
    public Notification.Builder newOngoingNotification(Context context) {
        return newNotification(context, ongoing);
    }

    private Notification.Builder newNotification(Context context, NotificationValueHolder holder) {

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
        return builder;
    }

    void updateOngoing() {
        notMan.notify(ongoing.id, newNotification(context, ongoing).build());
    }

    void updateFinished() {
        notMan.notify(finished.id, newNotification(context, finished).build());
    }

    // Query API for use information

    @Subscribe
    public void onEvent(GetSongsToSyncEvent.Started e) {
        if (!observeEvents) {
            return;
        }
        log(e);
        ongoing.subtitle = context.getString(R.string.status_retrieving_profile);
        updateOngoing();
    }

    @Subscribe
    public void onEvent(GetSongsToSyncEvent.ProgressChanged e) {
        if (!observeEvents) {
            return;
        }
        log(e);
        ongoing.progressPercent = (int) e.percentage;
        updateOngoing();
    }

    // Build sync plan

    @Subscribe
    public void onEvent(BuildSyncPlanEvent.Started e) {
        if (!observeEvents) {
            return;
        }
        log(e);
        ongoing.progressPercent = 0;
        ongoing.subtitle = context.getString(R.string.status_building_sync_plan);
        updateOngoing();
    }

    @Subscribe
    public void onEvent(BuildSyncPlanEvent.ProgressChanged e) {
        if (!observeEvents) {
            return;
        }
        log(e);
        ongoing.progressPercent = (int) e.percentage;
        updateOngoing();
    }

    // Delete files

    boolean startedDeletion;

    @Subscribe
    public void onEvent(DeleteFileEvent.Started e) {
        if (!observeEvents) {
            return;
        }
        log(e);
        if (!startedDeletion) {
            startedDeletion = true;
            ongoing.progressPercent = 0;
            ongoing.subtitle = context.getString(R.string.status_deleting_items);
            updateOngoing();
        }
    }

    // Download song

    boolean startedDownload;

    Map<Song, Float> downloadingSongs = Maps.newLinkedHashMap();

    @Subscribe
    public void onEvent(DownloadSongEvent.Started e) {
        if (!observeEvents) {
            return;
        }
        log(e);
        ongoing.progressPercent = 0;
        ongoing.subtitle = context.getString(R.string.status_downloading_song,
                e.task.song.getArtistName(),
                e.task.song.getName());
        updateOngoing();
    }

    @Subscribe
    public void onEvent(DownloadSongEvent.ProgressChanged e) {
        if (!observeEvents) {
            return;
        }
        log(e);
        ongoing.progressPercent = (int) e.percentage;
        ongoing.subtitle = context.getString(R.string.status_downloading_song,
                e.task.song.getArtistName(),
                e.task.song.getName());
        updateOngoing();
    }

    // Generate playlist

    @Subscribe
    public void onEvent(GeneratePlaylistsEvent.Started e) {
        if (!observeEvents) {
            return;
        }
        log(e);
        ongoing.subtitle = context.getString(R.string.status_generating_playlists);
        ongoing.progressPercent = 0;
        updateOngoing();
    }

    @Subscribe
    public void onEvent(GeneratePlaylistsEvent.ProgressChanged e) {
        if (!observeEvents) {
            return;
        }
        log(e);
        ongoing.progressPercent = (int) e.percentage;
        updateOngoing();
    }

    // Outcome

    @Subscribe
    public void onEvent(SyncEvent.Finished e) {
        if (!observeEvents) {
            return;
        }
        log(e);

        Outcome outcome = e.outcome;

        stopObservingEvents();

        Resources res = context.getResources();
        finished.viewPendingIntent = viewPendingIntent;
        finished.dismissOnClick = true;
        finished.ticker = context.getString(R.string.notif_finished_success_ticker);
        if (outcome.downloaded > 0 && outcome.deleted > 0) {
            finished.subtitle = res.getQuantityString(R.plurals.notif_finished_success_subtitle_added_and_removed,
                    outcome.downloaded,
                    outcome.downloaded,
                    outcome.deleted);
        } else if (outcome.downloaded > 0) {
            finished.subtitle = res.getQuantityString(R.plurals.notif_finished_success_subtitle_added,
                    outcome.downloaded,
                    outcome.downloaded);
        } else if (outcome.deleted > 0) {
            finished.subtitle = res.getQuantityString(R.plurals.notif_finished_success_subtitle_removed,
                    outcome.deleted,
                    outcome.deleted);
        } else {
            finished.subtitle = context.getString(R.string.notif_finished_success_subtitle_unchanged);
        }
        finished.title = context.getString(R.string.notif_finished_success_title);
        finished.showProgress = false;
        finished.hasCancelButton = false;
        finished.ongoing = false;
        finished.iconId = android.R.drawable.stat_notify_sync_noanim;
        updateFinished();
    }

    @Subscribe
    public void onEvent(SyncEvent.FinishedWithError e) {
        if (!observeEvents) {
            return;
        }
        log(e);

        Throwable throwable = Throwables.getRootCause(e.e);

        stopObservingEvents();

        if ((throwable instanceof InterruptedException) || (throwable instanceof CancellationException)) {
            // Finish silently
        } else {
            finished.viewPendingIntent = viewPendingIntent;
            finished.dismissOnClick = true;
            finished.ticker = context.getString(R.string.notif_finished_failure_ticker);
            if (throwable instanceof IOException) {
                finished.subtitle = context.getString(R.string.notif_finished_failure_subtitle_noconnection);
            } else if (throwable instanceof InvalidCredentialsException) {
                finished.subtitle = context.getString(R.string.notif_finished_failure_subtitle_invalidcredentials);
            } else if (throwable instanceof RateLimitedException || throwable instanceof ServerErrorException) {
                finished.subtitle = context.getString(R.string.notif_finished_failure_subtitle_serverissue);
            } else {
                finished.subtitle = context.getString(R.string.notif_finished_failure_subtitle_unknown);
            }
            finished.title = context.getString(R.string.notif_finished_failure_title);
            finished.showProgress = false;
            finished.hasCancelButton = false;
            finished.ongoing = false;
            finished.iconId = R.drawable.stat_notify_sync_error;
            updateFinished();
        }
    }

    private void startObservingEvents(EventBus syncEventBus) {
        Log.d("BaskingNotificationManager", "Registering on bus: " + this.toString());
        this.observeEvents = true;
        syncEventBus.register(this);
    }

    private void stopObservingEvents() {
        Log.d("BaskingNotificationManager", "Unregistering from bus: " + this.toString());
        if (syncServiceEventBus != null) {
            // We may receive events for a short period after calling
            // unregister() as an async event bus is being used. To work around,
            // set a flag and check it before handling events.
            if (observeEvents) {
                this.observeEvents = false;
                syncServiceEventBus.unregister(this);
            }
        }
    }

    private void log(Object e) {
        Log.d("BaskingNotificationManager", e.toString());
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
