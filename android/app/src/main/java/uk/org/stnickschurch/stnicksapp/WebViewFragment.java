package uk.org.stnickschurch.stnicksapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.ShareCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import uk.org.stnickschurch.stnicksapp.core.Events;

public abstract class WebViewFragment extends Fragment {
    /**
     * R.strings id for the message to show when the view is manually refreshed
     */
    protected abstract int refreshMessage();

    /**
     * R.strings id for the URL to load into this view
     */
    protected abstract int url();

    public WebViewFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_webview, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh_webview:
                View view = getView();
                if (view != null) {
                    ((WebView) view.findViewById(R.id.webview_content)).reload();
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
        View root = inflater.inflate(R.layout.fragment_webview, container, false);
        WebView content = root.findViewById(R.id.webview_content);
        content.getSettings().setJavaScriptEnabled(true); // Needed for Google Calendar
        content.loadUrl(getString(url()));
        content.setWebViewClient(new WebViewClient() {
            boolean mFirstLoad = true;
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!mFirstLoad) {
                    Events.SINGLETON.get(getContext()).publishMessage(refreshMessage());
                }
                mFirstLoad = false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // We don't want to follow links inside this webview, which is confusing
                // - so pop out to let the system handle them
                view.getContext().startActivity(new Intent()
                        .setAction(Intent.ACTION_VIEW)
                        .setData(Uri.parse(url))
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;
            }
        });
        return root;
    }
}
