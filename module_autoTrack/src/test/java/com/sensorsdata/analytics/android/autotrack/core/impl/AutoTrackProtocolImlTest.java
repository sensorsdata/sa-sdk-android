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

package com.sensorsdata.analytics.android.autotrack.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.ListActivity;
import android.view.View;
import android.widget.Button;

import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.autotrack.SAHelper;
import com.sensorsdata.analytics.android.autotrack.core.autotrack.ActivityLifecycleCallbacks;
import com.sensorsdata.analytics.android.autotrack.core.autotrack.ActivityLifecycleCallbacksTest;
import com.sensorsdata.analytics.android.sdk.SensorsAnalyticsAutoTrackEventType;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataTrackEventCallBack;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstStart;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class AutoTrackProtocolImlTest {
    Application mApplication = ApplicationProvider.getApplicationContext();
    ActivityLifecycleCallbacksTest.AutoTrackActivity mActivity = Robolectric.setupActivity(ActivityLifecycleCallbacksTest.AutoTrackActivity.class);
    SensorsDataAPI mSensorsDataAPI;
    AutoTrackProtocolIml mAutoTrackImp;

    @Test
    public void testAutoTrackProtocolImp() {
        enableAutoTrack();
        disableAutoTrack();
        testDisableAutoTrack();
        isAutoTrackEnabled();
        ignoreAutoTrackActivities();
        resumeAutoTrackActivities();
        ignoreAutoTrackActivity();
        resumeAutoTrackActivity();
        isActivityAutoTrackAppViewScreenIgnored();
        isActivityAutoTrackAppClickIgnored();
        isAutoTrackEventTypeIgnored();
        testIsAutoTrackEventTypeIgnored();
        setViewID();
        testSetViewID();
        testSetViewID1();
        setViewActivity();
        setViewFragmentName();
        ignoreView();
        testIgnoreView();
        try {
            setViewProperties();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        getIgnoredViewTypeList();
        ignoreViewType();
        getLastScreenUrl();
        clearReferrerWhenAppEnd();
        clearLastScreenUrl();
        getLastScreenTrackProperties();
        isTrackFragmentAppViewScreenEnabled();
        enableAutoTrackFragment();
        enableAutoTrackFragments();
        ignoreAutoTrackFragments();
        ignoreAutoTrackFragment();
        resumeIgnoredAutoTrackFragments();
        resumeIgnoredAutoTrackFragment();
//        trackViewScreen();
//        testTrackViewScreen();
//        testTrackViewScreen1();
//        trackViewAppClick();
//        testTrackViewAppClick();
//        trackFragmentAppViewScreen();
    }

    public void setUp() {
        mSensorsDataAPI = null;
        mAutoTrackImp = null;
        mSensorsDataAPI = SAHelper.initSensors(ApplicationProvider.getApplicationContext());
        List<SensorsDataAPI.AutoTrackEventType> list = new ArrayList<>();
        list.add(SensorsDataAPI.AutoTrackEventType.APP_VIEW_SCREEN);
        mSensorsDataAPI.enableAutoTrack(list);
        mAutoTrackImp = new AutoTrackProtocolIml(mSensorsDataAPI.getSAContextManager());
    }

    public void enableAutoTrack() {
        setUp();
        List<SensorsDataAPI.AutoTrackEventType> list = new ArrayList<>();
        list.add(SensorsDataAPI.AutoTrackEventType.APP_START);
        list.add(SensorsDataAPI.AutoTrackEventType.APP_VIEW_SCREEN);
        mAutoTrackImp.enableAutoTrack(list);
        System.out.println(mAutoTrackImp.isAutoTrackEnabled());
        Assert.assertTrue(mAutoTrackImp.isAutoTrackEnabled());
    }

    public void disableAutoTrack() {
        setUp();
        mAutoTrackImp.disableAutoTrack(SensorsDataAPI.AutoTrackEventType.APP_START);
        Assert.assertTrue(mAutoTrackImp.isAutoTrackEnabled());
    }

    public void testDisableAutoTrack() {
        setUp();
        List<SensorsDataAPI.AutoTrackEventType> typeList = new ArrayList<>();
        typeList.add(SensorsDataAPI.AutoTrackEventType.APP_START);
        typeList.add(SensorsDataAPI.AutoTrackEventType.APP_END);
        mAutoTrackImp.disableAutoTrack(typeList);
        Assert.assertTrue(mAutoTrackImp.isAutoTrackEnabled());
    }

    public void isAutoTrackEnabled() {
        setUp();
        List<SensorsDataAPI.AutoTrackEventType> list = new ArrayList<>();
        list.add(SensorsDataAPI.AutoTrackEventType.APP_START);
        mAutoTrackImp.enableAutoTrack(list);
        Assert.assertTrue(mAutoTrackImp.isAutoTrackEnabled());
    }

    public void ignoreAutoTrackActivities() {
        setUp();
        List<Class<?>> activities = new ArrayList<>();
        activities.add(mActivity.getClass());
        activities.add(ListActivity.class);
        mAutoTrackImp.ignoreAutoTrackActivities(activities);
        Assert.assertTrue(mAutoTrackImp.isActivityAutoTrackAppClickIgnored(mActivity.getClass()));
    }

    public void resumeAutoTrackActivities() {
        setUp();
        List<Class<?>> activities = new ArrayList<>();
        activities.add(mActivity.getClass());
        activities.add(ListActivity.class);
        mAutoTrackImp.resumeAutoTrackActivities(activities);
        Assert.assertFalse(mAutoTrackImp.isActivityAutoTrackAppClickIgnored(mActivity.getClass()));
    }

    public void ignoreAutoTrackActivity() {
        setUp();
        mAutoTrackImp.ignoreAutoTrackActivity(mActivity.getClass());
        Assert.assertTrue(mAutoTrackImp.isActivityAutoTrackAppClickIgnored(mActivity.getClass()));
    }

    public void resumeAutoTrackActivity() {
        setUp();
        mAutoTrackImp.ignoreAutoTrackActivity(mActivity.getClass());
        mAutoTrackImp.resumeAutoTrackActivity(mActivity.getClass());
        Assert.assertFalse(mAutoTrackImp.isActivityAutoTrackAppClickIgnored(mActivity.getClass()));
    }

    public void isActivityAutoTrackAppViewScreenIgnored() {
        setUp();
        mAutoTrackImp.ignoreAutoTrackActivity(mActivity.getClass());
        Assert.assertTrue(mAutoTrackImp.isActivityAutoTrackAppViewScreenIgnored(mActivity.getClass()));
    }

    public void isActivityAutoTrackAppClickIgnored() {
        setUp();
        mAutoTrackImp.ignoreAutoTrackActivity(mActivity.getClass());
        Assert.assertTrue(mAutoTrackImp.isActivityAutoTrackAppViewScreenIgnored(mActivity.getClass()));
    }

    public void isAutoTrackEventTypeIgnored() {
        setUp();
        List<SensorsDataAPI.AutoTrackEventType> list = new ArrayList<>();
        list.add(SensorsDataAPI.AutoTrackEventType.APP_START);
        list.add(SensorsDataAPI.AutoTrackEventType.APP_VIEW_SCREEN);
        list.add(SensorsDataAPI.AutoTrackEventType.APP_CLICK);
        mSensorsDataAPI.enableAutoTrack(list);
        Assert.assertFalse(mAutoTrackImp.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_CLICK));
        mAutoTrackImp.disableAutoTrack(SensorsDataAPI.AutoTrackEventType.APP_CLICK);
        Assert.assertTrue(mAutoTrackImp.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_CLICK));
    }

    public void testIsAutoTrackEventTypeIgnored() {
        setUp();
        Assert.assertFalse(mAutoTrackImp.isAutoTrackEventTypeIgnored(SensorsAnalyticsAutoTrackEventType.APP_START));
    }

    public void setViewID() {
        setUp();
        View view = new View(mApplication);
        mAutoTrackImp = new AutoTrackProtocolIml(mSensorsDataAPI.getSAContextManager());
        mAutoTrackImp.setViewID(view, "R.id.login");
        Object tag = view.getTag(com.sensorsdata.analytics.android.sdk.R.id.sensors_analytics_tag_view_id);
        Assert.assertEquals("R.id.login", tag);
    }

    public void testSetViewID() {
        setUp();
        Dialog view = new Dialog(mApplication);
        mAutoTrackImp.setViewID(view, "R.id.login");
        Object tag = view.getWindow().getDecorView().getTag(com.sensorsdata.analytics.android.sdk.R.id.sensors_analytics_tag_view_id);
        Assert.assertEquals("R.id.login", tag);
    }

    public void testSetViewID1() {
        setUp();
        Object view = new androidx.appcompat.app.AlertDialog.Builder(mApplication).create();
        mAutoTrackImp.setViewID(view, "R.id.login");
        Object tag = ((androidx.appcompat.app.AlertDialog) view).getWindow().getDecorView().getTag(com.sensorsdata.analytics.android.sdk.R.id.sensors_analytics_tag_view_id);
        Assert.assertEquals("R.id.login", tag);
    }

    public void setViewActivity() {
        setUp();
        View view = new View(mApplication);
        mAutoTrackImp.setViewActivity(view, mActivity);
        Object tag = view.getTag(com.sensorsdata.analytics.android.sdk.R.id.sensors_analytics_tag_view_activity);
        Assert.assertEquals(mActivity, tag);
    }

    public void setViewFragmentName() {
        setUp();
        View view = new View(mApplication);
        mAutoTrackImp.setViewFragmentName(view, "com.sensorsdata.fragment");
        Object tag = view.getTag(com.sensorsdata.analytics.android.sdk.R.id.sensors_analytics_tag_view_fragment_name2);
        Assert.assertEquals("com.sensorsdata.fragment", tag);
    }

    public void ignoreView() {
        setUp();
        View view = new View(mApplication);
        mAutoTrackImp.ignoreView(view);
        Object tag = view.getTag(com.sensorsdata.analytics.android.sdk.R.id.sensors_analytics_tag_view_ignored);
        Assert.assertEquals("1", tag);
    }

    public void testIgnoreView() {
        setUp();
        View view = new View(mApplication);
        mAutoTrackImp.ignoreView(view, true);
        Object tag = view.getTag(com.sensorsdata.analytics.android.sdk.R.id.sensors_analytics_tag_view_ignored);
        Assert.assertEquals("1", tag);
    }

    public void setViewProperties() throws JSONException {
        setUp();
        View view = new View(mApplication);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", "view");
        mAutoTrackImp.setViewProperties(view, jsonObject);
        Object tag = view.getTag(com.sensorsdata.analytics.android.sdk.R.id.sensors_analytics_tag_view_properties);
        Assert.assertNotNull(tag);
        Assert.assertEquals(jsonObject, tag);
    }

    public void getIgnoredViewTypeList() {
        setUp();
        mAutoTrackImp.ignoreViewType(Button.class);
        Assert.assertEquals(1, mAutoTrackImp.getIgnoredViewTypeList().size());
    }

    public void ignoreViewType() {
        setUp();
        mAutoTrackImp.ignoreViewType(Button.class);
        Assert.assertEquals(1, mAutoTrackImp.getIgnoredViewTypeList().size());
    }

    public void getLastScreenUrl() {
        setUp();
        Assert.assertNull(mAutoTrackImp.getLastScreenUrl());
    }

    public void clearReferrerWhenAppEnd() {
        setUp();
        mAutoTrackImp.clearReferrerWhenAppEnd();
        Assert.assertNull(mAutoTrackImp.getLastScreenUrl());
    }

    public void clearLastScreenUrl() {
        setUp();
        mAutoTrackImp.clearLastScreenUrl();
        Assert.assertNull(mAutoTrackImp.getLastScreenUrl());
    }

    public void getLastScreenTrackProperties() {
        setUp();
        Assert.assertNull(mAutoTrackImp.getLastScreenTrackProperties());
    }

    public void trackViewScreen() {
        setUp();
        mSensorsDataAPI.setTrackEventCallBack(null);
        mSensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                assertTrue(SAHelper.checkPresetProperty(eventProperties));
                assertEquals("$AppViewScreen", eventName);
                assertTrue(eventProperties.has("$screen_name"));
                assertTrue(eventProperties.optBoolean("$title"));
                return false;
            }
        });
        mAutoTrackImp.trackViewScreen("AppViewScreenUrl", new JSONObject());
    }

    public void testTrackViewScreen() {
        setUp();
        mSensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                assertTrue(SAHelper.checkPresetProperty(eventProperties));
                assertEquals("$AppViewScreen", eventName);
                assertTrue(eventProperties.has("$screen_name"));
                assertTrue(eventProperties.optBoolean("$title"));
                return false;
            }
        });
        mSensorsDataAPI.trackViewScreen(new Fragment());
    }

    public void testTrackViewScreen1() {
        setUp();
        mSensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                assertTrue(SAHelper.checkPresetProperty(eventProperties));
                assertEquals("$AppViewScreen", eventName);
                assertTrue(eventProperties.has("$screen_name"));
                assertTrue(eventProperties.optBoolean("$title"));
                return false;
            }
        });
        mSensorsDataAPI.trackViewScreen(mActivity);
    }

    public void trackViewAppClick() {
        setUp();
        View view = new View(mActivity);
        mSensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                assertEquals("$AppClick", eventName);
                assertEquals("code", eventProperties.optString("$lib_method"));
                return false;
            }
        });
        mSensorsDataAPI.trackViewAppClick(view);
    }

    public void testTrackViewAppClick() {
        setUp();
        View view = new View(mActivity);
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("click", "click");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                assertEquals("$AppClick", eventName);
                assertEquals("code", eventProperties.optString("$lib_method"));
                assertEquals("click", eventProperties.optString("click"));
                return false;
            }
        });
        mSensorsDataAPI.trackViewAppClick(view, jsonObject);
    }

    public void trackFragmentAppViewScreen() {
        setUp();
        mAutoTrackImp.trackFragmentAppViewScreen();
        Assert.assertTrue(mAutoTrackImp.isTrackFragmentAppViewScreenEnabled());
    }

    public void isTrackFragmentAppViewScreenEnabled() {
        setUp();
        Assert.assertFalse(mAutoTrackImp.isTrackFragmentAppViewScreenEnabled());
    }

    public void enableAutoTrackFragment() {
        setUp();
        mAutoTrackImp.trackFragmentAppViewScreen();
        mAutoTrackImp.enableAutoTrackFragment(Fragment.class);
        Assert.assertTrue(mAutoTrackImp.isFragmentAutoTrackAppViewScreen(Fragment.class));
    }

    public void enableAutoTrackFragments() {
        setUp();
        mAutoTrackImp.trackFragmentAppViewScreen();
        ArrayList<Class<?>> fragments = new ArrayList<>();
        fragments.add(Fragment.class);
        fragments.add(DialogFragment.class);
        mAutoTrackImp.enableAutoTrackFragments(fragments);
        Assert.assertTrue(mAutoTrackImp.isFragmentAutoTrackAppViewScreen(Fragment.class));
        Assert.assertTrue(mAutoTrackImp.isFragmentAutoTrackAppViewScreen(DialogFragment.class));
    }

    @Test
    public void isFragmentAutoTrackAppViewScreen() {
        setUp();
        mAutoTrackImp.trackFragmentAppViewScreen();
        Assert.assertTrue(mAutoTrackImp.isTrackFragmentAppViewScreenEnabled());
    }

    public void ignoreAutoTrackFragments() {
        setUp();
        mAutoTrackImp.trackFragmentAppViewScreen();
        ArrayList<Class<?>> fragments = new ArrayList<>();
        fragments.add(Fragment.class);
        fragments.add(DialogFragment.class);
        mAutoTrackImp.ignoreAutoTrackFragments(fragments);
        Assert.assertFalse(mAutoTrackImp.isFragmentAutoTrackAppViewScreen(Fragment.class));
        Assert.assertFalse(mAutoTrackImp.isFragmentAutoTrackAppViewScreen(DialogFragment.class));
    }

    public void ignoreAutoTrackFragment() {
        setUp();
        mAutoTrackImp.trackFragmentAppViewScreen();
        mAutoTrackImp.ignoreAutoTrackFragment(DialogFragment.class);
        Assert.assertFalse(mAutoTrackImp.isFragmentAutoTrackAppViewScreen(DialogFragment.class));
    }

    public void resumeIgnoredAutoTrackFragments() {
        setUp();
        mAutoTrackImp.trackFragmentAppViewScreen();
        ArrayList<Class<?>> fragments = new ArrayList<>();
        fragments.add(Fragment.class);
        fragments.add(DialogFragment.class);
        mAutoTrackImp.ignoreAutoTrackFragments(fragments);
        mAutoTrackImp.resumeIgnoredAutoTrackFragments(fragments);
        Assert.assertTrue(mAutoTrackImp.isFragmentAutoTrackAppViewScreen(Fragment.class));
        Assert.assertTrue(mAutoTrackImp.isFragmentAutoTrackAppViewScreen(DialogFragment.class));
    }

    public void resumeIgnoredAutoTrackFragment() {
        setUp();
        mAutoTrackImp.trackFragmentAppViewScreen();
        mAutoTrackImp.ignoreAutoTrackFragment(DialogFragment.class);
        mAutoTrackImp.ignoreAutoTrackFragment(ActivityLifecycleCallbacksTest.AutoTrackActivity.class);
        mAutoTrackImp.resumeIgnoredAutoTrackFragment(DialogFragment.class);
        Assert.assertTrue(mAutoTrackImp.isFragmentAutoTrackAppViewScreen(DialogFragment.class));
        Assert.assertFalse(mAutoTrackImp.isFragmentAutoTrackAppViewScreen(ActivityLifecycleCallbacksTest.AutoTrackActivity.class));
    }
}