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

	private int id;
	private int iconId;
	private PendingIntent viewPendingIntent;
	private PendingIntent cancelPendingIntent;
	private String title;
	private String subtitle;
	private String ticker;
	private boolean showProgress;
	private int progressPercent;
	private boolean canCancel;
	private boolean ongoing;

	public BaskingNotificationManager(Context context) {
		this.context = context;
		this.notMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		this.id = NOTIFICATION_ID_ONGOING;
		this.iconId = R.drawable.ic_menu_refresh;
		this.viewPendingIntent = PendingIntent.getActivity(context, 0, new Intent(context,
				MainActivity.class), Intent.FLAG_ACTIVITY_NEW_TASK);
		this.cancelPendingIntent = PendingIntent.getService(context, 0,
				BaskingSyncService.newStopIntent(context), 0);

		this.title = context.getString(R.string.notif_ongoing_title);
		this.subtitle = null;
		this.ticker = title;
		this.showProgress = true;
		this.progressPercent = 0;
		this.canCancel = true;
		this.ongoing = true;
	}

	void setSyncFuture(ListenableFuture<Outcome> syncFuture) {
		Futures.addCallback(syncFuture, syncFutureCallback, MainThreadExecutorService.get());
	}

	Notification.Builder newNotification(Context context) {

		Notification.Builder builder = new Notification.Builder(context);
		builder.setSmallIcon(iconId);
		builder.setContentTitle(title);
		if (subtitle != null) {
			builder.setContentText(subtitle);
		}
		builder.setTicker(ticker);
		builder.setContentIntent(viewPendingIntent);
		if (canCancel) {
			builder.addAction(android.R.drawable.ic_menu_close_clear_cancel,
					context.getString(android.R.string.cancel), cancelPendingIntent);
		}
		if (showProgress) {
			builder.setProgress(100, progressPercent, false);
		}
		builder.setOngoing(ongoing);
		return builder;
	}

	void update() {
		notMan.notify(id, newNotification(context).build());
	}

	// Query API for use information

	@Subscribe
	public void onEvent(GetSongsToSyncEvent.Started e) {
		this.subtitle = context.getString(R.string.status_retrieving_profile);
		update();
	}

	@Subscribe
	public void onEvent(GetSongsToSyncEvent.ProgressChanged e) {
		this.progressPercent = (int) e.percentage;
		update();
	}

	// Build sync plan

	@Subscribe
	public void onEvent(BuildSyncPlanEvent.Started e) {
		this.progressPercent = 0;
		this.subtitle = context.getString(R.string.status_building_sync_plan);
		update();
	}

	@Subscribe
	public void onEvent(BuildSyncPlanEvent.ProgressChanged e) {
		this.progressPercent = (int) e.percentage;
		update();
	}

	// Delete files

	boolean startedDeletion;

	@Subscribe
	public void onEvent(DeleteFileEvent.Started e) {
		if (!startedDeletion) {
			startedDeletion = true;
			this.progressPercent = 0;
			this.subtitle = context.getString(R.string.status_deleting_items);
			update();
		}
	}

	// Download song

	boolean startedDownload;

	Map<Song, Float> downloadingSongs = Maps.newLinkedHashMap();

	@Subscribe
	public void onEvent(DownloadSongEvent.Started e) {
		this.progressPercent = 0;
		this.subtitle = context.getString(R.string.status_downloading_song, e.task.song.artistName,
				e.task.song.name);
		update();
	}

	@Subscribe
	public void onEvent(DownloadSongEvent.ProgressChanged e) {
		this.progressPercent = (int) e.percentage;
		this.subtitle = context.getString(R.string.status_downloading_song, e.task.song.artistName,
				e.task.song.name);
		update();
	}

	// Generate playlist

	@Subscribe
	public void onEvent(GeneratePlaylistsEvent.Started e) {
		this.subtitle = context.getString(R.string.status_generating_playlists);
		this.progressPercent = 0;
		update();
	}

	@Subscribe
	public void onEvent(GeneratePlaylistsEvent.ProgressChanged e) {
		this.progressPercent = (int) e.percentage;
		update();
	}

	// Outcome
	
	// TODO separate finished notification vars

	private final FutureCallback<Outcome> syncFutureCallback = new FutureCallback<SyncTask.Outcome>() {

		@Override
		public void onSuccess(Outcome arg0) {
			BaskingNotificationManager.this.id = NOTIFICATION_ID_FINISHED;
			BaskingNotificationManager.this.ticker = context
					.getString(R.string.notif_finished_success_ticker);
			String subtitle;
			if ((arg0.downloaded + arg0.deleted) > 0) {
				subtitle = context.getString(R.string.notif_finished_success_subtitle_changes,
						arg0.downloaded + arg0.deleted);
			} else {
				subtitle = context.getString(R.string.notif_finished_success_subtitle_no_changes);
			}
			BaskingNotificationManager.this.subtitle = subtitle;
			BaskingNotificationManager.this.title = context
					.getString(R.string.notif_finished_success_title);
			BaskingNotificationManager.this.showProgress = false;
			BaskingNotificationManager.this.canCancel = false;
			BaskingNotificationManager.this.ongoing = false;
			update();
		}

		@Override
		public void onFailure(Throwable arg0) {
			BaskingNotificationManager.this.ticker = context
					.getString(R.string.notif_finished_failure_ticker);
			String subtitle;
			arg0 = arg0.getCause();
			if (arg0 instanceof IOException) {
				subtitle = context.getString(R.string.notif_finished_failure_subtitle_no_network);
			} else {
				subtitle = context.getString(R.string.notif_finished_failure_subtitle_unknown);
			}
			BaskingNotificationManager.this.subtitle = subtitle;
			BaskingNotificationManager.this.title = context
					.getString(R.string.notif_finished_failure_title);
			BaskingNotificationManager.this.id = NOTIFICATION_ID_FINISHED;
			BaskingNotificationManager.this.showProgress = false;
			BaskingNotificationManager.this.canCancel = false;
			BaskingNotificationManager.this.ongoing = false;
			update();
		}
	};
}
