package uk.org.stnickschurch.stnicksapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import butterknife.BindView;
import io.reactivex.functions.Consumer;

public class HomeActivity extends BaseActivity {
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.container) ViewPager mViewPager;
    @BindView(R.id.tabs) TabLayout mTabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_home);
        super.onCreate(savedInstanceState);
        setSupportActionBar(mToolbar);
        mViewPager.setAdapter(new SectionsPagerAdapter(getSupportFragmentManager()));
        mTabLayout.setupWithViewPager(mViewPager);
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
            case R.id.menu_settings:
                startActivity(new Intent().setClass(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class PrayerFragment extends WebViewFragment {
        @Override
        protected int refreshMessage() {
            return R.string.message_prayers_refreshed;
        }
        @Override
        protected int url() {
            return R.string.prayers;
        }
    }

    public static class CalendarFragment extends WebViewFragment {
        @Override
        protected int refreshMessage() {
            return R.string.message_calendar_refreshed;
        }
        @Override
        protected int url() {
            return R.string.calendar;
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case 0:
                    return new SermonListFragment();
                case 1:
                    return new CalendarFragment();
                case 2:
                    return new PrayerFragment();
            }
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.section_sermons);
                case 1:
                    return getString(R.string.section_calendar);
                case 2:
                    return getString(R.string.section_prayer);
            }
            return null;
        }
    }
}
