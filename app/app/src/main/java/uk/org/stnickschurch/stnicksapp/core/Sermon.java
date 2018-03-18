package uk.org.stnickschurch.stnicksapp.core;

import com.google.common.base.MoreObjects;
import com.google.common.io.BaseEncoding;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sermon {
    public final String id;
    public final String audio;
    public final String series;
    public final String title;
    public final String passage;
    public final String speaker;
    public final DateTime time;

    private String calculateId() {
        try {
            Charset utf8 = Charset.forName("UTF-8");
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(this.series.getBytes(utf8));
            digest.update(this.title.getBytes(utf8));
            digest.update(this.passage.getBytes(utf8));
            digest.update(this.speaker.getBytes(utf8));
            digest.update(time.toString(ISODateTimeFormat.dateTime()).getBytes(utf8));
            digest.update(this.audio.getBytes(utf8));
            return BaseEncoding.base32Hex().encode(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new Utility.ReallyBadError("Missing MD5", e);
        }
    }

    public Sermon(String audio, String series, String title, String passage, DateTime time, String speaker) {
        this.series = series;
        this.title = title;
        this.passage = passage;
        this.time = time;
        this.speaker = speaker;
        this.audio = audio;
        this.id = calculateId();
    }

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
                .add("series", series)
                .add("title", title)
                .add("passage", passage)
                .add("speaker", speaker)
                .add("time", time)
                .add("audio", audio)
                .toString();
    }

    public static Sermon fromJson(JSONObject obj) throws JSONException {
        return new Sermon(
                obj.getString("audio"),
                obj.getString("series"),
                obj.getString("title"),
                obj.getString("passage"),
                ISODateTimeFormat.dateTimeNoMillis().parseDateTime(obj.getString("time")),
                obj.getString("speaker")
        );
    }
}
