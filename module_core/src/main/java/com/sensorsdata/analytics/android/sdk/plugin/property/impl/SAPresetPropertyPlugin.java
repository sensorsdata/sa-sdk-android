/*
 * Created by luweibin on 2021/12/16.
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

package com.sensorsdata.analytics.android.sdk.plugin.property.impl;

import android.content.Context;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.plugin.property.SAPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertiesFetcher;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertyFilter;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.Base64Coder;
import com.sensorsdata.analytics.android.sdk.util.DeviceUtils;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 预置属性插件
 */
public final class SAPresetPropertyPlugin extends SAPropertyPlugin {
    private final Context mContext;
    private final boolean mDisableTrackDeviceId;
    private final boolean mDisableAndroidId;
    private JSONObject presetProperty;

    public SAPresetPropertyPlugin(SAContextManager contextManager) {
        this.mContext = contextManager.getContext();
        this.mDisableTrackDeviceId = contextManager.getInternalConfigs().isTrackDeviceId;
        this.mDisableAndroidId = contextManager.getInternalConfigs().saConfigOptions.isDisableDeviceId();
    }

    @Override
    public boolean isMatchedWithFilter(SAPropertyFilter filter) {
        EventType type = filter.getType();
        return type.isTrack();
    }

    @Override
    public void properties(SAPropertiesFetcher saPropertiesFetcher) {
        try {
            JSONObject jsonObject = getPresetProperties();
            //之前可能会因为没有权限无法获取运营商信息，检测再次获取
            if (TextUtils.isEmpty(jsonObject.optString("$carrier"))) {
                String carrier = SensorsDataUtils.getOperator(mContext);
                if (!TextUtils.isEmpty(carrier)) {
                    jsonObject.put("$carrier", carrier);
                }
            }
            // 防止覆盖客户自己传递的，SDK 内部有补发的 $AppEnd 场景
            if (saPropertiesFetcher.getProperties().has("$lib_version")) {
                jsonObject.remove("$lib_version");
            }
            if (saPropertiesFetcher.getProperties().has("$lib")) {
                jsonObject.remove("$lib");
            }
            if (saPropertiesFetcher.getProperties().has("$app_version")) {
                jsonObject.remove("$app_version");
            }
            JSONUtils.mergeJSONObject(jsonObject, saPropertiesFetcher.getProperties());
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private JSONObject getPreset() {
        JSONObject properties = new JSONObject();
        try {
            String osVersion = DeviceUtils.getHarmonyOSVersion();
            if (!TextUtils.isEmpty(osVersion)) {
                properties.put("$os", "HarmonyOS");
                properties.put("$os_version", osVersion);
            } else {
                properties.put("$os", "Android");
                properties.put("$os_version", DeviceUtils.getOS());
            }

            properties.put("$lib", "Android");
            properties.put("$lib_version", SensorsDataAPI.sharedInstance().getSDKVersion());
            properties.put("$manufacturer", DeviceUtils.getManufacturer());
            properties.put("$model", DeviceUtils.getModel());
            properties.put("$brand", DeviceUtils.getBrand());
            properties.put("$app_version", AppInfoUtils.getAppVersionName(mContext));
            int[] size = DeviceUtils.getDeviceSize(mContext);
            properties.put("$screen_width", size[0]);
            properties.put("$screen_height", size[1]);

            String carrier = SensorsDataUtils.getOperator(mContext);
            if (!TextUtils.isEmpty(carrier)) {
                properties.put("$carrier", carrier);
            }

            Integer zone_offset = TimeUtils.getZoneOffset();
            if (zone_offset != null) {
                properties.put("$timezone_offset", zone_offset);
            }

            properties.put("$app_id", AppInfoUtils.getProcessName(mContext));
            properties.put("$app_name", AppInfoUtils.getAppName(mContext));
            String mAndroidId = SensorsDataUtils.getIdentifier(mContext);
            if (!mDisableTrackDeviceId && !TextUtils.isEmpty(mAndroidId)) {
                if (mDisableAndroidId) {
                    properties.put("$anonymization_id", Base64Coder.encodeString(mAndroidId));
                } else {
                    properties.put("$device_id", mAndroidId);
                }
            }
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
        return properties;
    }

    /**
     * 获取预置属性
     *
     * @return 预置属性
     */
    public JSONObject getPresetProperties() {
        try {
            if (presetProperty == null) {
                JSONObject jsonObject = getPreset();
                presetProperty = new JSONObject(jsonObject.toString());
            }
            String networkType = NetworkUtils.networkType(mContext);
            presetProperty.put("$wifi", "WIFI".equals(networkType));
            presetProperty.put("$network_type", networkType);
            return new JSONObject(presetProperty.toString());
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
        return new JSONObject();
    }
}
