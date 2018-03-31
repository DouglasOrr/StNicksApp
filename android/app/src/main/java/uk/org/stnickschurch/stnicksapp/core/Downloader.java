package uk.org.stnickschurch.stnicksapp.core;

import android.content.Context;
import android.net.Uri;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONObject;

import java.io.File;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import uk.org.stnickschurch.stnicksapp.R;

public class Downloader {
    public final Observable<Sermons> sermons;
    public final ExoPlayer player;
    public final BehaviorSubject<Sermon> playing = BehaviorSubject.create();
    private final RequestQueue mQueue;
    private final File mDownloadCache;
    private final Scheduler mDownloader;

    public void play(Sermon sermon) {
        playing.onNext(sermon);
        player.setPlayWhenReady(true);
    }

    public Observable<JSONObject> restGet(String url, Map<String, String> headers, long timeoutMs) {
        return Utility.requestCachedGet(mQueue, url, headers, mDownloader, mDownloadCache, timeoutMs);
    }

    private Downloader(Context context) {
        mQueue = Volley.newRequestQueue(context);
        mDownloadCache = new File(context.getFilesDir(), "cache");
        mDownloadCache.mkdirs();
        mDownloader = Schedulers.newThread();

        long sermonsRefreshMs = Utility.getPeriodMs(context.getString(R.string.sermons_list_foreground_refresh));
        sermons = restGet(context.getString(R.string.sermons_list), null, sermonsRefreshMs)
                .map(new Function<JSONObject, Sermons>() {
                    @Override
                    public Sermons apply(JSONObject obj) throws Exception {
                        return Sermons.fromJson(obj);
                    }
                });

        player = ExoPlayerFactory.newSimpleInstance(context, new DefaultTrackSelector(new DefaultBandwidthMeter()));
        final ExtractorMediaSource.Factory mediaFactory = new ExtractorMediaSource.Factory(
                new DefaultHttpDataSourceFactory(Util.getUserAgent(context, "stnicksapp")));
        playing.subscribeOn(Schedulers.io())
                .subscribe(new Consumer<Sermon>() {
            @Override
            public void accept(Sermon sermon) throws Exception {
                player.prepare(mediaFactory.createMediaSource(Uri.parse(sermon.audio)));
            }
        });
    }

    private static final Object SINGLETON_LOCK = new Object();
    private static Downloader mSingleton = null;
    public static Downloader get(Context context) {
        if (mSingleton != null) {
            return mSingleton;
        }
        synchronized (SINGLETON_LOCK) {
            if (mSingleton == null) {
                mSingleton = new Downloader(context.getApplicationContext());
            }
            return mSingleton;
        }
    }
}
