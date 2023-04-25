/*
 * Created by dengshiwei on 2022/06/29.
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

package com.sensorsdata.analytics.android.unit_utils;

import static org.junit.Assert.assertEquals;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SensorsAnalyticsAutoTrackEventType;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.DeviceUtils;
import com.sensorsdata.analytics.android.sdk.util.SASpUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;

import org.json.JSONObject;
import org.robolectric.RuntimeEnvironment;

public class SAHelper {
    static Application mApplication = RuntimeEnvironment.getApplication();
    private final static String SA_SERVER_URL = "https://sdkdebugtest.datasink.sensorsdata.cn/sa?project=default&token=cfb8b60e42e0ae9b";

    public static SensorsDataAPI initSensors(Context context) {
        SASpUtils.setSharedPreferencesProvider(new SASpUtils.ISharedPreferencesProvider() {
            @Override
            public SharedPreferences createSharedPreferences(Context context, String name, int mode) {
                //ShadowPreferenceManager.getDefaultSharedPreferences(Robolectric.application.getApplicationContext());
                return RuntimeEnvironment.getApplication().getSharedPreferences(name, mode);
            }
        });
        SAConfigOptions configOptions = new SAConfigOptions(SA_SERVER_URL);
        // 打开自动采集, 并指定追踪哪些 AutoTrack 事件
        configOptions.setAutoTrackEventType(SensorsAnalyticsAutoTrackEventType.APP_START |
                SensorsAnalyticsAutoTrackEventType.APP_END |
                SensorsAnalyticsAutoTrackEventType.APP_VIEW_SCREEN |
                SensorsAnalyticsAutoTrackEventType.APP_CLICK)
                .enableJavaScriptBridge(true)
                .enableHeatMap(true)
                .enableVisualizedAutoTrack(true);
        SensorsDataAPI.startWithConfigOptions(context, configOptions);
        SensorsDataAPI.sharedInstance(context).trackFragmentAppViewScreen();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {

            }
        });
        return SensorsDataAPI.sharedInstance();
    }

    public static String getSaServerUrl() {
        return SA_SERVER_URL;
    }

    public static void checkPresetProperty(JSONObject jsonObject, SensorsDataAPI sensorsDataAPI) {
        // 检查预置属性
        String version = DeviceUtils.getHarmonyOSVersion();
        if (TextUtils.isEmpty(version)) {
            assertEquals(jsonObject.opt("$os"), "Android");
            assertEquals(jsonObject.opt("$os_version"), DeviceUtils.getOS());
        } else {
            assertEquals(jsonObject.opt("$os"), "HarmonyOS");
            assertEquals(jsonObject.opt("$os_version"), version);
        }

        assertEquals(jsonObject.opt("$lib"), "Android");
        assertEquals(jsonObject.opt("$lib_version"), sensorsDataAPI.getSDKVersion());
        assertEquals(jsonObject.opt("$manufacturer"), DeviceUtils.getManufacturer());
        assertEquals(jsonObject.opt("$model"), DeviceUtils.getModel());
        assertEquals(jsonObject.opt("$brand"), DeviceUtils.getBrand());
        assertEquals(jsonObject.opt("$app_version"), AppInfoUtils.getAppVersionName(mApplication));
        int[] size = DeviceUtils.getDeviceSize(mApplication);
        assertEquals(jsonObject.opt("$screen_width"), size[0]);
        assertEquals(jsonObject.opt("$screen_height"), size[1]);
        assertEquals(jsonObject.opt("$carrier"), SensorsDataUtils.getOperator(mApplication));
        assertEquals(jsonObject.opt("$timezone_offset"), TimeUtils.getZoneOffset());
        assertEquals(jsonObject.opt("$app_id"), AppInfoUtils.getProcessName(mApplication));
        assertEquals(jsonObject.opt("$app_name"), AppInfoUtils.getAppName(mApplication));
    }
}
