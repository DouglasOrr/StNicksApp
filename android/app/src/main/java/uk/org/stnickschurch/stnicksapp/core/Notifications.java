package uk.org.stnickschurch.stnicksapp.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import uk.org.stnickschurch.stnicksapp.PlaybackService;
import uk.org.stnickschurch.stnicksapp.PlayerActivity;
import uk.org.stnickschurch.stnicksapp.R;

/**
 * Creates & manages notifications to show to the user.
 */
public class Notifications {
    private final Context mContext;
    private Notifications(Context context) {
        mContext = context;
    }
    private NotificationManager notificationManager() {
        return (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
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
     * We use a single ID for every notification for a given sermon, so that you never have multiple
     * notifications for the same sermon.
     */
    public static int notificationId(String sermonId) {
        return sermonId.hashCode();
    }

    public void notifyDownloadComplete(Sermon sermon) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, downloadChannel())
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(sermon.userTitle())
                .setContentText(sermon.userDescription(", "))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(sermon.userDescription("\n")))
                .setAutoCancel(true)
                .setContentIntent(PlaybackService.Client.SINGLETON.get(mContext)
                        .pendingIntent(PlaybackService.ACTION_PLAY, sermon.id));
        notificationManager().notify(notificationId(sermon.id), builder.build());
    }

    public Notification getPlayback(Sermon sermon, boolean isPlaying) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, playbackChannel())
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(sermon.userTitle())
                .setContentText(sermon.title)
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1));

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
