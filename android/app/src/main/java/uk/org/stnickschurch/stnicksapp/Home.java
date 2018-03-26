package uk.org.stnickschurch.stnicksapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import io.reactivex.functions.Consumer;
import uk.org.stnickschurch.stnicksapp.core.Downloader;
import uk.org.stnickschurch.stnicksapp.core.Sermon;

public class Home extends BaseActivity {
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

        disposeOnDestroy(Downloader.get(this).playing.subscribe(new Consumer<Sermon>() {
            @Override
            public void accept(Sermon sermon) throws Exception {
                startActivity(new Intent().setClass(Home.this, Player.class));
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
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // TODO: go to settings
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class CalendarFragment extends Fragment {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_home, container, false);
            ((TextView) rootView.findViewById(R.id.section_label)).setText(R.string.section_calendar);
            return rootView;
        }
    }

    public static class PrayerFragment extends Fragment {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_home, container, false);
            ((TextView) rootView.findViewById(R.id.section_label)).setText(R.string.section_prayer);
            return rootView;
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
                    return new MediaFragment();
                case 1:
                    return new CalendarFragment();
                case 2:
                    return new PrayerFragment();
            }
            // TODO: raise nonfatal
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.section_media);
                case 1:
                    return getString(R.string.section_calendar);
                case 2:
                    return getString(R.string.section_prayer);
            }
            return null;
        }
    }
}
