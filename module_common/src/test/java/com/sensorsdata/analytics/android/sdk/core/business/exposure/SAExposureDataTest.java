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

package com.sensorsdata.analytics.android.sdk.core.business.exposure;

import static org.junit.Assert.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class SAExposureDataTest {

    @Test
    public void getExposureConfig() {
        SAExposureData exposureData = new SAExposureData("ExposeEvent");
        Assert.assertNull(exposureData.getExposureConfig());
    }

    @Test
    public void setExposureConfig() {
        SAExposureData exposureData = new SAExposureData("ExposeEvent");
        exposureData.setExposureConfig(new SAExposureConfig(1, 1,false));
        Assert.assertNotNull(exposureData.getExposureConfig());
    }

    @Test
    public void getProperties() {
        SAExposureData exposureData = new SAExposureData("ExposeEvent");
        exposureData.setProperties(new JSONObject());
        Assert.assertNotNull(exposureData.getProperties());
    }

    @Test
    public void setProperties() throws JSONException {
        SAExposureData exposureData = new SAExposureData("ExposeEvent");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("expose", "expose");
        exposureData.setProperties(jsonObject);
        Assert.assertEquals(exposureData.getProperties().optString("expose"), "expose");
    }

    @Test
    public void getEvent() {
        SAExposureData exposureData = new SAExposureData("ExposeEvent");
        Assert.assertEquals("ExposeEvent", exposureData.getEvent());
    }

    @Test
    public void setEvent() {
        SAExposureData exposureData = new SAExposureData("ExposeEvent");
        exposureData.setEvent("SetExposeEvent");
        Assert.assertEquals("SetExposeEvent", exposureData.getEvent());
    }

    @Test
    public void getIdentifier() {
        SAExposureData exposureData = new SAExposureData("ExposeEvent", "viewIdentifier");
        Assert.assertEquals("viewIdentifier", exposureData.getIdentifier());
    }

    @Test
    public void testEquals() {
    }

    @Test
    public void testToString() {
    }
}