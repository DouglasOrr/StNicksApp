package uk.org.stnickschurch.stnicksapp;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.SeekBar;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.common.collect.ImmutableMap;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import io.reactivex.functions.Consumer;
import uk.org.stnickschurch.stnicksapp.core.Downloader;
import uk.org.stnickschurch.stnicksapp.core.Sermon;

public class Player extends BaseActivity {
    @BindView(R.id.seekbar_player) SeekBar mSeekBar;
    @BindView(R.id.webview_player) WebView mBibleView;
    private Timer mTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_player);
        super.onCreate(savedInstanceState);
        disposeOnDestroy(Downloader.get(this).playing.subscribe(new Consumer<Sermon>() {
            @Override
            public void accept(Sermon sermon) throws Exception {
                getSupportActionBar().setTitle(sermon.passage);
                loadPassage(sermon.passage);
            }
        }));
        mTimer = new Timer("seekbar");
        mTimer.scheduleAtFixedRate(new TimerTask() {
            private Timeline.Window mWindow = new Timeline.Window();
            @Override
            public void run() {
                ExoPlayer player = Downloader.get(Player.this).player;
                if (!player.getCurrentTimeline().isEmpty()) {
                    player.getCurrentTimeline().getWindow(player.getCurrentWindowIndex(), mWindow);
                    mSeekBar.setMax((int) mWindow.getDurationMs());
                    mSeekBar.setProgress((int) player.getCurrentPosition());
                    mSeekBar.setSecondaryProgress((int) player.getBufferedPosition());
                }
            }
        }, 0, getPeriodMs(R.string.seekbar_refresh));
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    Downloader.get(Player.this).player.seekTo(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
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
        Downloader.get(this).restGet(
                url, null,
                ImmutableMap.of(
                        "Authorization",
                        "Token 7a226c2dcd345957fa82736b2f558d8c3126159e")
        ).subscribe(new Consumer<JSONObject>() {
            @Override
            public void accept(JSONObject response) throws Exception {
                String passageHtml = response.getJSONArray("passages").getString(0);
                mBibleView.loadDataWithBaseURL("file:///android_asset/",
                        getString(R.string.bible_html_header) + passageHtml,
                        "text/html", "UTF-8", null);
            }
        });
    }

    @Override
    protected void onDestroy() {
        mTimer.cancel();
        mTimer = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_player, menu);
        boolean playing = Downloader.get(this).player.getPlayWhenReady();
        menu.findItem(R.id.menu_play).setVisible(!playing);
        menu.findItem(R.id.menu_pause).setVisible(playing);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_play:
                Downloader.get(this).player.setPlayWhenReady(true);
                invalidateOptionsMenu();
                return true;
            case R.id.menu_pause:
                Downloader.get(this).player.setPlayWhenReady(false);
                invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
