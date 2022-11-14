/*
 * Created by dengshiwei on 2022/10/27.
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

package com.sensorsdata.analytics.android.sdk.monitor;

import static org.junit.Assert.*;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.listener.SAFunctionListener;
import com.sensorsdata.analytics.android.unit_utils.SAHelper;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class TrackMonitorTest {
    Application mApplication = ApplicationProvider.getApplicationContext();
    @Test
    public void addFunctionListener() {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        sensorsDataAPI.addFunctionListener(new SAFunctionListener() {
            @Override
            public void call(String function, JSONObject args) {
                Assert.assertEquals("trackEvent", function);
            }
        });
        sensorsDataAPI.track("AppTest");
    }

    @Test
    public void removeFunctionListener() {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        SAFunctionListener listener = new SAFunctionListener() {
            @Override
            public void call(String function, JSONObject args) {
                Assert.fail();
            }
        };
        sensorsDataAPI.addFunctionListener(listener);
        sensorsDataAPI.removeFunctionListener(listener);
        sensorsDataAPI.track("AppTest");
    }

    @Test
    public void callTrack() {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        sensorsDataAPI.addFunctionListener(new SAFunctionListener() {
            @Override
            public void call(String function, JSONObject args) {
                Assert.assertEquals("trackEvent", function);
            }
        });
        sensorsDataAPI.track("AppTest");
    }

    @Test
    public void callResetAnonymousId() {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        sensorsDataAPI.addFunctionListener(new SAFunctionListener() {
            @Override
            public void call(String function, JSONObject args) {
                Assert.assertTrue(true);
            }
        });
        sensorsDataAPI.resetAnonymousId();
    }

    @Test
    public void callLogin() {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        sensorsDataAPI.addFunctionListener(new SAFunctionListener() {
            @Override
            public void call(String function, JSONObject args) {
                Assert.assertEquals("login", function);
            }
        });
        TrackMonitor.getInstance().callLogin("loginIds");
    }

    @Test
    public void callLogout() {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        sensorsDataAPI.addFunctionListener(new SAFunctionListener() {
            @Override
            public void call(String function, JSONObject args) {
                Assert.assertEquals("logout", function);
            }
        });
        sensorsDataAPI.logout();
    }

    @Test
    public void callIdentify() {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        sensorsDataAPI.addFunctionListener(new SAFunctionListener() {
            @Override
            public void call(String function, JSONObject args) {
                Assert.assertEquals("identify", function);
            }
        });
        sensorsDataAPI.identify("abcded");
    }

    @Test
    public void callEnableDataCollect() {
    }
}