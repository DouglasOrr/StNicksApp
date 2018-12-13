package uk.org.stnickschurch.stnicksapp.data;

import android.support.annotation.NonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import org.joda.time.DateTime;

/**
 * Core datatype for a sermon, as displayed to the user.
 */
public class Sermon {
    public final long id;

    public final @NonNull StringWithSnippet passage;

    public final @NonNull DateTime time;

    public final @NonNull StringWithSnippet series;

    public final @NonNull StringWithSnippet title;

    public final @NonNull StringWithSnippet speaker;

    public enum DownloadState { NONE, ATTEMPTED, DOWNLOADED }
    public final DownloadState download;

    public Sermon(long id,
                  @NonNull StringWithSnippet passage,
                  @NonNull DateTime time,
                  @NonNull StringWithSnippet series,
                  @NonNull StringWithSnippet title,
                  @NonNull StringWithSnippet speaker,
                  DownloadState download) {
        this.id = id;
        this.passage = passage;
        this.time = time;
        this.series = series;
        this.title = title;
        this.speaker = speaker;
        this.download = download;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, passage, time, series, title, speaker, download);
    }

    @Override
    public boolean equals(Object that_) {
        if (that_ == null || this.getClass() != that_.getClass()) {
            return false;
        }
        Sermon that = (Sermon) that_;
        return this.id == that.id
                && Objects.equal(this.passage, that.passage)
                && Objects.equal(this.time, that.time)
                && Objects.equal(this.series, that.series)
                && Objects.equal(this.title, that.title)
                && Objects.equal(this.speaker, that.speaker)
                && this.download == that.download;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("passage", passage)
                .add("time", time)
                .add("series", series)
                .add("title", title)
                .add("speaker", speaker)
                .add("download", download)
                .toString();
    }
}
