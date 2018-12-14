/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.webkit.WebView;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface ISensorsDataAPI {
    /**
     * 返回预置属性
     * @return JSONObject 预置属性
     */
    public JSONObject getPresetProperties();

    /**
     * 设置当前 serverUrl
     * @param serverUrl 当前 serverUrl
     */
    public void setServerUrl(String serverUrl);

    /**
     * 设置是否开启 log
     * @param enable boolean
     */
    public void enableLog(boolean enable);

    /**
     * 获取本地缓存上限制
     * @return 字节
     */
    public long getMaxCacheSize();

    /**
     * 返回档期是否是开启 debug 模式
     * @return true：是，false：不是
     */
    public boolean isDebugMode();

    /**
     * 返回是否允许后台上传数据，默认是true
     * @return 是否允许后台上传数据
     */
    boolean isFlushInBackground();

    /**
     * 设置是否允许后台上传数据，默认是 true
     * @param isFlush boolean
     */
    void setFlushInBackground(boolean isFlush);

    /**
     * 设置本地缓存上限值，单位 byte，默认为 32MB：32 * 1024 * 1024
     * @param maxCacheSize 单位 byte
     */
    public void setMaxCacheSize(long maxCacheSize);

    /**
     * 设置 flush 时网络发送策略，默认 3G、4G、WI-FI 环境下都会尝试 flush
     * @param networkType int 网络类型
     */
    public void setFlushNetworkPolicy(int networkType);

    /**
     * 两次数据发送的最小时间间隔，单位毫秒
     *
     * 默认值为15 * 1000毫秒
     * 在每次调用track、signUp以及profileSet等接口的时候，都会检查如下条件，以判断是否向服务器上传数据:
     *
     * 1. 是否是WIFI/3G/4G网络条件
     * 2. 是否满足发送条件之一:
     * 1) 与上次发送的时间间隔是否大于 flushInterval
     * 2) 本地缓存日志数目是否大于 flushBulkSize
     *
     * 如果满足这两个条件，则向服务器发送一次数据；如果不满足，则把数据加入到队列中，等待下次检查时把整个队列的内
     * 容一并发送。需要注意的是，为了避免占用过多存储，队列最多只缓存20MB数据。
     *
     * @return 返回时间间隔，单位毫秒
     */
    public int getFlushInterval();

    /**
     * 设置两次数据发送的最小时间间隔
     * @param flushInterval 时间间隔，单位毫秒
     */
    public void setFlushInterval(int flushInterval);

    /**
     * 返回本地缓存日志的最大条目数
     * @return 条数
     */
    public int getFlushBulkSize();

    /**
     * 设置本地缓存日志的最大条目数
     *
     * @param flushBulkSize 缓存数目
     */
    public void setFlushBulkSize(int flushBulkSize);

    /**
     * 设置 App 切换到后台与下次事件的事件间隔
     *
     * 默认值为 30*1000 毫秒
     *
     * 若 App 在后台超过设定事件，则认为当前 Session 结束，发送 $AppEnd 事件
     * @param sessionIntervalTime int
     */
    public void setSessionIntervalTime(int sessionIntervalTime);

    /**
     * 设置 App 切换到后台与下次事件的事件间隔
     *
     * 默认值为 30*1000 毫秒
     *
     * 若 App 在后台超过设定事件，则认为当前 Session 结束，发送 $AppEnd 事件
     *
     * @return 返回设置的 SessionIntervalTime ，默认是 30s
     */
    public int getSessionIntervalTime();

    /**
     * 打开 SDK 自动追踪
     *
     * 该功能自动追踪 App 的一些行为，例如 SDK 初始化、App 启动（$AppStart） / 关闭（$AppEnd）、
     * 进入页面（$AppViewScreen）等等，具体信息请参考文档:
     * https://sensorsdata.cn/manual/android_sdk.html
     *
     * 该功能仅在 API 14 及以上版本中生效，默认关闭
     */
    @Deprecated
    public void enableAutoTrack();

    /**
     * 打开 SDK 自动追踪
     *
     * 该功能自动追踪 App 的一些行为，指定哪些 AutoTrack 事件被追踪，具体信息请参考文档:
     * https://sensorsdata.cn/manual/android_sdk.html
     *
     * 该功能仅在 API 14 及以上版本中生效，默认关闭
     *
     * @param eventTypeList 开启 AutoTrack 的事件列表
     */
    public void enableAutoTrack(List<SensorsDataAPI.AutoTrackEventType> eventTypeList);

    /**
     * 关闭 AutoTrack 中的部分事件
     * @param eventTypeList AutoTrackEventType 类型 List
     */
    public void disableAutoTrack(List<SensorsDataAPI.AutoTrackEventType> eventTypeList);

    /**
     * 关闭 AutoTrack 中的某个事件
     * @param autoTrackEventType AutoTrackEventType 类型
     */
    public void disableAutoTrack(SensorsDataAPI.AutoTrackEventType autoTrackEventType);

    /**
     * 自动收集 App Crash 日志，该功能默认是关闭的
     */
    public void trackAppCrash();

    /**
     * 是否开启 AutoTrack
     * @return true: 开启 AutoTrack; false：没有开启 AutoTrack
     */
    public boolean isAutoTrackEnabled();

    /**
     * 是否开启了支持 Butterknife
     * @return true：支持，false：不支持
     */
    public boolean isButterknifeOnClickEnabled();

    /**
     * 是否开启自动追踪 Fragment 的 $AppViewScreen 事件
     * 默认不开启
     */
    public void trackFragmentAppViewScreen();

    public boolean isTrackFragmentAppViewScreenEnabled();

    /**
     * 开启 AutoTrack 支持 React Native
     */
    public void enableReactNativeAutoTrack();

    public boolean isReactNativeAutoTrackEnabled();

    /**
     * 向WebView注入本地方法, 将distinctId传递给当前的WebView
     *
     * @param webView 当前WebView
     * @param isSupportJellyBean 是否支持API level 16及以下的版本。
     * 因为API level 16及以下的版本, addJavascriptInterface有安全漏洞,请谨慎使用
     */
    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    public void showUpWebView(WebView webView, boolean isSupportJellyBean);

    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    public void showUpWebView(WebView webView, boolean isSupportJellyBean, boolean enableVerify);

    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    public void showUpWebView(WebView webView, JSONObject properties, boolean isSupportJellyBean, boolean enableVerify);

    /**
     * 向WebView注入本地方法, 将distinctId传递给当前的WebView
     *
     * @param webView 当前WebView
     * @param isSupportJellyBean 是否支持API level 16及以下的版本。
     *                           因为API level 16及以下的版本, addJavascriptInterface有安全漏洞,请谨慎使用
     * @param properties 用户自定义属性
     */
    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    public void showUpWebView(WebView webView, boolean isSupportJellyBean, JSONObject properties);

    public void showUpX5WebView(Object x5WebView, JSONObject properties, boolean isSupportJellyBean, boolean enableVerify);

    public void showUpX5WebView(Object x5WebView, boolean enableVerify);

    public void showUpX5WebView(Object x5WebView);

    /**
     * 指定哪些 activity 不被AutoTrack
     *
     * 指定activity的格式为：activity.getClass().getCanonicalName()
     *
     * @param activitiesList  activity列表
     */
    public void ignoreAutoTrackActivities(List<Class<?>> activitiesList);

    /**
     * 恢复不被 AutoTrack 的 activity
     * @param activitiesList List
     */
    public void resumeAutoTrackActivities(List<Class<?>> activitiesList);

    /**
     * 指定某个 activity 不被 AutoTrack
     * @param activity Activity
     */
    public void ignoreAutoTrackActivity(Class<?> activity);

    /**
     * 恢复不被 AutoTrack 的 activity
     * @param activity Class
     */
    public void resumeAutoTrackActivity(Class<?> activity);

    /**
     * 指定 fragment 被 AutoTrack 采集
     * @param fragment Fragment
     */
    void enableAutoTrackFragment(Class<?> fragment);

    /**
     * 指定 fragments 被 AutoTrack 采集
     * @param fragmentsList Fragment 集合
     */
    void enableAutoTrackFragments(List<Class<?>> fragmentsList);

    /**
     * 指定 fragment 被 AutoTrack 采集
     * @param fragmentName String
     */
    void enableAutoTrackFragment(String fragmentName);

    /**
     * 判断 AutoTrack 时，某个 Activity 的 $AppViewScreen 是否被过滤
     * 如果过滤的话，会过滤掉 Activity 的 $AppViewScreen 事件
     * @param activity Activity
     * @return Activity 是否被采集
     */
    public boolean isActivityAutoTrackAppViewScreenIgnored(Class<?> activity);

    /**
     * 判断 AutoTrack 时，某个 Fragment 的 $AppViewScreen 是否被采集
     * @param fragment Fragment
     * @return Fragment 是否被采集
     */
    boolean isFragmentAutoTrackAppViewScreen(Class<?> fragment);

    /**
     * 判断 AutoTrack 时，某个 Activity 的 $AppClick 是否被过滤
     * 如果过滤的话，会过滤掉 Activity 的 $AppClick 事件
     * @param activity Activity
     * @return Activity 是否被过滤
     */
    public boolean isActivityAutoTrackAppClickIgnored(Class<?> activity);

    /**
     * 过滤掉 AutoTrack 的某个事件类型
     * @param autoTrackEventType AutoTrackEventType
     */
    @Deprecated
    public void ignoreAutoTrackEventType(SensorsDataAPI.AutoTrackEventType autoTrackEventType);

    /**
     * 过滤掉 AutoTrack 的某些事件类型
     * @param eventTypeList AutoTrackEventType List
     */
    @Deprecated
    public void ignoreAutoTrackEventType(List<SensorsDataAPI.AutoTrackEventType> eventTypeList);

    /**
     * 判断 某个 AutoTrackEventType 是否被忽略
     * @param eventType AutoTrackEventType
     * @return true 被忽略; false 没有被忽略
     */
    public boolean isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType eventType);

    /**
     * 设置界面元素ID
     *
     * @param view   要设置的View
     * @param viewID String 给这个View的ID
     */
    public void setViewID(View view, String viewID);

    /**
     * 设置界面元素ID
     *
     * @param view   要设置的View
     * @param viewID String 给这个View的ID
     */
    public void setViewID(android.app.Dialog view, String viewID);

    /**
     * 设置界面元素ID
     *
     * @param view   要设置的View
     * @param viewID String 给这个View的ID
     */
    public void setViewID(Object view, String viewID);

    /**
     * 设置 View 所属 Activity
     *
     * @param view   要设置的View
     * @param activity Activity View 所属 Activity
     */
    public void setViewActivity(View view, Activity activity);

    /**
     * 设置 View 所属 Fragment 名称
     *
     * @param view   要设置的View
     * @param fragmentName String View 所属 Fragment 名称
     */
    public void setViewFragmentName(View view, String fragmentName);

    /**
     * 忽略View
     *
     * @param view 要忽略的View
     */
    public void ignoreView(View view);

    /**
     * 忽略View
     * @param view View
     * @param ignore 是否忽略
     */
    public void ignoreView(View view, boolean ignore);

    /**
     * 设置View属性
     *
     * @param view       要设置的View
     * @param properties 要设置的View的属性
     */
    public void setViewProperties(View view, JSONObject properties);

    public List<Class> getIgnoredViewTypeList();

    /**
     * 获取需要采集的 Fragment 集合
     * @return Set
     */
    Set<Integer> getAutoTrackFragments();

    /**
     * 忽略某一类型的 View
     *
     * @param viewType Class
     */
    public void ignoreViewType(Class viewType);

    public boolean isHeatMapActivity(Class<?> activity);

    public void addHeatMapActivity(Class<?> activity);

    public void addHeatMapActivities(List<Class<?>> activitiesList);

    public boolean isHeatMapEnabled();

    public void enableAppHeatMapConfirmDialog(boolean enable);

    /**
     * 开启 HeatMap，$AppClick 事件将会采集控件的 viewPath
     */
    public void enableHeatMap();

    /**
     * 获取当前用户的distinctId
     *
     * 若调用前未调用 {@link #identify(String)} 设置用户的 distinctId，SDK 会调用 {@link java.util.UUID} 随机生成
     * UUID，作为用户的 distinctId
     *
     * 该方法已不推荐使用，请参考 {@link #getAnonymousId()}
     *
     * @return 当前用户的distinctId
     */
    @Deprecated
    public String getDistinctId();

    /**
     * 获取当前用户的匿名id
     *
     * 若调用前未调用 {@link #identify(String)} 设置用户的匿名id，SDK 会调用 {@link java.util.UUID} 随机生成
     * UUID，作为用户的匿名id
     *
     * @return 当前用户的匿名id
     */
    public String getAnonymousId();

    /**
     * 重置默认匿名id
     */
    public void resetAnonymousId();

    /**
     * 获取当前用户的 loginId
     *
     * 若调用前未调用 {@link #login(String)} 设置用户的 loginId，会返回null
     *
     * @return 当前用户的 loginId
     */
    public String getLoginId();

    /**
     * 设置当前用户的distinctId。一般情况下，如果是一个注册用户，则应该使用注册系统内
     * 的user_id，如果是个未注册用户，则可以选择一个不会重复的匿名ID，如设备ID等，如果
     * 客户没有调用identify，则使用SDK自动生成的匿名ID
     *
     * @param distinctId 当前用户的distinctId，仅接受数字、下划线和大小写字母
     */
    public void identify(String distinctId);

    /**
     * 登录，设置当前用户的 loginId
     *
     * @param loginId 当前用户的 loginId，不能为空，且长度不能大于255
     */
    public void login(String loginId);

    /**
     * 登录，设置当前用户的 loginId
     *
     * @param loginId 当前用户的 loginId，不能为空，且长度不能大于255
     * @param properties 用户登录属性
     */
    public void login(final String loginId , final JSONObject properties);

    /**
     * 注销，清空当前用户的 loginId
     */
    public void logout();

    /**
     * 记录第一次登录行为
     *
     * 这个接口是一个较为复杂的功能，请在使用前先阅读相关说明:
     * http://www.sensorsdata.cn/manual/track_signup.html
     * 并在必要时联系我们的技术支持人员。
     * 该方法已不推荐使用，可以具体参考 {@link #login(String)} 方法
     *
     * @param newDistinctId 用户完成注册后生成的注册ID
     * @param properties    事件的属性
     */
    @Deprecated
    public void trackSignUp(String newDistinctId, JSONObject properties);

    /**
     * 与 {@link #trackSignUp(String, JSONObject)} 类似，无事件属性
     *
     * 这个接口是一个较为复杂的功能，请在使用前先阅读相关说明:
     * http://www.sensorsdata.cn/manual/track_signup.html，
     * 并在必要时联系我们的技术支持人员。
     * 该方法已不推荐使用，可以具体参考 {@link #login(String)} 方法
     *
     * @param newDistinctId 用户完成注册后生成的注册ID
     */
    @Deprecated
    public void trackSignUp(String newDistinctId);

    /**
     * 用于在 App 首次启动时追踪渠道来源，并设置追踪渠道事件的属性。
     *
     * 这是 Sensors Analytics 进阶功能，请参考文档 https://sensorsdata.cn/manual/track_installation.html
     *
     * @param eventName  渠道追踪事件的名称
     * @param properties 渠道追踪事件的属性
     * @param disableCallback 是否关闭这次渠道匹配的回调请求
     */
    public void trackInstallation(String eventName, JSONObject properties, boolean disableCallback);

    /**
     * 用于在 App 首次启动时追踪渠道来源，并设置追踪渠道事件的属性。
     *
     * 这是 Sensors Analytics 进阶功能，请参考文档 https://sensorsdata.cn/manual/track_installation.html
     *
     * @param eventName  渠道追踪事件的名称
     * @param properties 渠道追踪事件的属性
     */
    public void trackInstallation(String eventName, JSONObject properties);

    /**
     * 用于在 App 首次启动时追踪渠道来源，并设置追踪渠道事件的属性。
     *
     * 这是 Sensors Analytics 进阶功能，请参考文档 https://sensorsdata.cn/manual/track_installation.html
     *
     * @param eventName  渠道追踪事件的名称
     */
    public void trackInstallation(String eventName);

    /**
     * 调用track接口，追踪一个带有属性的事件
     *
     * @param eventName  事件的名称
     * @param properties 事件的属性
     */
    public void track(String eventName, JSONObject properties);

    /**
     * 与 {@link #track(String, JSONObject)} 类似，无事件属性
     *
     * @param eventName 事件的名称
     */
    public void track(String eventName);

    /**
     * 初始化事件的计时器，默认计时单位为毫秒。
     *
     * 详细用法请参考 trackTimer(String, TimeUnit)
     *
     * @param eventName 事件的名称
     */
    @Deprecated
    public void trackTimer(final String eventName);

    /**
     * 初始化事件的计时器。
     *
     * 若需要统计某个事件的持续时间，先在事件开始时调用 trackTimer("Event") 记录事件开始时间，该方法并不会真正发
     * 送事件；随后在事件结束时，调用 track("Event", properties)，SDK 会追踪 "Event" 事件，并自动将事件持续时
     * 间记录在事件属性 "event_duration" 中。
     *
     * 多次调用 trackTimer("Event") 时，事件 "Event" 的开始时间以最后一次调用时为准。
     *
     * @param eventName 事件的名称
     * @param timeUnit  计时结果的时间单位
     */
    @Deprecated
    public void trackTimer(final String eventName, final TimeUnit timeUnit);

    /**
     * 初始化事件的计时器。
     *
     * 若需要统计某个事件的持续时间，先在事件开始时调用 trackTimer("Event") 记录事件开始时间，该方法并不会真正发
     * 送事件；随后在事件结束时，调用 track("Event", properties)，SDK 会追踪 "Event" 事件，并自动将事件持续时
     * 间记录在事件属性 "event_duration" 中。
     *
     * 多次调用 trackTimer("Event") 时，事件 "Event" 的开始时间以最后一次调用时为准。
     *
     * @param eventName 事件的名称
     * @param eventTimer 事件的自定义 EventTimer
     */
    @Deprecated
    void trackTimer(final String eventName, final EventTimer eventTimer);

    /**
     * 删除事件的计时器
     *
     * @param eventName 事件名称
     */
    void removeTimer(final String eventName);

    /**
     * 初始化事件的计时器，默认计时单位为毫秒。
     *
     * 详细用法请参考 trackTimerBegin(String, TimeUnit)
     *
     * @param eventName 事件的名称
     */
    public void trackTimerBegin(final String eventName);

    /**
     * 初始化事件的计时器。
     *
     * 若需要统计某个事件的持续时间，先在事件开始时调用 trackTimerBegin("Event") 记录事件开始时间，该方法并不会真正发
     * 送事件；随后在事件结束时，调用 track("Event", properties)，SDK 会追踪 "Event" 事件，并自动将事件持续时
     * 间记录在事件属性 "event_duration" 中。
     *
     * 多次调用 trackTimerBegin("Event") 时，事件 "Event" 的开始时间以最后一次调用时为准。
     *
     * @param eventName 事件的名称
     * @param timeUnit  计时结果的时间单位
     */
    public void trackTimerBegin(final String eventName, final TimeUnit timeUnit);

    /**
     * 停止事件计时器
     * @param eventName 事件的名称
     * @param properties 事件的属性
     */
    public void trackTimerEnd(final String eventName, JSONObject properties);

    /**
     * 停止事件计时器
     * @param eventName 事件的名称
     */
    public void trackTimerEnd(final String eventName);

    /**
     * 清除所有事件计时器
     */
    public void clearTrackTimer();

    /**
     * 获取LastScreenUrl
     * @return String
     */
    public String getLastScreenUrl();

    /**
     * App 退出或进到后台时清空 referrer，默认情况下不清空
     */
    public void clearReferrerWhenAppEnd();

    public void clearLastScreenUrl();

    public String getMainProcessName();

    /**
     * 获取LastScreenTrackProperties
     * @return JSONObject
     */
    public JSONObject getLastScreenTrackProperties();

    /**
     * Track 进入页面事件 ($AppViewScreen)
     * @param url String
     * @param properties JSONObject
     */
    public void trackViewScreen(String url, JSONObject properties);

    /**
     * Track Activity 进入页面事件($AppViewScreen)
     * @param activity activity Activity，当前 Activity
     */
    public void trackViewScreen(Activity activity);

    public void trackViewScreen(Object fragment);

    /**
     * 将所有本地缓存的日志发送到 Sensors Analytics.
     */
    public void flush();

    /**
     * 以阻塞形式将所有本地缓存的日志发送到 Sensors Analytics，该方法不能在 UI 线程调用。
     */
    public void flushSync();

    /**
     * 注册事件动态公共属性
     *
     * @param dynamicSuperProperties 事件动态公共属性回调接口
     */
    public void registerDynamicSuperProperties(SensorsDataDynamicSuperProperties dynamicSuperProperties);

    /**
     * 获取事件公共属性
     *
     * @return 当前所有Super属性
     */
    public JSONObject getSuperProperties();

    /**
     * 注册所有事件都有的公共属性
     *
     * @param superProperties 事件公共属性
     */
    public void registerSuperProperties(JSONObject superProperties);

    /**
     * 删除事件公共属性
     *
     * @param superPropertyName 事件属性名称
     */
    public void unregisterSuperProperty(String superPropertyName);

    /**
     * 删除所有事件公共属性
     */
    public void clearSuperProperties();

    /**
     * 设置用户的一个或多个Profile。
     * Profile如果存在，则覆盖；否则，新创建。
     *
     * @param properties 属性列表
     */
    public void profileSet(JSONObject properties);

    /**
     * 设置用户的一个Profile，如果之前存在，则覆盖，否则，新创建
     *
     * @param property 属性名称
     * @param value    属性的值，值的类型只允许为
     *                 {@link String}, {@link Number}, {@link java.util.Date}, {@link List}
     */
    public void profileSet(String property, Object value);

    /**
     * 首次设置用户的一个或多个Profile。
     * 与profileSet接口不同的是，如果之前存在，则忽略，否则，新创建
     *
     * @param properties 属性列表
     */
    public void profileSetOnce(JSONObject properties);

    /**
     * 首次设置用户的一个Profile
     * 与profileSet接口不同的是，如果之前存在，则忽略，否则，新创建
     *
     * @param property 属性名称
     * @param value    属性的值，值的类型只允许为
     *                 {@link String}, {@link Number}, {@link java.util.Date}, {@link List}
     */
    public void profileSetOnce(String property, Object value);

    /**
     * 给一个或多个数值类型的Profile增加一个数值。只能对数值型属性进行操作，若该属性
     * 未设置，则添加属性并设置默认值为0
     *
     * @param properties 一个或多个属性集合
     */
    public void profileIncrement(Map<String, ? extends Number> properties);

    /**
     * 给一个数值类型的Profile增加一个数值。只能对数值型属性进行操作，若该属性
     * 未设置，则添加属性并设置默认值为0
     *
     * @param property 属性名称
     * @param value    属性的值，值的类型只允许为 {@link Number}
     */
    public void profileIncrement(String property, Number value);

    /**
     * 给一个列表类型的Profile增加一个元素
     *
     * @param property 属性名称
     * @param value    新增的元素
     */
    public void profileAppend(String property, String value);

    /**
     * 给一个列表类型的Profile增加一个或多个元素
     *
     * @param property 属性名称
     * @param values   新增的元素集合
     */
    public void profileAppend(String property, Set<String> values);

    /**
     * 删除用户的一个Profile
     *
     * @param property 属性名称
     */
    public void profileUnset(String property);

    /**
     * 删除用户所有Profile
     */
    public void profileDelete();

    public void trackEventFromH5(String eventInfo, boolean enableVerify);

    public void trackEventFromH5(String eventInfo);

    /**
     * 更新 GPS 位置信息
     * @param latitude 纬度
     * @param longitude 经度
     */
    public void setGPSLocation(double latitude, double longitude);

    /**
     * 清楚 GPS 位置信息
     */
    public void clearGPSLocation();

    /**
     * 开启/关闭采集屏幕方向
     * @param enable true：开启 false：关闭
     */
    public void enableTrackScreenOrientation(boolean enable);

    /**
     * 恢复采集屏幕方向
     */
    public void resumeTrackScreenOrientation();

    /**
     * 暂停采集屏幕方向
     */
    public void stopTrackScreenOrientation();

    /**
     * 获取当前屏幕方向
     * @return portrait:竖屏 landscape:横屏
     */
    public String getScreenOrientation();

    /**
     * 初始化事件的计时器，计时单位为秒。
     *
     * @param eventName 事件的名称
     */
    public void trackTimerStart(final String eventName);

    /**
     * 设置 Cookie，flush 的时候会设置 HTTP 的 cookie
     * 内部会 URLEncoder.encode(cookie, "UTF-8")
     * @param cookie String cookie
     * @param encode boolean 是否 encode
     */
    public void setCookie(final String cookie, boolean encode);

    /**
     * 获取已设置的 Cookie
     * URLDecoder.decode(Cookie, "UTF-8")
     * @param decode String
     * @return String cookie
     */
    public String getCookie(boolean decode);

    /**
     * 删除本地缓存的全部事件
     */
    public void deleteAll();

    /**
     * 保存用户推送 ID 到用户表
     * @param propertyKey 属性名称（例如 jgId）
     * @param pushId  推送 ID
     *                使用 profilePushId("jgId",JPushInterface.getRegistrationID(this))
     */

    void profilePushId(String propertyKey, String pushId);
}
