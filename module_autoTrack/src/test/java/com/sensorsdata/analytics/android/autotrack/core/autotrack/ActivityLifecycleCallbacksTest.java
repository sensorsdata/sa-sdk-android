/*
 * Created by dengshiwei on 2022/07/13.
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

package com.sensorsdata.analytics.android.autotrack.core.autotrack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.autotrack.SAHelper;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataTrackEventCallBack;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstStart;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class ActivityLifecycleCallbacksTest {
    Application mApplication = ApplicationProvider.getApplicationContext();
    ActivityLifecycleCallbacks mActivityLifecycle;
    AutoTrackActivity mActivity = Robolectric.setupActivity(AutoTrackActivity.class);
    SensorsDataAPI mSensorsDataAPI;
    @Before
    public void setUp() {
        mSensorsDataAPI = SAHelper.initSensors(mApplication);
        mActivityLifecycle = new ActivityLifecycleCallbacks(mSensorsDataAPI.getSAContextManager());
    }

    @Test
    public void onActivityCreated() {
        mActivityLifecycle.onActivityCreated(mActivity, null);
    }

    @Test
    public void onActivityStarted() throws InterruptedException {
        mActivityLifecycle.onActivityStarted(mActivity);
        mSensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                System.out.println(eventName);
                System.out.println(eventProperties);
                assertTrue(SAHelper.checkPresetProperty(eventProperties));
                assertEquals("$AppStart", eventName);
                assertTrue(eventProperties.optBoolean("$is_first_time"));
                assertTrue(eventProperties.has("$screen_name"));
                assertTrue(eventProperties.optBoolean("$title"));
                return true;
            }
        });
        onActivityResumed();
        onActivityStopped();
    }

    public void onActivityResumed() throws InterruptedException {
        mActivityLifecycle.onActivityResumed(mActivity);
        mSensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                System.out.println(eventName);
                System.out.println(eventProperties);
                assertTrue(SAHelper.checkPresetProperty(eventProperties));
                assertEquals("$AppViewScreen", eventName);
                assertTrue(eventProperties.has("$screen_name"));
                assertTrue(eventProperties.optBoolean("$title"));
                return true;
            }
        });
    }

    @Test
    public void onActivityPaused() {
        mActivityLifecycle.onActivityPaused(mActivity);
    }

    @Test
    public void onActivitySaveInstanceState() {
        mActivityLifecycle.onActivitySaveInstanceState(mActivity, null);
    }

    public void onActivityStopped() {
        mActivityLifecycle.onActivityStopped(mActivity);
        mSensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                System.out.println(eventName);
                System.out.println(eventProperties);
                assertTrue(SAHelper.checkPresetProperty(eventProperties));
                assertEquals("$AppEnd", eventName);
                assertTrue(eventProperties.has("$screen_name"));
                assertTrue(eventProperties.optBoolean("$title"));
                return true;
            }
        });
    }

    @Test
    public void onActivityDestroyed() {
        mActivityLifecycle.onActivityDestroyed(mActivity);
    }

    @Test
    public void onNewIntent() {
    }

    @Test
    public void uncaughtException() {
    }

    @Test
    public void addActivity() {
        mActivityLifecycle.addActivity(mActivity);
    }

    @Test
    public void hasActivity() {
        mActivityLifecycle.addActivity(mActivity);
        assertTrue(mActivityLifecycle.hasActivity(mActivity));
    }

    @Test
    public void removeActivity() {
        mActivityLifecycle.removeActivity(mActivity);
    }

    public static class AutoTrackActivity extends Activity {
        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setTheme(android.R.style.Theme_Material);
        }
    }
}