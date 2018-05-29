package uk.org.stnickschurch.stnicksapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import java.util.Random;

import uk.org.stnickschurch.stnicksapp.core.Store;
import uk.org.stnickschurch.stnicksapp.core.Utility;

public class SyncBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Store.SINGLETON.get(context).sync();
    }

    public static void schedule(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long interval = Utility.getPeriodMs(context.getString(R.string.background_sync));
        long start = SystemClock.elapsedRealtime() + new Random().nextInt((int) interval);
        PendingIntent intent = PendingIntent.getBroadcast(
                context, 0, new Intent(context, SyncBroadcastReceiver.class), 0);

        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("sync_enabled", true)) {
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, start, interval, intent);
        } else {
            alarmManager.cancel(intent);
        }
    }
}
