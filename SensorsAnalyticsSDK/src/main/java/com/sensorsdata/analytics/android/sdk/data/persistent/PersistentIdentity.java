/*
 * Created by wangzhuozhou on 2015/08/01.
 * Copyright 2015－2019 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.sensorsdata.analytics.android.sdk.data.persistent;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Build;

import com.sensorsdata.analytics.android.sdk.SALog;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@SuppressLint("CommitPrefEdits")
public abstract class PersistentIdentity<T> {

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

    public T get() {
        if (this.item == null) {
            String data = null;
            synchronized (loadStoredPreferences) {
                try {
                    SharedPreferences sharedPreferences = loadStoredPreferences.get();
                    if (sharedPreferences != null) {
                        data = sharedPreferences.getString(persistentKey, null);
                    }
                } catch (final ExecutionException e) {
                    SALog.d(LOGTAG, "Cannot read distinct ids from sharedPreferences.", e.getCause());
                } catch (final InterruptedException e) {
                    SALog.d(LOGTAG, "Cannot read distinct ids from sharedPreferences.", e);
                }

                if (data == null) {
                    item = (T) serializer.create();
                } else {
                    item = (T) serializer.load(data);
                }
            }
        }
        return this.item;
    }

    public void commit(T item) {
        this.item = item;

        synchronized (loadStoredPreferences) {
            SharedPreferences sharedPreferences = null;
            try {
                sharedPreferences = loadStoredPreferences.get();
            } catch (final ExecutionException e) {
                SALog.d(LOGTAG, "Cannot read distinct ids from sharedPreferences.", e.getCause());
            } catch (final InterruptedException e) {
                SALog.d(LOGTAG, "Cannot read distinct ids from sharedPreferences.", e);
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
