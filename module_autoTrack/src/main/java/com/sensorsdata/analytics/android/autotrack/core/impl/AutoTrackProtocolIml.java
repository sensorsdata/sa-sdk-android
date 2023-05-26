/*
 * Created by dengshiwei on 2022/07/01.
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

import android.app.Activity;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;

import com.sensorsdata.analytics.android.autotrack.R;
import com.sensorsdata.analytics.android.autotrack.core.beans.AutoTrackConstants;
import com.sensorsdata.analytics.android.autotrack.core.beans.ViewContext;
import com.sensorsdata.analytics.android.autotrack.core.business.SAPageTools;
import com.sensorsdata.analytics.android.autotrack.utils.AopUtil;
import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.ScreenAutoTracker;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataFragmentTitle;
import com.sensorsdata.analytics.android.sdk.SensorsDataIgnoreTrackAppClick;
import com.sensorsdata.analytics.android.sdk.SensorsDataIgnoreTrackAppViewScreen;
import com.sensorsdata.analytics.android.sdk.SensorsDataIgnoreTrackAppViewScreenAndAppClick;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.core.SACoreHelper;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.core.mediator.SAModuleManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.autotrack.AutoTrackProtocol;
import com.sensorsdata.analytics.android.sdk.core.mediator.autotrack.IFragmentAPI;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.SAPageInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class AutoTrackProtocolIml implements AutoTrackProtocol {
    private static final String TAG = "SA.AutoTrackProtocolIml";
    protected IFragmentAPI mFragmentAPI;
    private final SAConfigOptions mSAConfigOptions;
    private final SAContextManager mContextManager;
    protected List<Class<?>> mIgnoredViewTypeList = new ArrayList<>();
    protected List<Integer> mAutoTrackIgnoredActivities;
    protected boolean mClearReferrerWhenAppEnd = false;
    /* SDK 自动采集事件 */
    protected boolean mAutoTrack;

    public AutoTrackProtocolIml(SAContextManager contextManager) {
        this.mFragmentAPI = new FragmentAPI();
        mAutoTrackIgnoredActivities = new ArrayList<>();
        mContextManager = contextManager;
        mSAConfigOptions = contextManager.getInternalConfigs().saConfigOptions;
        Bundle configBundle = AppInfoUtils.getAppInfoBundle(mContextManager.getContext());
        this.mAutoTrack = configBundle.getBoolean("com.sensorsdata.analytics.android.AutoTrack",
                false);
        if (mSAConfigOptions.getAutoTrackEventType() != 0) {
            enableAutoTrack(mSAConfigOptions.getAutoTrackEventType());
            this.mAutoTrack = true;
        }
    }

    @Override
    public void enableAutoTrack(List<SensorsDataAPI.AutoTrackEventType> eventTypeList) {
        try {
            if (eventTypeList == null || eventTypeList.isEmpty()) {
                return;
            }
            this.mAutoTrack = true;
            for (SensorsDataAPI.AutoTrackEventType autoTrackEventType : eventTypeList) {
                mSAConfigOptions.setAutoTrackEventType(mSAConfigOptions.getAutoTrackEventType() | autoTrackEventType.eventValue);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void disableAutoTrack(List<SensorsDataAPI.AutoTrackEventType> eventTypeList) {
        try {
            if (eventTypeList == null) {
                return;
            }

            if (mSAConfigOptions.getAutoTrackEventType() == 0) {
                return;
            }

            for (SensorsDataAPI.AutoTrackEventType autoTrackEventType : eventTypeList) {
                if ((mSAConfigOptions.getAutoTrackEventType() | autoTrackEventType.eventValue) == mSAConfigOptions.getAutoTrackEventType()) {
                    mSAConfigOptions.setAutoTrackEventType(mSAConfigOptions.getAutoTrackEventType() ^ autoTrackEventType.eventValue);
                }
            }

            if (mSAConfigOptions.getAutoTrackEventType() == 0) {
                this.mAutoTrack = false;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void disableAutoTrack(SensorsDataAPI.AutoTrackEventType autoTrackEventType) {
        try {
            if (autoTrackEventType == null) {
                return;
            }

            if (mSAConfigOptions.getAutoTrackEventType() == 0) {
                return;
            }

            int union = mSAConfigOptions.getAutoTrackEventType() | autoTrackEventType.eventValue;
            if (union == autoTrackEventType.eventValue) {
                mSAConfigOptions.setAutoTrackEventType(0);
            } else {
                mSAConfigOptions.setAutoTrackEventType(autoTrackEventType.eventValue ^ union);
            }

            if (mSAConfigOptions.getAutoTrackEventType() == 0) {
                this.mAutoTrack = false;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public boolean isAutoTrackEnabled() {
        if (mContextManager.getRemoteManager() != null) {
            Boolean isAutoTrackEnabled = mContextManager.getRemoteManager().isAutoTrackEnabled();
            if (isAutoTrackEnabled != null) {
                return isAutoTrackEnabled;
            }
        }
        return mAutoTrack;
    }

    @Override
    public void ignoreAutoTrackActivities(List<Class<?>> activitiesList) {
        if (activitiesList == null || activitiesList.size() == 0) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        int hashCode;
        for (Class<?> activity : activitiesList) {
            if (activity != null) {
                hashCode = activity.hashCode();
                if (!mAutoTrackIgnoredActivities.contains(hashCode)) {
                    mAutoTrackIgnoredActivities.add(hashCode);
                }
            }
        }
    }

    @Override
    public void resumeAutoTrackActivities(List<Class<?>> activitiesList) {
        if (activitiesList == null || activitiesList.size() == 0) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        try {
            int hashCode;
            for (Class<?> activity : activitiesList) {
                if (activity != null) {
                    hashCode = activity.hashCode();
                    if (mAutoTrackIgnoredActivities.contains(hashCode)) {
                        mAutoTrackIgnoredActivities.remove(Integer.valueOf(hashCode));
                    }
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void ignoreAutoTrackActivity(Class<?> activity) {
        if (activity == null) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        try {
            int hashCode = activity.hashCode();
            if (!mAutoTrackIgnoredActivities.contains(hashCode)) {
                mAutoTrackIgnoredActivities.add(hashCode);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void resumeAutoTrackActivity(Class<?> activity) {
        if (activity == null) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        try {
            int hashCode = activity.hashCode();
            if (mAutoTrackIgnoredActivities.contains(hashCode)) {
                mAutoTrackIgnoredActivities.remove(Integer.valueOf(hashCode));
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public boolean isActivityAutoTrackAppViewScreenIgnored(Class<?> activity) {
        if (activity == null) {
            return false;
        }
        if (mAutoTrackIgnoredActivities != null &&
                mAutoTrackIgnoredActivities.contains(activity.hashCode())) {
            return true;
        }

        if (activity.getAnnotation(SensorsDataIgnoreTrackAppViewScreenAndAppClick.class) != null) {
            return true;
        }

        return activity.getAnnotation(SensorsDataIgnoreTrackAppViewScreen.class) != null;
    }

    @Override
    public boolean isActivityAutoTrackAppClickIgnored(Class<?> activity) {
        if (activity == null) {
            return false;
        }
        if (mAutoTrackIgnoredActivities != null &&
                mAutoTrackIgnoredActivities.contains(activity.hashCode())) {
            return true;
        }

        if (activity.getAnnotation(SensorsDataIgnoreTrackAppViewScreenAndAppClick.class) != null) {
            return true;
        }

        return activity.getAnnotation(SensorsDataIgnoreTrackAppClick.class) != null;
    }

    @Override
    public boolean isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType eventType) {
        if (eventType == null) {
            return false;
        }
        return isAutoTrackEventTypeIgnored(eventType.eventValue);
    }

    @Override
    public boolean isAutoTrackEventTypeIgnored(int autoTrackEventType) {
        if (mContextManager.getRemoteManager() != null) {
            Boolean isIgnored = mContextManager.getRemoteManager().isAutoTrackEventTypeIgnored(autoTrackEventType);
            if (isIgnored != null) {
                if (isIgnored) {
                    SALog.i(TAG, "remote config: " + SensorsDataAPI.AutoTrackEventType.autoTrackEventName(autoTrackEventType) + " is ignored by remote config");
                }
                return isIgnored;
            }
        }

        return (mSAConfigOptions.getAutoTrackEventType() | autoTrackEventType) != mSAConfigOptions.getAutoTrackEventType();
    }

    @Override
    public void setViewID(View view, String viewID) {
        if (view != null && !TextUtils.isEmpty(viewID)) {
            view.setTag(com.sensorsdata.analytics.android.sdk.R.id.sensors_analytics_tag_view_id, viewID);
        }
    }

    @Override
    public void setViewID(Dialog view, String viewID) {
        try {
            if (view != null && !TextUtils.isEmpty(viewID)) {
                if (view.getWindow() != null) {
                    view.getWindow().getDecorView().setTag(com.sensorsdata.analytics.android.sdk.R.id.sensors_analytics_tag_view_id, viewID);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void setViewID(Object alertDialog, String viewID) {
        try {
            if (alertDialog == null) {
                return;

            }

            Class<?> supportAlertDialogClass = null;
            Class<?> androidXAlertDialogClass = null;
            Class<?> currentAlertDialogClass;
            try {
                supportAlertDialogClass = Class.forName("android.support.v7.app.AlertDialog");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXAlertDialogClass = Class.forName("androidx.appcompat.app.AlertDialog");
            } catch (Exception e) {
                //ignored
            }

            if (supportAlertDialogClass != null) {
                currentAlertDialogClass = supportAlertDialogClass;
            } else {
                currentAlertDialogClass = androidXAlertDialogClass;
            }

            if (currentAlertDialogClass == null) {
                return;
            }

            if (!currentAlertDialogClass.isInstance(alertDialog)) {
                return;
            }

            if (!TextUtils.isEmpty(viewID)) {
                Method getWindowMethod = alertDialog.getClass().getMethod("getWindow");
                if (getWindowMethod == null) {
                    return;
                }

                Window window = (Window) getWindowMethod.invoke(alertDialog);
                if (window != null) {
                    window.getDecorView().setTag(com.sensorsdata.analytics.android.sdk.R.id.sensors_analytics_tag_view_id, viewID);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void setViewActivity(View view, Activity activity) {
        try {
            if (view == null || activity == null) {
                return;
            }
            view.setTag(com.sensorsdata.analytics.android.sdk.R.id.sensors_analytics_tag_view_activity, activity);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void setViewFragmentName(View view, String fragmentName) {
        try {
            if (view == null || TextUtils.isEmpty(fragmentName)) {
                return;
            }
            view.setTag(com.sensorsdata.analytics.android.sdk.R.id.sensors_analytics_tag_view_fragment_name2, fragmentName);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void ignoreView(View view) {
        if (view != null) {
            view.setTag(com.sensorsdata.analytics.android.sdk.R.id.sensors_analytics_tag_view_ignored, "1");
        }
    }

    @Override
    public void ignoreView(View view, boolean ignore) {
        if (view != null) {
            view.setTag(com.sensorsdata.analytics.android.sdk.R.id.sensors_analytics_tag_view_ignored, ignore ? "1" : "0");
        }
    }

    @Override
    public void setViewProperties(View view, JSONObject properties) {
        if (view == null || properties == null) {
            return;
        }

        view.setTag(R.id.sensors_analytics_tag_view_properties, properties);
    }

    @Override
    public List<Class<?>> getIgnoredViewTypeList() {
        if (mIgnoredViewTypeList == null) {
            mIgnoredViewTypeList = new ArrayList<>();
        }

        return mIgnoredViewTypeList;
    }

    @Override
    public void ignoreViewType(Class<?> viewType) {
        if (viewType == null) {
            return;
        }

        if (mIgnoredViewTypeList == null) {
            mIgnoredViewTypeList = new ArrayList<>();
        }

        if (!mIgnoredViewTypeList.contains(viewType)) {
            mIgnoredViewTypeList.add(viewType);
        }
    }

    @Override
    public String getLastScreenUrl() {
        return SAPageTools.getLastScreenUrl();
    }

    @Override
    public void clearReferrerWhenAppEnd() {
        mClearReferrerWhenAppEnd = true;
    }

    @Override
    public void clearLastScreenUrl() {
        if (mClearReferrerWhenAppEnd) {
            SAPageTools.setLastScreenUrl(null);
        }
    }

    @Override
    public JSONObject getLastScreenTrackProperties() {
        try {
            JSONObject jsonObject = JSONUtils.cloneJsonObject(SAPageTools.getLastTrackProperties());
            if (jsonObject != null) {
                jsonObject.remove("$lib_method");
            }
            return jsonObject;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return new JSONObject();
    }

    @Override
    public void trackViewScreen(final String url, JSONObject properties) {
        try {
            final JSONObject cloneProperties = JSONUtils.cloneJsonObject(properties);
            SACoreHelper.getInstance().trackQueueEvent(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!TextUtils.isEmpty(url) || cloneProperties != null) {
                            JSONObject trackProperties = new JSONObject();
                            String currentUrl = url;
                            if (cloneProperties != null) {
                                if (cloneProperties.has("$title")) {
                                    SAPageTools.setCurrentTitle(cloneProperties.getString("$title"));
                                } else {
                                    SAPageTools.setCurrentTitle(null);
                                }
                                if (cloneProperties.has("$url")) {
                                    currentUrl = cloneProperties.optString("$url");
                                }
                            }
                            SAPageTools.setCurrentScreenUrl(currentUrl);
                            if (SAPageTools.getReferrer() != null) {
                                trackProperties.put("$referrer", SAPageTools.getReferrer());
                            }

                            trackProperties.put("$url", SAPageTools.getCurrentScreenUrl());
                            if (cloneProperties != null) {
                                JSONUtils.mergeJSONObject(cloneProperties, trackProperties);
                            }

                            SAPageTools.setCurrentScreenTrackProperties(trackProperties);
                            SACoreHelper.getInstance().trackEvent(new InputData().setEventName("$AppViewScreen").setEventType(EventType.TRACK).setProperties(trackProperties));
                        }
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            });

        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void trackViewScreen(final Activity activity) {
        try {
            if (activity == null) {
                return;
            }
            JSONObject properties = SAPageInfoUtils.getActivityPageInfo(activity);
            trackViewScreen(SAPageTools.getScreenUrl(activity), properties);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void trackViewScreen(final Object fragment) {
        if (fragment == null) {
            return;
        }

        Class<?> supportFragmentClass = null;
        Class<?> appFragmentClass = null;
        Class<?> androidXFragmentClass = null;

        try {
            try {
                supportFragmentClass = Class.forName("android.support.v4.app.Fragment");
            } catch (Exception e) {
                //ignored
            }

            try {
                appFragmentClass = Class.forName("android.app.Fragment");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXFragmentClass = Class.forName("androidx.fragment.app.Fragment");
            } catch (Exception e) {
                //ignored
            }
        } catch (Exception e) {
            //ignored
        }

        if (!(supportFragmentClass != null && supportFragmentClass.isInstance(fragment)) &&
                !(appFragmentClass != null && appFragmentClass.isInstance(fragment)) &&
                !(androidXFragmentClass != null && androidXFragmentClass.isInstance(fragment))) {
            return;
        }

        try {
            JSONObject properties = new JSONObject();
            String screenName = fragment.getClass().getCanonicalName();

            String title = null;

            if (fragment.getClass().isAnnotationPresent(SensorsDataFragmentTitle.class)) {
                SensorsDataFragmentTitle sensorsDataFragmentTitle = fragment.getClass().getAnnotation(SensorsDataFragmentTitle.class);
                if (sensorsDataFragmentTitle != null) {
                    title = sensorsDataFragmentTitle.title();
                }
            }

            if (Build.VERSION.SDK_INT >= 11) {
                Activity activity = null;
                try {
                    Method getActivityMethod = fragment.getClass().getMethod("getActivity");
                    if (getActivityMethod != null) {
                        activity = (Activity) getActivityMethod.invoke(fragment);
                    }
                } catch (Exception e) {
                    //ignored
                }
                if (activity != null) {
                    if (TextUtils.isEmpty(title)) {
                        title = SensorsDataUtils.getActivityTitle(activity);
                    }
                    screenName = String.format(TimeUtils.SDK_LOCALE, "%s|%s", activity.getClass().getCanonicalName(), screenName);
                }
            }

            if (!TextUtils.isEmpty(title)) {
                properties.put(AutoTrackConstants.TITLE, title);
            }
            properties.put("$screen_name", screenName);
            if (fragment instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) fragment;
                JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                if (otherProperties != null) {
                    JSONUtils.mergeJSONObject(otherProperties, properties);
                }
            }
            trackViewScreen(SAPageTools.getScreenUrl(fragment), properties);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void trackViewAppClick(View view) {
        trackViewAppClick(view, null);

    }

    @Override
    public void trackViewAppClick(final View view, final JSONObject properties) {
        if (view == null) {
            return;
        }
        try {
            JSONObject cloneProperties = properties != null ? JSONUtils.cloneJsonObject(properties) : new JSONObject();
            final JSONObject propertyJson = AopUtil.injectClickInfo(new ViewContext(view), cloneProperties, true);
            if (propertyJson != null) {
                SACoreHelper.getInstance().trackQueueEvent(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            SAModuleManager.getInstance().invokeModuleFunction(Modules.Visual.MODULE_NAME, Modules.Visual.METHOD_MERGE_VISUAL_PROPERTIES, propertyJson, view);
                            SACoreHelper.getInstance().trackEvent(new InputData().setEventName(AutoTrackConstants.APP_CLICK_EVENT_NAME).setEventType(EventType.TRACK).setProperties(propertyJson));
                        } catch (Exception e) {
                            SALog.printStackTrace(e);
                        }
                    }
                });
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void trackFragmentAppViewScreen() {
        mFragmentAPI.trackFragmentAppViewScreen();
    }

    @Override
    public boolean isTrackFragmentAppViewScreenEnabled() {
        return mFragmentAPI.isTrackFragmentAppViewScreenEnabled();
    }

    @Override
    public void enableAutoTrackFragment(Class<?> fragment) {
        mFragmentAPI.enableAutoTrackFragment(fragment);
    }

    @Override
    public void enableAutoTrackFragments(List<Class<?>> fragmentsList) {
        mFragmentAPI.enableAutoTrackFragments(fragmentsList);
    }

    @Override
    public boolean isFragmentAutoTrackAppViewScreen(Class<?> fragment) {
        return mFragmentAPI.isFragmentAutoTrackAppViewScreen(fragment);
    }

    @Override
    public void ignoreAutoTrackFragments(List<Class<?>> fragmentList) {
        mFragmentAPI.ignoreAutoTrackFragments(fragmentList);
    }

    @Override
    public void ignoreAutoTrackFragment(Class<?> fragment) {
        mFragmentAPI.ignoreAutoTrackFragment(fragment);
    }

    @Override
    public void resumeIgnoredAutoTrackFragments(List<Class<?>> fragmentList) {
        mFragmentAPI.resumeIgnoredAutoTrackFragments(fragmentList);
    }

    @Override
    public void resumeIgnoredAutoTrackFragment(Class<?> fragment) {
        mFragmentAPI.resumeIgnoredAutoTrackFragment(fragment);
    }

    private void enableAutoTrack(int autoTrackEventType) {
        try {
            if (autoTrackEventType <= 0 || autoTrackEventType > 15) {
                return;
            }
            this.mAutoTrack = true;
            mSAConfigOptions.setAutoTrackEventType(mSAConfigOptions.getAutoTrackEventType() | autoTrackEventType);
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }
}
