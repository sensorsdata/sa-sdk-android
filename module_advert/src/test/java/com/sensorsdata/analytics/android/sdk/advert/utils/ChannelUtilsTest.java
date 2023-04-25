/*
 * Created by dengshiwei on 2022/12/27.
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

package com.sensorsdata.analytics.android.sdk.advert.utils;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.SAHelper;
import com.sensorsdata.analytics.android.sdk.advert.deeplink.DeepLinkManagerTest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class ChannelUtilsTest {
    Application mApplication = ApplicationProvider.getApplicationContext();

    @Before
    public void initSDK() {
        SAHelper.initSensors(mApplication);
    }

    @Test
    public void getUtmProperties() {
        JSONObject jsonObject = ChannelUtils.getUtmProperties();
        Assert.assertNotNull(jsonObject);
    }

    @Test
    public void getLatestUtmProperties() {
        JSONObject jsonObject = ChannelUtils.getLatestUtmProperties();
        Assert.assertNotNull(jsonObject);
    }

    @Test
    public void hasUtmProperties() {
        JSONObject jsonObject = new JSONObject();
        Assert.assertFalse(ChannelUtils.hasUtmProperties(jsonObject));
        try {
            jsonObject.put("$utm_source", "huawei");
            jsonObject.put("$utm_medium", "yingyong");
            Assert.assertTrue(ChannelUtils.hasUtmProperties(jsonObject));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void hasLinkUtmProperties() {
        Set sets = new HashSet();
        sets.add("utm_source");
        sets.add("utm_medium");
        sets.add("utm_content");
        Assert.assertTrue(ChannelUtils.hasLinkUtmProperties(sets));
    }

    @Test
    public void getDeviceInfo() {
        String str = ChannelUtils.getDeviceInfo(mApplication, "", "abdd12312838_oaid","abdd12312838_oaid");
        Assert.assertNotNull(str);
    }

    @Test
    public void mergeUtmByMetaData() {
        try {
            ChannelUtils.mergeUtmByMetaData(mApplication, new JSONObject());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void setSourceChannelKeys() {
        ChannelUtils.setSourceChannelKeys();
    }

    @Test
    public void parseParams() {
        Map<String, String> params = new HashMap<>();
        params.put("utm_source", "source_value");
        params.put("utm_medium", "medium_value");
        ChannelUtils.parseParams(params);
        JSONObject jsonObject = ChannelUtils.getUtmProperties();
        Assert.assertTrue(jsonObject.has("$utm_source"));
        Assert.assertTrue(jsonObject.has("$utm_medium"));
    }

    @Test
    public void loadUtmByLocal() {
        ChannelUtils.loadUtmByLocal();
    }

    @Test
    public void clearLocalUtm() {
        ChannelUtils.clearLocalUtm();
    }

    @Test
    public void clearMemoryUtm() {
        ChannelUtils.clearMemoryUtm();
    }

    @Test
    public void clearUtm() {
        ChannelUtils.clearUtm();
        JSONObject jsonObject = ChannelUtils.getUtmProperties();
        Assert.assertEquals(0, jsonObject.length());
    }

    @Test
    public void removeDeepLinkInfo() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("$latest_abc", "abc");
            jsonObject.put("normal", "abc_normal");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ChannelUtils.removeDeepLinkInfo(jsonObject);
        Assert.assertFalse(jsonObject.has("$latest_abc"));
        Assert.assertTrue(jsonObject.has("normal"));
    }

    @Test
    public void saveDeepLinkInfo() {
        ChannelUtils.saveDeepLinkInfo();
    }

    @Test
    public void checkOrSetChannelCallbackEvent() {
        final String eventName = "eventName";
        JSONObject property = new JSONObject();
        try {
            property.put("a", "a");
            property.put("b", "b");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            ChannelUtils.checkOrSetChannelCallbackEvent(eventName, property, mApplication);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertTrue(property.optBoolean("$is_channel_callback_event"));
    }

    @Test
    public void isFirstChannelEvent() {
    }

    @Test
    public void hasUtmByMetaData() {
        Assert.assertFalse(ChannelUtils.hasUtmByMetaData(mApplication));
    }

    @Test
    public void isGetDeviceInfo() {
    }

    @Test
    public void isTrackInstallation() {
    }

    @Test
    public void isCorrectTrackInstallation() {
    }

    @Test
    public void saveCorrectTrackInstallation() {
    }

    @Test
    public void checkDeviceInfo() {
    }

    @Test
    public void isDeepLinkBlackList() {
        Assert.assertFalse(ChannelUtils.isDeepLinkBlackList(Robolectric.setupActivity(DeepLinkManagerTest.DeepActivity.class)));
    }

    @Test
    public void commitRequestDeferredDeeplink() {
    }
}