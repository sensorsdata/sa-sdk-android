/*
 * Created by dengshiwei on 2022/06/30.
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

package com.sensorsdata.analytics.android.sdk;

import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.sdk.deeplink.SADeepLinkObject;
import com.sensorsdata.analytics.android.sdk.deeplink.SensorsDataDeferredDeepLinkCallback;
import com.sensorsdata.analytics.android.sdk.listener.SAJSListener;
import com.sensorsdata.analytics.android.sdk.plugin.property.SAPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertiesFetcher;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class SensorsDataAPIEmptyImplementationTest {
    SensorsDataAPI mSensorsAPI = new SensorsDataAPIEmptyImplementation();
    Application mApplication = ApplicationProvider.getApplicationContext();

    @Test
    public void getPresetProperties() {
        JSONObject jsonObject = mSensorsAPI.getPresetProperties();
        Assert.assertEquals(0, jsonObject.length());
    }

    @Test
    public void enableAutoTrackFragment() {
        mSensorsAPI.enableAutoTrackFragment(Fragment.class);
        Assert.assertFalse(mSensorsAPI.isFragmentAutoTrackAppViewScreen(Fragment.class));
    }

    @Test
    public void enableAutoTrackFragments() {
        ArrayList<Class<?>> fragments = new ArrayList<>();
        fragments.add(Fragment.class);
        fragments.add(DialogFragment.class);
        mSensorsAPI.enableAutoTrackFragments(fragments);
        Assert.assertFalse(mSensorsAPI.isFragmentAutoTrackAppViewScreen(Fragment.class));
        Assert.assertFalse(mSensorsAPI.isFragmentAutoTrackAppViewScreen(DialogFragment.class));
    }

    @Test
    public void ignoreAutoTrackFragments() {
        ArrayList<Class<?>> fragments = new ArrayList<>();
        fragments.add(Fragment.class);
        fragments.add(DialogFragment.class);
        mSensorsAPI.ignoreAutoTrackFragments(fragments);
        Assert.assertFalse(mSensorsAPI.isFragmentAutoTrackAppViewScreen(Fragment.class));
        Assert.assertFalse(mSensorsAPI.isFragmentAutoTrackAppViewScreen(DialogFragment.class));
    }

    @Test
    public void ignoreAutoTrackFragment() {
        mSensorsAPI.ignoreAutoTrackFragment(DialogFragment.class);
        Assert.assertFalse(mSensorsAPI.isFragmentAutoTrackAppViewScreen(DialogFragment.class));
    }

    @Test
    public void resumeIgnoredAutoTrackFragments() {
        ArrayList<Class<?>> fragments = new ArrayList<>();
        fragments.add(Fragment.class);
        fragments.add(DialogFragment.class);
        mSensorsAPI.ignoreAutoTrackFragments(fragments);
        mSensorsAPI.resumeIgnoredAutoTrackFragments(fragments);
        Assert.assertFalse(mSensorsAPI.isFragmentAutoTrackAppViewScreen(Fragment.class));
        Assert.assertFalse(mSensorsAPI.isFragmentAutoTrackAppViewScreen(DialogFragment.class));
    }

    @Test
    public void resumeIgnoredAutoTrackFragment() {
        mSensorsAPI.ignoreAutoTrackFragment(DialogFragment.class);
        mSensorsAPI.resumeIgnoredAutoTrackFragment(DialogFragment.class);
        Assert.assertFalse(mSensorsAPI.isFragmentAutoTrackAppViewScreen(DialogFragment.class));
    }

    @Test
    public void isFragmentAutoTrackAppViewScreen() {
        Assert.assertFalse(mSensorsAPI.isTrackFragmentAppViewScreenEnabled());
    }

    @Test
    public void getServerUrl() {
        Assert.assertNull(mSensorsAPI.getServerUrl());
    }

    @Test
    public void setServerUrl() {
        mSensorsAPI.setServerUrl("https://sensorsdata.cn");
        Assert.assertNull(mSensorsAPI.getServerUrl());
    }

    @Test
    public void testSetServerUrl() {
        mSensorsAPI.setServerUrl("https://sensorsdata.cn", false);
        Assert.assertNull(mSensorsAPI.getServerUrl());
    }

    @Test
    public void enableLog() {
        mSensorsAPI.enableLog(true);
        Assert.assertFalse(SALog.isLogEnabled());
    }

    @Test
    public void isDebugMode() {
        Assert.assertFalse(mSensorsAPI.isDebugMode());
    }

    @Test
    public void getMaxCacheSize() {
        Assert.assertEquals(32 * 1024 * 1024, mSensorsAPI.getMaxCacheSize());
    }

    @Test
    public void setMaxCacheSize() {
        mSensorsAPI.setMaxCacheSize(16 * 1024 * 1024);
        Assert.assertEquals(32 * 1024 * 1024, mSensorsAPI.getMaxCacheSize());
    }

    @Test
    public void setFlushNetworkPolicy() {
        mSensorsAPI.setFlushNetworkPolicy(SensorsDataAPI.NetworkType.TYPE_5G);
        Assert.assertEquals(SensorsDataAPI.NetworkType.TYPE_NONE, mSensorsAPI.getFlushNetworkPolicy());
    }

    @Test
    public void getFlushInterval() {
        Assert.assertEquals(15 * 1000, mSensorsAPI.getFlushInterval());
    }

    @Test
    public void setFlushInterval() {
        mSensorsAPI.setFlushInterval(20 * 1000);
        Assert.assertEquals(15 * 1000, mSensorsAPI.getFlushInterval());
    }

    @Test
    public void getFlushBulkSize() {
        Assert.assertEquals(100, mSensorsAPI.getFlushBulkSize());
    }

    @Test
    public void setFlushBulkSize() {
        mSensorsAPI.setFlushBulkSize(2000);
        Assert.assertEquals(100, mSensorsAPI.getFlushBulkSize());
    }

    @Test
    public void getSessionIntervalTime() {
        Assert.assertEquals(30 * 1000, mSensorsAPI.getSessionIntervalTime());
    }

    @Test
    public void setSessionIntervalTime() {
        mSensorsAPI.setSessionIntervalTime(50 * 100);
        Assert.assertEquals(30 * 1000, mSensorsAPI.getSessionIntervalTime());
    }

    @Test
    public void enableAutoTrack() {
        List<SensorsDataAPI.AutoTrackEventType> list = new ArrayList<>();
        list.add(SensorsDataAPI.AutoTrackEventType.APP_START);
        mSensorsAPI.enableAutoTrack(list);
        Assert.assertFalse(mSensorsAPI.isAutoTrackEnabled());
    }

    @Test
    public void disableAutoTrack() {
        mSensorsAPI.disableAutoTrack(SensorsDataAPI.AutoTrackEventType.APP_START);
        Assert.assertFalse(mSensorsAPI.isAutoTrackEnabled());
    }

    @Test
    public void testDisableAutoTrack() {
        List<SensorsDataAPI.AutoTrackEventType> typeList = new ArrayList<>();
        typeList.add(SensorsDataAPI.AutoTrackEventType.APP_START);
        typeList.add(SensorsDataAPI.AutoTrackEventType.APP_END);
        mSensorsAPI.disableAutoTrack(typeList);
        Assert.assertFalse(mSensorsAPI.isAutoTrackEnabled());
    }

    @Test
    public void isAutoTrackEnabled() {
        List<SensorsDataAPI.AutoTrackEventType> typeList = new ArrayList<>();
        typeList.add(SensorsDataAPI.AutoTrackEventType.APP_START);
        typeList.add(SensorsDataAPI.AutoTrackEventType.APP_END);
        mSensorsAPI.disableAutoTrack(typeList);
        Assert.assertFalse(mSensorsAPI.isAutoTrackEnabled());
    }

    @Test
    public void trackFragmentAppViewScreen() {
        mSensorsAPI.trackFragmentAppViewScreen();
        Assert.assertFalse(mSensorsAPI.isTrackFragmentAppViewScreenEnabled());
    }

    @Test
    public void isTrackFragmentAppViewScreenEnabled() {
        Assert.assertFalse(mSensorsAPI.isTrackFragmentAppViewScreenEnabled());
    }

    @Test
    public void showUpWebView() {
        WebView webView = new WebView(mApplication);
        mSensorsAPI.showUpWebView(webView, false);
    }

    @Test
    public void testShowUpWebView() {
        WebView webView = new WebView(mApplication);
        mSensorsAPI.showUpWebView(webView, false);
    }

    @Test
    public void testShowUpWebView1() {
        WebView webView = new WebView(mApplication);
        mSensorsAPI.showUpWebView(webView, false);
    }

    @Test
    public void testShowUpWebView2() {
        WebView webView = new WebView(mApplication);
        mSensorsAPI.showUpWebView(webView, false);
    }

    @Test
    public void showUpX5WebView() {
        WebView webView = new WebView(mApplication);
        mSensorsAPI.showUpX5WebView(webView, false);
    }

    @Test
    public void testShowUpX5WebView() {
        WebView webView = new WebView(mApplication);
        mSensorsAPI.showUpX5WebView(webView, false);
    }

    @Test
    public void testShowUpX5WebView1() {
        WebView webView = new WebView(mApplication);
        mSensorsAPI.showUpX5WebView(webView, false);
    }

    @Test
    public void ignoreAutoTrackActivities() {
        List<Class<?>> activities = new ArrayList<>();
        activities.add(EmptyActivity.class);
        activities.add(ListActivity.class);
        mSensorsAPI.ignoreAutoTrackActivities(activities);
        Assert.assertTrue(mSensorsAPI.isActivityAutoTrackAppClickIgnored(EmptyActivity.class));
    }

    @Test
    public void resumeAutoTrackActivities() {
        List<Class<?>> activities = new ArrayList<>();
        activities.add(EmptyActivity.class);
        activities.add(ListActivity.class);
        mSensorsAPI.ignoreAutoTrackActivities(activities);
        mSensorsAPI.resumeAutoTrackActivities(activities);
        Assert.assertTrue(mSensorsAPI.isActivityAutoTrackAppClickIgnored(EmptyActivity.class));
    }

    @Test
    public void ignoreAutoTrackActivity() {
        mSensorsAPI.ignoreAutoTrackActivity(EmptyActivity.class);
        Assert.assertTrue(mSensorsAPI.isActivityAutoTrackAppClickIgnored(EmptyActivity.class));
    }

    @Test
    public void resumeAutoTrackActivity() {
        mSensorsAPI.ignoreAutoTrackActivity(EmptyActivity.class);
        mSensorsAPI.resumeAutoTrackActivity(EmptyActivity.class);
        Assert.assertTrue(mSensorsAPI.isActivityAutoTrackAppClickIgnored(EmptyActivity.class));
    }

    @Test
    public void isActivityAutoTrackAppViewScreenIgnored() {
        mSensorsAPI.ignoreAutoTrackActivity(EmptyActivity.class);
        Assert.assertTrue(mSensorsAPI.isActivityAutoTrackAppViewScreenIgnored(EmptyActivity.class));
    }

    @Test
    public void isActivityAutoTrackAppClickIgnored() {
        mSensorsAPI.ignoreAutoTrackActivity(EmptyActivity.class);
        Assert.assertTrue(mSensorsAPI.isActivityAutoTrackAppViewScreenIgnored(EmptyActivity.class));
    }

    @Test
    public void isAutoTrackEventTypeIgnored() {
        Assert.assertTrue(mSensorsAPI.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_CLICK));
    }

    @Test
    public void setViewID() {
        View view = new View(mApplication);
        mSensorsAPI.setViewID(view, "R.id.login");
        Object tag = view.getTag(R.id.sensors_analytics_tag_view_id);
        Assert.assertNull(tag);
    }

    @Test
    public void testSetViewID() {
        Dialog view = new Dialog(mApplication);
        mSensorsAPI.setViewID(view, "R.id.login");
        Object tag = view.getWindow().getDecorView().getTag(R.id.sensors_analytics_tag_view_id);
        Assert.assertNull(tag);
    }

    @Test
    public void testSetViewID1() {
        Object view = new AlertDialog.Builder(mApplication).create();
        mSensorsAPI.setViewID(view, "R.id.login");
        Object tag = ((AlertDialog) view).getWindow().getDecorView().getTag(R.id.sensors_analytics_tag_view_id);
        Assert.assertNull(tag);
    }

    @Test
    public void setViewActivity() {
        View view = new View(mApplication);
        mSensorsAPI.setViewActivity(view, new EmptyActivity());
        Object tag = view.getTag(R.id.sensors_analytics_tag_view_activity);
        Assert.assertNull(tag);
    }

    @Test
    public void setViewFragmentName() {
        View view = new View(mApplication);
        mSensorsAPI.setViewFragmentName(view, "com.sensorsdata.fragment");
        Object tag = view.getTag(R.id.sensors_analytics_tag_view_fragment_name2);
        Assert.assertNull(tag);
    }

    @Test
    public void ignoreView() {
        View view = new View(mApplication);
        mSensorsAPI.ignoreView(view);
        Object tag = view.getTag(R.id.sensors_analytics_tag_view_ignored);
        Assert.assertNull(tag);
    }

    @Test
    public void testIgnoreView() {
        View view = new View(mApplication);
        mSensorsAPI.ignoreView(view, true);
        Object tag = view.getTag(R.id.sensors_analytics_tag_view_ignored);
        Assert.assertNull(tag);
    }

    @Test
    public void setViewProperties() {
        View view = new View(mApplication);
        mSensorsAPI.setViewProperties(view, new JSONObject());
        Object tag = view.getTag(R.id.sensors_analytics_tag_view_properties);
        Assert.assertNull(tag);
    }

    @Test
    public void getIgnoredViewTypeList() {
        mSensorsAPI.ignoreViewType(Button.class);
        Assert.assertEquals(0, mSensorsAPI.getIgnoredViewTypeList().size());
    }

    @Test
    public void ignoreViewType() {
        mSensorsAPI.ignoreViewType(Button.class);
        Assert.assertEquals(0, mSensorsAPI.getIgnoredViewTypeList().size());
    }

    @Test
    public void isHeatMapActivity() {
        Assert.assertFalse(mSensorsAPI.isHeatMapActivity(EmptyActivity.class));
    }

    @Test
    public void addHeatMapActivity() {
        mSensorsAPI.addHeatMapActivity(EmptyActivity.class);
        Assert.assertFalse(mSensorsAPI.isHeatMapActivity(EmptyActivity.class));
    }

    @Test
    public void addHeatMapActivities() {
        List<Class<?>> activities = new ArrayList<>();
        activities.add(EmptyActivity.class);
        activities.add(ListActivity.class);
        mSensorsAPI.addHeatMapActivities(activities);
        Assert.assertFalse(mSensorsAPI.isHeatMapActivity(EmptyActivity.class));
    }

    @Test
    public void isHeatMapEnabled() {
        Assert.assertFalse(mSensorsAPI.isHeatMapEnabled());
    }

    @Test
    public void getDistinctId() {
        Assert.assertNull(mSensorsAPI.getDistinctId());
    }

    @Test
    public void getAnonymousId() {
        Assert.assertNull(mSensorsAPI.getAnonymousId());
    }

    @Test
    public void resetAnonymousId() {
        mSensorsAPI.resetAnonymousId();
        Assert.assertNull(mSensorsAPI.getAnonymousId());
    }

    @Test
    public void getLoginId() {
        Assert.assertNull(mSensorsAPI.getLoginId());
    }

    @Test
    public void identify() {
        mSensorsAPI.identify("abcde");
        Assert.assertNull(mSensorsAPI.getAnonymousId());
    }

    @Test
    public void login() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.login("abcde");
        Assert.assertNull(mSensorsAPI.getLoginId());
    }

    @Test
    public void testLogin() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.login("abcde", new JSONObject());
        Assert.assertNull(mSensorsAPI.getLoginId());
    }

    @Test
    public void loginWithKey() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.loginWithKey("login_user", "login_user");
        Assert.assertNull(mSensorsAPI.getLoginId());
    }

    @Test
    public void testLoginWithKey() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.loginWithKey("login_user", "login_user", new JSONObject());
        Assert.assertNull(mSensorsAPI.getLoginId());
    }

    @Test
    public void logout() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.login("abcde");
        mSensorsAPI.logout();
        Assert.assertNull(mSensorsAPI.getLoginId());
    }

    @Test
    public void getIdentities() {
        Assert.assertEquals(0, mSensorsAPI.getIdentities().length());
    }

    @Test
    public void trackInstallation() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackInstallation("AppInstall");
    }

    @Test
    public void testTrackInstallation() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackInstallation("AppInstall", new JSONObject());
    }

    @Test
    public void testTrackInstallation1() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackInstallation("AppInstall", new JSONObject(), false);
    }

    @Test
    public void trackAppInstall() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackAppInstall();
    }

    @Test
    public void testTrackAppInstall() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackAppInstall(new JSONObject());
    }

    @Test
    public void testTrackAppInstall1() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackAppInstall(new JSONObject(), false);
    }

    @Test
    public void trackChannelEvent() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackChannelEvent("TestChannelEvent");
    }

    @Test
    public void testTrackChannelEvent() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackChannelEvent("TestChannelEvent", new JSONObject());
    }

    @Test
    public void track() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.track("TestTrackEvent");
    }

    @Test
    public void testTrack() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.track("TestTrackEvent", new JSONObject());
    }

    @Test
    public void trackTimer() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackTimer("TestTimerEvent", TimeUnit.SECONDS);
        mSensorsAPI.trackTimerEnd("TestTimerEvent");
    }

    @Test
    public void removeTimer() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackTimer("TestTimerEvent", TimeUnit.SECONDS);
        mSensorsAPI.removeTimer("TestTimerEvent");
        mSensorsAPI.trackTimerEnd("TestTimerEvent");
    }

    @Test
    public void trackTimerStart() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackTimerStart("TestTimerEvent");
        mSensorsAPI.trackTimerEnd("TestTimerEvent");
    }

    @Test
    public void trackTimerEnd() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackTimerStart("TestTimerEvent");
        mSensorsAPI.trackTimerEnd("TestTimerEvent");
    }

    @Test
    public void testTrackTimerEnd() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackTimerStart("TestTimerEvent");
        mSensorsAPI.trackTimerEnd("TestTimerEvent", new JSONObject());
    }

    @Test
    public void clearTrackTimer() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackTimerStart("TestTimerEvent");
        mSensorsAPI.clearTrackTimer();
        mSensorsAPI.trackTimerEnd("TestTimerEvent");
    }

    @Test
    public void getLastScreenUrl() {
        Assert.assertNull(mSensorsAPI.getLastScreenUrl());
    }

    @Test
    public void clearReferrerWhenAppEnd() {
        mSensorsAPI.clearReferrerWhenAppEnd();
        Assert.assertNull(mSensorsAPI.getLastScreenUrl());
    }

    @Test
    public void clearLastScreenUrl() {
        mSensorsAPI.clearLastScreenUrl();
        Assert.assertNull(mSensorsAPI.getLastScreenUrl());
    }

    @Test
    public void getLastScreenTrackProperties() {
        Assert.assertEquals(0, mSensorsAPI.getLastScreenTrackProperties().length());
    }

    @Test
    public void trackViewScreen() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackViewScreen("TestTimerEvent", new JSONObject());
    }

    @Test
    public void testTrackViewScreen() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackViewScreen(new Fragment());
    }

    @Test
    public void testTrackViewScreen1() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackViewScreen(new EmptyActivity());
    }

    @Test
    public void trackViewAppClick() {
        View view = new View(mApplication);
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackViewAppClick(view);
    }

    @Test
    public void testTrackViewAppClick() {
        View view = new View(mApplication);
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackViewAppClick(view, new JSONObject());
    }

    @Test
    public void flush() {
        mSensorsAPI.flush();
    }

    @Test
    public void flushSync() {
        mSensorsAPI.flushSync();
    }

    @Test
    public void flushScheduled() {
        mSensorsAPI.flushScheduled();
    }

    @Test
    public void registerDynamicSuperProperties() {
        mSensorsAPI.registerDynamicSuperProperties(new SensorsDataDynamicSuperProperties() {
            @Override
            public JSONObject getDynamicSuperProperties() {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("dynamic", "json_dynamic");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return jsonObject;
            }
        });
        mSensorsAPI.track("DynamicTest");
    }

    @Test
    public void setTrackEventCallBack() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.track("TestTrackEvent", new JSONObject());
    }

    @Test
    public void registerPropertyPlugin() {
        mSensorsAPI.registerPropertyPlugin(new SAPropertyPlugin() {
            @Override
            public void properties(SAPropertiesFetcher fetcher) {
                try {
                    fetcher.getProperties().put("Plugin", "Plugin");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        mSensorsAPI.track("TestTrackEvent", new JSONObject());
    }

    @Test
    public void unregisterPropertyPlugin() {
        mSensorsAPI.unregisterPropertyPlugin(null);
    }

    @Test
    public void setDeepLinkCallback() {
        mSensorsAPI.setDeepLinkCompletion(new SensorsDataDeferredDeepLinkCallback() {
            @Override
            public boolean onReceive(SADeepLinkObject saDeepLinkObject) {
                Assert.fail();
                return false;
            }
        });
    }

    @Test
    public void setDeepLinkCompletion() {
        mSensorsAPI.setDeepLinkCompletion(new SensorsDataDeferredDeepLinkCallback() {
            @Override
            public boolean onReceive(SADeepLinkObject saDeepLinkObject) {
                Assert.fail();
                return false;
            }
        });
    }

    @Test
    public void deleteAll() {
        mSensorsAPI.deleteAll();
    }

    @Test
    public void getSuperProperties() {
        Assert.assertEquals(0, mSensorsAPI.getSuperProperties().length());
    }

    @Test
    public void registerSuperProperties() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("super", "super");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSensorsAPI.registerSuperProperties(jsonObject);
        Assert.assertEquals(0, mSensorsAPI.getSuperProperties().length());
    }

    @Test
    public void unregisterSuperProperty() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("super", "super");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSensorsAPI.registerSuperProperties(jsonObject);
        mSensorsAPI.unregisterSuperProperty("super");
        Assert.assertEquals(0, mSensorsAPI.getSuperProperties().length());
    }

    @Test
    public void clearSuperProperties() {
        mSensorsAPI.clearSuperProperties();
        Assert.assertEquals(0, mSensorsAPI.getSuperProperties().length());
    }

    @Test
    public void profileSet() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.profileSet("abcde", "123");
    }

    @Test
    public void testProfileSet() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("abcd", "123");
            jsonObject.put("abcd2", "1232");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSensorsAPI.profileSet(jsonObject);
    }

    @Test
    public void profileSetOnce() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.profileSetOnce("abcde", "123");
    }

    @Test
    public void testProfileSetOnce() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("abcd", "123");
            jsonObject.put("abcd2", "1232");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSensorsAPI.profileSetOnce(jsonObject);
    }

    @Test
    public void profileIncrement() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.profileIncrement("abcde", 123);
    }

    @Test
    public void testProfileIncrement() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        Map<String, Integer> properties = new HashMap<>();
        properties.put("n1", 111);
        properties.put("n2", 1121);
        mSensorsAPI.profileIncrement(properties);
    }

    @Test
    public void profileAppend() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.profileAppend("abcde", "123");
    }

    @Test
    public void testProfileAppend() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        Set<String> values = new HashSet<>();
        values.add("12");
        values.add("123");
        mSensorsAPI.profileAppend("abcde", values);
    }

    @Test
    public void profileUnset() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.profileUnset("abcde");
    }

    @Test
    public void profileDelete() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.profileDelete();
    }

    @Test
    public void trackEventFromH5() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackEventFromH5("adfadsfasd", false);
    }

    @Test
    public void testTrackEventFromH5() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackEventFromH5("adfadsfasd");
    }

    @Test
    public void trackTimerPause() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackTimerStart("TestTimerEvent");
        mSensorsAPI.trackTimerPause("TestTimerEvent");
        mSensorsAPI.trackTimerEnd("TestTimerEvent");
    }

    @Test
    public void trackTimerResume() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackTimerStart("TestTimerEvent");
        mSensorsAPI.trackTimerPause("TestTimerEvent");
        mSensorsAPI.trackTimerResume("TestTimerEvent");
        mSensorsAPI.trackTimerEnd("TestTimerEvent");
    }

    @Test
    public void testIsAutoTrackEventTypeIgnored() {
        Assert.assertTrue(mSensorsAPI.isAutoTrackEventTypeIgnored(SensorsAnalyticsAutoTrackEventType.APP_START));
    }

    @Test
    public void setDebugMode() {
        mSensorsAPI.setDebugMode(SensorsDataAPI.DebugMode.DEBUG_OFF);
        Assert.assertFalse(SALog.isDebug());
    }

    @Test
    public void testEnableAutoTrack() {
        List<SensorsDataAPI.AutoTrackEventType> types = new ArrayList<>();
        types.add(SensorsDataAPI.AutoTrackEventType.APP_START);
        types.add(SensorsDataAPI.AutoTrackEventType.APP_END);
        mSensorsAPI.enableAutoTrack(types);
        Assert.assertFalse(mSensorsAPI.isAutoTrackEnabled());
    }

    @Test
    public void setGPSLocation() {
        List<SensorsDataAPI.AutoTrackEventType> types = new ArrayList<>();
        types.add(SensorsDataAPI.AutoTrackEventType.APP_START);
        types.add(SensorsDataAPI.AutoTrackEventType.APP_END);
        mSensorsAPI.setGPSLocation(1000.0, 45.5, "GPS");
    }

    @Test
    public void testSetGPSLocation() {
        List<SensorsDataAPI.AutoTrackEventType> types = new ArrayList<>();
        types.add(SensorsDataAPI.AutoTrackEventType.APP_START);
        types.add(SensorsDataAPI.AutoTrackEventType.APP_END);
        mSensorsAPI.setGPSLocation(1000.0, 45.5);
    }

    @Test
    public void clearGPSLocation() {
        mSensorsAPI.clearGPSLocation();
    }

    @Test
    public void enableTrackScreenOrientation() {
        mSensorsAPI.enableTrackScreenOrientation(true);
    }

    @Test
    public void resumeTrackScreenOrientation() {
        mSensorsAPI.resumeTrackScreenOrientation();
    }

    @Test
    public void stopTrackScreenOrientation() {
        mSensorsAPI.stopTrackScreenOrientation();
    }

    @Test
    public void setCookie() {
        mSensorsAPI.setCookie("cookie", false);
        Assert.assertNull(mSensorsAPI.getCookie(false));
    }

    @Test
    public void getCookie() {
        mSensorsAPI.setCookie("cookie", false);
        Assert.assertNull(mSensorsAPI.getCookie(false));
    }

    @Test
    public void profilePushId() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.profilePushId("jpush_id", "eacsdilkjiads123");
    }

    @Test
    public void profileUnsetPushId() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.profileUnsetPushId("jpush_id");
    }

    @Test
    public void isVisualizedAutoTrackActivity() {
        Assert.assertFalse(mSensorsAPI.isVisualizedAutoTrackActivity(EmptyActivity.class));
    }

    @Test
    public void addVisualizedAutoTrackActivity() {
        mSensorsAPI.addVisualizedAutoTrackActivity(EmptyActivity.class);
        Assert.assertFalse(mSensorsAPI.isVisualizedAutoTrackActivity(EmptyActivity.class));
    }

    @Test
    public void addVisualizedAutoTrackActivities() {
        List<Class<?>> activities = new ArrayList<>();
        activities.add(EmptyActivity.class);
        activities.add(ListActivity.class);
        mSensorsAPI.addVisualizedAutoTrackActivities(activities);
        Assert.assertFalse(mSensorsAPI.isHeatMapActivity(EmptyActivity.class));
    }

    @Test
    public void isVisualizedAutoTrackEnabled() {
        Assert.assertFalse(mSensorsAPI.isVisualizedAutoTrackEnabled());
    }

    @Test
    public void itemSet() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.itemSet("item", "1232", null);
    }

    @Test
    public void itemDelete() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.itemDelete("item", "123");
    }

    @Test
    public void enableNetworkRequest() {
        mSensorsAPI.enableNetworkRequest(false);
        Assert.assertFalse(mSensorsAPI.isNetworkRequestEnable());
    }

    @Test
    public void startTrackThread() {
        mSensorsAPI.startTrackThread();
    }

    @Test
    public void stopTrackThread() {
        mSensorsAPI.stopTrackThread();
    }

    @Test
    public void addSAJSListener() {
        mSensorsAPI.addSAJSListener(new SAJSListener() {
            @Override
            public void onReceiveJSMessage(WeakReference<View> view, String message) {
                Assert.fail();
            }
        });
    }

    @Test
    public void getFlushNetworkPolicy() {
        mSensorsAPI.stopTrackThread();
    }

    @Test
    public void getScreenOrientation() {
        Assert.assertEquals("", mSensorsAPI.getScreenOrientation());
    }

    @Test
    public void isNetworkRequestEnable() {
        Assert.assertFalse(mSensorsAPI.isNetworkRequestEnable());
    }

    @Test
    public void enableDeepLinkInstallSource() {
        mSensorsAPI.enableDeepLinkInstallSource(true);
        Assert.assertFalse(mSensorsAPI.isNetworkRequestEnable());
    }

    @Test
    public void trackDeepLinkLaunch() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackDeepLinkLaunch("https://deeplink.com");
    }

    @Test
    public void testTrackDeepLinkLaunch() {
        mSensorsAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                Assert.fail();
                return false;
            }
        });
        mSensorsAPI.trackDeepLinkLaunch("https://deeplink.com", "kjsdakfjkadsljf");
    }

    @Test
    public void requestDeferredDeepLink() {
        mSensorsAPI.requestDeferredDeepLink(new JSONObject());
    }

    static class EmptyActivity extends android.app.Activity {
        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }
    }
}