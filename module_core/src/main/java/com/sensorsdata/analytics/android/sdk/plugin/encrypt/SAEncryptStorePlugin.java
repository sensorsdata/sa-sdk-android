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
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.core.SAModuleManager;
import com.sensorsdata.analytics.android.sdk.util.SASpUtils;

public class SAEncryptStorePlugin implements StorePlugin {

    private final SharedPreferences mStoreSp;
    private final String mFileName;

    public SAEncryptStorePlugin(Context context) {
        this(context, "com.sensorsdata.analytics.android.sdk");
    }

    public SAEncryptStorePlugin(Context context, String fileName) {
        this.mStoreSp = SASpUtils.getSharedPreferences(context, fileName, Context.MODE_PRIVATE);
        this.mFileName = fileName;
    }

    @Override
    public void setString(String key, String value) {
        mStoreSp.edit().putString(encryptValue(key), encryptValue(value)).apply();
    }

    @Override
    public void setBool(String key, boolean value) {
        mStoreSp.edit().putString(encryptValue(key), encryptValue(String.valueOf(value))).apply();
    }

    @Override
    public void setInteger(String key, int value) {
        mStoreSp.edit().putString(encryptValue(key), encryptValue(String.valueOf(value))).apply();
    }

    @Override
    public void setFloat(String key, float value) {
        mStoreSp.edit().putString(encryptValue(key), encryptValue(String.valueOf(value))).apply();
    }

    @Override
    public void setLong(String key, long value) {
        mStoreSp.edit().putString(encryptValue(key), encryptValue(String.valueOf(value))).apply();
    }

    @Override
    public String getString(String key) {
        String value = mStoreSp.getString(encryptValue(key), null);
        if (!TextUtils.isEmpty(value)) {
            return decryptValue(value);
        }
        return null;
    }

    @Override
    public Boolean getBool(String key) {
        String value = getString(key);
        if (!TextUtils.isEmpty(value)) {
            return Boolean.parseBoolean(value);
        }
        return null;
    }

    @Override
    public Integer getInteger(String key) {
        String value = getString(key);
        if (!TextUtils.isEmpty(value)) {
            return Integer.parseInt(value);
        }
        return null;
    }

    @Override
    public Float getFloat(String key) {
        String value = getString(key);
        if (!TextUtils.isEmpty(value)) {
            return Float.parseFloat(value);
        }
        return null;
    }

    @Override
    public Long getLong(String key) {
        String value = getString(key);
        if (!TextUtils.isEmpty(value)) {
            return Long.parseLong(value);
        }
        return null;
    }

    @Override
    public void remove(String key) {
        mStoreSp.edit().remove(encryptValue(key)).apply();
    }

    @Override
    public boolean isExists(String key) {
        return mStoreSp.contains(encryptValue(key));
    }

    @Override
    public String type() {
        return mFileName;
    }

    @Override
    public void upgrade(StorePlugin oldPlugin) {

    }

    private String decryptValue(String value) {
        String decryptValue = SAModuleManager.getInstance().invokeEncryptModuleFunction("decryptAES", value);
        return TextUtils.isEmpty(decryptValue) ? value : decryptValue;
    }

    private String encryptValue(String value) {
        String encryptValue = SAModuleManager.getInstance().invokeEncryptModuleFunction("encryptAES", value);
        return TextUtils.isEmpty(encryptValue) ? value : encryptValue;
    }
}
