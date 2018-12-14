package uk.org.stnickschurch.stnicksapp.core;

import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Arrays;

import uk.org.stnickschurch.stnicksapp.data.ListRefresh;
import uk.org.stnickschurch.stnicksapp.data.Sermon;
import uk.org.stnickschurch.stnicksapp.data.SermonQuery;
import uk.org.stnickschurch.stnicksapp.data.StringWithSnippet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

@RunWith(AndroidJUnit4.class)
public class StoreTest {
    @Test
    public void parseTime() {
        assertThat(Store.ISO_TIME_FORMAT.parseDateTime("2018-03-11T11:00:00+00:00"),
                equalTo(new DateTime(2018, 3, 11, 11, 0, 0, DateTimeZone.UTC)));
        assertThat(Store.ISO_TIME_FORMAT.parseDateTime("2018-03-11T11:00:00+01:00"),
                equalTo(new DateTime(2018, 3, 11, 11, 0, 0, DateTimeZone.forID("+01:00"))));
    }

    private JSONObject mSyncResponse;

    @Before
    public void setUp() throws JSONException {
        JSONObject syncSermon1 = new JSONObject()
                .put("id", 0)
                .put("passage", "Luke 15:25-32")
                .put("time", "2018-03-11T11:00:00+00:00")
                .put("series", "Tales Of The Unexpected")
                .put("title", "Tales Of The Unexpected 4")
                .put("speaker", "Chris Fishlock")
                .put("audio", "https://stnickschurch.s3.amazonaws.com/abc%20def.mp4");
        JSONObject syncSermon2 = new JSONObject()
                .put("id", 1)
                .put("passage", "Luke 15:11-24")
                .put("time", "2018-03-04T11:00:00+00:00")
                .put("series", "Tales Of The Unexpected")
                .put("title", "Tales Of The Unexpected 3")
                .put("speaker", "Chris Fishlock")
                .put("audio", "https://stnickschurch.s3.amazonaws.com/an%20other.mp4");
        mSyncResponse = new JSONObject()
                .put("sermons", new JSONArray().put(syncSermon1).put(syncSermon2));
    }

    @Test
    public void listSermons() throws JSONException {
        Store.Db dbHelper = new Store.Db(InstrumentationRegistry.getContext(), null);
        Store.doSync(dbHelper.getWritableDatabase(), mSyncResponse);

        assertThat(Store.doListSermons(dbHelper.getReadableDatabase(), new SermonQuery("", false)),
                equalTo(Arrays.asList(
                        new Sermon(0,
                                new StringWithSnippet("Luke 15:25-32", null),
                                new DateTime(2018, 3, 11, 11, 0, 0, DateTimeZone.UTC),
                                new StringWithSnippet("Tales Of The Unexpected", null),
                                new StringWithSnippet("Tales Of The Unexpected 4", null),
                                new StringWithSnippet("Chris Fishlock", null),
                                Sermon.DownloadState.NONE
                        ),
                        new Sermon(1,
                                new StringWithSnippet("Luke 15:11-24", null),
                                new DateTime(2018, 3, 4, 11, 0, 0, DateTimeZone.UTC),
                                new StringWithSnippet("Tales Of The Unexpected", null),
                                new StringWithSnippet("Tales Of The Unexpected 3", null),
                                new StringWithSnippet("Chris Fishlock", null),
                                Sermon.DownloadState.NONE
                        ))));

        assertThat(Store.doListSermons(dbHelper.getReadableDatabase(), new SermonQuery("luke 15 of Fishlock \"unexpected 3\"", false)),
                equalTo(Arrays.asList(
                        new Sermon(1,
                                new StringWithSnippet("Luke 15:11-24", "<b>Luke</b> <b>15</b>:11-24"),
                                new DateTime(2018, 3, 4, 11, 0, 0, DateTimeZone.UTC),
                                new StringWithSnippet("Tales Of The Unexpected", "Tales <b>Of</b> The Unexpected"),
                                new StringWithSnippet("Tales Of The Unexpected 3", "Tales <b>Of</b> The <b>Unexpected</b> <b>3</b>"),
                                new StringWithSnippet("Chris Fishlock", "Chris <b>Fishlock</b>"),
                                Sermon.DownloadState.NONE
                        ))));

        assertThat("prefix query",
                Store.doListSermons(dbHelper.getReadableDatabase(), new SermonQuery("fish", false)),
                hasSize(2));

        assertThat("non-prefix query",
                Store.doListSermons(dbHelper.getReadableDatabase(), new SermonQuery("fish ", false)),
                hasSize(0));

        assertThat("unbalanced \"",
                Store.doListSermons(dbHelper.getReadableDatabase(), new SermonQuery("\"the un", false)),
                hasSize(2));

        assertThat("disallowed syntax NEAR",
                Store.doListSermons(dbHelper.getReadableDatabase(), new SermonQuery("NEAR ", false)),
                hasSize(0));

        assertThat("passage phrase search",
                Store.doListSermons(dbHelper.getReadableDatabase(), new SermonQuery("\"luke 15:11-24\"", false)),
                hasSize(1));
    }

