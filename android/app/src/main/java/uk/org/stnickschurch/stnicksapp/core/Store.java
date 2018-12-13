package uk.org.stnickschurch.stnicksapp.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.LongSparseArray;
import android.util.Pair;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import uk.org.stnickschurch.stnicksapp.R;
import uk.org.stnickschurch.stnicksapp.data.ListRefresh;
import uk.org.stnickschurch.stnicksapp.data.Sermon;
import uk.org.stnickschurch.stnicksapp.data.SermonQuery;
import uk.org.stnickschurch.stnicksapp.data.StringWithSnippet;

/**
 * Data store for sermon metadata, for syncing, and for local audio downloads.
 */
public class Store {
    private final Context mContext;
    private final Db mDatabaseHelper;
    private final BehaviorSubject<Integer> mUpdates = BehaviorSubject.createDefault(0);

    // API

    public Observable<List<Sermon>> listSermons(Observable<SermonQuery> query) {
        return Observable.combineLatest(query, mUpdates, Utility.toPair())
                .distinctUntilChanged()
                .observeOn(Schedulers.io())
                .map(new Function<Pair<SermonQuery, Integer>, List<Sermon>>() {
                    @Override
                    public List<Sermon> apply(Pair<SermonQuery, Integer> queryUpdate) {
                        return doListSermons(mDatabaseHelper.getReadableDatabase(), queryUpdate.first);
                    }
                });
    }

