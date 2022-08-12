/*
 * Created by chenru on 2022/7/6 上午10:08.
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

package com.sensorsdata.analytics.android.sdk.advert.scan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Application;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.SAHelper;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.ServerUrl;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class WhiteListTest {
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


    @Test
    public void scanTest() {
        initSensorsSDKTest();
        WhiteListScanHelper scanHelper = new WhiteListScanHelper();
        TestActivity activity = Robolectric.setupActivity(TestActivity.class);
        Uri uri = Uri.parse("sa34312b30://adsScanDeviceInfo?apiurl=http%3A%2F%2F10.120.219.0%3A8107%2Fapi%2Fadvertising-management%2Fdebug%2Fscan%2Fupdate&device_type=2&info_id=9e7dc7114c0d2&project=production");
        scanHelper.handlerScanUri(activity, uri);
        Assert.assertEquals(uri.getQueryParameter("device_type"), "2");
        Assert.assertNotNull(uri.getQueryParameter("apiurl"));
        Assert.assertNotNull(uri.getQueryParameter("info_id"));
        ServerUrl serverUrl = new ServerUrl(SensorsDataAPI.sharedInstance().getServerUrl());
        Assert.assertEquals(uri.getQueryParameter("project"), serverUrl.getProject());
    }

    static class TestActivity extends Activity{
    }
}
