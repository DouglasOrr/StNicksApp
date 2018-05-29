package uk.org.stnickschurch.stnicksapp.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import uk.org.stnickschurch.stnicksapp.PlayBroadcastReceiver;
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

    private String downloadChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String id = "download";
            notificationManager().createNotificationChannel(
                    new NotificationChannel(id, "Download", NotificationManager.IMPORTANCE_DEFAULT));
            return id;
        } else {
            return null;
        }
    }

    public void notifyDownloadComplete(Sermon sermon) {
        int notificationId = sermon.hashCode();
        Notification notification = new NotificationCompat.Builder(mContext, downloadChannel())
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(sermon.userTitle())
                .setContentText(sermon.userDescription(", "))
                .setContentIntent(PlayBroadcastReceiver.createIntent(mContext, sermon.id, notificationId))
                .build();
        notificationManager().notify(sermon.id.hashCode(), notification);
    }

    public void cancel(int notificationId) {
        notificationManager().cancel(notificationId);
    }

    public static final Singleton<Notifications> SINGLETON = new Singleton<Notifications>() {
        @Override
        Notifications newInstance(Context context) {
            return new Notifications(context);
        }
    };
}
