/*
 * Created by zhangwei on 2019/04/25.
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
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
public class SensorsDataUtilsTest {
    private static final String TAG = "SensorsDataUtilsTest";
    @Rule
    public GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_PHONE_STATE);
    @Rule
    public GrantPermissionRule readPermissionRule =
            GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
    @Rule
    public GrantPermissionRule writePermissionRule =
            GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private Context context;

    public static void beforeClass() {
    }

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Ignore
    @Test
    public void getIMEITest() {
        String imei = SensorsDataUtils.getIMEI(context);
        Log.i(TAG, "imei value= " + imei);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            assertTrue(TextUtils.isEmpty(imei));
        } else {
            //如果是模拟器
            assertFalse("You should run this unit test on a real device, not an emulator", TextUtils.isEmpty(imei));
        }
    }
}
