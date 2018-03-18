package uk.org.stnickschurch.stnicksapp.core;

import android.content.Context;
import android.net.Uri;

import com.android.volley.Request;
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

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import uk.org.stnickschurch.stnicksapp.R;

public class Downloader {
    public final Observable<Sermons> sermons;
    public final ExoPlayer player;
    public final PublishSubject<Sermon> playing = PublishSubject.create();

    public Downloader(Context context) {
        RequestQueue queue = Volley.newRequestQueue(context);

        sermons = Utility.request(queue, Request.Method.GET, context.getString(R.string.sermons_list), null)
                .map(new Function<JSONObject, Sermons>() {
                    @Override
                    public Sermons apply(JSONObject obj) throws Exception {
                        return Sermons.fromJson(obj);
                    }
                });

        player = ExoPlayerFactory.newSimpleInstance(context, new DefaultTrackSelector(new DefaultBandwidthMeter()));
        player.setPlayWhenReady(true);
        final ExtractorMediaSource.Factory mediaFactory = new ExtractorMediaSource.Factory(
                new DefaultHttpDataSourceFactory(Util.getUserAgent(context, "stnicksapp")));
        playing.subscribeOn(Schedulers.computation()).subscribe(new Consumer<Sermon>() {
            @Override
            public void accept(Sermon sermon) throws Exception {
                player.prepare(mediaFactory.createMediaSource(Uri.parse(sermon.audio)));
            }
        });
    }
}
