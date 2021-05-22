/*
 * Created by zhangwei on 2019/04/30.
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

package com.sensorsdata.analytics.android.sdk.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sensorsdata.analytics.android.sdk.PropertyBuilder;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentAppEndData;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentAppPaused;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentAppStartTime;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentDistinctId;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstDay;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstStart;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstTrackInstallation;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstTrackInstallationWithCallback;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentIdentity;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoginId;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentRemoteSDKConfig;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentSuperProperties;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * 测试持久化相关逻辑
 */
@RunWith(AndroidJUnit4.class)
public class PersistentTest {
    private static Context context;

    @BeforeClass
    public static void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Ignore
    @Test(expected = RuntimeException.class)
    public void loadInitTest() {
        PersistentLoader.loadPersistent("foo");
    }

    @Test
    public void loadPreferencesTest() {
        PersistentLoader identity = PersistentLoader.initLoader(context);
        PersistentLoader identity2 = PersistentLoader.initLoader(context);
        assertNotNull(identity);
        assertNotNull(identity2);
        assertEquals(identity, identity2);

        PersistentIdentity persistentIdentity = PersistentLoader.loadPersistent(null);
        assertNull(persistentIdentity);
        persistentIdentity = PersistentLoader.loadPersistent("foo");
        assertNull(persistentIdentity);
        persistentIdentity = PersistentLoader.loadPersistent("");
        assertNull(persistentIdentity);
    }

    @Test
    public void persistentDataTest() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.sensorsdata.analytics.android.sdk.SensorsDataAPI", Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();

        // PersistentSuperProperties
        sharedPreferences.edit().clear().apply();
        {
            PersistentSuperProperties persistentSuperProperties = (PersistentSuperProperties) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.SUPER_PROPERTIES);
            assertNotNull(persistentSuperProperties);
            //获取初始化的值
            JSONObject jsonObject = persistentSuperProperties.get();
            assertNotNull(jsonObject);
            assertEquals(0, jsonObject.length());
            //提交一个空值
            persistentSuperProperties.commit(null);
            jsonObject = persistentSuperProperties.get();
            assertNotNull(jsonObject);
            assertEquals(0, jsonObject.length());
            //提交一个非空值
            jsonObject = PropertyBuilder.newInstance().append("key1", "value1", "key2", "value2").toJSONObject();
            persistentSuperProperties.commit(jsonObject);
            JSONObject tmpJsonObj = persistentSuperProperties.get();
            assertNotNull(tmpJsonObj);
            assertEquals(2, tmpJsonObj.length());
            try {
                assertEquals("value1", tmpJsonObj.getString("key1"));
                assertEquals("value2", tmpJsonObj.getString("key2"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //  PersistentAppEndData
        sharedPreferences.edit().clear().apply();
        {
            PersistentAppEndData persistentAppEndData = (PersistentAppEndData) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.APP_END_DATA);
            assertNotNull(persistentAppEndData);
            String dataStr = persistentAppEndData.get();
            assertThat(dataStr, allOf(notNullValue(), equalTo("")));

            persistentAppEndData.commit(null);
            dataStr = persistentAppEndData.get();
            assertThat(dataStr, allOf(notNullValue(), equalTo("")));

            String testStr = "foobar";
            persistentAppEndData.commit(testStr);
            String tmp = persistentAppEndData.get();
            assertThat(tmp, allOf(notNullValue(), equalTo(testStr)));
        }

        //  PersistentAppPaused
        sharedPreferences.edit().clear().apply();
        {
            PersistentAppPaused persistentAppPaused = (PersistentAppPaused) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.APP_PAUSED_TIME);
            assertNotNull(persistentAppPaused);
            long result = persistentAppPaused.get();
            assertEquals(0L, result);

            persistentAppPaused.commit(null);
            result = persistentAppPaused.get();
            assertEquals(0L, result);

            long time = System.currentTimeMillis();
            persistentAppPaused.commit(time);
            result = persistentAppPaused.get();
            assertEquals(time, result);
        }

        //  PersistentAppStartTime
        sharedPreferences.edit().clear().apply();
        {
            PersistentAppStartTime persistentAppStartTime = (PersistentAppStartTime) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.APP_START_TIME);
            assertNotNull(persistentAppStartTime);
            long result = persistentAppStartTime.get();
            assertEquals(0L, result);

            persistentAppStartTime.commit(null);
            result = persistentAppStartTime.get();
            assertEquals(0L, result);

            long time = System.currentTimeMillis();
            persistentAppStartTime.commit(time);
            result = persistentAppStartTime.get();
            assertEquals(time, result);
        }

        //  PersistentDistinctId
        sharedPreferences.edit().clear().apply();
        {
            PersistentDistinctId persistentDistinctId = (PersistentDistinctId) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.DISTINCT_ID);
            assertNotNull(persistentDistinctId);
            String dataStr = persistentDistinctId.get();
            assertThat(dataStr, allOf(notNullValue(), equalTo(SensorsDataUtils.getAndroidID(context))));

            persistentDistinctId.commit(null);
            dataStr = persistentDistinctId.get();
            assertThat(dataStr, allOf(notNullValue(), equalTo(SensorsDataUtils.getAndroidID(context))));

