package uk.org.stnickschurch.stnicksapp.core;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.google.common.io.Files;

import java.io.File;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import uk.org.stnickschurch.stnicksapp.PlaybackService;
import uk.org.stnickschurch.stnicksapp.PlayerActivity;
import uk.org.stnickschurch.stnicksapp.R;
import uk.org.stnickschurch.stnicksapp.data.DataView;
import uk.org.stnickschurch.stnicksapp.data.Sermon;

/**
 * Creates & manages notifications to show to the user.
 * Also manages interaction with the DownloadManager for sermons.
 */
public class Notifications {
    private final Context mContext;
    private Notifications(Context context) {
        mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    final long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
                    Schedulers.io().scheduleDirect(new Runnable() {
                        @Override
                        public void run() {
                            finishDownload(downloadId);
                        }
                    });
                }
            }
        }, filter);
    }

    public static int notificationId(long sermon) {
        // Guard against zero ID (which cannot be used as a notification ID)
        if (sermon <= 0) {
            return Integer.MAX_VALUE + (int) sermon;
        } else {
            return (int) sermon;
        }
    }

    private NotificationManager notificationManager() {
        return (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }
    private DownloadManager downloadManager() {
        return (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
    }
    private @Nullable String downloadChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String id = "download";
            notificationManager().createNotificationChannel(
                    new NotificationChannel(id,
                            mContext.getString(R.string.notification_channel_download_name),
                            NotificationManager.IMPORTANCE_DEFAULT));
            return id;
        } else {
            return null;
        }
    }
    private @Nullable String playbackChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String id = "playback";
            NotificationChannel channel = new NotificationChannel(id,
                    mContext.getString(R.string.notification_channel_playback_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.enableLights(false);
            channel.enableVibration(false);
            notificationManager().createNotificationChannel(channel);
            return id;
        } else {
            return null;
        }
    }

    /**
     * Begin a background (re)download of the sermon.
     */
    public void download(Sermon sermon) {
        Store.SINGLETON.get(mContext)
                .getAudio(sermon.id, true)
                .subscribe(new Consumer<Uri>() {
                    @Override
                    public void accept(Uri audio) {
                        doDownload(sermon, audio);
                    }
                });
    }
    private void doDownload(Sermon sermon, Uri audio) {
        File local = new File(mContext.getExternalFilesDir("sermons"),
                sermon.title + "." + Files.getFileExtension(audio.getPath()));
        if (local.exists()) {
            local.delete();
        }
        DownloadManager.Request request = new DownloadManager.Request(audio)
                .setDestinationUri(Uri.fromFile(local))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setTitle(DataView.uiTitle(sermon))
                .setDescription(DataView.longDescription(sermon, ", "));
        Store.SINGLETON.get(mContext).startDownload(sermon.id, downloadManager().enqueue(request));
    }
    private void finishDownload(long downloadId) {
        Cursor cursor = downloadManager().query(new DownloadManager.Query().setFilterById(downloadId));
        if (!cursor.moveToFirst()) {
            Utility.log("Error! couldn't find downloaded file");
            return;
        }
        int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
        if (DownloadManager.STATUS_SUCCESSFUL == status) {
            String localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
            File localPath = new File(Uri.parse(localUri).getPath());
            Store.SINGLETON.get(mContext)
                    .finishDownload(downloadId, localPath)
                    .subscribe(new SingleObserver<Sermon>() {
                        @Override
                        public void onSubscribe(Disposable d) { }

                        @Override
                        public void onSuccess(Sermon sermon) {
                            notifyDownloadComplete(sermon);
                        }

                        @Override
                        public void onError(Throwable e) {
                            // Something else has happened, and the download doesn't match
                            // - we don't need the "orphaned" file anymore
                            Utility.log("Warning! couldn't find sermon for downloaded file %s", localPath);
                            localPath.delete();
                        }
                    });
        } else {
            Store.SINGLETON.get(mContext).cancelDownload(downloadId); // Try to clean up
            if (status != DownloadManager.STATUS_FAILED) {
                Utility.nonFatal(new IllegalStateException("Unexpected ACTION_DOWNLOAD_COMPLETE with status: " + status));
            }
            Events.SINGLETON.get(mContext).publish(Events.Level.ERROR, R.string.error_failed_download);
        }
    }
    private void notifyDownloadComplete(Sermon sermon) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, downloadChannel())
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(DataView.uiTitle(sermon))
                .setContentText(DataView.longDescription(sermon, ", "))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(DataView.longDescription(sermon, "\n")))
                .setAutoCancel(true)
                .setContentIntent(PlaybackService.Client.SINGLETON.get(mContext)
                        .pendingIntent(PlaybackService.ACTION_PLAY, sermon.id));
        notificationManager().notify(notificationId(sermon.id), builder.build());
    }

    public Single<Notification> getPlayback(long sermon, boolean isPlaying) {
        return Store.SINGLETON.get(mContext)
                .getSermon(sermon)
                .map(new Function<Sermon, Notification>() {
                    @Override
                    public Notification apply(Sermon sermon) {
                        return doGetPlayback(sermon, isPlaying);
                    }
                });
    }
    private Notification doGetPlayback(Sermon sermon, boolean isPlaying) {
        String title = DataView.uiTitle(sermon);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, playbackChannel())
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1));

        String contentText = sermon.title.getSnippetOrText();
        if (!title.equals(contentText)) {
            builder.setContentText(contentText);
        }

        // Add various click actions
        builder.setContentIntent(PendingIntent.getActivity(mContext, 0,
                new Intent().setClass(mContext, PlayerActivity.class),
                0));
        PlaybackService.Client client = PlaybackService.Client.SINGLETON.get(mContext);
        if (!isPlaying) {
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_play, mContext.getString(R.string.menu_play_title),
                    client.pendingIntent(PlaybackService.ACTION_PLAY, sermon.id)));
        }
        if (isPlaying) {
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_pause, mContext.getString(R.string.menu_pause_title),
                    client.pendingIntent(PlaybackService.ACTION_PAUSE, sermon.id)));
        }
        builder.addAction(new NotificationCompat.Action(
                R.drawable.ic_stop, mContext.getString(R.string.menu_stop_title),
                client.pendingIntent(PlaybackService.ACTION_STOP, sermon.id)));

        return builder.build();
    }

    public static final Singleton<Notifications> SINGLETON = new Singleton<Notifications>() {
        @Override
        protected Notifications newInstance(Context context) {
            return new Notifications(context);
        }
    };
}
