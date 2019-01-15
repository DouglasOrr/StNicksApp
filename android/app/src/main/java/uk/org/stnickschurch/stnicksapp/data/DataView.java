package uk.org.stnickschurch.stnicksapp.data;

import com.google.common.base.Joiner;

import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import uk.org.stnickschurch.stnicksapp.PlaybackService;

/**
 * Methods that define standard views of data.
 */
public class DataView {
    /**
     * The title to use for popups and notifications.
     */
    public static String uiTitle(Sermon sermon) {
        String title = sermon.passage.getSnippetOrText();
        if (title.length() == 0) {
            title = sermon.title.getSnippetOrText();
            if (title.length() == 0) {
                title = sermon.series.getSnippetOrText();
                if (title.length() == 0) {
                    title = sermon.speaker.getSnippetOrText();
                }
            }
        }
        return title;
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

    private static final PeriodFormatter DURATION_FORMAT = new PeriodFormatterBuilder()
            .appendHours().appendSeparator(":")
            .minimumPrintedDigits(2).printZeroAlways()
            .appendMinutes().appendSeparator(":")
            .appendSeconds()
            .toFormatter();
    private static String printDuration(int ms) {
        return DURATION_FORMAT.print(new Period(ms, PeriodType.time()));
    }
    /**
     * Elapsed and total time of a sermon.
     */
    public static String elapsedAndTotal(PlaybackService.Progress progress) {
        return printDuration(progress.position) + " / " + printDuration(progress.max);
    }
}
