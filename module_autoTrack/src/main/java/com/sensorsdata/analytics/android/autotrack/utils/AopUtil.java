/*
 * Created by dengshiwei on 2022/07/01.
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

package com.sensorsdata.analytics.android.autotrack.utils;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.ToggleButton;

import com.sensorsdata.analytics.android.autotrack.core.beans.ViewContext;
import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.ScreenAutoTracker;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.autotrack.core.business.SAPageTools;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.SAFragmentUtils;
import com.sensorsdata.analytics.android.sdk.util.SAViewUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class AopUtil {
    private static final String TAG = "SA.AopUtil";

    /**
     * 构建 Title 和 Screen 的名称
     *
     * @param activity 页面
     * @return JSONObject
     */
    public static JSONObject buildTitleNoAutoTrackerProperties(Activity activity) {
        JSONObject propertyJSON = new JSONObject();
        try {
            propertyJSON.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
            String activityTitle = SAPageTools.getActivityTitle(activity);
            if (!TextUtils.isEmpty(activityTitle)) {
                propertyJSON.put(AopConstants.TITLE, activityTitle);
            }
            if (activity instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) activity;
                JSONObject trackProperties = screenAutoTracker.getTrackProperties();
                if (trackProperties != null) {
                    if (trackProperties.has(AopConstants.SCREEN_NAME)) {
                        propertyJSON.put(AopConstants.SCREEN_NAME, trackProperties.optString(AopConstants.SCREEN_NAME));
                    }
                    if (trackProperties.has(AopConstants.TITLE)) {
                        propertyJSON.put(AopConstants.TITLE, trackProperties.optString(AopConstants.TITLE));
                    }
                }
            }
        } catch (Exception ex) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(ex);
            return new JSONObject();
        }
        return propertyJSON;
    }

    /**
     * properties 注入点击事件信息
     * 属性的优先级为：预置属性低于 getTrackProperties() 低于 setViewProperties(View, JSONObject)} 低于 trackViewAppClick(View, JSONObject)}
     *
     * @param viewContext 点击的 ViewContext
     * @param properties 事件属性
     * @param isFromUser 是否由用户触发
     * @return isTrackEvent 是否发送事件
     */
    public static JSONObject injectClickInfo(ViewContext viewContext, JSONObject properties, boolean isFromUser) {
        if (viewContext.view == null) {
            return properties;
        }
        try {
            if (!isTrackEvent(viewContext.view, isFromUser)) {
                return null;
            }
            Context context = viewContext.view.getContext();
            JSONObject eventJson = new JSONObject();
            //1.获取预置属性
            String idString = SAViewUtils.getViewId(viewContext.view);
            if (!TextUtils.isEmpty(idString)) {
                //ViewId
                eventJson.put(AopConstants.ELEMENT_ID, idString);
            }

            String viewText = SAViewUtils.getViewContent(viewContext.view);
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                eventJson.put(AopConstants.ELEMENT_CONTENT, viewText);
            }
            //$element_type
            eventJson.put(AopConstants.ELEMENT_TYPE, SAViewUtils.getViewType(viewContext.view));

            Activity activity = viewContext.activity != null ? viewContext.activity : SAViewUtils.getActivityOfView(context, viewContext.view);
            //2.获取 Activity 页面信息及 ScreenAutoTracker 定义的属性
            if (activity != null) {
                JSONUtils.mergeJSONObject(SAPageTools.getActivityPageInfo(activity), eventJson);
            }

            //fragmentName
            Object fragment = viewContext.fragment != null ? viewContext.fragment : SAFragmentUtils.getFragmentFromView(viewContext.view, activity);
            if (fragment != null) {
                JSONUtils.mergeJSONObject(SAPageTools.getFragmentPageInfo(activity, fragment), eventJson);
            }
            //3.获取 View 自定义属性
            JSONObject p = (JSONObject) viewContext.view.getTag(R.id.sensors_analytics_tag_view_properties);
            if (p != null) {
                JSONUtils.mergeJSONObject(p, eventJson);
            }
            //4.事件传入的自定义属性
            JSONUtils.mergeDistinctProperty(eventJson, properties);
            return properties;
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
        return properties;
    }

    static boolean isTrackEvent(View view, boolean isFromUser) {
        if (view instanceof CheckBox) {
            if (!isFromUser) {
                return false;
            }
        } else if (view instanceof RadioButton) {
            if (!isFromUser) {
                return false;
            }
        } else if (view instanceof ToggleButton) {
            if (!isFromUser) {
                return false;
            }
        } else if (view instanceof CompoundButton) {
            if (!isFromUser) {
                return false;
            }
        }
        if (view instanceof RatingBar) {
            return isFromUser;
        }
        return true;
    }
}