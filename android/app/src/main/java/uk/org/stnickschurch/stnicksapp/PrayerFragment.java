package uk.org.stnickschurch.stnicksapp;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import uk.org.stnickschurch.stnicksapp.core.Events;

public class PrayerFragment extends Fragment {
    public PrayerFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_prayer, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh_prayers:
                View view = getView();
                if (view != null) {
                    ((WebView) view.findViewById(R.id.webview_prayer)).reload();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_prayer, container, false);
        WebView content = root.findViewById(R.id.webview_prayer);
        content.loadUrl(getString(R.string.prayers));
        content.setWebViewClient(new WebViewClient() {
            boolean mFirstLoad = true;
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!mFirstLoad) {
                    Events.SINGLETON.get(getContext()).publishMessage(R.string.message_prayers_refreshed);
                }
                mFirstLoad = false;
            }
        });
        return root;
    }
}
