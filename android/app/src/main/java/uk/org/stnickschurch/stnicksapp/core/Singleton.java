package uk.org.stnickschurch.stnicksapp.core;

import android.content.Context;

/**
 * Provides standard implementation of a lazy singleton, which may require application context.
 *
 * Usage:
 * <pre>{@code
 *     public class MyClass {
 *         MyClass(Context context) { ... }
 *
 *         public static final Singleton<MyClass> SINGLETON = new Singleton<MyClass> {
 *             MyClass newInstance(Context context) {
 *                 return new MyClass(context);
 *             }
 *         }
 *     }
 * </pre>
 * @param <T>
 */
public abstract class Singleton<T> {
    private final Object INSTANCE_LOCK = new Object();
    private T mInstance = null;

    public T get(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        synchronized (INSTANCE_LOCK) {
            if (mInstance == null) {
                mInstance = newInstance(context.getApplicationContext());
            }
            return mInstance;
        }
    }

    protected abstract T newInstance(Context context);
}
