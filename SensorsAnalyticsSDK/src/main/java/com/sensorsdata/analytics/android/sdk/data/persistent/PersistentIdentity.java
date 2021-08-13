/*
 * Created by wangzhuozhou on 2015/08/01.
 * Copyright 2015－2021 Sensors Data Inc.
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

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@SuppressLint("CommitPrefEdits")
public abstract class PersistentIdentity<T> {

    private static final String TAG = "SA.PersistentIdentity";
    private final Future<SharedPreferences> loadStoredPreferences;
    private final PersistentSerializer serializer;
    private final String persistentKey;
    private T item;

    PersistentIdentity(final Future<SharedPreferences> loadStoredPreferences, final String
            persistentKey, final PersistentSerializer<T> serializer) {
        this.loadStoredPreferences = loadStoredPreferences;
        this.serializer = serializer;
        this.persistentKey = persistentKey;
    }

    /**
     * 获取存储的值
     *
     * @return 存储的值
     */
    @SuppressWarnings("unchecked")
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
                    SALog.d(TAG, "Cannot read distinct ids from sharedPreferences.", e.getCause());
                } catch (final InterruptedException e) {
                    SALog.d(TAG, "Cannot read distinct ids from sharedPreferences.", e);
                }

                if (data == null) {
                    item = (T) serializer.create();
                    commit(item);
                } else {
                    item = (T) serializer.load(data);
                }
            }
        }
        return this.item;
    }

    /**
     * 保存数据值
     *
     * @param item 数据值
     */
    @SuppressWarnings("unchecked")
    public void commit(T item) {
        if (SensorsDataAPI.getConfigOptions().isDisableSDK()) {
            return;
        }
        this.item = item;

        synchronized (loadStoredPreferences) {
            SharedPreferences sharedPreferences = null;
            try {
                sharedPreferences = loadStoredPreferences.get();
            } catch (final ExecutionException e) {
                SALog.d(TAG, "Cannot read distinct ids from sharedPreferences.", e.getCause());
            } catch (final InterruptedException e) {
                SALog.d(TAG, "Cannot read distinct ids from sharedPreferences.", e);
            }

            if (sharedPreferences == null) {
                return;
            }

            final SharedPreferences.Editor editor = sharedPreferences.edit();
            if (this.item == null) {
                this.item = (T) serializer.create();
            }
            editor.putString(persistentKey, serializer.save(this.item));
            editor.apply();
        }
    }

    /**
     * Persistent 序列化接口
     *
     * @param <T> 数据类型
     */
    interface PersistentSerializer<T> {
        /**
         * 读取数据
         *
         * @param value，Value 值
         * @return 返回值
         */
        T load(final String value);

        /**
         * 保存数据
         *
         * @param item 数据值
         * @return 返回存储的值
         */
        String save(T item);

        /**
         * 创建默认值
         *
         * @return 默认值
         */
        T create();
    }
}