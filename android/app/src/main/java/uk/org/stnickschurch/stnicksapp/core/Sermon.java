package uk.org.stnickschurch.stnicksapp.core;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
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
        @ColumnInfo(name = "local_downloaded")
        public boolean downloaded = false;
    }
    @NonNull @Embedded
    public Local local = new Local();

    // Object identity

    @Override
    public boolean equals(Object that) {
        return this == that
                || (that != null
                && this.getClass() == that.getClass()
                && this.id.equals(((Sermon) that).id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
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
                .add("local_downloaded", local.downloaded)
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
