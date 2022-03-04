/*
 * Created by dengshiwei on 2021/07/04.
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

package com.sensorsdata.analytics.android.sdk.util;

import android.content.Context;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.listener.SAEventListener;
import com.sensorsdata.analytics.android.sdk.plugin.property.SAPresetPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.plugin.property.SensorsDataPropertyPluginManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SAContextManager {
    private final Context mContext;
    private Map<String, Object> mDeviceInfo;
    private List<SAEventListener> mEventListenerList;
    /* AndroidID */
    private String mAndroidId;
    private boolean isAppStartSuccess;

    public SAContextManager(Context context) {
        this.mContext = context;
    }

    /**
     * 获取 SDK 事件监听回调
     *
     * @return 事件监听回调
     */
    public List<SAEventListener> getEventListenerList() {
        return mEventListenerList;
    }

    /**
     * SDK 事件回调监听，目前用于弹窗业务
     *
     * @param eventListener 事件监听
     */
    public void addEventListener(SAEventListener eventListener) {
        try {
            if (this.mEventListenerList == null) {
                this.mEventListenerList = new ArrayList<>();
            }
            this.mEventListenerList.add(eventListener);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 移除 SDK 事件回调监听
     *
     * @param eventListener 事件监听
     */
    public void removeEventListener(SAEventListener eventListener) {
        try {
            if (mEventListenerList != null && mEventListenerList.contains(eventListener)) {
                this.mEventListenerList.remove(eventListener);
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 从 DeviceInfo 中添加指定 Key
     *
     * @param jsonObject JSONObject
     * @param key 指定 Key
     */
    public void addKeyIfExist(JSONObject jsonObject, String key) {
        try {
            setupDeviceInfo();
            if (mDeviceInfo != null && mDeviceInfo.containsKey(key)) {
                jsonObject.put(key, mDeviceInfo.get(key));
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 获取 AndroidID
     *
     * @return AndroidID
     */
    public String getAndroidId() {
        if (TextUtils.isEmpty(mAndroidId) && SensorsDataAPI.getConfigOptions().isDataCollectEnable()) {
            mAndroidId = SensorsDataUtils.getAndroidID(mContext);
        }
        return mAndroidId;
    }

    /**
     * 获取预置属性信息
     *
     * @return 预置属性信息
     */
    public JSONObject getPresetProperties() {
        JSONObject properties = new JSONObject();
        try {
            setupDeviceInfo();
            properties.put("$app_version", mDeviceInfo.get("$app_version"));
            properties.put("$lib", "Android");
            properties.put("$lib_version", mDeviceInfo.get("$lib_version"));
            properties.put("$manufacturer", mDeviceInfo.get("$manufacturer"));
            properties.put("$model", mDeviceInfo.get("$model"));
            properties.put("$brand", mDeviceInfo.get("$brand"));
            properties.put("$os", mDeviceInfo.get("$os"));
            properties.put("$os_version", mDeviceInfo.get("$os_version"));
            properties.put("$screen_height", mDeviceInfo.get("$screen_height"));
            properties.put("$screen_width", mDeviceInfo.get("$screen_width"));
            String networkType = NetworkUtils.networkType(mContext);
            properties.put("$wifi", "WIFI".equals(networkType));
            properties.put("$network_type", networkType);
            properties.put("$carrier", mDeviceInfo.get("$carrier"));
            properties.put("$app_id", mDeviceInfo.get("$app_id"));
            properties.put("$timezone_offset", mDeviceInfo.get("$timezone_offset"));
            if (mDeviceInfo.containsKey("$anonymization_id")) {
                properties.put("$anonymization_id", mDeviceInfo.get("$anonymization_id"));
            }
            if (mDeviceInfo.containsKey("$device_id")) {
                properties.put("$device_id", mDeviceInfo.get("$device_id"));
            }
            properties.put("$app_name", mDeviceInfo.get("$app_name"));
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return properties;
    }

    /**
     * 获取并配置 App 的一些基本属性
     */
    private void setupDeviceInfo() {
        if (mDeviceInfo == null || mDeviceInfo.isEmpty()) {
            mDeviceInfo = Collections.unmodifiableMap(SensorsDataPropertyPluginManager.getInstance().getPropertiesByPlugin(SAPresetPropertyPlugin.class));
        }
    }

    public boolean isAppStartSuccess() {
        return isAppStartSuccess;
    }

    public void setAppStartSuccess(boolean appStartSuccess) {
        isAppStartSuccess = appStartSuccess;
    }
}
