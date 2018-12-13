package uk.org.stnickschurch.stnicksapp.core;

import android.content.Context;

import io.reactivex.Observable;
import io.reactivex.functions.Predicate;
import io.reactivex.subjects.PublishSubject;

public class Events {
    public enum Level { DEBUG, INFO, ERROR }
    public static class Event {
        public final Level level;
        public final String message;
        public Event(Level level, String message) {
            this.level = level;
            this.message = message;
        }
    }

    private final PublishSubject<Event> mEvents = PublishSubject.create();

    public void publish(Level level, int messageId, Object... args) {
        mEvents.onNext(new Event(level, mContext.getString(messageId, args)));
    }

    public Observable<Event> events(Level minLevel) {
        return mEvents.filter(new Predicate<Event>() {
            @Override
            public boolean test(Event event) {
                return minLevel.ordinal() <= event.level.ordinal();
            }
        });
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
