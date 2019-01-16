package uk.org.stnickschurch.stnicksapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import org.joda.time.Instant;

import uk.org.stnickschurch.stnicksapp.android.AppCompatPreferenceActivity;

public class SettingsActivity extends AppCompatPreferenceActivity {
    public static class SettingsFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            refreshAutoSyncSummary(getPreferenceScreen().getSharedPreferences());
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("sync_enabled")) {
                SyncBroadcastReceiver.schedule(getActivity());
            }
            if (key.equals("sync_enabled") || key.equals("last_auto_sync")) {
                refreshAutoSyncSummary(sharedPreferences);
            }
        }

        private void refreshAutoSyncSummary(SharedPreferences sharedPreferences) {
            if (sharedPreferences.getBoolean("sync_enabled", true)) {
                long lastAutoSync = sharedPreferences.getLong("last_auto_sync", -1);
                String lastAutoSyncText = lastAutoSync < 0 ? getString(R.string.never)
                    : DateUtils.getRelativeTimeSpanString(
                            lastAutoSync,
                            Instant.now().getMillis(),
                            DateUtils.MINUTE_IN_MILLIS).toString();
                findPreference("sync_enabled")
                        .setSummary(getString(R.string.sync_enabled_summary_on, lastAutoSyncText));
            } else {
                findPreference("sync_enabled")
                        .setSummary(getString(R.string.sync_enabled_summary));
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
        // We had to set the window background to work around a white status bar Android issue
        // so must set the content background back to white here
        getListView().setBackgroundColor(Color.WHITE);
    }
}
