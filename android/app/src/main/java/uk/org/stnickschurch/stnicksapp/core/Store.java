package uk.org.stnickschurch.stnicksapp.core;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
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

        @TypeConverters(Sermon.Local.DownloadState.Converter.class)
        @Query("UPDATE sermon SET local_download_state = :state WHERE id = :id")
        abstract void setDownloadState(String id, Sermon.Local.DownloadState state);

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

    @Database(entities = {Sermon.class}, version = 2)
    static abstract class AppDatabase extends RoomDatabase {
        public abstract SermonDao sermonDao();
    }

    private final Context mContext;
    private final AppDatabase mDatabase;
    private static final Object UPDATE = new Object();  // Just a marker object
    private final BehaviorSubject<Object> mUpdates = BehaviorSubject.<Object> createDefault(UPDATE);

    private Store(Context context, AppDatabase database) {
        mContext = context;
        mDatabase = database;
    }

    public Observable<List<Sermon>> recentSermons() {
        return mUpdates
                .observeOn(Schedulers.io())
                .map(new Function<Object, List<Sermon>>() {
                    @Override
                    public List<Sermon> apply(Object ignored) {
                        return mDatabase.sermonDao().recentSermons();
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
                            mDatabase.sermonDao().sync(sermons);
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

    public void setDownloadState(final Sermon sermon, final Sermon.Local.DownloadState state) {
        Schedulers.io().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                mDatabase.sermonDao().setDownloadState(sermon.id, state);
                mUpdates.onNext(UPDATE);
            }
        });
    }

    public static final Singleton<Store> SINGLETON = new Singleton<Store>() {
        @Override
        Store newInstance(Context context) {
            return new Store(
                context,
                Room.databaseBuilder(
                    context, AppDatabase.class,"stnicksapp-core"
                ).fallbackToDestructiveMigration().build()); // TODO: DO NOT COMMIT
        }
    };
}
