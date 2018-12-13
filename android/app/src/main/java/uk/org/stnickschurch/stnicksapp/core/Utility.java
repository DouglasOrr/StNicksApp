package uk.org.stnickschurch.stnicksapp.core;

import android.support.annotation.NonNull;
import android.support.v7.recyclerview.extensions.ListAdapter;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;

import com.android.volley.BuildConfig;
import com.google.common.io.BaseEncoding;

import org.joda.time.Instant;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;

public class Utility {
    /**
     * Provide a short word-based format for periods such as:
     * "3h", "1m30s", "100ms"
     */
    public static final PeriodFormatter PERIOD_SHORT_FORMAT =
            new PeriodFormatterBuilder()
                    .printZeroRarelyLast()
                    .rejectSignedValues(true)
                    .appendYears()
                    .appendSuffix("y")
                    .appendMonths()
                    .appendSuffix("M")
                    .appendWeeks()
                    .appendSuffix("w")
                    .appendDays()
                    .appendSuffix("d")
                    .appendHours()
                    .appendSuffix("h")
                    .appendMinutes()
                    .appendSuffix("m")
                    .appendSeconds()
                    .appendSuffix("s")
                    .appendMillis()
                    .appendSuffix("ms")
                    .toFormatter();

    /**
     * Parse a period according to {@link #PERIOD_SHORT_FORMAT}.
     */
    public static Period getPeriod(String period) {
        return Utility.PERIOD_SHORT_FORMAT.parsePeriod(period.replace(" ", ""));
    }

    /**
     * Get the number of ms in a period, starting from now.
     */
    public static long getPeriodMs(String period) {
        return getPeriod(period).toDurationFrom(Instant.now()).getMillis();
    }

    /**
     * Compute & return the MD5 sum of a list of strings (order-dependent),
     * formatted as Base-32 HEX.
     */
    public static String md5(String... strings) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            Charset utf8 = Charset.forName("UTF-8");
            for (String s : strings) {
                digest.update(s.getBytes(utf8));
            }
            return BaseEncoding.base32Hex().encode(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new Utility.ReallyBadError("Missing MD5", e);
        }
    }

    /**
     * A generic unchecked error, which we don't think should happen.
     */
    public static class ReallyBadError extends RuntimeException {
        public ReallyBadError(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static <T extends Exception> void nonFatal(T e) throws T {
        if (BuildConfig.DEBUG) {
            throw e;
        } else {
            Utility.log("Caught nonfatal error %s", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create a bifunction that puts the arguments in an android.util.Pair.
     */
    public static <A, B> BiFunction<A, B, Pair<A, B>> toPair() {
        return new BiFunction<A, B, Pair<A, B>>() {
            @Override
            public Pair<A, B> apply(A a, B b) throws Exception {
                return new Pair<>(a, b);
            }
        };
    }

    /**
     * Implement Consumer & ListAdapter, to create a base adapter class that can
     * subscribe to an observable list of items.
     */
    public static abstract class ObserverListAdapter<T, TH extends RecyclerView.ViewHolder>
            extends ListAdapter<T, TH> implements Consumer<List<T>> {
        protected ObserverListAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback) {
            super(diffCallback);
        }
        @Override
        public void accept(List<T> items) {
            submitList(items);
        }
    }

    /**
     * General logging helper.
     */
    public static void log(String format, Object... args) {
        Log.d("StNicksApp", String.format(format, args));
    }
}
