package uk.org.stnickschurch.stnicksapp;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.common.base.Optional;

import io.reactivex.functions.Consumer;
import uk.org.stnickschurch.stnicksapp.core.Notifications;
import uk.org.stnickschurch.stnicksapp.core.Player;
import uk.org.stnickschurch.stnicksapp.core.Sermon;
import uk.org.stnickschurch.stnicksapp.core.Store;
import uk.org.stnickschurch.stnicksapp.core.Utility;

public class PlayBroadcastReceiver extends BroadcastReceiver {
    public static final String EXTRA_SERMON_ID = "sermon_id";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

    @Override
    public void onReceive(final Context context, Intent intent) {
        Notifications.cancel(context, intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0));

        final String sermonId = intent.getStringExtra(EXTRA_SERMON_ID);
        Store.SINGLETON.get(context)
                .getSermon(sermonId)
                .subscribe(new Consumer<Optional<Sermon>>() {
                    @Override
                    public void accept(Optional<Sermon> sermon) {
                        if (sermon.isPresent()) {
                            Player.SINGLETON.get(context).play(sermon.get());
                        } else {
                            Utility.log("Warning! Failed to play sermon %s", sermonId);
                        }
                    }
                });
    }

    public static PendingIntent createIntent(Context context, String sermonId, int notificationId) {
        return PendingIntent.getBroadcast(context, notificationId,
                new Intent(context, PlayBroadcastReceiver.class)
                        .putExtra(EXTRA_SERMON_ID, sermonId)
                        .putExtra(EXTRA_NOTIFICATION_ID, notificationId),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
