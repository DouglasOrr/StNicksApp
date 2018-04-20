package uk.org.stnickschurch.stnicksapp.core;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverter;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import com.google.common.base.MoreObjects;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "sermon")
public class Sermon {
    @NonNull @PrimaryKey
    public String id;
    public void recomputeId() {
        id = Utility.md5(
                this.audio,
                this.series,
                this.title,
                this.passage,
                this.speaker,
                this.time);
    }

    @NonNull @ColumnInfo(name = "audio")
    public String audio;

    @NonNull @ColumnInfo(name = "series")
    public String series;

    @NonNull @ColumnInfo(name = "title")
    public String title;

    @NonNull @ColumnInfo(name = "passage")
    public String passage;

    @NonNull @ColumnInfo(name = "speaker")
    public String speaker;

    @NonNull @ColumnInfo(name = "time")
    public String time;
    private static final DateTimeFormatter TIME_FORMAT = ISODateTimeFormat.dateTimeNoMillis();
    public DateTime getTime() {
        return TIME_FORMAT.parseDateTime(time);
    }
    public void setTime(DateTime time) {
        this.time = time.toString(TIME_FORMAT);
    }

    /**
     * Locally cached data.
     *  - not considered in equals(), hashCode()
     *  - all data here should have default initialization (for newly downloaded sermons)
     */
    public static class Local {
        public enum DownloadState {
            NONE,
            DOWNLOADING,
            DOWNLOADED,
            FAILED;
            public static class Converter {
                @TypeConverter
                public static DownloadState toDownloadState(Integer state) {
                    return DownloadState.values()[state];
                }
                @TypeConverter
                public static Integer toInteger(DownloadState state) {
                    return state.ordinal();
                }
            }
        }
        @TypeConverters(DownloadState.Converter.class)
        @ColumnInfo(name = "local_download_state")
        public DownloadState downloadState = DownloadState.NONE;

        @Override
        public boolean equals(Object that) {
            return this == that
                    || (that != null
                    && this.getClass() == that.getClass()
                    && this.downloadState == ((Local) that).downloadState);
        }

        @Override
        public int hashCode() {
            return downloadState.hashCode();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("download_state", downloadState)
                    .toString();
        }
    }
    @NonNull @Embedded
    public Local local = new Local();

    // Object equality

    @Override
    public boolean equals(Object that) {
        return this == that
                || (that != null
                && this.getClass() == that.getClass()
                && this.id.equals(((Sermon) that).id)
                && this.local.equals(((Sermon) that).local));
    }

    @Override
    public int hashCode() {
        return id.hashCode() + local.hashCode();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("audio", audio)
                .add("series", series)
                .add("title", title)
                .add("passage", passage)
                .add("speaker", speaker)
                .add("time", time)
                .add("local", local)
                .toString();
    }

    // Read from JSON

    public static Sermon readSermon(JSONObject obj) throws JSONException {
        Sermon sermon = new Sermon();
        sermon.audio = obj.getString("audio");
        sermon.series = obj.getString("series");
        sermon.title = obj.getString("title");
        sermon.passage = obj.getString("passage");
        sermon.speaker = obj.getString("speaker");
        sermon.time = obj.getString("time");
        sermon.recomputeId();
        return sermon;
    }

    public static List<Sermon> readSermons(JSONObject obj) throws JSONException {
        JSONArray objSermons = obj.getJSONArray("sermons");
        List<Sermon> sermons = new ArrayList<>(objSermons.length());
        for (int i = 0; i < objSermons.length(); ++i) {
            sermons.add(readSermon(objSermons.getJSONObject(i)));
        }
        return sermons;
    }
}
