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

package com.sensorsdata.analytics.android.autotrack;

import android.content.Context;

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SensorsAnalyticsAutoTrackEventType;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;

import org.json.JSONObject;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

public class SAHelper {

    private final static String SA_SERVER_URL = "https://sdkdebugtest.datasink.sensorsdata.cn/sa?project=default&token=cfb8b60e42e0ae9b";

    public static SensorsDataAPI initSensors(Context context) {
        SAConfigOptions configOptions = new SAConfigOptions(SA_SERVER_URL);
        // 打开自动采集, 并指定追踪哪些 AutoTrack 事件
        configOptions.setAutoTrackEventType(SensorsAnalyticsAutoTrackEventType.APP_CLICK|SensorsAnalyticsAutoTrackEventType.APP_VIEW_SCREEN)
                .enableJavaScriptBridge(true)
                .enableHeatMap(true)
                .enableVisualizedAutoTrack(true);
        SensorsDataAPI.startWithConfigOptions(context, configOptions);
        SensorsDataAPI.sharedInstance(context).trackFragmentAppViewScreen();
        SensorsDataAPI.sharedInstance(context).setTrackEventCallBack(null);
        return SensorsDataAPI.sharedInstance();
    }

    public static SensorsDataAPI initWithAppClick(Context context) {
        SAConfigOptions configOptions = new SAConfigOptions(SA_SERVER_URL);
        // 打开自动采集, 并指定追踪哪些 AutoTrack 事件
        configOptions.setAutoTrackEventType(SensorsAnalyticsAutoTrackEventType.APP_CLICK)
                .enableJavaScriptBridge(true)
                .enableHeatMap(true)
                .enableVisualizedAutoTrack(true);
        SensorsDataAPI.startWithConfigOptions(context, configOptions);
        SensorsDataAPI.sharedInstance(context).setTrackEventCallBack(null);
        List<SensorsDataAPI.AutoTrackEventType> list = new ArrayList<>();
        list.add(SensorsDataAPI.AutoTrackEventType.APP_START);
        list.add(SensorsDataAPI.AutoTrackEventType.APP_VIEW_SCREEN);
        list.add(SensorsDataAPI.AutoTrackEventType.APP_END);
        SensorsDataAPI.sharedInstance().disableAutoTrack(list);
        return SensorsDataAPI.sharedInstance();
    }

    public static String getSaServerUrl() {
        return SA_SERVER_URL;
    }

    public static boolean checkPresetProperty(JSONObject jsonObject) {
        Assert.assertNotNull(jsonObject);
        Assert.assertTrue(jsonObject.has("$os_version"));
        Assert.assertTrue(jsonObject.has("$is_first_day"));
        Assert.assertTrue(jsonObject.has("$model"));
        Assert.assertTrue(jsonObject.has("$os"));
        Assert.assertTrue(jsonObject.has("$screen_width"));
        Assert.assertTrue(jsonObject.has("$brand"));
        Assert.assertTrue(jsonObject.has("$wifi"));
        Assert.assertTrue(jsonObject.has("$network_type"));
        Assert.assertTrue(jsonObject.has("$screen_height"));
        Assert.assertEquals("Android", jsonObject.optString("$lib"));
        Assert.assertTrue(jsonObject.has("$device_id"));
        Assert.assertEquals("com.sensorsdata.analytics.android.autotrack.test", jsonObject.optString("$app_name"));
        Assert.assertEquals(SensorsDataAPI.sharedInstance().getSDKVersion(), jsonObject.optString("$lib_version"));
        Assert.assertTrue(jsonObject.has("$timezone_offset"));
        Assert.assertEquals("com.sensorsdata.analytics.android.autotrack.test", jsonObject.optString("$app_id"));
        Assert.assertTrue(jsonObject.has("$manufacturer"));
        return true;
    }
}
