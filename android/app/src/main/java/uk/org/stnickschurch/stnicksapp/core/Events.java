package uk.org.stnickschurch.stnicksapp.core;

import android.content.Context;

import io.reactivex.subjects.PublishSubject;

public class Events {
    public final PublishSubject<String> errors = PublishSubject.create();
    public void publishError(int id) {
        errors.onNext(mContext.getString(id));
    }
    public final PublishSubject<String> messages = PublishSubject.create();
    public void publishMessage(int id) {
        messages.onNext(mContext.getString(id));
    }

    private final Context mContext;
    private Events(Context context) {
        mContext = context;
    }
    public static final Singleton<Events> SINGLETON = new Singleton<Events>() {
        @Override
        protected Events newInstance(Context context) {
            return new Events(context);
        }
    };
}
