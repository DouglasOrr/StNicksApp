package uk.org.stnickschurch.stnicksapp.core;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.common.io.Files;

import java.io.File;

import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

/**
 * Manage playing & syncing of sermons.
 */
public class Player {
    public static class Action {
        public enum Type {
            PLAY, DOWNLOAD, DELETE
        }
        public final Sermon sermon;
        public final Type type;

        public Action(Sermon sermon, Type type) {
            this.sermon = sermon;
            this.type = type;
        }
    }

    public final BehaviorSubject<Sermon> playing = BehaviorSubject.create();
    public final PublishSubject<Action> actions = PublishSubject.create();
    public final ExoPlayer player;
    private final Context mContext;
    private final ExtractorMediaSource.Factory mMediaFactory;

    private File localPath(Sermon sermon) {
        return new File(
                mContext.getExternalFilesDir("sermons"),
                sermon.id + "." + Files.getFileExtension(sermon.audio)
        );
    }
    private void startPlayback(Sermon sermon) {
        File local = localPath(sermon);
        Uri uri = local.exists() ? Uri.fromFile(local) : Uri.parse(sermon.audio);
        player.prepare(mMediaFactory.createMediaSource(uri));
        player.setPlayWhenReady(true);
        playing.onNext(sermon);
    }
    private void download(Sermon sermon) {
        File local = localPath(sermon);
        if (local.exists()) {
            local.delete();
        }
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(sermon.audio))
                .setDestinationUri(Uri.fromFile(local))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setTitle(sermon.title);
        DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);
    }
    private void delete(Sermon sermon) {
        File local = localPath(sermon);
        if (local.delete()) {
            Store.SINGLETON.get(mContext).setDownloadState(sermon, Sermon.Local.DownloadState.NONE);
        }
    }
    private void handleAction(Action action) {
        switch (action.type) {
            case PLAY:
                startPlayback(action.sermon);
                break;
            case DOWNLOAD:
                download(action.sermon);
                break;
            case DELETE:
                delete(action.sermon);
                break;
            default:
                throw new AssertionError("Unexpected action type " + action.type);
        }
    }

    private Player(Context context) {
        mContext = context;
        player = ExoPlayerFactory.newSimpleInstance(context, new DefaultTrackSelector(new DefaultBandwidthMeter()));
        mMediaFactory = new ExtractorMediaSource.Factory(
                new DefaultDataSourceFactory(context, Util.getUserAgent(context, "stnicksapp")));
        actions.subscribeOn(Schedulers.io())
                .subscribe(new Consumer<Action>() {
                    @Override
                    public void accept(Action action) {
                        handleAction(action);
                    }
                });
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Utility.log("Download complete %s", intent);
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
                DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
                Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(id));
                if (!cursor.moveToFirst()) {
                    Utility.log("Error! couldn't find downloaded file");
                    return;
                }
                String localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                Utility.log("Downloaded URI: %s", localUri);
                // TODO: detect what is downloaded
            }
        }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    public static Singleton<Player> SINGLETON = new Singleton<Player>() {
        @Override
        Player newInstance(Context context) {
            return new Player(context);
        }
    };
}
