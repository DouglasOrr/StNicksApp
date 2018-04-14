package uk.org.stnickschurch.stnicksapp.core;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.Transaction;
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

    @Database(entities = {Sermon.class}, version = 1)
    static abstract class AppDatabase extends RoomDatabase {
        public abstract SermonDao sermonDao();
    }

    private final Context mContext;
    private final AppDatabase mDatabase;
    private final BehaviorSubject<Object> mUpdates = BehaviorSubject.<Object> createDefault(false);

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
        Downloader.get(mContext)
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
                            mUpdates.onNext(false);
                        } catch (JSONException e) {
                            Errors.get(mContext).publish(R.string.error_bad_sermon_list);
                        }
                    }
                    @Override
                    public void onError(Throwable error) {
                        Errors.get(mContext).publish(R.string.error_no_sermon_list);
                    }
                });
    }

    private static final Object SINGLETON_LOCK = new Object();
    private static Store mSingleton = null;
    public static Store get(Context context) {
        if (mSingleton != null) {
            return mSingleton;
        }
        synchronized (SINGLETON_LOCK) {
            if (mSingleton == null) {
                mSingleton = new Store(
                        context.getApplicationContext(),
                        Room.databaseBuilder(
                                context.getApplicationContext(),
                                AppDatabase.class,
                                "stnicksapp-core"
                        ).build());
            }
            return mSingleton;
        }
    }
}
