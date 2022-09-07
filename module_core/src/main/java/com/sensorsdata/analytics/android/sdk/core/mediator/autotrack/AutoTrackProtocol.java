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

package com.sensorsdata.analytics.android.sdk.core.mediator.autotrack;

import android.app.Activity;
import android.view.View;

import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;

import org.json.JSONObject;

import java.util.List;

public interface AutoTrackProtocol extends IFragmentAPI {
    /**
     * 打开 SDK 自动追踪
     * 该功能自动追踪 App 的一些行为，指定哪些 AutoTrack 事件被追踪，具体信息请参考文档:
     * https://sensorsdata.cn/manual/android_sdk.html
     * 该功能仅在 API 14 及以上版本中生效，默认关闭
     *
     * @param eventTypeList 开启 AutoTrack 的事件列表
     */
    void enableAutoTrack(List<SensorsDataAPI.AutoTrackEventType> eventTypeList);

    /**
     * 关闭 AutoTrack 中的部分事件
     *
     * @param eventTypeList AutoTrackEventType 类型 List
     */
    void disableAutoTrack(List<SensorsDataAPI.AutoTrackEventType> eventTypeList);

    /**
     * 关闭 AutoTrack 中的某个事件
     *
     * @param autoTrackEventType AutoTrackEventType 类型
     */
    void disableAutoTrack(SensorsDataAPI.AutoTrackEventType autoTrackEventType);

    /**
     * 是否开启 AutoTrack
     *
     * @return true: 开启 AutoTrack; false：没有开启 AutoTrack
     */
    boolean isAutoTrackEnabled();

    /**
     * 指定哪些 activity 不被 AutoTrack
     * 指定 activity 的格式为：activity.getClass().getCanonicalName()
     *
     * @param activitiesList activity 列表
     */
    void ignoreAutoTrackActivities(List<Class<?>> activitiesList);

    /**
     * 恢复不被 AutoTrack 的 activity
     *
     * @param activitiesList List
     */
    void resumeAutoTrackActivities(List<Class<?>> activitiesList);

    /**
     * 指定某个 activity 不被 AutoTrack
     *
     * @param activity Activity
     */
    void ignoreAutoTrackActivity(Class<?> activity);

    /**
     * 恢复不被 AutoTrack 的 activity
     *
     * @param activity Class
     */
    void resumeAutoTrackActivity(Class<?> activity);

    /**
     * 判断 AutoTrack 时，某个 Activity 的 $AppViewScreen 是否被过滤
     * 如果过滤的话，会过滤掉 Activity 的 $AppViewScreen 事件
     *
     * @param activity Activity
     * @return Activity 是否被采集
     */
    boolean isActivityAutoTrackAppViewScreenIgnored(Class<?> activity);

    /**
     * 判断 AutoTrack 时，某个 Activity 的 $AppClick 是否被过滤
     * 如果过滤的话，会过滤掉 Activity 的 $AppClick 事件
     *
     * @param activity Activity
     * @return Activity 是否被过滤
     */
    boolean isActivityAutoTrackAppClickIgnored(Class<?> activity);

    /**
     * 判断某个 AutoTrackEventType 是否被忽略
     *
     * @param eventType AutoTrackEventType
     * @return true 被忽略; false 没有被忽略
     */
    boolean isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType eventType);

    /**
     * 判断某个 AutoTrackEventType 是否被忽略
     *
     * @param autoTrackEventType SensorsAnalyticsAutoTrackEventType 中的事件类型，可通过 '|' 进行连接传递
     * @return true 被忽略; false 没有被忽略
     */
    boolean isAutoTrackEventTypeIgnored(int autoTrackEventType);

    /**
     * 设置界面元素 ID
     *
     * @param view 要设置的 View
     * @param viewID String 给这个 View 的 ID
     */
    void setViewID(View view, String viewID);

    /**
     * 设置界面元素 ID
     *
     * @param view 要设置的 View
     * @param viewID String 给这个 View 的 ID
     */
    void setViewID(android.app.Dialog view, String viewID);

    /**
     * 设置界面元素 ID
     *
     * @param view 要设置的 View
     * @param viewID String 给这个 View 的 ID
     */
    void setViewID(Object view, String viewID);

    /**
     * 设置 View 所属 Activity
     *
     * @param view 要设置的 View
     * @param activity Activity View 所属 Activity
     */
    void setViewActivity(View view, Activity activity);

    /**
     * 设置 View 所属 Fragment 名称
     *
     * @param view 要设置的 View
     * @param fragmentName String View 所属 Fragment 名称
     */
    void setViewFragmentName(View view, String fragmentName);

    /**
     * 忽略 View
     *
     * @param view 要忽略的 View
     */
    void ignoreView(View view);

    /**
     * 忽略View
     *
     * @param view View
     * @param ignore 是否忽略
     */
    void ignoreView(View view, boolean ignore);

    /**
     * 设置View属性
     *
     * @param view 要设置的 View
     * @param properties 要设置的 View 的属性
     */
    void setViewProperties(View view, JSONObject properties);

    /**
     * 获取忽略采集 View 的集合
     *
     * @return 忽略采集的 View 集合
     */
    List<Class<?>> getIgnoredViewTypeList();

    /**
     * 忽略某一类型的 View
     *
     * @param viewType Class
     */
    void ignoreViewType(Class<?> viewType);

    /**
     * 获取 LastScreenUrl
     *
     * @return String
     */
    String getLastScreenUrl();

    /**
     * App 退出或进到后台时清空 referrer，默认情况下不清空
     */
    void clearReferrerWhenAppEnd();

    /**
     * 清除 LastScreenUrl
     */
    void clearLastScreenUrl();

    /**
     * 获取 LastScreenTrackProperties
     *
     * @return JSONObject
     */
    JSONObject getLastScreenTrackProperties();

    /**
     * Track 进入页面事件 ($AppViewScreen)，该接口需要在 properties 中手动设置 $screen_name 和 $title 属性。
     *
     * @param url String
     * @param properties JSONObject
     */
    void trackViewScreen(String url, JSONObject properties);

    /**
     * Track Activity 进入页面事件($AppViewScreen)
     *
     * @param activity activity Activity，当前 Activity
     */
    void trackViewScreen(Activity activity);

    /**
     * Track  Fragment 进入页面事件 ($AppViewScreen)
     *
     * @param fragment Fragment
     */
    void trackViewScreen(Object fragment);

    /**
     * Track 控件点击事件 ($AppClick)
     *
     * @param view View
     */
    void trackViewAppClick(View view);

    /**
     * Track 控件点击事件 ($AppClick)
     *
     * @param view View
     * @param properties 事件属性
     */
    void trackViewAppClick(View view, JSONObject properties);


}
