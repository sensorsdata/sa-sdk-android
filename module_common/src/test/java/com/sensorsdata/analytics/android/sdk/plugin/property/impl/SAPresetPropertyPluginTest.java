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

import static org.junit.Assert.*;

import android.app.Application;
import android.text.TextUtils;

import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertiesFetcher;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertyFilter;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.DeviceUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;
import com.sensorsdata.analytics.android.unit_utils.SAHelper;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class SAPresetPropertyPluginTest {
    Application mApplication = ApplicationProvider.getApplicationContext();
    @Test
    public void isMatchedWithFilter() {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        SAPresetPropertyPlugin presetPropertyPlugin = new SAPresetPropertyPlugin(sensorsDataAPI.getSAContextManager());
        SAPropertyFilter saPropertyFilter = new SAPropertyFilter();
        saPropertyFilter.setType(EventType.ITEM_SET);
        Assert.assertFalse(presetPropertyPlugin.isMatchedWithFilter(saPropertyFilter));
        saPropertyFilter.setType(EventType.TRACK);
        Assert.assertTrue(presetPropertyPlugin.isMatchedWithFilter(saPropertyFilter));
    }

    @Test
    public void properties() {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        SAPresetPropertyPlugin presetPropertyPlugin = new SAPresetPropertyPlugin(sensorsDataAPI.getSAContextManager());
        SAPropertiesFetcher saPropertiesFetcher = new SAPropertiesFetcher();
        JSONObject jsonObject = new JSONObject();
        saPropertiesFetcher.setProperties(jsonObject);
        presetPropertyPlugin.properties(saPropertiesFetcher);
        SAHelper.checkPresetProperty(jsonObject, sensorsDataAPI);
    }

    @Test
    public void getPresetProperties() {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        SAPresetPropertyPlugin presetPropertyPlugin = new SAPresetPropertyPlugin(sensorsDataAPI.getSAContextManager());
        JSONObject jsonObject = presetPropertyPlugin.getPresetProperties();
        SAHelper.checkPresetProperty(jsonObject, sensorsDataAPI);
    }
}