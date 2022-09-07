/*
 * Created by dengshiwei on 2022/07/08.
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

package com.sensorsdata.analytics.android.autotrack.core.business;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;

import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.ScreenAutoTracker;
import com.sensorsdata.analytics.android.sdk.SensorsDataAutoTrackAppViewScreenUrl;
import com.sensorsdata.analytics.android.sdk.SensorsDataFragmentTitle;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.ReflectUtil;
import com.sensorsdata.analytics.android.sdk.util.SAFragmentUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;

import org.json.JSONObject;

public class SAPageTools {
    /* last page Url */
    private static String mLastScreenUrl;
    /*current page url */
    private static String mCurrentScreenUrl;
    /* last page Title */
    private static String mReferrerScreenTitle;
    /* current page Title */
    private static String mCurrentScreenTitle;
    /* current page property */
    private static JSONObject mCurrentScreenTrackProperties;

    public static String getLastScreenUrl() {
        return mLastScreenUrl;
    }

    public static void setLastScreenUrl(String lastScreenUrl) {
        mLastScreenUrl = lastScreenUrl;
    }

    public static String getReferrerScreenTitle() {
        return mReferrerScreenTitle;
    }

    public static String getCurrentScreenTitle() {
        return mCurrentScreenTitle;
    }

    public static void setCurrentScreenTitle(String currentScreenTitle) {
        mReferrerScreenTitle = mCurrentScreenTitle;
        mCurrentScreenTitle = currentScreenTitle;
    }

    public static void setCurrentScreenTrackProperties(JSONObject currentScreenTrackProperties) {
        mCurrentScreenTrackProperties = currentScreenTrackProperties;
    }

    public static String getCurrentScreenUrl() {
        return mCurrentScreenUrl;
    }

    public static void setCurrentScreenUrl(String currentScreenUrl) {
        mLastScreenUrl = mCurrentScreenUrl;
        mCurrentScreenUrl = currentScreenUrl;
    }

    public static JSONObject getCurrentScreenTrackProperties() {
        return mCurrentScreenTrackProperties;
    }

    /**
     * 尝试读取页面 title
     *
     * @param fragment Fragment
     * @param activity Activity
     */
    public static JSONObject getFragmentPageInfo(Activity activity, Object fragment) {
        JSONObject properties = new JSONObject();
        try {
            String screenName = null;
            String title = null;
            if (fragment instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) fragment;
                JSONObject trackProperties = screenAutoTracker.getTrackProperties();
                if (trackProperties != null) {
                    if (trackProperties.has(AopConstants.SCREEN_NAME)) {
                        screenName = trackProperties.optString(AopConstants.SCREEN_NAME);
                    }

                    if (trackProperties.has(AopConstants.TITLE)) {
                        title = trackProperties.optString(AopConstants.TITLE);
                    }
                    JSONUtils.mergeJSONObject(trackProperties, properties);
                }
            }
            boolean isTitleNull = TextUtils.isEmpty(title);
            boolean isScreenNameNull = TextUtils.isEmpty(screenName);
            if (isTitleNull && fragment.getClass().isAnnotationPresent(SensorsDataFragmentTitle.class)) {
                SensorsDataFragmentTitle sensorsDataFragmentTitle = fragment.getClass().getAnnotation(SensorsDataFragmentTitle.class);
                if (sensorsDataFragmentTitle != null) {
                    title = sensorsDataFragmentTitle.title();
                }
            }
            isTitleNull = TextUtils.isEmpty(title);
            if (isTitleNull || isScreenNameNull) {
                if (activity == null) {
                    activity = SAFragmentUtils.getActivityFromFragment(fragment);
                }
                if (activity != null) {
                    if (isTitleNull) {
                        title = SensorsDataUtils.getActivityTitle(activity);
                    }

                    if (isScreenNameNull) {
                        screenName = fragment.getClass().getCanonicalName();
                        screenName = String.format(TimeUtils.SDK_LOCALE, "%s|%s", activity.getClass().getCanonicalName(), screenName);
                    }
                }
            }

            if (!TextUtils.isEmpty(title)) {
                properties.put(AopConstants.TITLE, title);
            }

            if (TextUtils.isEmpty(screenName)) {
                screenName = fragment.getClass().getCanonicalName();
            }
            properties.put("$screen_name", screenName);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return properties;
    }

    /**
     * 构建 Title 和 Screen 的名称
     *
     * @param activity 页面
     * @return JSONObject
     */
    public static JSONObject getActivityPageInfo(Activity activity) {
        JSONObject propertyJSON = new JSONObject();
        try {
            propertyJSON.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
            String activityTitle = getActivityTitle(activity);
            if (!TextUtils.isEmpty(activityTitle)) {
                propertyJSON.put(AopConstants.TITLE, activityTitle);
            }

            if (activity instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) activity;
                JSONUtils.mergeJSONObject(screenAutoTracker.getTrackProperties(), propertyJSON);
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
            return new JSONObject();
        }
        return propertyJSON;
    }

    /**
     * 获取 Activity 的 title
     *
     * @param activity Activity
     * @return Activity 的 title
     */
    public static String getActivityTitle(Activity activity) {
        try {
            if (activity != null) {
                try {
                    String activityTitle = null;
                    if (!TextUtils.isEmpty(activity.getTitle())) {
                        activityTitle = activity.getTitle().toString();
                    }

                    if (Build.VERSION.SDK_INT >= 11) {
                        String toolbarTitle = SensorsDataUtils.getToolbarTitle(activity);
                        if (!TextUtils.isEmpty(toolbarTitle)) {
                            activityTitle = toolbarTitle;
                        }
                    }

                    if (TextUtils.isEmpty(activityTitle)) {
                        PackageManager packageManager = activity.getPackageManager();
                        if (packageManager != null) {
                            ActivityInfo activityInfo = packageManager.getActivityInfo(activity.getComponentName(), 0);
                            if (!TextUtils.isEmpty(activityInfo.loadLabel(packageManager))) {
                                activityTitle = activityInfo.loadLabel(packageManager).toString();
                            }
                        }
                    }

                    return activityTitle;
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return null;
        }
    }

    public static JSONObject getRNPageInfo() {
        return getRNPageInfo(null);
    }

    /**
     * 如果存在 RN 页面，优先获取 RN 的 screen_name
     *
     * @param view View
     * @return JSONObject
     */
    public static JSONObject getRNPageInfo(View view) {
        try {
            Class<?> rnViewUtils = ReflectUtil.getCurrentClass(new String[]{"com.sensorsdata.analytics.utils.RNViewUtils"});
            String properties = ReflectUtil.callStaticMethod(rnViewUtils, "getVisualizeProperties");
            if (!TextUtils.isEmpty(properties)) {
                JSONObject object = new JSONObject(properties);
                if (view != null && object.optBoolean("isSetRNViewTag", false)) {
                    Object isRNView = view.getTag(R.id.sensors_analytics_tag_view_rn_key);
                    if (isRNView == null || !(Boolean) isRNView) {
                        return null;
                    }
                }
                String rnScreenName = object.optString("$screen_name");
                String rnActivityTitle = object.optString("$title");
                JSONObject jsonObject = new JSONObject();
                if (jsonObject.has(AopConstants.SCREEN_NAME)) {
                    jsonObject.put(AopConstants.SCREEN_NAME, rnScreenName);
                }
                if (jsonObject.has(AopConstants.TITLE)) {
                    jsonObject.put(AopConstants.TITLE, rnActivityTitle);
                }
                return jsonObject;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    /**
     * get ScreenUrl
     *
     * @param object activity/fragment
     * @return screenUrl
     */
    public static String getScreenUrl(Object object) {
        if (object == null) {
            return null;
        }
        String screenUrl = null;
        try {
            if (object instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) object;
                screenUrl = screenAutoTracker.getScreenUrl();
            } else {
                SensorsDataAutoTrackAppViewScreenUrl autoTrackAppViewScreenUrl = object.getClass().getAnnotation(SensorsDataAutoTrackAppViewScreenUrl.class);
                if (autoTrackAppViewScreenUrl != null) {
                    screenUrl = autoTrackAppViewScreenUrl.url();
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        if (screenUrl == null) {
            screenUrl = object.getClass().getCanonicalName();
        }
        return screenUrl;
    }

}
