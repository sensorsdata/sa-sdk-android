/*
 * Created by dengshiwei on 2022/07/11.
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

package com.sensorsdata.analytics.android.autotrack.utils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class AppPageLeaveUtilsTest {
    final String url = "com.sensorsdata.test";
    @Test
    public void getLastScreenUrl() {
        AppPageLeaveUtils.setLastScreenUrl(url);
        Assert.assertEquals(url, AppPageLeaveUtils.getLastScreenUrl());
    }

    @Test
    public void setLastScreenUrl() {
        AppPageLeaveUtils.setLastScreenUrl(url);
        Assert.assertEquals(url, AppPageLeaveUtils.getLastScreenUrl());
    }
}