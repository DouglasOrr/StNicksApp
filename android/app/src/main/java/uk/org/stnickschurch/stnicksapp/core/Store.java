package uk.org.stnickschurch.stnicksapp.core;

import android.app.DownloadManager;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.Transaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;

import com.google.common.base.Optional;
import com.google.common.io.Files;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import uk.org.stnickschurch.stnicksapp.R;

/**
 * Manages a local database, that can be periodically synced with the server.
 */
public class Store {
    @Dao
    static abstract class SermonDao {
        @Query("SELECT * FROM sermon ORDER BY time DESC")
        abstract List<Sermon> recentSermons();

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        abstract void insertAll(List<Sermon> sermons);

        @Query("DELETE FROM sermon WHERE id NOT IN (:ids)")
        abstract void keepOnly(List<String> ids);

        @Transaction
        void sync(List<Sermon> sermons) {
            insertAll(sermons);
            List<String> ids = new ArrayList<>(sermons.size());
            for (Sermon sermon : sermons) {
                ids.add(sermon.id);
            }
            keepOnly(ids);
        }
    }
    @Dao
    static abstract class DownloadDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        abstract void start(SermonDownload sermonDownload);

        @Query("UPDATE sermon_download" +
               " SET local_path = :local_path, download_id = NULL" +
               " WHERE download_id = :download_id")
        abstract int finish(long download_id, String local_path);

        @Query("SELECT * FROM sermon_download WHERE sermon_id = :sermon_id")
        abstract SermonDownload get(String sermon_id);

        @Query("SELECT * FROM sermon_download WHERE sermon_id = :sermon_id")
        abstract Flowable<List<SermonDownload>> watch(String sermon_id);

        @Query("DELETE FROM sermon_download WHERE sermon_id = :sermon_id")
        abstract void delete(String sermon_id);
    }

    @Database(entities = {Sermon.class, SermonDownload.class}, version = 5)
    static abstract class AppDatabase extends RoomDatabase {
        public abstract SermonDao sermons();
        public abstract DownloadDao downloads();
    }

    private final Context mContext;
    private final AppDatabase mDatabase;
    private static final Object UPDATE = new Object();  // Just a marker object
    private final BehaviorSubject<Object> mUpdates = BehaviorSubject.<Object> createDefault(UPDATE);

    private Store(Context context) {
        mContext = context;
        mDatabase = Room.databaseBuilder(
                context, AppDatabase.class,"stnicksapp-core"
        ).build();

        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
                Schedulers.io().scheduleDirect(new Runnable() {
                    @Override
                    public void run() {
                        finishDownload(id);
                    }
                });
            }
        }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    public Observable<List<Sermon>> recentSermons() {
        return mUpdates
                .observeOn(Schedulers.io())
                .map(new Function<Object, List<Sermon>>() {
                    @Override
                    public List<Sermon> apply(Object ignored) {
                        return mDatabase.sermons().recentSermons();
                    }
                });
    }

    public void sync() {
        Downloader.SINGLETON.get(mContext)
                .getRequest(mContext.getString(R.string.sermons_list), null)
                .observeOn(Schedulers.io())
                .subscribe(new Observer<JSONObject>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }
                    @Override
                    public void onComplete() {
                    }
                    @Override
                    public void onNext(JSONObject response) {
                        try {
                            List<Sermon> sermons = Sermon.readSermons(response);
                            Utility.log("Syncing %d sermons", sermons.size());
                            mDatabase.sermons().sync(sermons);
                            mUpdates.onNext(UPDATE);
                        } catch (JSONException e) {
                            Errors.SINGLETON.get(mContext).publish(R.string.error_bad_sermon_list);
                        }
                    }
                    @Override
                    public void onError(Throwable error) {
                        Errors.SINGLETON.get(mContext).publish(R.string.error_no_sermon_list);
                    }
                });
    }

    public Observable<Uri> getLocalOrRemoteAudio(final Sermon sermon) {
        return Observable.create(new ObservableOnSubscribe<Uri>() {
            @Override
            public void subscribe(ObservableEmitter<Uri> emitter) throws Exception {
                SermonDownload download = mDatabase.downloads().get(sermon.id);
                if (download != null && download.local_path != null) {
                    emitter.onNext(Uri.fromFile(new File(download.local_path)));
                } else {
                    emitter.onNext(Uri.parse(sermon.audio));
                }
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io());
    }

    public Flowable<Optional<SermonDownload>> watchDownload(Sermon sermon) {
        return mDatabase.downloads()
                .watch(sermon.id)
                .map(new Function<List<SermonDownload>, Optional<SermonDownload>>() {
            @Override
            public Optional<SermonDownload> apply(List<SermonDownload> sermonDownloads) {
                if (sermonDownloads.isEmpty()) {
                    return Optional.absent();
                } else {
                    return Optional.of(sermonDownloads.get(0));
                }
            }
        });
    }

    private void finishDownload(long id) {
        DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(id));
        if (!cursor.moveToFirst()) {
            Utility.log("Error! couldn't find downloaded file");
            return;
        }
        String localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
        String localPath = Uri.parse(localUri).getPath();
        if (mDatabase.downloads().finish(id, localPath) == 0) {
            // Something else has happened, and the download doesn't match - we don't need the "orphaned" file anymore
            Utility.log("Warning! couldn't find sermon for downloaded file %s", localPath);
            new File(localPath).delete();
        }
    }

    private void executeDownload(Sermon sermon) {
        File local = new File(mContext.getExternalFilesDir("sermons"),
                sermon.title + "." + Files.getFileExtension(sermon.audio));
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(sermon.audio))
                .setDestinationUri(Uri.fromFile(local))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setTitle(sermon.title);
        DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = downloadManager.enqueue(request);
        mDatabase.downloads().start(new SermonDownload(sermon.id, downloadId));
    }
    public void download(final Sermon sermon) {
        Schedulers.io().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                executeDownload(sermon);
            }
        });
    }

    private void executeDelete(Sermon sermon) {
        SermonDownload download = mDatabase.downloads().get(sermon.id);
        if (download != null) {
            if (download.local_path != null) {
                new File(download.local_path).delete();
                mDatabase.downloads().delete(sermon.id);
            }
        }
    }
    public void delete(final Sermon sermon) {
        Schedulers.io().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                executeDelete(sermon);
            }
        });
    }

    public static final Singleton<Store> SINGLETON = new Singleton<Store>() {
        @Override
        Store newInstance(Context context) {
            return new Store(context);
        }
    };
}
