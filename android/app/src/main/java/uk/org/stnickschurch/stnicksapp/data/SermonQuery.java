package uk.org.stnickschurch.stnicksapp.data;

import android.support.annotation.NonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * A free text query for sermons.
 */
public class SermonQuery {
    public final @NonNull String search_text;
    public final boolean downloaded_only;

    public SermonQuery(@NonNull String search_text, boolean downloaded_only) {
        this.search_text = search_text;
        this.downloaded_only = downloaded_only;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(search_text, downloaded_only);
    }

    @Override
    public boolean equals(Object that_) {
        if (that_ == null || this.getClass() != that_.getClass()) {
            return false;
        }
        SermonQuery that = (SermonQuery) that_;
        return Objects.equal(this.search_text, that.search_text)
                && this.downloaded_only == that.downloaded_only;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("search_text", search_text)
                .add("downloaded_only", downloaded_only)
                .toString();
    }
}
