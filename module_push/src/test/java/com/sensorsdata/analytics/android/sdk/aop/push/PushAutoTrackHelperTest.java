/*
 * Created by dengshiwei on 2022/11/16.
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

package com.sensorsdata.analytics.android.sdk.aop.push;

import static org.junit.Assert.assertEquals;

import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.sdk.MockDataTest;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataTrackEventCallBack;
import com.sensorsdata.analytics.android.sdk.SAHelper;
import com.sensorsdata.analytics.android.sdk.dialog.SchemeActivity;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class PushAutoTrackHelperTest {
    Application mApplication = ApplicationProvider.getApplicationContext();

    @Test
    public void trackJPushOpenActivity() throws InterruptedException {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                assertEquals("$AppPushClick", eventName);
                assertEquals("mock_title", eventProperties.optString("$app_push_msg_title"));
                assertEquals("mock_content", eventProperties.optString("$app_push_msg_content"));
                assertEquals("JPush", eventProperties.optString("$app_push_service_name"));
                countDownLatch.countDown();
                return true;
            }
        });
        try {
            Thread.sleep(1000);
            Robolectric.getForegroundThreadScheduler().advanceTo(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        PushAutoTrackHelper.trackJPushOpenActivity(MockDataTest.mockJPushIntent());
        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void trackNotificationOpenedEvent() {
        try {
            trackJPushOpenActivity();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void trackGeTuiNotificationClicked() {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                assertEquals("$AppPushClick", eventName);
                assertEquals("mock_title", eventProperties.optString("$app_push_msg_title"));
                assertEquals("mock_content", eventProperties.optString("$app_push_msg_content"));
                assertEquals("JPush", eventProperties.optString("$app_push_service_name"));
                countDownLatch.countDown();
                return true;
            }
        });
        try {
            Thread.sleep(1000);
            Robolectric.getForegroundThreadScheduler().advanceTo(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        PushAutoTrackHelper.trackGeTuiNotificationClicked("mock_title", "mock_content", "", 0L);
        try {
            countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void trackJPushAppOpenNotification() {
        PushAutoTrackHelper.trackJPushAppOpenNotification("", "mock_jpush", "mock_content", "JPush");
    }

    @Test
    public void trackMeizuAppOpenNotification() throws InterruptedException {
        PushAutoTrackHelper.trackMeizuAppOpenNotification("", "mock_meizu", "mock_content", "JPush");
    }

    @Test
    public void onGeTuiNotificationClicked() {
        PushAutoTrackHelper.onGeTuiNotificationClicked(new GetTuiData());
    }

    @Test
    public void onGeTuiReceiveMessageData() {
        PushAutoTrackHelper.onGeTuiReceiveMessageData(new GetTuiData());
    }

    @Test
    public void onUMengNotificationClick() {
        PushAutoTrackHelper.onUMengNotificationClick(null);
    }

    @Test
    public void onUMengActivityMessage() {
        PushAutoTrackHelper.onUMengActivityMessage(null);
    }

    @Test
    public void onNewIntent() {
        SchemeActivity activity = Robolectric.setupActivity(SchemeActivity.class);
        PushAutoTrackHelper.onNewIntent(activity, activity.getIntent());
    }

    @Test
    public void onBroadcastReceiver() {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

            }
        };
        PushAutoTrackHelper.onBroadcastReceiver(broadcastReceiver, mApplication, null);
    }

    @Test
    public void onServiceStart() {
        Service service = new Service() {
            @Nullable
            @Override
            public IBinder onBind(Intent intent) {
                return null;
            }
        };
        PushAutoTrackHelper.onServiceStart(service, null, 100);
    }

    @Test
    public void onServiceStartCommand() {
        Service service = new Service() {
            @Nullable
            @Override
            public IBinder onBind(Intent intent) {
                return null;
            }
        };
        PushAutoTrackHelper.onServiceStartCommand(service, null, 100,100);
    }

    @Test
    public void hookIntentGetBroadcast() {
        PushAutoTrackHelper.hookIntentGetBroadcast(mApplication, 100, MockDataTest.mockJPushIntent(), 100);
    }

    @Test
    public void hookPendingIntentGetBroadcast() {
        PushAutoTrackHelper.hookPendingIntentGetBroadcast(MockDataTest.mockPendingIntent(), mApplication, 100, MockDataTest.mockJPushIntent(), 100);
    }

    @Test
    public void hookIntentGetService() {
        PushAutoTrackHelper.hookIntentGetService(mApplication, 100, MockDataTest.mockJPushIntent(), 100);
    }

    @Test
    public void hookPendingIntentGetService() {
        PushAutoTrackHelper.hookPendingIntentGetService(MockDataTest.mockPendingIntent(), mApplication, 100, null, 100);
    }

    @Test
    public void hookIntentGetForegroundService() {
        PushAutoTrackHelper.hookIntentGetForegroundService(mApplication,100, MockDataTest.mockJPushIntent(),  100);
    }

    @Test
    public void hookPendingIntentGetForegroundService() {
        PushAutoTrackHelper.hookPendingIntentGetForegroundService(MockDataTest.mockPendingIntent(), mApplication, 100, null, 100);
    }

    class GetTuiData {
        public String getMessageId() {
            return "getuiId";
        }
        public String getTitle() {
            return "getuiTitle";
        }
        public String getContent() {
            return "getuiContent";
        }
        public String getPayload() {
            return "getPayload";
        }
    }
}