    @Test
    public void sync() throws JSONException {
        Store.Db dbHelper = new Store.Db(InstrumentationRegistry.getContext(), null);

        // Add 2
        assertThat(Store.doSync(dbHelper.getWritableDatabase(), mSyncResponse),
                equalTo(new ListRefresh(2, 0, 0)));

        assertThat(Store.doListSermons(dbHelper.getReadableDatabase(), new SermonQuery("", false)),
                hasSize(2));
        assertThat(Store.doGetSermon(dbHelper.getReadableDatabase(), 1).speaker,
                equalTo(new StringWithSnippet("Chris Fishlock", null)));

        // Update 1
        mSyncResponse.getJSONArray("sermons")
                .getJSONObject(1)
                .put("speaker", "Fish Chrislock");
        assertThat(Store.doSync(dbHelper.getWritableDatabase(), mSyncResponse),
                equalTo(new ListRefresh(0, 1, 0)));

        assertThat(Store.doGetSermon(dbHelper.getReadableDatabase(), 1).speaker,
                equalTo(new StringWithSnippet("Fish Chrislock", null)));

        // Remove 1
        mSyncResponse.getJSONArray("sermons").remove(0);
        assertThat(Store.doSync(dbHelper.getWritableDatabase(), mSyncResponse),
                equalTo(new ListRefresh(0, 0, 1)));

        assertThat(Store.doGetSermon(dbHelper.getReadableDatabase(), 0), nullValue());

        dbHelper.close();
    }

    @Test
    public void download() throws JSONException {
        Store.Db dbHelper = new Store.Db(InstrumentationRegistry.getContext(), null);
        Store.doSync(dbHelper.getWritableDatabase(), mSyncResponse);

        assertThat(Store.doGetAudio(dbHelper.getReadableDatabase(), 1, false),
                equalTo(Uri.parse("https://stnickschurch.s3.amazonaws.com/an%20other.mp4")));

        // startDownload
        Store.doStartDownload(dbHelper.getWritableDatabase(), 1, 123);

        assertThat(Store.doGetAudio(dbHelper.getReadableDatabase(), 1, false),
                equalTo(Uri.parse("https://stnickschurch.s3.amazonaws.com/an%20other.mp4")));

        // finishDownload
        Sermon sermon = Store.doFinishDownload(dbHelper.getWritableDatabase(), 123,
                new File("foobar/Tales Of 3.mp4"));
        assertThat(sermon.title.toString(), equalTo("Tales Of The Unexpected 3"));
        assertThat(sermon.download, equalTo(Sermon.DownloadState.DOWNLOADED));

        assertThat(Store.doGetAudio(dbHelper.getReadableDatabase(), 1, false),
                equalTo(Uri.fromFile(new File("foobar/Tales Of 3.mp4"))));
        assertThat(Store.doGetAudio(dbHelper.getReadableDatabase(), 1, true),
                equalTo(Uri.parse("https://stnickschurch.s3.amazonaws.com/an%20other.mp4")));

        // can't finishDownload twice
        assertThat(Store.doFinishDownload(dbHelper.getWritableDatabase(), 123,
                new File("different/Tales Of 3.mp4")),
                nullValue());

        assertThat(Store.doGetAudio(dbHelper.getReadableDatabase(), 1, false),
                equalTo(Uri.fromFile(new File("foobar/Tales Of 3.mp4"))));

        // deleteDownload
        File delete = Store.doDeleteDownload(dbHelper.getWritableDatabase(), 1);
        assertThat(delete, equalTo(new File("foobar/Tales Of 3.mp4")));

        // deleteDownload of already deleted
        assertThat(Store.doDeleteDownload(dbHelper.getWritableDatabase(), 1), nullValue());
    }
}
