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
import android.os.Build;
import android.text.TextUtils;
import android.util.LruCache;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.util.SASpUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 其它 SDK 会继承此类实现插件，谨慎修改相关方法
 */
public abstract class AbstractStoreManager {

    private final List<StorePlugin> mStorePluginList;
    private final Set<String> mStoreTypes;
    private final LruCacheData mLruCacheSPData;
    private StorePlugin mMaxPriorityPlugin;
    private String mMaxPluginType;
    private final Lock mLock;
    protected boolean mDefaultState = true;
    private static final String TAG = "SA.AbstractStoreManager";

    protected AbstractStoreManager() {
        mStorePluginList = new ArrayList<>();
        mLruCacheSPData = new LruCacheData(10);
        mLock = new ReentrantLock(true);
        mStoreTypes = new HashSet<>();
    }

    /**
     * 插件注册，AB SDK 会调用此方法，谨慎修改
     * @param plugin 存储插件
     */
    public void registerPlugin(StorePlugin plugin) {
        if (plugin == null) {
            return;
        }
        String pluginType = plugin.type();
        if (TextUtils.isEmpty(pluginType)) {
            SALog.i(TAG, "PluginType is null");
            return;
        }
        if (!mStoreTypes.contains(pluginType)) {
            mStoreTypes.add(pluginType);
        } else {
            for (StorePlugin storePlugin : mStorePluginList) {
                if (TextUtils.equals(pluginType, storePlugin.type())) {
                    mStorePluginList.remove(storePlugin);
                    break;
                }
            }
        }
        mStorePluginList.add(0, plugin);
        mMaxPriorityPlugin = plugin;
        mMaxPluginType = plugin.type();
    }

    public void setString(final String key, final String value) {
        mLock.lock();
        try {
            if (mDefaultState) {//默认处理方式
                storeKeys(key, value, "String");
                return;
            }
            // value 为 null 时，无法加密，此时应该是移除数据
            if (value == null) {
                for (StorePlugin plugin : mStorePluginList) {
                    plugin.remove(plugin.type() + key);
                }
                mLruCacheSPData.remove(key);
            } else {
                removeUselessValue(key);
                mMaxPriorityPlugin.setString(mMaxPluginType + key, value);
                mLruCacheSPData.put(key, value);
            }
        } catch (Exception e) {
            SALog.i(TAG, "save data failed,key = " + key + "value = " + value, e);
        } finally {
            mLock.unlock();
        }
    }

    public void setBool(final String key, final boolean value) {
        mLock.lock();
        try {
            if (mDefaultState) {//默认处理方式
                storeKeys(key, value, "Bool");
                return;
            }
            removeUselessValue(key);
            mMaxPriorityPlugin.setBool(mMaxPluginType + key, value);
            mLruCacheSPData.put(key, value);
        } catch (Exception e) {
            SALog.i(TAG, "save data failed,key = " + key + "value = " + value, e);
        } finally {
            mLock.unlock();
        }
    }

    public void setInteger(final String key, final int value) {
        mLock.lock();
        try {
            if (mDefaultState) {//默认处理方式
                storeKeys(key, value, "Integer");
                return;
            }
            removeUselessValue(key);
            mMaxPriorityPlugin.setInteger(mMaxPluginType + key, value);
            mLruCacheSPData.put(key, value);
        } catch (Exception e) {
            SALog.i(TAG, "save data failed,key = " + key + "value = " + value, e);
        } finally {
            mLock.unlock();
        }
    }

    public void setFloat(final String key, final float value) {
        mLock.lock();
        try {
            if (mDefaultState) {//默认处理方式
                storeKeys(key, value, "Float");
                return;
            }
            removeUselessValue(key);
            mMaxPriorityPlugin.setFloat(mMaxPluginType + key, value);
            mLruCacheSPData.put(key, value);
        } catch (Exception e) {
            SALog.i(TAG, "save data failed,key = " + key + "value = " + value, e);
        } finally {
            mLock.unlock();
        }
    }

    public void setLong(final String key, final long value) {
        mLock.lock();
        try {
            if (mDefaultState) {//默认处理方式
                storeKeys(key, value, "Long");
                return;
            }
            removeUselessValue(key);
            mMaxPriorityPlugin.setLong(mMaxPluginType + key, value);
            mLruCacheSPData.put(key, value);
        } catch (Exception e) {
            SALog.i(TAG, "save data failed,key = " + key + "value = " + value, e);
        } finally {
            mLock.unlock();
        }
    }

