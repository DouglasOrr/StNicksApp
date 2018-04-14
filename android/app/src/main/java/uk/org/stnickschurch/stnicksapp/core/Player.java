package uk.org.stnickschurch.stnicksapp.core;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

public class Player {
    public final BehaviorSubject<Sermon> playing = BehaviorSubject.create();
    public final ExoPlayer player;

    public void play(Sermon sermon) {
        playing.onNext(sermon);
    }

    private Player(Context context) {
        player = ExoPlayerFactory.newSimpleInstance(context, new DefaultTrackSelector(new DefaultBandwidthMeter()));
        final ExtractorMediaSource.Factory mediaFactory = new ExtractorMediaSource.Factory(
                new DefaultHttpDataSourceFactory(Util.getUserAgent(context, "stnicksapp")));
        playing.subscribeOn(Schedulers.io())
                .subscribe(new Consumer<Sermon>() {
                    @Override
                    public void accept(Sermon sermon) {
                        player.prepare(mediaFactory.createMediaSource(Uri.parse(sermon.audio)));
                        player.setPlayWhenReady(true);
                    }
                });
    }

    private static final Object SINGLETON_LOCK = new Object();
    private static Player mSingleton = null;
    public static Player get(Context context) {
        if (mSingleton != null) {
            return mSingleton;
        }
        synchronized (SINGLETON_LOCK) {
            if (mSingleton == null) {
                mSingleton = new Player(context.getApplicationContext());
            }
            return mSingleton;
        }
    }
}
