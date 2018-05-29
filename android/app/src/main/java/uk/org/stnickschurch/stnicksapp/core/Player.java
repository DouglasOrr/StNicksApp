package uk.org.stnickschurch.stnicksapp.core;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;

/**
 * Manage playing & syncing of sermons.
 */
public class Player {
    public final BehaviorSubject<Sermon> playing = BehaviorSubject.create();
    public final ExoPlayer player;
    private final Context mContext;
    private final ExtractorMediaSource.Factory mMediaFactory;

    public void play(final Sermon sermon) {
        Store.SINGLETON.get(mContext).getLocalOrRemoteAudio(sermon)
                .subscribe(new Consumer<Uri>() {
            @Override
            public void accept(Uri uri) {
                Utility.log("Playing %s", uri);
                player.prepare(mMediaFactory.createMediaSource(uri));
                player.setPlayWhenReady(true);
                playing.onNext(sermon);
            }
        });
    }

    private Player(Context context) {
        mContext = context;
        player = ExoPlayerFactory.newSimpleInstance(context, new DefaultTrackSelector(new DefaultBandwidthMeter()));
        mMediaFactory = new ExtractorMediaSource.Factory(
                new DefaultDataSourceFactory(context, Util.getUserAgent(context, "stnicksapp")));
    }

    public static Singleton<Player> SINGLETON = new Singleton<Player>() {
        @Override
        Player newInstance(Context context) {
            return new Player(context);
        }
    };
}
