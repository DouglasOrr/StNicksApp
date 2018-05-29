package uk.org.stnickschurch.stnicksapp.core;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.File;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

/**
 * Issue REST & other HTTP requests.
 */
public class Downloader {
    private final RequestQueue mQueue;
    private final File mDownloadCache;
    private final Scheduler mDownloader;

    public Observable<JSONObject> getRequest(String url, Map<String, String> headers) {
        return Utility.request(mQueue, Request.Method.GET, url, headers);
    }

    public Observable<JSONObject> cachedGetRequest(String url, Map<String, String> headers, long refreshMs) {
        return Utility.requestCachedGet(mQueue, url, headers, mDownloader, mDownloadCache, refreshMs);
    }

    private Downloader(Context context) {
        mQueue = Volley.newRequestQueue(context);
        mDownloadCache = new File(context.getFilesDir(), "cache");
        mDownloadCache.mkdirs();
        mDownloader = Schedulers.newThread();
    }

    public static final Singleton<Downloader> SINGLETON = new Singleton<Downloader>() {
        @Override
        Downloader newInstance(Context context) {
            return new Downloader(context);
        }
    };
}
