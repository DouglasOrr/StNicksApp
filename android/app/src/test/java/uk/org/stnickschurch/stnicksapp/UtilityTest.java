package uk.org.stnickschurch.stnicksapp;

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

        assertThat(Utility.PERIOD_SHORT_FORMAT.parsePeriod("5d"),
                is(Period.days(5)));

        assertThat(Utility.PERIOD_SHORT_FORMAT.parsePeriod("2h"),
                is(Period.hours(2)));

        assertThat(Utility.PERIOD_SHORT_FORMAT.parsePeriod("30m"),
                is(Period.minutes(30)));

        assertThat(Utility.PERIOD_SHORT_FORMAT.parsePeriod("5s"),
                is(Period.seconds(5)));

        assertThat(Utility.PERIOD_SHORT_FORMAT.parsePeriod("120ms"),
                is(Period.millis(120)));

        assertThat(Utility.PERIOD_SHORT_FORMAT.parsePeriod("1d2h30m5s10ms"),
                is(new Period(0, 0, 0, 1, 2, 30, 5, 10)));

        // Durations can vary in length, but seconds & milliseconds don't
        // (leap seconds are the shortest practical variation)
        assertThat(Utility.getPeriodMs("5s100ms"), is(5100L));
    }
}
