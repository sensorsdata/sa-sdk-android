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

import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertiesFetcher;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertyFilter;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class InternalCustomPropertyPluginTest {

    @Test
    public void isMatchedWithFilter() {
        InternalCustomPropertyPlugin customPropertyPlugin = new InternalCustomPropertyPlugin();
        SAPropertyFilter propertyFilter = new SAPropertyFilter();
        JSONObject libProperty = new JSONObject();
        try {
            libProperty.put("$lib", "Android");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        propertyFilter.setEventJson(SAPropertyFilter.LIB, libProperty);
        Assert.assertTrue(customPropertyPlugin.isMatchedWithFilter(propertyFilter));
        try {
            libProperty.put("$lib", "iOS");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        propertyFilter.setEventJson(SAPropertyFilter.LIB, libProperty);
        Assert.assertFalse(customPropertyPlugin.isMatchedWithFilter(propertyFilter));
    }

    @Test
    public void properties() {
        InternalCustomPropertyPlugin customPropertyPlugin = new InternalCustomPropertyPlugin();
        JSONObject libProperty = new JSONObject();
        try {
            libProperty.put("custom", "customAndroid");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        customPropertyPlugin.saveCustom(libProperty);
        JSONObject property = new JSONObject();
        SAPropertiesFetcher propertiesFetcher = new SAPropertiesFetcher();
        propertiesFetcher.setProperties(property);
        customPropertyPlugin.properties(propertiesFetcher);
        Assert.assertEquals("customAndroid", property.optString("custom"));
    }

    @Test
    public void saveCustom() {
        InternalCustomPropertyPlugin customPropertyPlugin = new InternalCustomPropertyPlugin();
        JSONObject libProperty = new JSONObject();
        try {
            libProperty.put("custom", "customAndroid");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        customPropertyPlugin.saveCustom(libProperty);
        JSONObject property = new JSONObject();
        SAPropertiesFetcher propertiesFetcher = new SAPropertiesFetcher();
        propertiesFetcher.setProperties(property);
        customPropertyPlugin.properties(propertiesFetcher);
        Assert.assertEquals("customAndroid", property.optString("custom"));
    }
}