            String testStr = "foobar";
            persistentDistinctId.commit(testStr);
            String tmp = persistentDistinctId.get();
            assertThat(tmp, allOf(notNullValue(), equalTo(testStr)));
        }

        //  PersistentFirstDay
        sharedPreferences.edit().clear().apply();
        {
            PersistentFirstDay persistentFirstDay = (PersistentFirstDay) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.FIRST_DAY);
            assertNotNull(persistentFirstDay);
            String dataStr = persistentFirstDay.get();
            assertNull(dataStr);

            persistentFirstDay.commit(null);
            dataStr = persistentFirstDay.get();
            assertNull(dataStr);

            String testStr = "foobar";
            persistentFirstDay.commit(testStr);
            String tmp = persistentFirstDay.get();
            assertThat(tmp, allOf(notNullValue(), equalTo(testStr)));
        }

        // PersistentFirstStart
        sharedPreferences.edit().clear().apply();
        {
            PersistentFirstStart persistentFirstStart = (PersistentFirstStart) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.FIRST_START);
            assertNotNull(persistentFirstStart);
            boolean result = persistentFirstStart.get();
            assertTrue(result);

            persistentFirstStart.commit(null);
            result = persistentFirstStart.get();
            assertTrue(result);

            persistentFirstStart.commit(false);
            result = persistentFirstStart.get();
            assertFalse(result);

            persistentFirstStart.commit(true);
            result = persistentFirstStart.get();
            assertTrue(result);
        }

        // PersistentFirstTrackInstallation
        sharedPreferences.edit().clear().apply();
        {
            PersistentFirstTrackInstallation persistentFirstTrackInstallation = (PersistentFirstTrackInstallation) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.FIRST_INSTALL);
            assertNotNull(persistentFirstTrackInstallation);
            boolean result = persistentFirstTrackInstallation.get();
            assertTrue(result);

            persistentFirstTrackInstallation.commit(null);
            result = persistentFirstTrackInstallation.get();
            assertTrue(result);

            persistentFirstTrackInstallation.commit(false);
            result = persistentFirstTrackInstallation.get();
            assertFalse(result);

            persistentFirstTrackInstallation.commit(true);
            result = persistentFirstTrackInstallation.get();
            assertTrue(result);
        }

        // PersistentFirstTrackInstallation
        sharedPreferences.edit().clear().apply();
        {
            PersistentFirstTrackInstallationWithCallback persistentFirstTrackInstallationWithCallback =
                    (PersistentFirstTrackInstallationWithCallback) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.FIRST_INSTALL_CALLBACK);
            assertNotNull(persistentFirstTrackInstallationWithCallback);
            boolean result = persistentFirstTrackInstallationWithCallback.get();
            assertTrue(result);

            persistentFirstTrackInstallationWithCallback.commit(null);
            result = persistentFirstTrackInstallationWithCallback.get();
            assertTrue(result);

            persistentFirstTrackInstallationWithCallback.commit(false);
            result = persistentFirstTrackInstallationWithCallback.get();
            assertFalse(result);

            persistentFirstTrackInstallationWithCallback.commit(true);
            result = persistentFirstTrackInstallationWithCallback.get();
            assertTrue(result);
        }

        //  PersistentLoginId
        sharedPreferences.edit().clear().apply();
        {
            PersistentLoginId persistentLoginId = (PersistentLoginId) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.LOGIN_ID);
            assertNotNull(persistentLoginId);
            String dataStr = persistentLoginId.get();
            assertNull(dataStr);

            persistentLoginId.commit(null);
            dataStr = persistentLoginId.get();
            assertNull(dataStr);

            String testStr = "foobar";
            persistentLoginId.commit(testStr);
            String tmp = persistentLoginId.get();
            assertThat(tmp, allOf(notNullValue(), equalTo(testStr)));
        }

        //  PersistentRemoteSDKConfig
        sharedPreferences.edit().clear().apply();
        {
            PersistentRemoteSDKConfig persistentRemoteSDKConfig = (PersistentRemoteSDKConfig) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.REMOTE_CONFIG);
            assertNotNull(persistentRemoteSDKConfig);
            String dataStr = persistentRemoteSDKConfig.get();
            assertNull(dataStr);

            persistentRemoteSDKConfig.commit(null);
            dataStr = persistentRemoteSDKConfig.get();
            assertNull(dataStr);

            String testStr = "foobar";
            persistentRemoteSDKConfig.commit(testStr);
            String tmp = persistentRemoteSDKConfig.get();
            assertThat(tmp, allOf(notNullValue(), equalTo(testStr)));
        }

        //  PersistentSessionIntervalTime
        sharedPreferences.edit().clear().apply();
        {
            PersistentSessionIntervalTime persistentSessionIntervalTime = (PersistentSessionIntervalTime) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.APP_SESSION_TIME);
            assertNotNull(persistentSessionIntervalTime);
            int result = persistentSessionIntervalTime.get();
            assertEquals(30 * 1000, result);

            persistentSessionIntervalTime.commit(null);
            result = persistentSessionIntervalTime.get();
            assertEquals(30 * 1000, result);

            int session = 60 * 1000;
            persistentSessionIntervalTime.commit(session);
            result = persistentSessionIntervalTime.get();
            assertEquals(session, result);
        }
        //finally
        sharedPreferences.edit().clear().apply();
    }

}
