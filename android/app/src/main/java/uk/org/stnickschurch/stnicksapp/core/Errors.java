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

    public static final Singleton<Errors> SINGLETON = new Singleton<Errors>() {
        @Override
        Errors newInstance(Context context) {
            return new Errors(context);
        }
    };
}
