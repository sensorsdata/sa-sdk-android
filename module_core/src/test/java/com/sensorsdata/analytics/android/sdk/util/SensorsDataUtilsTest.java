/*
 * Created by dengshiwei on 2022/06/29.
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

package com.sensorsdata.analytics.android.sdk.util;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.widget.Button;

import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.sdk.ScreenAutoTracker;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class SensorsDataUtilsTest {
    private final static String SA_SERVER_URL = "https://sdkdebugtest.datasink.sensorsdata.cn/sa?project=default&token=cfb8b60e42e0ae9b";
    Application mApplication = ApplicationProvider.getApplicationContext();

    @Test
    public void getCarrier() {
        String carrier = SensorsDataUtils.getCarrier(mApplication);
        System.out.println("carrier = " + carrier);
    }

    @Test
    public void getActivityTitle() {
        TestActivity activity = Robolectric.setupActivity(TestActivity.class);
        String title = SensorsDataUtils.getActivityTitle(activity);
        System.out.println("ActivityTitle = " + title);
        Assert.assertEquals("com.sensorsdata.analytics.android.sdk.util.SensorsDataUtilsTest$TestActivity", title);
    }

    @Test
    public void getToolbarTitle() {
        TestActivity activity = Robolectric.setupActivity(TestActivity.class);
        String title = SensorsDataUtils.getToolbarTitle(activity);
        System.out.println("ActivityTitle = " + title);
        Assert.assertEquals("com.sensorsdata.analytics.android.sdk.util.SensorsDataUtilsTest$TestActivity", title);
    }

    @Test
    public void checkHasPermission() {
        Assert.assertTrue(SensorsDataUtils.checkHasPermission(mApplication, Manifest.permission.READ_PHONE_STATE));
        Assert.assertTrue(SensorsDataUtils.checkHasPermission(mApplication, Manifest.permission.ACCESS_WIFI_STATE));
    }

    @Test
    public void getIMEI() {
        String carrier = SensorsDataUtils.getIMEI(mApplication);
        System.out.println("IMEI = " + carrier);
    }

    @Test
    public void getIMEIOld() {
        String carrier = SensorsDataUtils.getIMEIOld(mApplication);
        System.out.println("IMEIOld = " + carrier);
    }

    @Test
    public void getSlot() {
        String Slot = SensorsDataUtils.getSlot(mApplication, 1);
        System.out.println("Slot = " + Slot);
    }

    @Test
    public void getMEID() {
        String meid = SensorsDataUtils.getMEID(mApplication);
        System.out.println("meid = " + meid);
    }

    @Test
    public void getAndroidID() {
        String androidID = SensorsDataUtils.getAndroidID(mApplication);
        System.out.println("androidID = " + androidID);
    }

    @Test
    public void getMacAddress() {
        String macAddress = SensorsDataUtils.getMacAddress(mApplication);
        System.out.println("macAddress = " + macAddress);
    }

    @Test
    public void isValidAndroidId() {
    }

    @Test
    public void checkVersionIsNew() {
        Assert.assertTrue(SensorsDataUtils.checkVersionIsNew(mApplication, "5.1.0-pre"));
        Assert.assertTrue(SensorsDataUtils.checkVersionIsNew(mApplication, "15.1.0-pre"));
    }

    @Test
    public void isDoubleClick() {
        Button button = new Button(mApplication);
        Assert.assertFalse(SensorsDataUtils.isDoubleClick(button));
        Assert.assertTrue(SensorsDataUtils.isDoubleClick(button));
    }

    @Test
    public void getScreenUrl() {
        TestActivity activity = Robolectric.setupActivity(TestActivity.class);
        String url = SensorsDataUtils.getScreenUrl(activity);
        Assert.assertEquals("UnitTestActivityUrl", url);
    }


    static class TestActivity extends Activity implements ScreenAutoTracker {

        @Override
        public String getScreenUrl() {
            return "UnitTestActivityUrl";
        }

        @Override
        public JSONObject getTrackProperties() throws JSONException {
            return null;
        }
    }
}