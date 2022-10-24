/*
 * Created by dengshiwei on 2022/06/24.
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

package com.sensorsdata.analytics.android.sdk.core.business;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;

import org.json.JSONArray;
import org.json.JSONObject;

public class SAPluginVersion {

    private static final String TAG = "SA.SAPluginVersion";
    private static boolean isTrackEventWithPluginVersion = false;

    public static void appendPluginVersion(JSONObject property) {
        if (!isTrackEventWithPluginVersion && !property.has("$lib_plugin_version")) {
            JSONArray libPluginVersion = getPluginVersion();
            if (libPluginVersion == null) {
                isTrackEventWithPluginVersion = true;
            } else {
                try {
                    property.put("$lib_plugin_version", libPluginVersion);
                    isTrackEventWithPluginVersion = true;
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        }
    }

    public static JSONArray getPluginVersion() {
        try {
            if (!TextUtils.isEmpty(SensorsDataAPI.ANDROID_PLUGIN_VERSION)) {
                SALog.i(TAG, "android plugin version: " + SensorsDataAPI.ANDROID_PLUGIN_VERSION);
                JSONArray libPluginVersion = new JSONArray();
                libPluginVersion.put("android:" + SensorsDataAPI.ANDROID_PLUGIN_VERSION);
                return libPluginVersion;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }
}
