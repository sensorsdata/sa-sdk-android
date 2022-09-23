/*
 * Created by dengshiwei on 2022/06/07.
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

package com.sensorsdata.analytics.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Application;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataDynamicSuperProperties;
import com.sensorsdata.analytics.android.sdk.SensorsDataTrackEventCallBack;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.DeviceUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;
import com.sensorsdata.analytics.android.unit_utils.DatabaseUtilsTest;
import com.sensorsdata.analytics.android.unit_utils.ProfileTestUtils;
import com.sensorsdata.analytics.android.unit_utils.SAHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class RegressionTesting {
    Application mApplication = ApplicationProvider.getApplicationContext();

    @Test
    public void initSensorsSDKTest() {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        // 校验全埋点开启状态;
        Assert.assertTrue(sensorsDataAPI.isAutoTrackEnabled());
        // 校验 Debug 模式
        Assert.assertFalse(sensorsDataAPI.isDebugMode());
        // 点击图是否开启
        Assert.assertTrue(sensorsDataAPI.isHeatMapEnabled());
        // 校验可视化是否开启
        Assert.assertTrue(sensorsDataAPI.isVisualizedAutoTrackEnabled());
        // 校验数据接收地址
        assertEquals(sensorsDataAPI.getServerUrl(), SAHelper.getSaServerUrl());
        assertTrue(sensorsDataAPI.isNetworkRequestEnable());
        assertTrue(sensorsDataAPI.isTrackFragmentAppViewScreenEnabled());
    }

    /**
     * 检查 点击事件及自定义属性覆盖问题
     */
    @Test
    public void trackAppViewClickTest() throws Exception {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        JSONObject properties = new JSONObject();
        final CountDownLatch downLatch = new CountDownLatch(1);
        properties.put("$title", "title");
        properties.put("$screen_name", "ScreenName");
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                if ("$AppClick".equals(eventName)) {
                    assertEquals(eventProperties.opt("$screen_name"), "ScreenName");
                    assertEquals(eventProperties.opt("$title"), "title");
                    downLatch.countDown();
                }
                return false;
            }
        });
        TextView view = new TextView(mApplication);
        view.setText("text");
        sensorsDataAPI.trackViewAppClick(view, properties);
        downLatch.await(500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void getPresetPropertiesTest() {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        JSONObject jsonObject = sensorsDataAPI.getPresetProperties();
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
        //assertEquals(jsonObject.opt("$app_name"), AppInfoUtils.getAppName(mApplication));
        Assert.assertTrue(jsonObject.optBoolean("$is_first_day"));
    }

    @Test
    public void trackTest() {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        final String eventUnitName = "UnitTest";
        final String unitKey = "unitTestKey";
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                assertEquals(eventUnitName, eventName);
                assertEquals(eventProperties.opt(unitKey), eventUnitName);
                return false;
            }
        });
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(unitKey, eventUnitName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sensorsDataAPI.track(eventUnitName, jsonObject);
    }

    @Test
    public void registerSuperPropertyTest() {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        final String eventUnitName = "SuperUnitTest";
        final String superKey = "superTestKey";
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                assertEquals(eventUnitName, eventName);
                assertEquals(eventProperties.opt(superKey), eventUnitName);
                return false;
            }
        });
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(superKey, eventUnitName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sensorsDataAPI.registerSuperProperties(jsonObject);
        sensorsDataAPI.track(eventUnitName);
        // 检查公共属性
        try {
            // 延迟保证数据库操作成功
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        JSONObject superProperty = sensorsDataAPI.getSuperProperties();
        checkJsonEquals(superProperty, jsonObject);
    }

    @Test
    public void registerDynamicPropertyTest() {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        final String eventUnitName = "DynamicUnitTest";
        final String superKey = "dynamicTestKey";
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                assertEquals(eventUnitName, eventName);
                assertEquals(eventProperties.opt(superKey), eventUnitName);
                return false;
            }
        });

        sensorsDataAPI.registerDynamicSuperProperties(new SensorsDataDynamicSuperProperties() {
            @Override
            public JSONObject getDynamicSuperProperties() {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put(superKey, eventUnitName);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return jsonObject;
            }
        });
        sensorsDataAPI.track(eventUnitName);
    }

    @Test
    public void trackTimerTest() {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        final String eventUnitName = "TimerUnitTest";
        final String superKey = "timerTestKey";
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                assertEquals(eventUnitName, eventName);
                assertEquals(eventProperties.opt(superKey), eventUnitName);
                Assert.assertTrue(eventProperties.has("event_duration"));
                try {
                    Float duration = (Float) eventProperties.get("event_duration");
                    Assert.assertTrue(duration >= 1 && duration <= 1.5);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(superKey, eventUnitName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sensorsDataAPI.trackTimerStart(eventUnitName);
        try {
            Thread.sleep(1000);
            Robolectric.getForegroundThreadScheduler().advanceTo(1200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sensorsDataAPI.trackTimerEnd(eventUnitName, jsonObject);
    }

    @Test
    public void getAnonymousIdTest() throws InterruptedException {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        assertNotNull(sensorsDataAPI.getAnonymousId());

        String identify = "SensorsDataAndroid";
        sensorsDataAPI.identify(identify);
        Thread.sleep(500);
        assertEquals(sensorsDataAPI.getAnonymousId(), identify);
    }

    @Test
    public void getDistinctIdTest() throws InterruptedException {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        assertEquals(sensorsDataAPI.getDistinctId(), sensorsDataAPI.getAnonymousId());

        String identify = "SensorsDataAndroid";
        sensorsDataAPI.identify(identify);
        Thread.sleep(500);
        assertEquals(sensorsDataAPI.getDistinctId(), identify);
    }

    @Test
    public void loginTest() throws Exception {
        final SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        final String login_id = "SensorsDataAndroid";
        sensorsDataAPI.login(login_id);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        assertEquals(sensorsDataAPI.getDistinctId(), login_id);
        assertEquals(sensorsDataAPI.getLoginId(), login_id);
        // Load Data From Db
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                if (eventName.equals("SignUp")) {
                    assertEquals(eventName, "$SignUp");
                    assertEquals(eventProperties.opt("type"), "track_signup");
                    assertEquals(eventProperties.opt("distinct_id"), login_id);
                    assertEquals(eventProperties.opt("login_id"), login_id);
                    JSONObject identityJson = eventProperties.optJSONObject("identities");
                    assertNotNull(identityJson);
                    assertEquals(identityJson.opt("$identity_login_id"), login_id);
                    countDownLatch.countDown();
                }
                // clear data
                sensorsDataAPI.logout();
                return false;
            }
        });
        countDownLatch.await(500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void profileSetTest() throws Exception {
        final String key = "profile_SetTest";
        final String value = "profile_SettTest";
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        sensorsDataAPI.deleteAll();
        // 检查事件类型和属性
        Thread.sleep(1500);
        sensorsDataAPI.profileSet(key, value);
        // 检查事件类型和属性
        Thread.sleep(1500);
        String eventData = DatabaseUtilsTest.loadEventFromDb(mApplication);
        assertNotNull(eventData);
        JSONObject jsonObject = new JSONObject(eventData);
        ProfileTestUtils.checkProfileEvent(jsonObject, "profile_set");
        JSONObject propertyJson = jsonObject.optJSONObject("properties");
        assertNotNull(propertyJson);
        assertEquals(propertyJson.opt(key), value);
    }

    @Test
    public void profileIncrementTest() throws Exception {
        final String key = "profile_IncrementTest";
        final int value = 2;
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        sensorsDataAPI.deleteAll();
        // 检查事件类型和属性
        Thread.sleep(1500);
        sensorsDataAPI.profileIncrement(key, value);
        // 检查事件类型和属性
        Thread.sleep(1500);
        String eventData = DatabaseUtilsTest.loadEventFromDb(mApplication);
        assertNotNull(eventData);
        JSONObject jsonObject = new JSONObject(eventData);
        ProfileTestUtils.checkProfileEvent(jsonObject, "profile_increment");
        JSONObject propertyJson = jsonObject.optJSONObject("properties");
        assertNotNull(propertyJson);
        assertEquals(propertyJson.opt(key), value);
    }

    @Test
    public void profileSetOnceTest() throws Exception {
        final String key = "profile_SetOnceTest";
        final String value = "profile_SetOnceTest";
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        sensorsDataAPI.deleteAll();
        // 检查事件类型和属性
        Thread.sleep(1500);
        sensorsDataAPI.profileSetOnce(key, value);
        // 检查事件类型和属性
        Thread.sleep(1500);
        String eventData = DatabaseUtilsTest.loadEventFromDb(mApplication);
        assertNotNull(eventData);
        JSONObject jsonObject = new JSONObject(eventData);
        ProfileTestUtils.checkProfileEvent(jsonObject, "profile_set_once");
        JSONObject propertyJson = jsonObject.optJSONObject("properties");
        assertNotNull(propertyJson);
        assertEquals(propertyJson.opt(key), value);
    }

    @Test
    public void profileAppendTest() throws Exception {
        final String key = "profile_AppendTest";
        final String value = "profile_AppendTest";
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        sensorsDataAPI.deleteAll();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        // 检查事件类型和属性
        sensorsDataAPI.profileAppend(key, value);
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                try {
                    ProfileTestUtils.checkProfileEvent(eventProperties, "profile_append");
                    JSONObject propertyJson = eventProperties.optJSONObject("properties");
                    assertNotNull(propertyJson);
                    assertEquals(propertyJson.optJSONArray(key).get(0), value);
                    countDownLatch.countDown();
                }catch (Exception e){

                }
                return false;
            }
        });
        countDownLatch.await(500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void profilePushIdTest() throws Exception {
        final String key = "profile_PushIdKeyTest";
        final String value = "profile_PushIdValueTest";
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        sensorsDataAPI.deleteAll();
        sensorsDataAPI.profilePushId(key, value);
        // 检查事件类型和属性
        Thread.sleep(1500);
        String distinctId = sensorsDataAPI.getDistinctId();
        String distinctPushId = distinctId + value;
        assertEquals(DbAdapter.getInstance().getPushId("distinctId_" + key), distinctPushId);
        String eventData = DatabaseUtilsTest.loadEventFromDb(mApplication);
        assertNotNull(eventData);
        JSONObject jsonObject = new JSONObject(eventData);
        ProfileTestUtils.checkProfileEvent(jsonObject, "profile_set");
        JSONObject propertyJson = jsonObject.optJSONObject("properties");
        assertNotNull(propertyJson);
        assertEquals(propertyJson.opt(key), value);
        // again
        Thread.sleep(1000);
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                fail();
                return false;
            }
        });
        sensorsDataAPI.profilePushId(key, value);
    }

    @Test
    public void profileUnsetPushIdTest() throws Exception {
        final String key = "profile_PushIdKeyTest";
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        sensorsDataAPI.profilePushId(key, "value");
        Thread.sleep(1500);
        // delete profile_set from push_id
        sensorsDataAPI.deleteAll();
        Thread.sleep(1500);
        sensorsDataAPI.profileUnsetPushId(key);
        // 检查事件类型和属性
        Thread.sleep(1500);
        assertEquals(DbAdapter.getInstance().getPushId("distinctId_" + key), "");
        String eventData = DatabaseUtilsTest.loadEventFromDb(mApplication);
        assertNotNull(eventData);
        JSONObject jsonObject = new JSONObject(eventData);
        ProfileTestUtils.checkProfileEvent(jsonObject, "profile_unset");
        JSONObject propertyJson = jsonObject.optJSONObject("properties");
        assertNotNull(propertyJson);
        assertEquals(propertyJson.opt(key), true);
        // again
        Thread.sleep(1000);
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                fail();
                return false;
            }
        });
        sensorsDataAPI.profileUnsetPushId(key);
    }

    @Test
    public void itemSetTest() throws Exception {
        final String itemType = "itemType_unitTest";
        final String itemId = "itemId_unitTest";
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        JSONObject jsonObjectProperty = new JSONObject();
        jsonObjectProperty.put(itemType, itemId);
        sensorsDataAPI.itemSet(itemType, itemId, jsonObjectProperty);
        // 检查事件类型和属性
        Thread.sleep(1500);
        String eventData = DatabaseUtilsTest.loadEventFromDb(mApplication);
        assertNotNull(eventData);
        JSONObject jsonObject = new JSONObject(eventData);
        ProfileTestUtils.checkItemEvent(jsonObject, "item_set", itemType, itemId);
        JSONObject propertyJson = jsonObject.optJSONObject("properties");
        assertNotNull(propertyJson);
        assertEquals(propertyJson.opt(itemType), itemId);
    }

    @Test
    public void itemDeleteTest() throws Exception {
        final String itemType = "itemType_unitTest";
        final String itemId = "itemId_unitTest";
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        sensorsDataAPI.itemDelete(itemType, itemId);
        // 检查事件类型和属性
        Thread.sleep(1500);
        String eventData = DatabaseUtilsTest.loadEventFromDb(mApplication);
        assertNotNull(eventData);
        JSONObject jsonObject = new JSONObject(eventData);
        ProfileTestUtils.checkItemEvent(jsonObject, "item_delete", itemType, itemId);
    }

    /**
     * 检查预置属性
     *
     * @param jsonObject JSONObject
     */
    private void checkPresetProperty(JSONObject jsonObject) {
        SensorsDataAPI sensorsDataAPI = SensorsDataAPI.sharedInstance();
        JSONObject presetProperty = sensorsDataAPI.getPresetProperties();
        assertNotNull(presetProperty);
        assertNotNull(jsonObject);
        for (Iterator<String> it = presetProperty.keys(); it.hasNext(); ) {
            String key = it.next();
            try {
                assertEquals(presetProperty.opt(key), jsonObject.get(key));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查 JSONObject 是否相等
     *
     * @param source JSONObject
     * @param dest JSONObject
     */
    private void checkJsonEquals(JSONObject source, JSONObject dest) {
        assertNotNull(source);
        assertNotNull(dest);
        assertEquals(source.length(), dest.length());
        for (Iterator<String> it = source.keys(); it.hasNext(); ) {
            String key = it.next();
            try {
                assertEquals(source.opt(key), dest.get(key));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

}
