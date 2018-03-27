package uk.org.stnickschurch.stnicksapp.core;

import android.support.annotation.NonNull;
import android.support.v7.recyclerview.extensions.ListAdapter;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.common.base.Objects;

import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
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
     * Create an Observable for a JSON request, which is made every time someone subscribes.
     *
     * @see JsonObjectRequest
     */
    public static Observable<JSONObject> request(
            final RequestQueue queue, final int method, final String url,
            final JSONObject request, final Map<String, String> headers) {
        return Observable.create(new ObservableOnSubscribe<JSONObject>() {
            @Override
            public void subscribe(final ObservableEmitter<JSONObject> emitter) throws Exception {
                queue.add(new JsonObjectRequest(
                    method, url, request,
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
