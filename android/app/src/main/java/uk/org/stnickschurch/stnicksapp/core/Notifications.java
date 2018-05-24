package uk.org.stnickschurch.stnicksapp.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import uk.org.stnickschurch.stnicksapp.PlayBroadcastReceiver;
import uk.org.stnickschurch.stnicksapp.R;

public class Notifications {
    public static String downloadChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String id = "download";
            notificationManager(context).createNotificationChannel(
                    new NotificationChannel(id, "Download", NotificationManager.IMPORTANCE_DEFAULT));
            return id;
        } else {
            return null;
        }
    }

    public static void notifyDownloadComplete(Context context, Sermon sermon) {
        Utility.log("Notifying %s", sermon.userTitle());
        int notificationId = sermon.hashCode();
        Notification notification = new NotificationCompat.Builder(context, downloadChannel(context))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(sermon.userTitle())
                .setContentText(sermon.userDescription())
                .setContentIntent(PlayBroadcastReceiver.createIntent(context, sermon.id, notificationId))
                .build();
        notificationManager(context).notify(sermon.id.hashCode(), notification);
    }

    public static void cancel(Context context, int notificationId) {
        notificationManager(context).cancel(notificationId);
    }

    private static NotificationManager notificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
