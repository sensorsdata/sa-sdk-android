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

import android.app.Activity;
import android.app.Application;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.ToggleButton;

import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.autotrack.core.beans.ViewContext;
import com.sensorsdata.analytics.android.autotrack.core.business.SAPageTools;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class AopUtilTest {
    Application mApplication = ApplicationProvider.getApplicationContext();

    @Test
    public void testAopUtil() {
        buildTitleNoAutoTrackerProperties();
        injectClickInfo();
        isTrackEvent();
    }

    public void buildTitleNoAutoTrackerProperties() {
        ScreenActivity activity = Robolectric.setupActivity(ScreenActivity.class);
        JSONObject jsonObject = AopUtil.buildTitleNoAutoTrackerProperties(activity);
        Assert.assertEquals(activity.getClass().getCanonicalName(), jsonObject.optString("$screen_name"));
        Assert.assertEquals(SAPageTools.getActivityTitle(activity), jsonObject.optString("$title"));
    }

    public void injectClickInfo() {
        ScreenActivity activity = Robolectric.setupActivity(ScreenActivity.class);
        Button button = new Button(mApplication);
        button.setText("UnitTest");
        JSONObject property = new JSONObject();
        Assert.assertNotNull(AopUtil.injectClickInfo(new ViewContext(button), property, true));
        Assert.assertEquals("UnitTest", property.optString("$element_content"));
        Assert.assertEquals("Button", property.optString("$element_type"));
    }

    public void isTrackEvent() {
        ScreenActivity activity = Robolectric.setupActivity(ScreenActivity.class);
        Button button = new Button(mApplication);
        CheckBox checkBox = new CheckBox(activity.getApplication());
        RadioButton RadioButton = new RadioButton(activity);
        ToggleButton ToggleButton = new ToggleButton(mApplication);
        Assert.assertTrue(AopUtil.isTrackEvent(button, true));
        Assert.assertTrue(AopUtil.isTrackEvent(button, false));
        Assert.assertTrue(AopUtil.isTrackEvent(checkBox, true));
        Assert.assertFalse(AopUtil.isTrackEvent(checkBox, false));
        Assert.assertTrue(AopUtil.isTrackEvent(RadioButton, true));
        Assert.assertFalse(AopUtil.isTrackEvent(RadioButton, false));
        Assert.assertTrue(AopUtil.isTrackEvent(ToggleButton, true));
        Assert.assertFalse(AopUtil.isTrackEvent(ToggleButton, false));
    }

    public static class ScreenActivity extends Activity {

    }
}