    public String getString(final String key, final String defaultValue) {
        mLock.lock();
        try {
            String value = (String) mLruCacheSPData.get(key);
            if (value != null) {
                return value;
            }
            if (mDefaultState) {//默认处理方式
                return getValue(key, "String", defaultValue);
            }

            for (StorePlugin plugin : mStorePluginList) {
                value = plugin.getString(plugin.type() + key);
                if (!TextUtils.isEmpty(value)) {
                    if (plugin != mMaxPriorityPlugin) {
                        plugin.remove(plugin.type() + key);
                        mMaxPriorityPlugin.setString(mMaxPluginType + key, value);
                    }
                    mLruCacheSPData.put(key, value);
                    break;
                }
            }
            return value == null ? defaultValue : value;
        } catch (Exception e) {
            SALog.i(TAG, "get data failed,key = " + key, e);
        } finally {
            mLock.unlock();
        }
        return defaultValue;
    }

    public boolean getBool(final String key, final boolean defaultValue) {
        mLock.lock();
        try {
            Boolean value = (Boolean) mLruCacheSPData.get(key);
            if (value != null) {
                return value;
            }
            if (mDefaultState) {//默认处理方式
                return getValue(key, "Bool", defaultValue);
            }
            for (StorePlugin plugin : mStorePluginList) {
                value = plugin.getBool(plugin.type() + key);
                if (value != null) {
                    if (plugin != mMaxPriorityPlugin) {
                        plugin.remove(plugin.type() + key);
                        mMaxPriorityPlugin.setBool(mMaxPluginType + key, value);
                    }
                    mLruCacheSPData.put(key, value);
                    break;
                }
            }
            return value == null ? defaultValue : value;
        } catch (Exception e) {
            SALog.i(TAG, "get data failed,key = " + key, e);
        } finally {
            mLock.unlock();
        }
        return defaultValue;
    }

    public int getInteger(final String key, final int defaultValue) {
        mLock.lock();
        try {
            Integer value = (Integer) mLruCacheSPData.get(key);
            if (value != null) {
                return value;
            }
            if (mDefaultState) {//默认处理方式
                return getValue(key, "Integer", defaultValue);
            }
            for (StorePlugin plugin : mStorePluginList) {
                value = plugin.getInteger(plugin.type() + key);
                if (value != null) {
                    if (plugin != mMaxPriorityPlugin) {
                        plugin.remove(plugin.type() + key);
                        mMaxPriorityPlugin.setInteger(mMaxPluginType + key, value);
                    }
                    mLruCacheSPData.put(key, value);
                    break;
                }
            }
            return value == null ? defaultValue : value;
        } catch (Exception e) {
            SALog.i(TAG, "get data failed,key = " + key, e);
        } finally {
            mLock.unlock();
        }
        return defaultValue;
    }

    public float getFloat(final String key, final float defaultValue) {
        mLock.lock();
        try {
            Float value = (Float) mLruCacheSPData.get(key);
            if (value != null) {
                return value;
            }
            if (mDefaultState) {//默认处理方式
                return getValue(key, "Float", defaultValue);
            }

            for (StorePlugin plugin : mStorePluginList) {
                value = plugin.getFloat(plugin.type() + key);
                if (value != null) {
                    if (plugin != mMaxPriorityPlugin) {
                        plugin.remove(plugin.type() + key);
                        mMaxPriorityPlugin.setFloat(mMaxPluginType + key, value);
                    }
                    mLruCacheSPData.put(key, value);
                    break;
                }
            }
            return value == null ? defaultValue : value;
        } catch (Exception e) {
            SALog.i(TAG, "get data failed,key = " + key, e);
        } finally {
            mLock.unlock();
        }
        return defaultValue;
    }

    public Long getLong(final String key, final long defaultValue) {
        mLock.lock();
        try {
            Long value = (Long) mLruCacheSPData.get(key);
            if (value != null) {
                return value;
            }
            if (mDefaultState) {//默认处理方式
                return getValue(key, "Long", defaultValue);
            }
            for (StorePlugin plugin : mStorePluginList) {
                value = plugin.getLong(plugin.type() + key);
                if (value != null) {
                    if (plugin != mMaxPriorityPlugin) {
                        plugin.remove(plugin.type() + key);
                        mMaxPriorityPlugin.setLong(mMaxPluginType + key, value);
                    }
                    mLruCacheSPData.put(key, value);
                    break;
                }
            }
            return value == null ? defaultValue : value;
        } catch (Exception e) {
            SALog.i(TAG, "get data failed,key = " + key, e);
        } finally {
            mLock.unlock();
        }
        return defaultValue;
    }

    public void remove(final String key) {
        mLock.lock();
        try {
            if (mDefaultState) {
                StorePlugin tempPlugin = mMaxPriorityPlugin;
                for (StorePlugin plugin : mStorePluginList) {
                    if (plugin instanceof DefaultStorePlugin && ((DefaultStorePlugin) plugin).storeKeys() != null
                            && ((DefaultStorePlugin) plugin).storeKeys().contains(key)) {
                        tempPlugin = plugin;
                        break;
                    }
                }
                tempPlugin.remove(tempPlugin.type() + key);
            } else {
                for (StorePlugin plugin : mStorePluginList) {
                    plugin.remove(plugin.type() + key);
                }
            }
            mLruCacheSPData.remove(key);
        } catch (Exception e) {
            SALog.i(TAG, "remove failed,key = " + key, e);
        } finally {
            mLock.unlock();
        }
    }

