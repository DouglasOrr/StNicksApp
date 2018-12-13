package uk.org.stnickschurch.stnicksapp.core;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.common.io.Files;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

/**
 * Issue REST & other HTTP requests.
 */
public class Downloader {
    private final RequestQueue mQueue;
    private final File mDownloadCache;
    private final Scheduler mDownloader;

    public Observable<JSONObject> getRequest(final String url, final Map<String, String> headers) {
        return Observable.create(new ObservableOnSubscribe<JSONObject>() {
            @Override
            public void subscribe(final ObservableEmitter<JSONObject> emitter) {
                mQueue.add(new JsonObjectRequest(
                        Request.Method.GET, url, null,
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
                    public Map<String, String> getHeaders() {
                        return headers == null ? Collections.<String, String> emptyMap() : headers;
                    }
                });
            }
        });
    }

    public Observable<JSONObject> cachedGetRequest(final String url, final Map<String, String> headers, final long refreshMs) {
        final BehaviorSubject<JSONObject> result = BehaviorSubject.create();
        mDownloader.scheduleDirect(new Runnable() {
            @Override
            public void run() {
                final File local = new File(mDownloadCache, Utility.md5(url) + ".json");
                boolean requiresRefresh = true;

                // First see if we have a cache hit
                if (local.exists()) {
                    requiresRefresh = local.lastModified() + refreshMs < System.currentTimeMillis();
                    try {
                        String json = new String(Files.asByteSource(local).read(), Charset.forName("UTF-8"));
                        result.onNext(new JSONObject(json));
                    } catch (IOException e) {
                        Utility.log("Cache error %s", e.getMessage());
                        requiresRefresh = true;
                    } catch (JSONException e) {
                        Utility.log("Cache error %s", e.getMessage());
                        requiresRefresh = true;
                    }
                }

                // If needed, perform the refresh
                if (requiresRefresh) {
                    getRequest(url, headers)
                            .subscribeOn(mDownloader)
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

    private Downloader(Context context) {
        mQueue = Volley.newRequestQueue(context);
        mDownloadCache = new File(context.getFilesDir(), "cache");
        mDownloadCache.mkdirs();
        mDownloader = Schedulers.newThread();
    }

    public static final Singleton<Downloader> SINGLETON = new Singleton<Downloader>() {
        @Override
        protected Downloader newInstance(Context context) {
            return new Downloader(context);
        }
    };
}
