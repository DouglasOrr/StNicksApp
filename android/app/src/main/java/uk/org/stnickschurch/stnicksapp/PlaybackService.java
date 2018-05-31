package uk.org.stnickschurch.stnicksapp;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;
import uk.org.stnickschurch.stnicksapp.core.Notifications;
import uk.org.stnickschurch.stnicksapp.core.Sermon;
import uk.org.stnickschurch.stnicksapp.core.Singleton;
import uk.org.stnickschurch.stnicksapp.core.Store;
import uk.org.stnickschurch.stnicksapp.core.Utility;

public class PlaybackService extends Service {
    /**
     * Play/pause/stop events emitted by the player.
     */
    public static class Event {
        public static final Event STOP = new Event(ACTION_STOP, null);

        public final String action;
        public final @Nullable Sermon sermon;

        public Event(String action, @Nullable Sermon sermon) {
            this.action = action;
            this.sermon = sermon;
        }
        @Override
        public boolean equals(Object that_) {
            if (that_ == null || this.getClass() != that_.getClass()) {
                return false;
            }
            Event that = (Event) that_;
            return Objects.equal(this.action, that.action)
                    && Objects.equal(this.sermon, that.sermon);
        }
        @Override
        public int hashCode() {
            return Objects.hashCode(this.action, this.sermon);
        }
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("action", action)
                    .add("sermon", sermon)
                    .toString();
        }
    }

    /**
     * Timeline "progress" bar events emitted by the player.
     */
    public static class Progress {
        public final int max;
        public final int position;
        public final int buffer;

        public Progress(int max, int position, int buffer) {
            this.max = max;
            this.position = position;
            this.buffer = buffer;
        }
        @Override
        public boolean equals(Object that_) {
            if (that_ == null || this.getClass() != that_.getClass()) {
                return false;
            }
            Progress that = (Progress) that_;
            return this.max == that.max && this.position == that.position && this.buffer == that.buffer;
        }
        @Override
        public int hashCode() {
            return Objects.hashCode(this.max, this.position, this.buffer);
        }
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("max", max).add("position", position).add("buffer", buffer)
                    .toString();
        }
    }

    /**
     * API for interfacing with the player, mainly through intents.
     */
    public static class Client {
        public final BehaviorSubject<Event> events = BehaviorSubject.createDefault(Event.STOP);
        public final BehaviorSubject<Progress> progress = BehaviorSubject.create();

        private final Context mContext;
        private Client(Context context) {
            mContext = context;
        }

        private Intent intent(String action, @Nullable String sermonId, @Nullable Integer seekToPosition) {
            Intent intent = new Intent(mContext, PlaybackService.class).setAction(action);
            if (sermonId != null) {
                intent.putExtra(EXTRA_SERMON_ID, sermonId);
            }
            if (seekToPosition != null) {
                intent.putExtra(EXTRA_SEEK_TO_POSITION, seekToPosition);
            }
            return intent;
        }

        public PendingIntent pendingIntent(String action, @Nullable String sermonId) {
            return PendingIntent.getService(mContext,
                    Objects.hashCode(action, sermonId),
                    intent(action, sermonId, null),
                    0);
        }

        public void start(String action, @Nullable String sermonId, @Nullable Integer seekToPosition) {
            Intent intent = intent(action, sermonId, seekToPosition);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mContext.startForegroundService(intent);
            } else {
                mContext.startService(intent);
            }
        }

        public static final Singleton<Client> SINGLETON = new Singleton<Client>() {
            @Override
            protected Client newInstance(Context context) {
                return new Client(context);
            }
        };
    }

    public static final String ACTION_PLAY = "uk.org.stnickschurch.PLAY";
    public static final String ACTION_PAUSE = "uk.org.stnickschurch.PAUSE";
    public static final String ACTION_STOP = "uk.org.stnickschurch.STOP";
    public static final String ACTION_SEEK_TO = "uk.org.stnickschurch.SEEK_TO";
    public static final String EXTRA_SERMON_ID = "sermon_id";
    public static final String EXTRA_SEEK_TO_POSITION = "seek_to_ms";

    private ExtractorMediaSource.Factory mMediaFactory;
    private ExoPlayer mPlayer;
    private Timer mTimer;

    @Override
    public void onCreate() {
        super.onCreate();
        mPlayer = ExoPlayerFactory.newSimpleInstance(this,
                new DefaultTrackSelector(new DefaultBandwidthMeter()));
        mMediaFactory = new ExtractorMediaSource.Factory(
                new DefaultDataSourceFactory(this,
                        Util.getUserAgent(this, "stnicksapp")));
        mTimer = new Timer("player-position");
        mTimer.scheduleAtFixedRate(new TimerTask() {
            private Timeline.Window mWindow = new Timeline.Window();
            @Override
            public void run() {
                if (!mPlayer.getCurrentTimeline().isEmpty()) {
                    mPlayer.getCurrentTimeline().getWindow(mPlayer.getCurrentWindowIndex(), mWindow);
                    Client.SINGLETON.get(PlaybackService.this)
                            .progress
                            .onNext(new Progress((int) mWindow.getDurationMs(),
                                    (int) mPlayer.getCurrentPosition(),
                                    (int) mPlayer.getBufferedPosition()));
                }
            }
        }, 0, Utility.getPeriodMs(getString(R.string.seekbar_refresh)));
    }

    @Override
    public void onDestroy() {
        mTimer.cancel();
        mTimer = null;
        super.onDestroy();
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) {
        return null; // does not support binding
    }

    private void onEvent(Event event) {
        Client.SINGLETON.get(this).events.onNext(event);
        if (ACTION_STOP.equals(event.action)) {
            stopSelf();
        } else {
            // Only ACTION_STOP is allowed to have a null sermon
            startForeground(Notifications.notificationId(event.sermon.id),
                    Notifications.SINGLETON.get(PlaybackService.this)
                            .getPlayback(event.sermon, ACTION_PLAY.equals(event.action)));
        }
    }

    private void execute(String action, @Nullable Sermon sermon, int seekToPosition) {
        final @Nullable Sermon oldSermon = Client.SINGLETON.get(this).events.getValue().sermon;

        // Fall back to ACTION_STOP if we're missing sermon info (for any reason)
        if (ACTION_STOP.equals(action) || (sermon == null && oldSermon == null)) {
            mPlayer.stop(true);
            onEvent(new Event(ACTION_STOP, null));

        } else {
            // Either sermon (preferred) or oldSermon (fallback) must be non-null
            final @NonNull Sermon activeSermon = sermon != null ? sermon : oldSermon;
            if (ACTION_PLAY.equals(action)) {
                if (!activeSermon.equals(oldSermon)) {
                    Store.SINGLETON.get(this)
                            .getLocalOrRemoteAudio(sermon)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<Uri>() {
                                @Override
                                public void accept(Uri uri) {
                                    mPlayer.prepare(mMediaFactory.createMediaSource(uri));
                                    mPlayer.setPlayWhenReady(true);
                                    onEvent(new Event(ACTION_PLAY, activeSermon));
                                }
                            });
                } else {
                    mPlayer.setPlayWhenReady(true);
                    onEvent(new Event(ACTION_PLAY, activeSermon));
                }

            } else if (ACTION_PAUSE.equals(action)) {
                mPlayer.setPlayWhenReady(false);
                onEvent(new Event(ACTION_PAUSE, activeSermon));

            } else if (ACTION_SEEK_TO.equals(action)) {
                mPlayer.seekTo(seekToPosition);
                // Don't generate an event - no-one cares!

            } else {
                Utility.log("Error! Unexpected action: %s", action);
            }
        }
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            final int seekToPosition = intent.getIntExtra(EXTRA_SEEK_TO_POSITION, 0);
            Store.SINGLETON.get(this)
                    .getSermon(intent.getStringExtra(EXTRA_SERMON_ID))
                    .subscribe(new Consumer<Optional<Sermon>>() {
                        @Override
                        public void accept(Optional<Sermon> sermon) {
                            execute(action, sermon.orNull(), seekToPosition);
                        }
                    });
        }
        return START_NOT_STICKY;
    }
}
