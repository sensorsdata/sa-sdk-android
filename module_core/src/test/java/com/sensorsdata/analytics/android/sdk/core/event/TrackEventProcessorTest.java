/*
 * Created by dengshiwei on 2022/06/30.
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

package com.sensorsdata.analytics.android.sdk.core.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.app.Application;
import android.text.TextUtils;

import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SensorsAnalyticsAutoTrackEventType;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataTrackEventCallBack;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.DeviceUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;
import com.sensorsdata.analytics.android.unit_utils.DatabaseUtilsTest;
import com.sensorsdata.analytics.android.unit_utils.ProfileTestUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class TrackEventProcessorTest {
    private final static String SA_SERVER_URL = "https://sdkdebugtest.datasink.sensorsdata.cn/sa?project=default&token=cfb8b60e42e0ae9b";
    Application mApplication = ApplicationProvider.getApplicationContext();

    @Test
    public void trackEvent() throws JSONException {
        initSensors();
        SensorsDataAPI.sharedInstance().setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                assertEquals("UnitTestTrack", eventName);
                String version = DeviceUtils.getHarmonyOSVersion();
                if (TextUtils.isEmpty(version)) {
                    assertEquals(eventProperties.opt("$os"), "Android");
                    assertEquals(eventProperties.opt("$os_version"), DeviceUtils.getOS());
                } else {
                    assertEquals(eventProperties.opt("$os"), "HarmonyOS");
                    assertEquals(eventProperties.opt("$os_version"), version);
                }

                assertEquals(eventProperties.opt("$lib"), "Android");
                assertEquals(eventProperties.opt("$lib_version"), SensorsDataAPI.sharedInstance().getSDKVersion());
                assertEquals(eventProperties.opt("$manufacturer"), DeviceUtils.getManufacturer());
                assertEquals(eventProperties.opt("$model"), DeviceUtils.getModel());
                assertEquals(eventProperties.opt("$brand"), DeviceUtils.getBrand());
                assertEquals(eventProperties.opt("$app_version"), AppInfoUtils.getAppVersionName(mApplication));
                int[] size = DeviceUtils.getDeviceSize(mApplication);
                assertEquals(eventProperties.opt("$screen_width"), size[0]);
                assertEquals(eventProperties.opt("$screen_height"), size[1]);
                assertEquals(eventProperties.opt("$carrier"), SensorsDataUtils.getOperator(mApplication));
                assertEquals(eventProperties.opt("$timezone_offset"), TimeUtils.getZoneOffset());
                assertEquals(eventProperties.opt("$app_id"), AppInfoUtils.getProcessName(mApplication));
                Assert.assertTrue(eventProperties.optBoolean("$is_first_day"));
                return true;
            }
        });
        InputData inputData = new InputData();
        inputData.setEventName("UnitTestTrack");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("track", "track");
        inputData.setProperties(jsonObject);
        TrackEventProcessor eventProcessor = new TrackEventProcessor(SensorsDataAPI.sharedInstance().getSAContextManager());
        eventProcessor.trackEvent(inputData);
    }

    @Test
    public void trackEventItem() throws JSONException {
        initSensors();
        String itemType = "product_id", itemId = "100";
        InputData inputData = new InputData();
        inputData.setEventType(EventType.ITEM_DELETE);
        inputData.setItemId(itemId);
        inputData.setItemType(itemType);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("item", "item");
        inputData.setProperties(jsonObject);
        TrackEventProcessor eventProcessor = new TrackEventProcessor(SensorsDataAPI.sharedInstance().getSAContextManager());
        eventProcessor.trackEvent(inputData);
        // 检查事件类型和属性
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String eventData = DatabaseUtilsTest.loadEventFromDb(mApplication);
        assertNotNull(eventData);
        JSONObject jsonData = new JSONObject(eventData);
        ProfileTestUtils.checkItemEvent(jsonData, "item_delete", itemType, itemId);
        JSONObject propertyJson = jsonData.optJSONObject("properties");
        assertNotNull(propertyJson);
        assertEquals("item", propertyJson.opt("item"));
    }

    @Test
    public void trackEventH5() {
        initSensors();
        SensorsDataAPI.sharedInstance().setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                assertEquals("$WebClick", eventName);
                String version = DeviceUtils.getHarmonyOSVersion();
                if (TextUtils.isEmpty(version)) {
                    assertEquals(eventProperties.opt("$os"), "Android");
                    assertEquals(eventProperties.opt("$os_version"), DeviceUtils.getOS());
                } else {
                    assertEquals(eventProperties.opt("$os"), "HarmonyOS");
                    assertEquals(eventProperties.opt("$os_version"), version);
                }

                assertEquals(eventProperties.opt("$lib"), "js");
                assertEquals(eventProperties.opt("$lib_version"), "1.14.23");
                assertEquals(eventProperties.opt("$manufacturer"), DeviceUtils.getManufacturer());
                assertEquals(eventProperties.opt("$model"), DeviceUtils.getModel());
                assertEquals(eventProperties.opt("$brand"), DeviceUtils.getBrand());
                assertEquals(eventProperties.opt("$app_version"), AppInfoUtils.getAppVersionName(mApplication));
                int[] size = DeviceUtils.getDeviceSize(mApplication);
                assertEquals(eventProperties.opt("$screen_width"), size[0]);
                assertEquals(eventProperties.opt("$screen_height"), size[1]);
                assertEquals(eventProperties.opt("$carrier"), SensorsDataUtils.getOperator(mApplication));
                assertEquals(eventProperties.opt("$timezone_offset"), TimeUtils.getZoneOffset());
                assertEquals(eventProperties.opt("$app_id"), AppInfoUtils.getProcessName(mApplication));
                Assert.assertTrue(eventProperties.optBoolean("$is_first_day"));
                return true;
            }
        });
        InputData inputData = new InputData();
        inputData.setExtras("{\"server_url\":\"https://sdkdebugtest.datasink.sensorsdata.cn/sa.gif?project=default&token=cfb8b60e42e0ae9b\"," +
                "\"distinct_id\":\"181b8fcc33747-0e98015efae734-5f2b2f1c-277920-181b8fcc33bec\",\"lib\":{\"$lib\":\"js\",\"$lib_method\":\"code\",\"$lib_version\":\"1.14.23\"}," +
                "\"properties\":{\"$screen_height\":772,\"$screen_width\":360,\"$lib\":\"js\",\"$lib_version\":\"1.14.23\",\"$latest_traffic_source_type\":\"url的domain解析失败\",\"$latest_search_keyword\":\"url的domain解析失败\",\"$latest_referrer\":\"url的domain解析失败\",\"$device_id\":\"181b8fcc33747-0e98015efae734-5f2b2f1c-277920-181b8fcc33bec\",\"$element_type\":\"button\",\"$element_class_name\":\"\",\"$element_content\":\"test\",\"$url\":\"file:///android_asset/new_h5_test/index.html\",\"$url_path\":\"/android_asset/new_h5_test/index.html\",\"$title\":\"sdk demo ls\",\"$viewport_width\":360,\"$element_selector\":\"body > div:nth-of-type(1) > button:nth-of-type(1)\",\"timepppp\":\"2022-07-01 17:00:50.771\",\"$is_first_day\":false},\"anonymous_id\":\"181b8fcc33747-0e98015efae734-5f2b2f1c-277920-181b8fcc33bec\"," +
                "\"type\":\"track\",\"event\":\"$WebClick\",\"time\":1656666050772,\"_track_id\":887940781,\"_flush_time\":1656666050781}");
        TrackEventProcessor eventProcessor = new TrackEventProcessor(SensorsDataAPI.sharedInstance().getSAContextManager());
        eventProcessor.trackEvent(inputData);
    }

    private SensorsDataAPI initSensors() {
        SAConfigOptions configOptions = new SAConfigOptions(SA_SERVER_URL);
        // 打开自动采集, 并指定追踪哪些 AutoTrack 事件
        configOptions.setAutoTrackEventType(SensorsAnalyticsAutoTrackEventType.APP_START |
                SensorsAnalyticsAutoTrackEventType.APP_END |
                SensorsAnalyticsAutoTrackEventType.APP_VIEW_SCREEN |
                SensorsAnalyticsAutoTrackEventType.APP_CLICK)
                .enableTrackAppCrash()
                .enableSession(true)
                .enableJavaScriptBridge(true)
                .enableHeatMap(true)
                .setNetworkTypePolicy(0)
                .enableVisualizedAutoTrack(true);
        SensorsDataAPI.startWithConfigOptions(mApplication, configOptions);
        SensorsDataAPI.sharedInstance(mApplication).trackFragmentAppViewScreen();
        return SensorsDataAPI.sharedInstance();
    }
}