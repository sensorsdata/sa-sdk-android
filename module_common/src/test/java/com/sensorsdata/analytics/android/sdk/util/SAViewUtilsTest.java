/*
 * Created by dengshiwei on 2022/11/08.
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

package com.sensorsdata.analytics.android.sdk.util;

import static org.junit.Assert.*;

import android.app.Application;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.unit_utils.SAHelper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class SAViewUtilsTest {
    Application mApplication = ApplicationProvider.getApplicationContext();
    @Test
    public void traverseView() {
        LinearLayout linearLayout = new LinearLayout(mApplication);
        TextView textView1 = new TextView(mApplication);
        textView1.setText("child1");
        TextView textView2 = new TextView(mApplication);
        textView2.setText("child2");
        linearLayout.addView(textView1);
        linearLayout.addView(textView2);
        StringBuilder stringBuilder = new StringBuilder();
        String result = SAViewUtils.traverseView(stringBuilder, linearLayout);
        Assert.assertEquals("child1-child2-", result);
    }

    @Test
    public void isViewIgnored() {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        TextView textView1 = new TextView(mApplication);
        textView1.setText("child1");
        sensorsDataAPI.ignoreView(textView1);
        Assert.assertTrue(SAViewUtils.isViewIgnored(textView1));
    }

    @Test
    public void testIsViewIgnored() {
        SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
        TextView textView1 = new TextView(mApplication);
        textView1.setText("child1");
        sensorsDataAPI.ignoreView(textView1);
        Assert.assertTrue(SAViewUtils.isViewIgnored(textView1));
    }

    @Test
    public void getViewTypeByReflect() {
        TextView textView1 = new TextView(mApplication);
        textView1.setText("child1");
        Assert.assertEquals("android.widget.TextView",
                SAViewUtils.getViewTypeByReflect(textView1));
    }

    @Test
    public void getActivityOfView() {
        TextView textView1 = new TextView(mApplication);
        textView1.setText("child1");
        Assert.assertNull(SAViewUtils.getActivityOfView(mApplication, textView1));
    }

    @Test
    public void getViewGroupTypeByReflect() {
        LinearLayout linearLayout = new LinearLayout(mApplication);
        Assert.assertEquals("android.widget.LinearLayout",
                SAViewUtils.getViewGroupTypeByReflect(linearLayout));
    }

    @Test
    public void getViewContent() {
        TextView textView1 = new TextView(mApplication);
        textView1.setText("child1");
        Assert.assertEquals("child1", SAViewUtils.getViewContent(textView1));
    }

    @Test
    public void testGetViewContent() {
        TextView textView1 = new TextView(mApplication);
        textView1.setText("child1");
        Assert.assertEquals("child1", SAViewUtils.getViewContent(textView1));
    }

    @Test
    public void getViewType() {
        CheckBox textView1 = new CheckBox(mApplication);
        textView1.setText("child1");
        Assert.assertEquals("CheckBox",
                SAViewUtils.getViewType(textView1));
    }

    @Test
    public void instanceOfBottomNavigationItemView() {
        CheckBox textView1 = new CheckBox(mApplication);
        textView1.setText("child1");
        Assert.assertFalse(SAViewUtils.instanceOfActionMenuItem(textView1));
    }

    @Test
    public void instanceOfNavigationView() {
        CheckBox textView1 = new CheckBox(mApplication);
        textView1.setText("child1");
        Assert.assertFalse(SAViewUtils.instanceOfNavigationView(textView1));
    }

    @Test
    public void instanceOfSupportListMenuItemView() {
        CheckBox textView1 = new CheckBox(mApplication);
        textView1.setText("child1");
        Assert.assertFalse(SAViewUtils.instanceOfSupportListMenuItemView(textView1));
    }

    @Test
    public void instanceOfAndroidXListMenuItemView() {
        CheckBox textView1 = new CheckBox(mApplication);
        textView1.setText("child1");
        Assert.assertFalse(SAViewUtils.instanceOfAndroidXListMenuItemView(textView1));
    }

    @Test
    public void instanceOfActionMenuItem() {
        CheckBox textView1 = new CheckBox(mApplication);
        textView1.setText("child1");
        Assert.assertFalse(SAViewUtils.instanceOfActionMenuItem(textView1));
    }

    @Test
    public void instanceOfToolbar() {
        CheckBox textView1 = new CheckBox(mApplication);
        textView1.setText("child1");
        Assert.assertFalse(SAViewUtils.instanceOfToolbar(textView1));
    }

    @Test
    public void getMenuItemData() {
        CheckBox textView1 = new CheckBox(mApplication);
        Assert.assertNull(SAViewUtils.getMenuItemData(textView1));
    }

    @Test
    public void isViewSelfVisible() {
        CheckBox textView1 = new CheckBox(mApplication);
        textView1.setVisibility(View.VISIBLE);
        Assert.assertFalse(SAViewUtils.isViewSelfVisible(textView1));
    }

    @Test
    public void getChildIndex() {
        LinearLayout linearLayout = new LinearLayout(mApplication);
        TextView textView1 = new TextView(mApplication);
        textView1.setText("child1");
        TextView textView2 = new TextView(mApplication);
        textView2.setText("child2");
        linearLayout.addView(textView1);
        linearLayout.addView(textView2);
        Assert.assertEquals(0, SAViewUtils.getChildIndex(linearLayout, textView1));
        Assert.assertEquals(1, SAViewUtils.getChildIndex(linearLayout, textView2));
    }

    @Test
    public void getViewId() {
        TextView textView1 = new TextView(mApplication);
        textView1.setText("child1");
        textView1.setId(123213);
        Assert.assertNull(SAViewUtils.getViewId(textView1));
    }

    @Test
    public void getScreenNameAndTitle() {
        TextView textView1 = new TextView(mApplication);
        textView1.setText("child1");
        Assert.assertNull(SAViewUtils.getScreenNameAndTitle(textView1));
    }

    @Test
    public void buildTitleAndScreenName() {
        TextView textView1 = new TextView(mApplication);
        textView1.setText("child1");
        SensorsDataUtilsTest.TestActivity activity = Robolectric.setupActivity(SensorsDataUtilsTest.TestActivity.class);
        SAViewUtils.buildTitleAndScreenName(activity);
    }
}