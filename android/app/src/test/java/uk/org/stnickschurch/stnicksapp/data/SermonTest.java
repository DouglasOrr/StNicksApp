package uk.org.stnickschurch.stnicksapp.data;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class SermonTest {
    @Test
    public void valueLike() {
        Sermon a = new Sermon(0,
                new StringWithSnippet("Luke 10:1-10", null),
                new DateTime(2018, 7, 1, 11, 0, 0),
                new StringWithSnippet("Tales of the Unexpected", "Tales of the <b>Unexpected</b>"),
                new StringWithSnippet(("Tales of the Unexpected 3"), null),
                new StringWithSnippet("Chris Fishlock", null),
                Sermon.DownloadState.DOWNLOADED);

        assertThat(a, equalTo(a));
        for (String contents : new String[] {"Luke", "Fishlock", "Tales of the <b>Unexpected</b>"}) {
            assertThat(a.toString(), containsString(contents));
        }
    }
}
