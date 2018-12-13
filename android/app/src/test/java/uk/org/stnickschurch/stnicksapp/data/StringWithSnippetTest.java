package uk.org.stnickschurch.stnicksapp.data;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class StringWithSnippetTest {
    @Test
    public void valueLike() {
        StringWithSnippet a = new StringWithSnippet("one time", "one <b>time</b>");
        StringWithSnippet b = new StringWithSnippet("one time", null);

        assertThat(a, not(equalTo(b)));
        assertThat(a, equalTo(new StringWithSnippet("one time", "one <b>time</b>")));

        assertThat(a.hashCode(), not(equalTo(b.hashCode())));
        assertThat(a.toString(), equalTo("one <b>time</b>"));
        assertThat(b.toString(), equalTo("one time"));
    }
}
