package uk.org.stnickschurch.stnicksapp.core;

import android.support.annotation.NonNull;
import android.support.v7.recyclerview.extensions.ListAdapter;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ViewGroup;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.common.base.Objects;

import org.json.JSONObject;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import uk.org.stnickschurch.stnicksapp.MediaFragment;

public class Utility {
    /**
     * Create an Observable for a JSON request, which is made every time someone subscribes.
     *
     * @see JsonObjectRequest
     */
    public static Observable<JSONObject> request(final RequestQueue queue, final int method, final String url, final JSONObject request) {
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
                ));
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

    public static abstract class ObserverListAdapter<T, TH extends RecyclerView.ViewHolder>
            extends ListAdapter<T, TH> implements Observer<List<T>> {
        protected ObserverListAdapter(@NonNull DiffUtil.ItemCallback<T> diffCallback) {
            super(diffCallback);
        }

        @Override
        public void onSubscribe(Disposable d) { }

        @Override
        public void onNext(List<T> items) {
            submitList(items);
        }

        @Override
        public void onError(Throwable e) {
            log("Observe error %s", e);
        }

        @Override
        public void onComplete() { }
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

    public static void log(String format, Object... args) {
        Log.d("StNicksApp", String.format(format, args));
    }
}
