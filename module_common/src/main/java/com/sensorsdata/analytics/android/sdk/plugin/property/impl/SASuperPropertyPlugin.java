/*
 * Created by yuejianzhong on 2022/05/10.
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

import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.plugin.property.SAPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertiesFetcher;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;

import org.json.JSONObject;

/**
 * 静态与动态公共属性插件
 */
public class SASuperPropertyPlugin extends SAPropertyPlugin {
    private final SensorsDataAPI mSensorsDataAPI;

    public SASuperPropertyPlugin(SensorsDataAPI sensorsDataAPI) {
        this.mSensorsDataAPI = sensorsDataAPI;
    }

    @Override
    public void properties(SAPropertiesFetcher saPropertiesFetcher) {
        // read super property
        JSONObject superProperties = mSensorsDataAPI.getSuperProperties();
        // read dynamic property
        JSONObject dynamicProperty = mSensorsDataAPI.getDynamicProperty();
        // merge super and dynamic property
        JSONObject removeDuplicateSuperProperties = JSONUtils.mergeSuperJSONObject(dynamicProperty, superProperties);
        // merge custom property and super property
        JSONUtils.mergeJSONObject(removeDuplicateSuperProperties, saPropertiesFetcher.getProperties());
    }
}
