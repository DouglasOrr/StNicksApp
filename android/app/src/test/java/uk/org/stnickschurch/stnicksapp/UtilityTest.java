package uk.org.stnickschurch.stnicksapp;

import org.joda.time.Instant;
import org.joda.time.Period;
import org.junit.Test;

import uk.org.stnickschurch.stnicksapp.core.Utility;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UtilityTest {
    @Test
    public void periodFormat() {
        assertThat(Utility.PERIOD_SHORT_FORMAT.parsePeriod("2y"),
                is(Period.years(2)));

        assertThat(Utility.PERIOD_SHORT_FORMAT.parsePeriod("4M"),
                is(Period.months(4)));

        assertThat(Utility.PERIOD_SHORT_FORMAT.parsePeriod("3w"),
                is(Period.weeks(3)));

        assertThat(Utility.PERIOD_SHORT_FORMAT.parsePeriod("2h"),
                is(Period.hours(2)));

        assertThat(Utility.PERIOD_SHORT_FORMAT.parsePeriod("30m"),
                is(Period.minutes(30)));

        assertThat(Utility.PERIOD_SHORT_FORMAT.parsePeriod("5s"),
                is(Period.seconds(5)));

        assertThat(Utility.PERIOD_SHORT_FORMAT.parsePeriod("120ms"),
                is(Period.millis(120)));

        assertThat(Utility.PERIOD_SHORT_FORMAT.parsePeriod("2h30m5s10ms"),
                is(new Period(0, 0, 0, 0, 2, 30, 5, 10)));

        // Durations can vary in length, but hours, minutes, seconds & milliseconds don't
        // (even leap seconds & timezone hour changes shouldn't matter here)
        assertThat(Period.seconds(5).toDurationFrom(Instant.now()).getMillis(), is(5000L));
    }
}
