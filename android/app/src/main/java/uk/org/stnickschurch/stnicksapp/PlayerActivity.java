package uk.org.stnickschurch.stnicksapp;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.SeekBar;

import com.google.common.collect.ImmutableMap;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import butterknife.BindView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import uk.org.stnickschurch.stnicksapp.core.Downloader;
import uk.org.stnickschurch.stnicksapp.core.Utility;

public class PlayerActivity extends BaseActivity {
    @BindView(R.id.seekbar_player) SeekBar mSeekBar;
    @BindView(R.id.webview_player) WebView mBibleView;
    private PlaybackService.Event mLastEvent = PlaybackService.Event.STOP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_player);
        super.onCreate(savedInstanceState);

        disposeOnDestroy(PlaybackService.Client.SINGLETON.get(this)
                .events
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<PlaybackService.Event>() {
                    @Override
                    public void accept(PlaybackService.Event event) throws Exception {
                        if (PlaybackService.ACTION_STOP.equals(event.action)) {
                            finish();
                        } else {
                            // event.sermon should not be null
                            if (!event.sermon.equals(mLastEvent.sermon)) {
                                getSupportActionBar().setTitle(event.sermon.passage);
                                loadPassage(event.sermon.passage);
                            }
                        }
                        mLastEvent = event;
                        invalidateOptionsMenu();
                    }
                }));

        disposeOnDestroy(PlaybackService.Client.SINGLETON.get(this)
                .progress
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<PlaybackService.Progress>() {
                    @Override
                    public void accept(PlaybackService.Progress progress) {
                        mSeekBar.setMax(progress.max);
                        mSeekBar.setProgress(progress.position);
                        mSeekBar.setSecondaryProgress(progress.buffer);
                    }
                }));

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    PlaybackService.Client.SINGLETON.get(PlayerActivity.this)
                            .start(PlaybackService.ACTION_SEEK_TO, null, progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    private void loadPassage(String passage) throws UnsupportedEncodingException {
        String url = String.format(
                "https://api.esv.org/v3/passage/html/?q=%s"
                        + "&include-headings=false"
                        + "&include-passage-references=false"
                        + "&include-first-verse-numbers=false"
                        + "&include-short-copyright=false"
                        + "&include-copyright=true"
                        + "&include-surrounding-chapters-below=true"
                        + "&link-url=%s",
                URLEncoder.encode(passage, "UTF-8"),
                URLEncoder.encode("https://www.esv.org/", "UTF-8"));
        disposeOnDestroy(Downloader.SINGLETON.get(this).cachedGetRequest(
                        url,
                        ImmutableMap.of(
                                "Authorization",
                                "Token 7a226c2dcd345957fa82736b2f558d8c3126159e"),
                        Utility.getPeriodMs(getString(R.string.passage_refresh)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<JSONObject>() {
                    @Override
                    public void accept(JSONObject response) throws Exception {
                        String passageHtml = response.getJSONArray("passages").getString(0);
                        mBibleView.loadDataWithBaseURL("file:///android_asset/",
                                getString(R.string.bible_html_header) + passageHtml,
                                "text/html", "UTF-8", null);
                    }
                }));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_player, menu);
        boolean playing = mLastEvent.action.equals(PlaybackService.ACTION_PLAY);
        menu.findItem(R.id.menu_play).setVisible(!playing);
        menu.findItem(R.id.menu_pause).setVisible(playing);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        PlaybackService.Client client = PlaybackService.Client.SINGLETON.get(this);
        switch (item.getItemId()) {
            case R.id.menu_play:
                client.start(PlaybackService.ACTION_PLAY, null, null);
                return true;
            case R.id.menu_pause:
                client.start(PlaybackService.ACTION_PAUSE, null, null);
                return true;
            case R.id.menu_stop:
                client.start(PlaybackService.ACTION_STOP, null, null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