    public Single<Sermon> getSermon(long id) {
        return Single.create(new SingleOnSubscribe<Sermon>() {
            @Override
            public void subscribe(SingleEmitter<Sermon> emitter) {
                Sermon sermon = doGetSermon(mDatabaseHelper.getReadableDatabase(), id);
                if (sermon != null) {
                    emitter.onSuccess(sermon);
                } else {
                    emitter.onError(new IllegalStateException("Cannot find sermon for ID " + id));
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Get the local or remote audio Uri for the sermon.
     * @param forceRemote if true, don't return a local file Uri, even if the sermon is downloaded
     */
    public Single<Uri> getAudio(long id, boolean forceRemote) {
        return Single.create(new SingleOnSubscribe<Uri>() {
            @Override
            public void subscribe(SingleEmitter<Uri> emitter) {
                Uri uri = doGetAudio(mDatabaseHelper.getReadableDatabase(), id, forceRemote);
                if (uri != null) {
                    emitter.onSuccess(uri);
                } else {
                    emitter.onError(new IllegalStateException("Cannot find audio for ID " + id));
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    private void tick() {
        mUpdates.onNext(mUpdates.getValue() + 1);
    }

    public void sync() {
        Downloader.SINGLETON.get(mContext)
                .getRequest(mContext.getString(R.string.sermon_list), null)
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
                            ListRefresh stats = doSync(mDatabaseHelper.getWritableDatabase(), response);
                            Events.SINGLETON.get(mContext).publish(Events.Level.INFO, R.string.message_sermons_refreshed,
                                    stats.added, stats.updated, stats.removed);
                            tick();
                        } catch (JSONException e) {
                            Events.SINGLETON.get(mContext).publish(Events.Level.ERROR, R.string.error_bad_sermon_list);
                        }
                    }
                    @Override
                    public void onError(Throwable error) {
                        Events.SINGLETON.get(mContext).publish(Events.Level.ERROR, R.string.error_no_sermon_list);
                    }
                });
    }

    public void startDownload(long id, long downloadId) {
        Schedulers.io().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                doStartDownload(mDatabaseHelper.getWritableDatabase(), id, downloadId);
            }
        });
    }

    public Single<Sermon> finishDownload(long downloadId, File localPath) {
        return Single.create(new SingleOnSubscribe<Sermon>() {
            @Override
            public void subscribe(SingleEmitter<Sermon> emitter) {
                Sermon sermon = doFinishDownload(mDatabaseHelper.getWritableDatabase(), downloadId, localPath);
                if (sermon != null) {
                    emitter.onSuccess(sermon);
                    tick();
                } else {
                    emitter.onError(new IllegalStateException("Cannot find sermon for download ID " + downloadId));
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    public void deleteDownload(long id) {
        Schedulers.io().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                File local = doDeleteDownload(mDatabaseHelper.getWritableDatabase(), id);
                if (local != null) {
                    local.delete();
                    tick();
                }
            }
        });
    }

    // Database

    static final DateTimeFormatter ISO_TIME_FORMAT =
            ISODateTimeFormat.dateTimeNoMillis().withOffsetParsed();

    static class Db extends SQLiteOpenHelper {
        public Db(Context context, String name) {
            super(context, name, null, 1);
        }

        public static String selectSnippet(String column) {
            int columnId;
            if ("passage".equals(column)) {
                columnId = 0;
            } else if ("series".equals(column)) {
                columnId = 1;
            } else if ("title".equals(column)) {
                columnId = 2;
            } else if ("speaker".equals(column)) {
                columnId = 3;
            } else {
                throw new IllegalArgumentException("Unexpected column " + column);
            }
            return "snippet(sermon_fts, \"<b>\", \"</b>\", \"<b>...</b>\", " + columnId + ", -64)";
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // WARNING: DO NOT CHANGE THIS WITHOUT UPDATING THE VERSION NUMBER & onUpgrade()

            db.execSQL("CREATE TABLE sermon (" +
                    " _id INTEGER PRIMARY KEY," +
                    " passage TEXT NOT NULL," +
                    " time TEXT NOT NULL," +
                    " series TEXT NOT NULL," +
                    " title TEXT NOT NULL," +
                    " speaker TEXT NOT NULL," +
                    " audio TEXT NOT NULL)");

            db.execSQL("CREATE TABLE sync (" +
                    " sermon_id INTEGER PRIMARY KEY," +
                    " hash TEXT NOT NULL," +
                    " FOREIGN KEY(sermon_id) REFERENCES sermon(_id) ON DELETE CASCADE)");

            db.execSQL("CREATE TABLE download (" +
                    " sermon_id INTEGER PRIMARY KEY," +
                    " download_id INTEGER," +
                    " local_path TEXT," +
                    " FOREIGN KEY(sermon_id) REFERENCES sermon(_id) ON DELETE CASCADE)");

            // FTS table & triggers
            db.execSQL("CREATE VIRTUAL TABLE sermon_fts USING FTS4 (" +
                    " content=\"sermon\"," +
                    " passage, series, title, speaker)"); // WARNING - REORDERING => selectSnippet()
            final String delete = "BEGIN DELETE FROM sermon_fts WHERE docid=old._id ; END";
            db.execSQL("CREATE TRIGGER sermon_before_update BEFORE UPDATE ON sermon " + delete);
            db.execSQL("CREATE TRIGGER sermon_before_delete BEFORE DELETE ON sermon " + delete);
            final String insert = "BEGIN INSERT INTO sermon_fts(docid, passage, series, title, speaker)" +
                    " VALUES (new._id, new.passage, new.series, new.title, new.speaker) ; END";
            db.execSQL("CREATE TRIGGER sermon_after_update AFTER UPDATE ON sermon " + insert);
            db.execSQL("CREATE TRIGGER sermon_after_insert AFTER INSERT ON sermon " + insert);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            throw new SQLiteException("Upgrade not supported");
        }
    }

    private static LongSparseArray<String> readSyncData(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            LongSparseArray<String> result = new LongSparseArray<>();
            cursor = db.rawQuery("SELECT * FROM sync", null);
            final int sermonId = cursor.getColumnIndexOrThrow("sermon_id");
            final int hash = cursor.getColumnIndexOrThrow("hash");
            while (cursor.moveToNext()) {
                result.put(cursor.getLong(sermonId), cursor.getString(hash));
            }
            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    static ListRefresh doSync(SQLiteDatabase db, JSONObject response)
            throws JSONException {
        int added = 0, updated = 0, removed = 0;
        db.beginTransaction();
        try {
            final LongSparseArray<String> syncData = readSyncData(db);
            final HashSet<Long> newSermonIds = new HashSet<>();
            final JSONArray sermons = response.getJSONArray("sermons");
            for (int i = 0; i < sermons.length(); ++i) {
                JSONObject sermon = sermons.getJSONObject(i);
                // Core properties
                long id = sermon.getLong("id");
                String passage = sermon.getString("passage");
                String time = sermon.getString("time");
                String series = sermon.getString("series");
                String title = sermon.getString("title");
                String speaker = sermon.getString("speaker");
                String audio = sermon.getString("audio");
                // Determine if we need to update
                String oldHash = syncData.get(id);
                String newHash = Utility.md5(passage, time, series, title, speaker, audio);
                // Perform the insert/update
                if (oldHash == null || !oldHash.equals(newHash)) {
                    ContentValues sermonContent = new ContentValues(7);
                    sermonContent.put("passage", passage);
                    sermonContent.put("time", time);
                    sermonContent.put("series", series);
                    sermonContent.put("title", title);
                    sermonContent.put("speaker", speaker);
                    sermonContent.put("audio", audio);
                    if (oldHash == null) {
                        sermonContent.put("_id", id);
                        db.insertWithOnConflict("sermon", null, sermonContent,
                                SQLiteDatabase.CONFLICT_REPLACE);
                        ++added;
                    } else {
                        db.update("sermon", sermonContent, "_id = ?",
                                new String[] { Long.toString(id) });
                        ++updated;
                    }
                    ContentValues syncContent = new ContentValues(2);
                    syncContent.put("sermon_id", id);
                    syncContent.put("hash", newHash);
                    db.insertWithOnConflict("sync", null, syncContent,
                            SQLiteDatabase.CONFLICT_REPLACE);
                }
                newSermonIds.add(id);
            }
            // Delete
            for (int i = 0; i < syncData.size(); ++i) {
                long id = syncData.keyAt(i);
                if (!newSermonIds.contains(id)) {
                    db.delete("sermon", "_id = ?",
                            new String[] { Long.toString(id) });
                    ++removed;
                }
            }
            db.setTransactionSuccessful();
            return new ListRefresh(added, updated, removed);
        } finally {
            db.endTransaction();
        }
    }

    private static class SermonCursor {
        private static final String SELECT_FROM =
                "SELECT sermon._id, sermon.passage, sermon.time," +
                        " sermon.series, sermon.title, sermon.speaker," +
                        " " + Db.selectSnippet("passage") + " AS passage_snippet," +
                        " " + Db.selectSnippet("series") + " AS series_snippet," +
                        " " + Db.selectSnippet("title") + " AS title_snippet," +
                        " " + Db.selectSnippet("speaker") + " AS speaker_snippet," +
                        " (download.local_path IS NOT NULL) AS downloaded," +
                        " (download.download_id IS NOT NULL) AS downloading" +
                        " FROM sermon LEFT JOIN download ON sermon._id = download.sermon_id" +
                        " JOIN sermon_fts ON sermon._id = sermon_fts.docid";

        private final Cursor mCursor;
        private final int mId,
                mPassage, mPassageSnippet,
                mTime,
                mSeries, mSeriesSnippet,
                mTitle, mTitleSnippet,
                mSpeaker, mSpeakerSnippet,
                mDownloaded, mDownloading;

        private SermonCursor(SQLiteDatabase db, String queryTail, String[] whereArgs) {
            mCursor = db.rawQuery(SermonCursor.SELECT_FROM + queryTail, whereArgs);
            mId = mCursor.getColumnIndexOrThrow("_id");
            mPassage = mCursor.getColumnIndexOrThrow("passage");
            mPassageSnippet = mCursor.getColumnIndexOrThrow("passage_snippet");
            mTime = mCursor.getColumnIndexOrThrow("time");
            mSeries = mCursor.getColumnIndexOrThrow("series");
            mSeriesSnippet = mCursor.getColumnIndexOrThrow("series_snippet");
            mTitle = mCursor.getColumnIndexOrThrow("title");
            mTitleSnippet = mCursor.getColumnIndexOrThrow("title_snippet");
            mSpeaker = mCursor.getColumnIndexOrThrow("speaker");
            mSpeakerSnippet = mCursor.getColumnIndexOrThrow("speaker_snippet");
            mDownloaded = mCursor.getColumnIndexOrThrow("downloaded");
            mDownloading = mCursor.getColumnIndexOrThrow("downloading");
        }

        private boolean moveToNext() {
            return mCursor.moveToNext();
        }

        private void close() {
            mCursor.close();
        }

        private StringWithSnippet getStringWithSnippet(int column, int snippetColumn) {
            String snippet = mCursor.getString(snippetColumn);
            return new StringWithSnippet(mCursor.getString(column),
                    (snippet == null || snippet.isEmpty()) ? null : snippet);
        }

        private Sermon.DownloadState getDownloadState() {
            if (mCursor.getInt(mDownloaded) != 0) {
                return Sermon.DownloadState.DOWNLOADED;
            }
            if (mCursor.getInt(mDownloading) != 0) {
                return Sermon.DownloadState.ATTEMPTED;
            }
            return Sermon.DownloadState.NONE;
        }

        private Sermon get() throws IllegalArgumentException {
            return new Sermon(
                    mCursor.getLong(mId),
                    getStringWithSnippet(mPassage, mPassageSnippet),
                    ISO_TIME_FORMAT.parseDateTime(mCursor.getString(mTime)),
                    getStringWithSnippet(mSeries, mSeriesSnippet),
                    getStringWithSnippet(mTitle, mTitleSnippet),
                    getStringWithSnippet(mSpeaker, mSpeakerSnippet),
                    getDownloadState()
            );
        }
    }

    static List<Sermon> doListSermons(SQLiteDatabase db, SermonQuery query) {
        String whereClause = " WHERE 1";
        ArrayList<String> whereArgs = new ArrayList<>();
        if (query.downloaded_only) {
            whereClause += " AND (download.local_path IS NOT NULL)";
        }
        if (!query.search_text.equals("")) {
            whereClause += " AND (sermon_fts.sermon_fts MATCH ?)";
            whereArgs.add(query.search_text);
        }
        SermonCursor cursor = new SermonCursor(db,
                whereClause + " ORDER BY time DESC", whereArgs.toArray(new String[0]));
        try {
            List<Sermon> results = new ArrayList<>();
            while (cursor.moveToNext()) {
                try {
                    results.add(cursor.get());
                } catch (IllegalArgumentException e) {
                    Utility.nonFatal(e);
                    // skip this Sermon (couldn't parse the date)
                }
            }
            return results;
        } finally {
            cursor.close();
        }
    }

    static Sermon doGetSermon(SQLiteDatabase db, long id) {
        SermonCursor cursor = new SermonCursor(db,
                " WHERE sermon._id=?", new String[] { Long.toString(id) });
        try {
            return cursor.moveToNext() ? cursor.get() : null;
        } catch (IllegalArgumentException e) {
            Utility.nonFatal(e);
            return null;
        } finally {
            cursor.close();
        }
    }

    // Database - downloads

    static void doStartDownload(SQLiteDatabase db, long id, long downloadId) {
        ContentValues content = new ContentValues(3);
        content.put("sermon_id", id);
        content.put("download_id", downloadId);
        content.putNull("local_path");
        db.insertWithOnConflict("download", null, content, SQLiteDatabase.CONFLICT_REPLACE);
    }

    static Sermon doFinishDownload(SQLiteDatabase db, long downloadId, File localPath) {
        // 1. Get the Sermon ID
        Cursor cursor = db.rawQuery("SELECT sermon_id FROM download WHERE download_id = ?",
                new String[] { Long.toString(downloadId) });
        try {
            if (!cursor.moveToNext()) {
                return null;
            }
            long id = cursor.getLong(cursor.getColumnIndexOrThrow("sermon_id"));
            // Update the download table with localPath
            ContentValues content = new ContentValues(2);
            content.putNull("download_id");
            content.put("local_path", localPath.getPath());
            db.update("download", content, "sermon_id = ?", new String[] { Long.toString(id) });
            // Return the sermon data
            return doGetSermon(db, id);
        } finally {
            cursor.close();
        }
    }

    static Uri doGetAudio(SQLiteDatabase db, long id, boolean forceRemote) {
        Cursor cursor = db.rawQuery("SELECT sermon.audio AS audio," +
                " download.local_path AS local_path" +
                " FROM sermon" +
                " LEFT JOIN download ON sermon._id = download.sermon_id" +
                " WHERE sermon._id = ?", new String[] { Long.toString(id) });
        try {
            if (!cursor.moveToNext()) {
                return null;
            }
            String localPath = cursor.getString(cursor.getColumnIndexOrThrow("local_path"));
            if (forceRemote || localPath == null) {
                return Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow("audio")));
            } else {
                return Uri.fromFile(new File(localPath));
            }
        } finally {
            cursor.close();
        }
    }

    static File doDeleteDownload(SQLiteDatabase db, long id) {
        Cursor cursor = db.rawQuery("SELECT local_path FROM download WHERE sermon_id = ?",
                new String[] { Long.toString(id) });
        try {
            if (!cursor.moveToNext()) {
                return null;
            }
            String localPath = cursor.getString(cursor.getColumnIndexOrThrow("local_path"));
            if (localPath == null) {
                return null;
            }
            db.delete("download", "sermon_id = ?", new String[] { Long.toString(id) });
            return new File(localPath);
        } finally {
            cursor.close();
        }
    }

    // Creation

    private Store(Context context) {
        mContext = context;
        mDatabaseHelper = new Db(context, "store");
    }

    public static final Singleton<Store> SINGLETON = new Singleton<Store>() {
        @Override
        protected Store newInstance(Context context) {
            return new Store(context);
        }
    };
}
