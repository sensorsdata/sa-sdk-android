package com.sensorsdata.analytics.android.sdk;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@SuppressLint("CommitPrefEdits")
/* package */ abstract class PersistentIdentity<T> {

    interface PersistentSerializer<T> {
        T load(final String value);

        String save(T item);

        T create();
    }

    PersistentIdentity(final Future<SharedPreferences> loadStoredPreferences, final String
            persistentKey, final PersistentSerializer<T> serializer) {
        this.loadStoredPreferences = loadStoredPreferences;
        this.serializer = serializer;
        this.persistentKey = persistentKey;
    }

    T get() {
        if (this.item == null) {
            String data = null;
            synchronized (loadStoredPreferences) {
                try {
                    SharedPreferences sharedPreferences = loadStoredPreferences.get();
                    if (sharedPreferences != null) {
                        data = sharedPreferences.getString(persistentKey, null);
                    }
                } catch (final ExecutionException e) {
                    Log.e(LOGTAG, "Cannot read distinct ids from sharedPreferences.", e.getCause());
                } catch (final InterruptedException e) {
                    Log.e(LOGTAG, "Cannot read distinct ids from sharedPreferences.", e);
                }

                T item = null;
                if (data == null) {
                    item = (T) serializer.create();
                } else {
                    item = (T) serializer.load(data);
                }

                if (item != null) {
                    commit(item);
                }
            }
        }
        return this.item;
    }

    void commit(T item) {
        this.item = item;

        synchronized (loadStoredPreferences) {
            SharedPreferences sharedPreferences = null;
            try {
                sharedPreferences = loadStoredPreferences.get();
            } catch (final ExecutionException e) {
                Log.e(LOGTAG, "Cannot read distinct ids from sharedPreferences.", e.getCause());
            } catch (final InterruptedException e) {
                Log.e(LOGTAG, "Cannot read distinct ids from sharedPreferences.", e);
            }

            if (sharedPreferences == null) {
                return;
            }

            final SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(persistentKey, serializer.save(this.item));

            if (Build.VERSION.SDK_INT >= 9) {
                editor.apply();
            } else {
                editor.commit();
            }
        }
    }

    private static final String LOGTAG = "SA.PersistentIdentity";

    private final Future<SharedPreferences> loadStoredPreferences;
    private final PersistentSerializer serializer;
    private final String persistentKey;

    private T item;
}
