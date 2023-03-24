/*
 * Created by dengshiwei on 2022/11/17.
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

package com.sensorsdata.analytics.android.sdk.push.utils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class PushUtilsTest {

    @Test
    public void getJPushSDKName() {
        Assert.assertEquals("Jpush", PushUtils.getJPushSDKName((byte) 0));
        Assert.assertEquals("Xiaomi", PushUtils.getJPushSDKName((byte) 1));
        Assert.assertEquals("HUAWEI", PushUtils.getJPushSDKName((byte) 2));
        Assert.assertEquals("Meizu", PushUtils.getJPushSDKName((byte) 3));
        Assert.assertEquals("OPPO", PushUtils.getJPushSDKName((byte) 4));
        Assert.assertEquals("vivo", PushUtils.getJPushSDKName((byte) 5));
        Assert.assertEquals("Asus", PushUtils.getJPushSDKName((byte) 6));
        Assert.assertEquals("fcm", PushUtils.getJPushSDKName((byte) 8));
        Assert.assertEquals("Jpush", PushUtils.getJPushSDKName((byte) 100));
    }
}