package uk.org.stnickschurch.stnicksapp.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Objects;

/**
 * Holds a string that can optionally be annotated with a snippet.
 */
public class StringWithSnippet {
    public final @NonNull String text;
    public final @Nullable String snippet;

    public StringWithSnippet(@NonNull String text, @Nullable String snippet) {
        this.text = text;
        this.snippet = snippet;
    }

    public String getSnippetOrText() {
        return snippet != null ? snippet : text;
    }

    public boolean hasSnippet() {
        return snippet != null;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(text, snippet);
    }

    @Override
    public boolean equals(Object that_) {
        if (that_ == null || this.getClass() != that_.getClass()) {
            return false;
        }
        StringWithSnippet that = (StringWithSnippet) that_;
        return Objects.equal(this.text, that.text)
                && Objects.equal(this.snippet, that.snippet);
    }

    @Override
    public String toString() {
        return getSnippetOrText();
    }
}