    public void upgrade() {
        mLock.lock();
        try {
            int size = mStorePluginList.size();
            for (int i = size - 1; i >= 0; i--) {
                StorePlugin storePlugin = mStorePluginList.get(i);
                StorePlugin previousPlugin = null;
                int previousIndex = i - 1;
                if (previousIndex >= 0) {
                    previousPlugin = mStorePluginList.get(previousIndex);
                }
                if (previousPlugin != null) {
                    previousPlugin.upgrade(storePlugin);
                }
            }
        } catch (Exception e) {
            SALog.i(TAG, "upgrade failed", e);
        } finally {
            mLock.unlock();
        }
    }

    public boolean isExists(final String key) {
        mLock.lock();
        try {
            if (TextUtils.isEmpty(key)) {
                return false;
            }
            for (StorePlugin plugin : mStorePluginList) {
                if (plugin.isExists(plugin.type() + key)) {
                    return true;
                }
            }
        } catch (Exception e) {
            SALog.i(TAG, "isExists failed,key = " + key, e);
        } finally {
            mLock.unlock();
        }
        return false;
    }

    protected boolean isRegisterPlugin(Context context, String name) {
        try {
            File SPFile = new File("data/data/" + context.getPackageName() + "/shared_prefs", name + ".xml");
            if (!SPFile.exists()) {
                return false;
            }
            SharedPreferences storeSp = SASpUtils.getSharedPreferences(context, name, Context.MODE_PRIVATE);
            Map<String, ?> stringMap = storeSp.getAll();
            if (stringMap.size() == 0) {
                SALog.i(TAG, "delete sp: " + name);
                return !SPFile.delete();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return true;
    }

    private void removeUselessValue(String key) {
        for (StorePlugin plugin : mStorePluginList) {
            if (plugin != mMaxPriorityPlugin) {
                plugin.remove(plugin.type() + key);
            }
        }
    }

    private void storeKeys(String key, Object value, String type) {
        StorePlugin tempPlugin = mMaxPriorityPlugin;
        for (StorePlugin plugin : mStorePluginList) {
            if (plugin instanceof DefaultStorePlugin && ((DefaultStorePlugin) plugin).storeKeys() != null
                    && ((DefaultStorePlugin) plugin).storeKeys().contains(key)) {
                tempPlugin = plugin;
                break;
            }
        }
        switch (type) {
            case "String":
                tempPlugin.setString(tempPlugin.type() + key, (String) value);
                break;
            case "Integer":
                tempPlugin.setInteger(tempPlugin.type() + key, (Integer) value);
                break;
            case "Float":
                tempPlugin.setFloat(tempPlugin.type() + key, (Float) value);
                break;
            case "Long":
                tempPlugin.setLong(tempPlugin.type() + key, (Long) value);
                break;
            case "Bool":
                tempPlugin.setBool(tempPlugin.type() + key, (Boolean) value);
                break;
        }
    }

    private <T> T getValue(String key, String type, T defaultValue) {
        //默认为 sensorsdata 插件
        StorePlugin tempPlugin = mMaxPriorityPlugin;
        for (StorePlugin plugin : mStorePluginList) {
            if (plugin instanceof DefaultStorePlugin && ((DefaultStorePlugin) plugin).storeKeys() != null
                    && ((DefaultStorePlugin) plugin).storeKeys().contains(key)) {
                tempPlugin = plugin;
                break;
            }
        }
        Object value = null;
        switch (type) {
            case "String":
                value = (T) tempPlugin.getString(tempPlugin.type() + key);
                break;
            case "Integer":
                value = (T) tempPlugin.getInteger(tempPlugin.type() + key);
                break;
            case "Float":
                value = (T) tempPlugin.getFloat(tempPlugin.type() + key);
                break;
            case "Long":
                value = (T) tempPlugin.getLong(tempPlugin.type() + key);
                break;
            case "Bool":
                value = (T) tempPlugin.getBool(tempPlugin.type() + key);
                break;
        }
        return value == null ? defaultValue : (T) value;
    }

    private class LruCacheData {
        private LruCache<String, Object> mCacheSPData;

        public LruCacheData(int maxSize) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                mCacheSPData = new LruCache<>(maxSize);
            }
        }

        Object get(String key) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                return mCacheSPData.get(mMaxPluginType + key);
            }
            return null;
        }

        void put(String key, Object value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                mCacheSPData.put(mMaxPluginType + key, value);
            }
        }

        void remove(String key) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                mCacheSPData.remove(mMaxPluginType + key);
            }
        }
    }
}

