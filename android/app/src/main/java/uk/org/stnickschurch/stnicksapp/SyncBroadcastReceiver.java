package uk.org.stnickschurch.stnicksapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
        syncOnFirstRun(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent intent = PendingIntent.getBroadcast(context, 0,
                new Intent(context, SyncBroadcastReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("sync_enabled", true)) {
            long interval = Utility.getPeriodMs(context.getString(R.string.background_sync));
            long start = SystemClock.elapsedRealtime() + new Random().nextInt((int) interval);
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, start, interval, intent);
        } else {
            alarmManager.cancel(intent);
        }
    }

    private static void syncOnFirstRun(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getBoolean("first_run", true)) {
            preferences.edit().putBoolean("first_run", false).apply();
            Store.SINGLETON.get(context).sync();
        }
    }
}
