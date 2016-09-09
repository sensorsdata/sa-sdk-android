package com.sensorsdata.analytics.android.sdk;

import android.os.Looper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Wrapper around a set that will throw RuntimeErrors if accessed in a thread that is not the main thread.
 */
public class UIThreadSet<T> {
    public UIThreadSet() {
        mSet = new HashSet<T>();
    }

    public void add(T item) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            throw new RuntimeException("Can't add an activity when not on the UI thread");
        }
        mSet.add(item);
    }

    public void remove(T item) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            throw new RuntimeException("Can't remove an activity when not on the UI thread");
        }
        mSet.remove(item);
    }

    public Set<T> getAll() {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            throw new RuntimeException("Can't remove an activity when not on the UI thread");
        }
        return Collections.unmodifiableSet(mSet);
    }

    public boolean isEmpty() {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            throw new RuntimeException("Can't check isEmpty() when not on the UI thread");
        }
        return mSet.isEmpty();
    }

    private Set<T> mSet;
}
