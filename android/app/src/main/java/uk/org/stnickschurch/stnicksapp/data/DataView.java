package uk.org.stnickschurch.stnicksapp.data;

import com.google.common.base.Joiner;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Methods that define standard views of data.
 */
public class DataView {
    /**
     * The title to use for popups and notifications.
     */
    public static String uiTitle(Sermon sermon) {
        return sermon.passage.getSnippetOrText();
    }
    /**
     * Medium-verbose information for popups and notifications.
     */
    public static String shortDescription(Sermon sermon) {
        return sermon.title.getSnippetOrText();
    }
    /**
     * Verbose description, for notifications & popups.
     */
    public static String longDescription(Sermon sermon, String separator) {
        return Joiner.on(separator).join(
                sermon.title.getSnippetOrText(), sermon.speaker.getSnippetOrText(), date(sermon));
    }

    private static final DateTimeFormatter USER_TIME_FORMAT = DateTimeFormat.forPattern("EEE d MMMM y");
    /**
     * The date of the sermon.
     */
    public static String date(Sermon sermon) {
        return sermon.time.toString(USER_TIME_FORMAT);
    }
}
