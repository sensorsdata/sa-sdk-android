/*
 * Created by dengshiwei on 2021/07/31.
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

package com.sensorsdata.analytics.android.sdk.autotrack;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.AppStateManager;
import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.ScreenAutoTracker;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.util.AopUtil;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;
import com.sensorsdata.analytics.android.sdk.util.SAFragmentUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONObject;

/**
 * Fragment 的页面浏览
 */
public class FragmentViewScreenCallbacks implements SAFragmentLifecycleCallbacks {

    @Override
    public void onCreate(Object object) {

    }

    @Override
    public void onViewCreated(Object object, View rootView, Bundle bundle) {
        try {
            //Fragment名称
            String fragmentName = object.getClass().getName();
            rootView.setTag(R.id.sensors_analytics_tag_view_fragment_name, fragmentName);

            if (rootView instanceof ViewGroup) {
                traverseView(fragmentName, (ViewGroup) rootView);
            }

            //获取所在的 Context
            Context context = rootView.getContext();
            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, rootView);
            if (activity != null) {
                Window window = activity.getWindow();
                if (window != null && window.isActive()) {
                    window.getDecorView().getRootView().setTag(R.id.sensors_analytics_tag_view_fragment_name, "");
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void onStart(Object object) {

    }

    @Override
    public void onResume(Object object) {
        try {
            if (SensorsDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_VIEW_SCREEN)) {
                return;
            }

            if (!SensorsDataAPI.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
                return;
            }

            if (SAFragmentUtils.isFragmentVisible(object)) {
                trackFragmentAppViewScreen(object);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void onPause(Object object) {

    }

    @Override
    public void onStop(Object object) {

    }

    @Override
    public void onHiddenChanged(Object object, boolean hidden) {
        try {
            if (SensorsDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_VIEW_SCREEN)) {
                return;
            }

            if (!SensorsDataAPI.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
                return;
            }

            if (SAFragmentUtils.isFragmentVisible(object)) {
                trackFragmentAppViewScreen(object);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void setUserVisibleHint(Object object, boolean isVisibleToUser) {
        try {
            if (SensorsDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_VIEW_SCREEN)) {
                return;
            }

            if (!SensorsDataAPI.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
                return;
            }

            if (SAFragmentUtils.isFragmentVisible(object)) {
                trackFragmentAppViewScreen(object);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private static void trackFragmentAppViewScreen(Object fragment) {
        try {
            if (SensorsDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_VIEW_SCREEN)) {
                return;
            }

            if (!SensorsDataAPI.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
                return;
            }

            if ("com.bumptech.glide.manager.SupportRequestManagerFragment".equals(fragment.getClass().getCanonicalName())) {
                return;
            }

            boolean isAutoTrackFragment = SensorsDataAPI.sharedInstance().isFragmentAutoTrackAppViewScreen(fragment.getClass());
            if (!isAutoTrackFragment) {
                return;
            }
            JSONObject properties = new JSONObject();
            AopUtil.getScreenNameAndTitleFromFragment(properties, fragment, null);
            AppStateManager.getInstance().setFragmentScreenName(fragment, properties.optString(AopConstants.SCREEN_NAME));
            if (fragment instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) fragment;
                JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                if (otherProperties != null) {
                    SensorsDataUtils.mergeJSONObject(otherProperties, properties);
                }
            }
            JSONObject eventProperties = SADataHelper.appendLibMethodAutoTrack(properties);
            SensorsDataAPI.sharedInstance().trackViewScreen(SensorsDataUtils.getScreenUrl(fragment), eventProperties);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private static void traverseView(String fragmentName, ViewGroup root) {
        try {
            if (TextUtils.isEmpty(fragmentName) || root == null) {
                return;
            }
            final int childCount = root.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                final View child = root.getChildAt(i);
                child.setTag(R.id.sensors_analytics_tag_view_fragment_name, fragmentName);
                if (child instanceof ViewGroup && !(child instanceof ListView ||
                        child instanceof GridView ||
                        child instanceof Spinner ||
                        child instanceof RadioGroup)) {
                    traverseView(fragmentName, (ViewGroup) child);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
