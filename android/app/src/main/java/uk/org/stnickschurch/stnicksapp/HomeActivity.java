package uk.org.stnickschurch.stnicksapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import io.reactivex.functions.Consumer;

public class HomeActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_home);
        super.onCreate(savedInstanceState);
        // don't show soft keyboard on startup
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_content, new SermonListFragment())
                    .commitNow();
        }
        SyncBroadcastReceiver.schedule(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // When the home screen is visible, playing a sermon should pop up the viewer activity
        // automatically
        disposeOnPause(PlaybackService.Client.SINGLETON.get(this)
                .events
                .skip(1)  // The "initial state" emitted by the BehaviorSubject
                .subscribe(new Consumer<PlaybackService.Event>() {
                    @Override
                    public void accept(PlaybackService.Event event) {
                        if (PlaybackService.ACTION_PLAY.equals(event.action)) {
                            startActivity(new Intent()
                                    .setClass(HomeActivity.this, PlayerActivity.class));
                        }
                    }
                }));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_calendar:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.calendar_link))));
                return true;
            case R.id.menu_settings:
                startActivity(new Intent().setClass(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
