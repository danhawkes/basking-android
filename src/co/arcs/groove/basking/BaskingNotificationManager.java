package co.arcs.groove.basking;

import java.io.IOException;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import co.arcs.android.util.MainThreadExecutorService;
import co.arcs.groove.basking.event.impl.BuildSyncPlanEvent;
import co.arcs.groove.basking.event.impl.DeleteFileEvent;
import co.arcs.groove.basking.event.impl.DownloadSongEvent;
import co.arcs.groove.basking.event.impl.GeneratePlaylistsEvent;
import co.arcs.groove.basking.event.impl.GetSongsToSyncEvent;
import co.arcs.groove.basking.task.SyncTask;
import co.arcs.groove.basking.task.SyncTask.Outcome;
import co.arcs.groove.thresher.Song;

import com.beust.jcommander.internal.Maps;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class BaskingNotificationManager {

	static final int NOTIFICATION_ID_ONGOING = 1;
	static final int NOTIFICATION_ID_FINISHED = 2;

	private final Context context;
	private final NotificationManager notMan;

	private final NotificationValueHolder ongoing = new NotificationValueHolder(
			NOTIFICATION_ID_ONGOING);
	private final NotificationValueHolder finished = new NotificationValueHolder(
			NOTIFICATION_ID_FINISHED);

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
		boolean canCancel;
		boolean ongoing;
	}

	public BaskingNotificationManager(Context context) {
		this.context = context;
		this.notMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		ongoing.iconId = android.R.drawable.stat_notify_sync;
		ongoing.viewPendingIntent = PendingIntent.getActivity(context, 0, new Intent(context,
				MainActivity.class), Intent.FLAG_ACTIVITY_NEW_TASK);
		ongoing.cancelPendingIntent = PendingIntent.getService(context, 0,
				BaskingSyncService.newStopIntent(context), 0);

		ongoing.title = context.getString(R.string.notif_ongoing_title);
		ongoing.subtitle = null;
		ongoing.ticker = ongoing.title;
		ongoing.showProgress = true;
		ongoing.progressPercent = 0;
		ongoing.canCancel = true;
		ongoing.ongoing = true;
	}

	/**
	 * Sets the sync future to be observed by the notification manager.
	 */
	void setSyncFuture(ListenableFuture<Outcome> syncFuture) {
		Futures.addCallback(syncFuture, syncFutureCallback, MainThreadExecutorService.get());
	}

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
		builder.setContentIntent(holder.viewPendingIntent);
		if (holder.canCancel) {
			builder.addAction(android.R.drawable.ic_menu_close_clear_cancel,
					context.getString(android.R.string.cancel), holder.cancelPendingIntent);
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
		ongoing.subtitle = context.getString(R.string.status_retrieving_profile);
		updateOngoing();
	}

	@Subscribe
	public void onEvent(GetSongsToSyncEvent.ProgressChanged e) {
		ongoing.progressPercent = (int) e.percentage;
		updateOngoing();
	}

	// Build sync plan

	@Subscribe
	public void onEvent(BuildSyncPlanEvent.Started e) {
		ongoing.progressPercent = 0;
		ongoing.subtitle = context.getString(R.string.status_building_sync_plan);
		updateOngoing();
	}

	@Subscribe
	public void onEvent(BuildSyncPlanEvent.ProgressChanged e) {
		ongoing.progressPercent = (int) e.percentage;
		updateOngoing();
	}

	// Delete files

	boolean startedDeletion;

	@Subscribe
	public void onEvent(DeleteFileEvent.Started e) {
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
		ongoing.progressPercent = 0;
		ongoing.subtitle = context.getString(R.string.status_downloading_song,
				e.task.song.artistName, e.task.song.name);
		updateOngoing();
	}

	@Subscribe
	public void onEvent(DownloadSongEvent.ProgressChanged e) {
		ongoing.progressPercent = (int) e.percentage;
		ongoing.subtitle = context.getString(R.string.status_downloading_song,
				e.task.song.artistName, e.task.song.name);
		updateOngoing();
	}

	// Generate playlist

	@Subscribe
	public void onEvent(GeneratePlaylistsEvent.Started e) {
		ongoing.subtitle = context.getString(R.string.status_generating_playlists);
		ongoing.progressPercent = 0;
		updateOngoing();
	}

	@Subscribe
	public void onEvent(GeneratePlaylistsEvent.ProgressChanged e) {
		ongoing.progressPercent = (int) e.percentage;
		updateOngoing();
	}

	// Outcome

	private final FutureCallback<Outcome> syncFutureCallback = new FutureCallback<SyncTask.Outcome>() {

		@Override
		public void onSuccess(Outcome arg0) {
			finished.ticker = context.getString(R.string.notif_finished_success_ticker);
			String subtitle;
			if ((arg0.downloaded + arg0.deleted) > 0) {
				subtitle = context.getString(R.string.notif_finished_success_subtitle_changes,
						arg0.downloaded + arg0.deleted);
			} else {
				subtitle = context.getString(R.string.notif_finished_success_subtitle_no_changes);
			}
			finished.subtitle = subtitle;
			finished.title = context.getString(R.string.notif_finished_success_title);
			finished.showProgress = false;
			finished.canCancel = false;
			finished.ongoing = false;
			finished.iconId = android.R.drawable.stat_notify_sync_noanim;
			updateFinished();
		}

		@Override
		public void onFailure(Throwable arg0) {
			finished.ticker = context.getString(R.string.notif_finished_failure_ticker);
			String subtitle;
			arg0 = arg0.getCause();
			if (arg0 instanceof IOException) {
				subtitle = context.getString(R.string.notif_finished_failure_subtitle_no_network);
			} else {
				subtitle = context.getString(R.string.notif_finished_failure_subtitle_unknown);
			}
			finished.subtitle = subtitle;
			finished.title = context.getString(R.string.notif_finished_failure_title);
			finished.showProgress = false;
			finished.canCancel = false;
			finished.ongoing = false;
			finished.iconId = R.drawable.stat_notify_sync_error;
			updateFinished();
		}
	};
}
