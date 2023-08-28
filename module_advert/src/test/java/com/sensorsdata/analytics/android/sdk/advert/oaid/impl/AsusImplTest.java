/*
 * Created by dengshiwei on 2022/12/26.
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

package com.sensorsdata.analytics.android.sdk.advert.oaid.impl;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class AsusImplTest {
    Application mApplication = ApplicationProvider.getApplicationContext();

    @Test
    public void isSupported() {
        AsusImpl asus = new AsusImpl(mApplication);
        Assert.assertTrue(asus.isSupported());
    }

    @Test
    public void getRomOAID() {
        AsusImpl asus = new AsusImpl(mApplication);
//        if(asus.isSupported()) {
//            Assert.assertNull(asus.getRomOAID());
//        }
    }
}