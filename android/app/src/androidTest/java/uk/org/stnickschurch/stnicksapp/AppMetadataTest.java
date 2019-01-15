package uk.org.stnickschurch.stnicksapp;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AppMetadataTest {
    @Test
    public void packageName() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertEquals(BuildConfig.DEBUG
                ? "uk.org.stnickschurch.stnicksapp.debug"
                : "uk.org.stnickschurch.stnicksapp",
                appContext.getPackageName());
    }
}
