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

package com.sensorsdata.analytics.android.sdk.plugin.property;

import android.content.Context;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.Base64Coder;
import com.sensorsdata.analytics.android.sdk.util.DeviceUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;

import java.util.Map;
import java.util.Set;

/**
 * 预置属性插件
 */
public final class SAPresetPropertyPlugin extends SAPropertyPlugin {
    private final Context mContext;
    private final boolean mDisableTrackDeviceId;
    private final boolean mDisableAndroidId;

    public SAPresetPropertyPlugin(Context context, boolean disableTrackDeviceId, boolean disableAndroidId) {
        this.mContext = context;
        this.mDisableTrackDeviceId = disableTrackDeviceId;
        this.mDisableAndroidId = disableAndroidId;
    }

    @Override
    public void appendProperties(Map<String, Object> properties) {
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

        String carrier = SensorsDataUtils.getCarrier(mContext);
        if (!TextUtils.isEmpty(carrier)) {
            properties.put("$carrier", carrier);
        }

        Integer zone_offset = TimeUtils.getZoneOffset();
        if (zone_offset != null) {
            properties.put("$timezone_offset", zone_offset);
        }

        properties.put("$app_id", AppInfoUtils.getProcessName(mContext));
        properties.put("$app_name", AppInfoUtils.getAppName(mContext));
        String mAndroidId = SensorsDataUtils.getAndroidID(mContext);
        if (!mDisableTrackDeviceId && !TextUtils.isEmpty(mAndroidId)) {
            if (mDisableAndroidId) {
                properties.put("$anonymization_id", Base64Coder.encodeString(mAndroidId));
            } else {
                properties.put("$device_id", mAndroidId);
            }
        }
    }

    @Override
    public void appendDynamicProperties(Map<String, Object> dynamicProperties) {

    }

    @Override
    public void eventNameFilter(Set<String> eventNameFilter) {

    }

    @Override
    public void eventTypeFilter(Set<EventType> eventTypeFilter) {
        eventTypeFilter.add(EventType.TRACK);
        eventTypeFilter.add(EventType.TRACK_SIGNUP);
        eventTypeFilter.add(EventType.TRACK_ID_BIND);
        eventTypeFilter.add(EventType.TRACK_ID_UNBIND);
    }

    @Override
    public void propertyKeyFilter(Set<String> propertyKeyFilter) {

    }

    @Override
    public SAPropertyPluginPriority priority() {
        return SAPropertyPluginPriority.LOW;
    }
}
