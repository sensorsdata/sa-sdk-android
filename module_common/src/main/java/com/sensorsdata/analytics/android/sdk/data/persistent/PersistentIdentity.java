/*
 * Created by wangzhuozhou on 2015/08/01.
 * Copyright 2015－2022 Sensors Data Inc.
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

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.plugin.encrypt.SAStoreManager;

@SuppressLint("CommitPrefEdits")
public abstract class PersistentIdentity<T> {

    private static final String TAG = "SA.PersistentIdentity";
    private final PersistentSerializer serializer;
    private final String persistentKey;
    private final SAStoreManager saStoreManager;
    private T item;

    PersistentIdentity(final String persistentKey, final PersistentSerializer<T> serializer) {
        this.serializer = serializer;
        this.persistentKey = persistentKey;
        this.saStoreManager = SAStoreManager.getInstance();
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
            synchronized (saStoreManager) {
                data = saStoreManager.getString(persistentKey, null);
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

        synchronized (saStoreManager) {
            if (this.item == null) {
                this.item = (T) serializer.create();
            }
            saStoreManager.setString(persistentKey, serializer.save(this.item));
        }
    }

    /**
     * 判断当前 Key 是否存在
     *
     * @return true： 存在，false：不存在
     */
    public boolean isExists() {
        try {
            return saStoreManager.isExists(persistentKey);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return false;
    }

    /**
     * 删除数据
     */
    public void remove() {
        synchronized (saStoreManager) {
            saStoreManager.remove(persistentKey);
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