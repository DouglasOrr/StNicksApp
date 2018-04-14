package uk.org.stnickschurch.stnicksapp.core;

import android.content.Context;

import io.reactivex.subjects.PublishSubject;

public class Errors {
    public final PublishSubject<String> errors = PublishSubject.create();
    private final Context mContext;
    private Errors(Context context) {
        mContext = context;
    }
    public void publish(int id) {
        errors.onNext(mContext.getString(id));
    }

    private static final Object SINGLETON_LOCK = new Object();
    private static Errors mSingleton = null;
    public static Errors get(Context context) {
        if (mSingleton != null) {
            return mSingleton;
        }
        synchronized (SINGLETON_LOCK) {
            if (mSingleton == null) {
                mSingleton = new Errors(context.getApplicationContext());
            }
            return mSingleton;
        }
    }
}
