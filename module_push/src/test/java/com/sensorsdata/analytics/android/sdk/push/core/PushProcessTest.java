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

package com.sensorsdata.analytics.android.sdk.push.core;

import android.app.Application;
import android.app.Notification;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.sdk.MockDataTest;
import com.sensorsdata.analytics.android.sdk.SAHelper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class PushProcessTest {
    Application mApplication = ApplicationProvider.getApplicationContext();

    @Test
    public void getInstance() {
        SAHelper.initSensors(mApplication);
        Assert.assertNotNull(PushProcess.getInstance());
    }

    @Test
    public void hookIntent() {
        SAHelper.initSensors(mApplication);
        Intent intent = new Intent();
        intent.putExtra("SA_PUSH_ID", "mock_push_id");
        PushProcess.getInstance().hookIntent(intent);
    }

    @Test
    public void hookPendingIntent() {
        SAHelper.initSensors(mApplication);
        Intent intent = new Intent();
        intent.putExtra("SA_PUSH_ID", "mock_push_id");
        PushProcess.getInstance().hookPendingIntent(intent, null);
    }

    @Test
    public void onNotificationClick() {
        SAHelper.initSensors(mApplication);
        Intent intent = new Intent();
        intent.putExtra("SA_PUSH_ID", "mock_push_id");
        PushProcess.getInstance().onNotificationClick(mApplication, intent);
    }

    @Test
    public void onNotify() {
        SAHelper.initSensors(mApplication);
        Notification notification = new Notification();
        notification.contentIntent = MockDataTest.mockPendingIntent();
//        notification.extras.putString("android.title", "notifyTitle");
//        notification.extras.putString("android.text", "notifyText");
        PushProcess.getInstance().onNotify("TAG",123, notification);
    }

    @Test
    public void trackGTClickDelayed() {
        SAHelper.initSensors(mApplication);
        PushProcess.getInstance().trackGTClickDelayed("sdajh-asdjfhjas", "mock_title", "mock_content");
    }

    @Test
    public void trackReceiveMessageData() {
        SAHelper.initSensors(mApplication);
        PushProcess.getInstance().trackReceiveMessageData("sdajh-asdjfhjas", "mock_123213");
    }

}