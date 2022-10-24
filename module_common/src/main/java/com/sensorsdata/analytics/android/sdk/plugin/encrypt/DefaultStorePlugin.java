/*
 * Created by yuejianzhong on 2021/12/14.
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

package com.sensorsdata.analytics.android.sdk.plugin.encrypt;

import android.content.Context;
import android.content.SharedPreferences;

import com.sensorsdata.analytics.android.sdk.util.SASpUtils;

import java.util.List;

public abstract class DefaultStorePlugin implements StorePlugin {
    private final SharedPreferences mStoreSp;
    private final String mFileName;

    public DefaultStorePlugin(Context context, String fileName) {
        mStoreSp = SASpUtils.getSharedPreferences(context, fileName, Context.MODE_PRIVATE);
        mFileName = fileName;
    }

    @Override
    public boolean isExists(String key) {
        return mStoreSp.contains(getKey(key));
    }

    @Override
    public String type() {
        return mFileName;
    }

    @Override
    public void upgrade(StorePlugin oldPlugin) {

    }

    @Override
    public void setString(String key, String value) {
        mStoreSp.edit().putString(getKey(key), value).apply();
    }

    @Override
    public void setBool(String key, boolean value) {
        mStoreSp.edit().putBoolean(getKey(key), value).apply();
    }

    @Override
    public void setInteger(String key, int value) {
        mStoreSp.edit().putInt(getKey(key), value).apply();
    }

    @Override
    public void setFloat(String key, float value) {
        mStoreSp.edit().putFloat(getKey(key), value).apply();
    }

    @Override
    public void setLong(String key, long value) {
        mStoreSp.edit().putLong(getKey(key), value).apply();
    }

    @Override
    public String getString(String key) {
        if (isExists(key)) {
            return mStoreSp.getString(getKey(key), null);
        }
        return null;
    }

    @Override
    public Boolean getBool(String key) {
        if (isExists(key)) {
            return mStoreSp.getBoolean(getKey(key), false);
        }
        return null;
    }

    @Override
    public Integer getInteger(String key) {
        if (isExists(key)) {
            return mStoreSp.getInt(getKey(key), 0);
        }
        return null;
    }

    @Override
    public Float getFloat(String key) {
        if (isExists(key)) {
            return mStoreSp.getFloat(getKey(key), 0F);
        }
        return null;
    }

    @Override
    public Long getLong(String key) {
        if (isExists(key)) {
            return mStoreSp.getLong(getKey(key), 0L);
        }
        return null;
    }

    @Override
    public void remove(String key) {
        mStoreSp.edit().remove(getKey(key)).apply();
    }

    /**
     * 指定插件可以存储的 Key
     * @return 指定 keys
     */
    public abstract List<String> storeKeys();

    private String getKey(String key) {
        return key.replaceFirst(type(), "");
    }
}
