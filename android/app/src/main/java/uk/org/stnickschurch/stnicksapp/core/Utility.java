package uk.org.stnickschurch.stnicksapp.core;

import android.support.annotation.NonNull;
import android.support.v7.recyclerview.extensions.ListAdapter;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.common.base.Objects;
import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;

import org.joda.time.Instant;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;

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
     * Create an Observable for a JSON request, which is made every time someone subscribes.
     *
     * @see JsonObjectRequest
     */
    public static Observable<JSONObject> request(
            final RequestQueue queue, final int method, final String url,
            final Map<String, String> headers) {
        return Observable.create(new ObservableOnSubscribe<JSONObject>() {
            @Override
            public void subscribe(final ObservableEmitter<JSONObject> emitter) throws Exception {
                queue.add(new JsonObjectRequest(
                    method, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            emitter.onNext(response);
                            emitter.onComplete();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            emitter.onError(error);
                        }
                    }
                ) {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        return headers == null ? Collections.<String, String> emptyMap() : headers;
                    }
                });
            }
        });
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
     * As {@link #request(RequestQueue, int, String, Map)}, but caching on disk
     */
    public static Observable<JSONObject> requestCachedGet(
            final RequestQueue queue, final String url, final Map<String, String> headers,
            final Scheduler scheduler, final File cache, final long timeoutMs) {
        final BehaviorSubject<JSONObject> result = BehaviorSubject.create();
        scheduler.scheduleDirect(new Runnable() {
            @Override
            public void run() {
                final File local = new File(cache, md5(url) + ".json");
                boolean requiresRefresh = true;

                // First see if we have a cache hit
                if (local.exists()) {
                    requiresRefresh = local.lastModified() + timeoutMs < System.currentTimeMillis();
                    try {
                        String json = new String(Files.asByteSource(local).read(), Charset.forName("UTF-8"));
                        result.onNext(new JSONObject(json));
                    } catch (IOException e) {
                        log("Cache error %s", e.getMessage());
                        requiresRefresh = true;
                    } catch (JSONException e) {
                        log("Cache error %s", e.getMessage());
                        requiresRefresh = true;
                    }
                }

                // If needed, perform the refresh
                if (requiresRefresh) {
                    request(queue, Request.Method.GET, url, headers)
                            .subscribeOn(scheduler)
                            .subscribe(new Consumer<JSONObject>() {
                                @Override
                                public void accept(JSONObject response) throws Exception {
                                    Files.write(response.toString().getBytes(Charset.forName("UTF-8")), local);
                                    result.onNext(response);
                                }
                            });
                }
            }
        });
        return result;
    }

    /**
     * A generic unchecked error, which we don't think should happen.
     */
    public static class ReallyBadError extends RuntimeException {
        public ReallyBadError(String message, Throwable cause) {
            super(message, cause);
        }
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
     * A DiffUtil that delegates to item's .equals.
     */
    public static <T> DiffUtil.ItemCallback<T> identityDiff() {
        return new DiffUtil.ItemCallback<T>() {
            @Override
            public boolean areItemsTheSame(T oldItem, T newItem) {
                return areContentsTheSame(oldItem, newItem);
            }
            @Override
            public boolean areContentsTheSame(T oldItem, T newItem) {
                return Objects.equal(oldItem, newItem);
            }
        };
    }

    /**
     * General logging helper.
     */
    public static void log(String format, Object... args) {
        Log.d("StNicksApp", String.format(format, args));
    }
}
