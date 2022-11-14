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

package com.sensorsdata.analytics.android.sdk.plugin.property.impl;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataTrackEventCallBack;
import com.sensorsdata.analytics.android.unit_utils.SAHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class SASuperPropertyPluginTest {
    static Application mApplication = ApplicationProvider.getApplicationContext();
    @Test
    public void properties() {
        JSONObject superProperty = new JSONObject();
        try {
            superProperty.put("superProperty", "superProperty");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        sensorsDataAPI.registerSuperProperties(superProperty);
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                if ("AppTest".equals(eventName)) {
                    Assert.assertTrue(eventProperties.has("superProperty"));
                }
                return false;
            }
        });
        sensorsDataAPI.track("AppTest");
    }
}