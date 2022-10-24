/*
 * Created by dengshiwei on 2022/10/14.
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

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;

import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.ScreenAutoTracker;
import com.sensorsdata.analytics.android.sdk.SensorsDataFragmentTitle;

import org.json.JSONObject;

public class SAPageInfoUtils {
    public static final String SCREEN_NAME = "$screen_name";
    public static final String TITLE = "$title";

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
                    if (trackProperties.has(SCREEN_NAME)) {
                        screenName = trackProperties.optString(SCREEN_NAME);
                    }

                    if (trackProperties.has(TITLE)) {
                        title = trackProperties.optString(TITLE);
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
                properties.put(TITLE, title);
            }

            if (TextUtils.isEmpty(screenName)) {
                screenName = fragment.getClass().getCanonicalName();
            }
            properties.put(SCREEN_NAME, screenName);
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
            propertyJSON.put(SCREEN_NAME, activity.getClass().getCanonicalName());
            String activityTitle = SensorsDataUtils.getActivityTitle(activity);
            if (!TextUtils.isEmpty(activityTitle)) {
                propertyJSON.put(TITLE, activityTitle);
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
                jsonObject.put(SCREEN_NAME, rnScreenName);
                jsonObject.put(TITLE, rnActivityTitle);
                return jsonObject;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }
}
