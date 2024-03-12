/*
 * Created by wangzhuozhou on 2015/08/01.
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
package com.sensorsdata.analytics.android.sdk;

import android.webkit.WebView;

import com.sensorsdata.analytics.android.sdk.core.mediator.advert.SAAdvertAPIProtocol;
import com.sensorsdata.analytics.android.sdk.core.mediator.autotrack.AutoTrackProtocol;
import com.sensorsdata.analytics.android.sdk.core.mediator.exposure.SAExposureAPIProtocol;
import com.sensorsdata.analytics.android.sdk.plugin.property.SAPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.useridentity.IUserIdentityAPI;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface ISensorsDataAPI extends IUserIdentityAPI, SAAdvertAPIProtocol, AutoTrackProtocol, SAExposureAPIProtocol {
    /**
     * 返回预置属性
     *
     * @return JSONObject 预置属性
     */
    JSONObject getPresetProperties();

    /**
     * 获取当前 serverUrl
     *
     * @return 当前 serverUrl
     */
    String getServerUrl();

    /**
     * 设置当前 serverUrl
     *
     * @param serverUrl 当前 serverUrl
     */
    void setServerUrl(String serverUrl);

    /**
     * 设置当前 serverUrl
     *
     * @param serverUrl 当前 serverUrl
     * @param isRequestRemoteConfig 是否立即请求当前 serverUrl 的远程配置
     */
    void setServerUrl(String serverUrl, boolean isRequestRemoteConfig);

    /**
     * 设置是否开启 log
     *
     * @param enable boolean
     */
    void enableLog(boolean enable);

    /**
     * 获取本地缓存上限制
     *
     * @return 字节
     */
    long getMaxCacheSize();

    /**
     * 设置本地缓存上限值，单位 byte，默认为 32MB：32 * 1024 * 1024，最小 16MB：16 * 1024 * 1024，若小于 16MB，则按 16MB 处理。
     *
     * @param maxCacheSize 单位 byte
     */
    void setMaxCacheSize(long maxCacheSize);

    /**
     * 是否是开启 debug 模式
     *
     * @return true：是，false：不是
     */
    boolean isDebugMode();

    /**
     * 是否请求网络，默认是 true
     *
     * @return 是否请求网络
     */
    boolean isNetworkRequestEnable();

    /**
     * 设置是否允许请求网络，默认是 true
     *
     * @param isRequest boolean
     */
    void enableNetworkRequest(boolean isRequest);

    /**
     * 设置 flush 时网络发送策略，默认 3G、4G、5G、WI-FI 环境下都会尝试 flush
     *
     * @param networkType int 网络类型
     */
    void setFlushNetworkPolicy(int networkType);

    /**
     * 两次数据发送的最小时间间隔，单位毫秒
     * 默认值为 15 * 1000 毫秒
     * 在每次调用 track、signUp 以及 profileSet 等接口的时候，都会检查如下条件，以判断是否向服务器上传数据:
     * 1. 是否是 WIFI/3G/4G 网络条件
     * 2. 是否满足发送条件之一:
     * 1) 与上次发送的时间间隔是否大于 flushInterval
     * 2) 本地缓存日志数目是否大于 flushBulkSize
     * 如果满足这两个条件，则向服务器发送一次数据；如果不满足，则把数据加入到队列中，等待下次检查时把整个队列的内
     * 容一并发送。需要注意的是，为了避免占用过多存储，队列最多只缓存 20MB 数据。
     *
     * @return 返回时间间隔，单位毫秒
     */
    int getFlushInterval();

    /**
     * 设置两次数据发送的最小时间间隔
     *
     * @param flushInterval 时间间隔，单位毫秒
     */
    void setFlushInterval(int flushInterval);

    /**
     * 返回本地缓存日志的最大条目数
     * 默认值为 100 条
     * 在每次调用 track、signUp 以及 profileSet 等接口的时候，都会检查如下条件，以判断是否向服务器上传数据:
     * 1. 是否是 WIFI/3G/4G 网络条件
     * 2. 是否满足发送条件之一:
     * 1) 与上次发送的时间间隔是否大于 flushInterval
     * 2) 本地缓存日志数目是否大于 flushBulkSize
     * 如果满足这两个条件，则向服务器发送一次数据；如果不满足，则把数据加入到队列中，等待下次检查时把整个队列的内
     * 容一并发送。需要注意的是，为了避免占用过多存储，队列最多只缓存 32MB 数据。
     *
     * @return 返回本地缓存日志的最大条目数
     */
    int getFlushBulkSize();

    /**
     * 设置本地缓存日志的最大条目数，最小 50 条
     *
     * @param flushBulkSize 缓存数目
     */
    void setFlushBulkSize(int flushBulkSize);

    /**
     * 设置 App 切换到后台与下次事件的事件间隔
     * 默认值为 30*1000 毫秒
     * 若 App 在后台超过设定事件，则认为当前 Session 结束，发送 $AppEnd 事件
     *
     * @return 返回设置的 SessionIntervalTime ，默认是 30s
     */
    int getSessionIntervalTime();

    /**
     * 设置 App 切换到后台与下次事件的事件间隔
     * 默认值为 30*1000 毫秒
     * 若 App 在后台超过设定事件，则认为当前 Session 结束，发送 $AppEnd 事件
     *
     * @param sessionIntervalTime int
     */
    void setSessionIntervalTime(int sessionIntervalTime);

    /**
     * 向 WebView 注入本地方法，默认不开启认证校验。
     *
     * @param webView 当前 WebView
     * @param isSupportJellyBean 是否支持 API level 16 及以下的版本。
     * 因为 API level 16 及以下的版本, addJavascriptInterface 有安全漏洞,请谨慎使用
     */
    void showUpWebView(WebView webView, boolean isSupportJellyBean);

    /**
     * 向 WebView 注入本地方法
     *
     * @param webView 当前 WebView
     * @param isSupportJellyBean 是否支持 API level 16 及以下的版本。
     * @param enableVerify 是否开启认证
     * 因为 API level 16 及以下的版本, addJavascriptInterface 有安全漏洞,请谨慎使用
     */
    void showUpWebView(WebView webView, boolean isSupportJellyBean, boolean enableVerify);

    /**
     * 向 WebView 注入本地方法, 将 distinctId 传递给当前的 WebView，该方法用于老版打通方式，已过时。
     *
     * @param webView 当前 WebView
     * @param properties 属性
     * @param isSupportJellyBean 是否支持 API level 16 及以下的版本。
     * @param enableVerify 是否开启认证
     * 因为 API level 16 及以下的版本, addJavascriptInterface 有安全漏洞,请谨慎使用。
     * 此方法谨慎修改，插件配置 disableJsInterface 会修改此方法。
     */
    @Deprecated
    void showUpWebView(WebView webView, JSONObject properties, boolean isSupportJellyBean, boolean enableVerify);

    /**
     * 向 WebView 注入本地方法, 将 distinctId 传递给当前的 WebView，该方法用于老版打通方式，已过时。
     *
     * @param webView 当前 WebView
     * @param isSupportJellyBean 是否支持 API level 16 及以下的版本。
     * 因为 API level 16 及以下的版本, addJavascriptInterface 有安全漏洞,请谨慎使用
     * @param properties 用户自定义属性
     */
    @Deprecated
    void showUpWebView(WebView webView, boolean isSupportJellyBean, JSONObject properties);

    /**
     * 向腾讯 x5WebView 注入本地方法, 将 distinctId 传递给当前的 WebView，该方法用于老版打通方式，已过时。
     *
     * @param x5WebView 腾讯 x5WebView
     * @param properties 属性
     * @param isSupportJellyBean 是否支持 API level 16 及以下的版本。
     * @param enableVerify 是否开启认证
     * 此方法谨慎修改，插件配置 disableJsInterface 会修改此方法。
     */
    @Deprecated
    void showUpX5WebView(Object x5WebView, JSONObject properties, boolean isSupportJellyBean, boolean enableVerify);

    /**
     * 向腾讯 x5WebView 注入本地方法
     *
     * @param x5WebView 腾讯 x5WebView
     * @param enableVerify 是否开启认证
     * 此方法谨慎修改，插件配置 disableJsInterface 会修改此方法。
     */
    void showUpX5WebView(Object x5WebView, boolean enableVerify);

    /**
     * 向腾讯 x5WebView 注入本地方法, 默认不开启认证校验。
     *
     * @param x5WebView 腾讯 x5WebView
     */
    void showUpX5WebView(Object x5WebView);

    /**
     * activity 是否开启了可视化全埋点
     *
     * @param activity activity 类的对象
     * @return true 代表 activity 开启了可视化全埋点，false 代表 activity 关闭了可视化全埋点
     */
    boolean isVisualizedAutoTrackActivity(Class<?> activity);

    /**
     * 开启某个 activity 的可视化全埋点
     *
     * @param activity activity 类的对象
     */
    void addVisualizedAutoTrackActivity(Class<?> activity);

    /**
     * 开启多个 activity 的可视化全埋点
     *
     * @param activitiesList activity 类的对象集合
     */
    void addVisualizedAutoTrackActivities(List<Class<?>> activitiesList);

    /**
     * 是否开启可视化全埋点
     *
     * @return true 代表开启了可视化全埋点， false 代表关闭了可视化全埋点
     */
    boolean isVisualizedAutoTrackEnabled();

    /**
     * activity 是否开启了点击图
     *
     * @param activity activity 类的对象
     * @return true 代表 activity 开启了点击图， false 代表 activity 关闭了点击图
     */
    boolean isHeatMapActivity(Class<?> activity);

    /**
     * 开启某个 activity 的点击图
     *
     * @param activity activity 类的对象
     */
    void addHeatMapActivity(Class<?> activity);

    /**
     * 开启多个 activity 的点击图
     *
     * @param activitiesList activity 类的对象集合
     */
    void addHeatMapActivities(List<Class<?>> activitiesList);

    /**
     * 是否开启点击图
     *
     * @return true 代表开启了点击图，false 代表关闭了点击图
     */
    boolean isHeatMapEnabled();

    /**
     * 获取当前用户的 distinctId
     *
     * @return 优先返回登录 ID，登录 ID 为空时，返回匿名 ID
     */
    String getDistinctId();

    /**
     * 获取当前用户的匿名 ID
     * 若调用前未调用 {@link #identify(String)} 设置用户的匿名 ID，SDK 会优先调用 SensorsDataUtils#getAndroidID 获取 Android ID，
     * 如获取的 Android ID 非法，则调用 {@link java.util.UUID} 随机生成 UUID，作为用户的匿名 ID
     *
     * @return 当前用户的匿名 ID
     */
    String getAnonymousId();

    /**
     * 重置默认匿名id
     */
    @Deprecated
    void resetAnonymousId();

    /**
     * 获取当前用户的 loginId
     * 若调用前未调用 {@link #login(String)} 设置用户的 loginId，会返回 null
     *
     * @return 当前用户的 loginId
     */
    String getLoginId();

    /**
     * 设置当前用户的 distinctId。一般情况下，如果是一个注册用户，则应该使用注册系统内
     * 的 user_id，如果是个未注册用户，则可以选择一个不会重复的匿名 ID，如设备 ID 等，如果
     * 客户没有调用 identify，则使用SDK自动生成的匿名 ID
     *
     * @param distinctId 当前用户的 distinctId，仅接受数字、下划线和大小写字母
     */
    void identify(String distinctId);

    /**
     * 登录，设置当前用户的 loginId
     *
     * @param loginId 当前用户的 loginId，不能为空，且长度不能大于 255
     */
    void login(String loginId);

    /**
     * 登录，设置当前用户的 loginId
     *
     * @param loginId 当前用户的 loginId，不能为空，且长度不能大于 255
     * @param properties 用户登录属性
     */
    void login(final String loginId, final JSONObject properties);

    /**
     * 注销，清空当前用户的 loginId
     */
    void logout();

    void track(String eventName, JSONObject properties);

    /**
     * 与 {@link #track(String, JSONObject)} 类似，无事件属性
     *
     * @param eventName 事件的名称
     */
    void track(String eventName);

    /**
     * 初始化事件的计时器。
     * 若需要统计某个事件的持续时间，先在事件开始时调用 trackTimer("Event") 记录事件开始时间，该方法并不会真正发
     * 送事件；随后在事件结束时，调用 track("Event", properties)，SDK 会追踪 "Event" 事件，并自动将事件持续时
     * 间记录在事件属性 "event_duration" 中。
     * 多次调用 trackTimer("Event") 时，事件 "Event" 的开始时间以最后一次调用时为准。
     *
     * @param eventName 事件的名称
     * @param timeUnit 计时结果的时间单位
     */
    @Deprecated
    void trackTimer(final String eventName, final TimeUnit timeUnit);

    /**
     * 删除事件的计时器
     *
     * @param eventName 事件名称
     */
    void removeTimer(final String eventName);

    /**
     * 停止事件计时器
     *
     * @param eventName 事件的名称，或者交叉计算场景时 trackTimerStart 的返回值
     * @param properties 事件的属性
     */
    void trackTimerEnd(final String eventName, JSONObject properties);

    /**
     * 停止事件计时器
     *
     * @param eventName 事件的名称，或者交叉计算场景时 trackTimerStart 的返回值
     */
    void trackTimerEnd(final String eventName);

    /**
     * 清除所有事件计时器
     */
    void clearTrackTimer();

    /**
     * 将所有本地缓存的日志发送到 Sensors Analytics.
     */
    void flush();

    /**
     * 将所有本地缓存的日志发送到 Sensors Analytics.
     */
    void flushSync();

    /**
     * 以轮询形式将所有本地缓存的日志发送到 Sensors Analytics.
     */
    void flushScheduled();

    /**
     * 注册事件动态公共属性
     *
     * @param dynamicSuperProperties 事件动态公共属性回调接口
     */
    void registerDynamicSuperProperties(SensorsDataDynamicSuperProperties dynamicSuperProperties);

    /**
     * 设置 track 事件回调
     *
     * @param trackEventCallBack track 事件回调接口
     */
    void setTrackEventCallBack(SensorsDataTrackEventCallBack trackEventCallBack);

    /**
     * 获取事件公共属性
     *
     * @return 当前所有 Super 属性
     */
    JSONObject getSuperProperties();

    /**
     * 注册所有事件都有的公共属性
     *
     * @param superProperties 事件公共属性
     */
    void registerSuperProperties(JSONObject superProperties);

    /**
     * 删除事件公共属性
     *
     * @param superPropertyName 事件属性名称
     */
    void unregisterSuperProperty(String superPropertyName);

    /**
     * 注册自定义插件
     *
     * @param plugin 自定义插件
     */
    public void registerPropertyPlugin(SAPropertyPlugin plugin);

    /**
     * 注销自定义插件
     *
     * @param plugin 自定义插件
     */
    public void unregisterPropertyPlugin(SAPropertyPlugin plugin);

    /**
     * 删除所有事件公共属性
     */
    void clearSuperProperties();

    /**
     * 设置用户的一个或多个 Profile。
     * Profile如果存在，则覆盖；否则，新创建。
     *
     * @param properties 属性列表
     */
    void profileSet(JSONObject properties);

    /**
     * 设置用户的一个 Profile，如果之前存在，则覆盖，否则，新创建
     *
     * @param property 属性名称
     * @param value 属性的值，值的类型只允许为
     * {@link String}, {@link Number}, {@link java.util.Date}, {@link Boolean}, {@link org.json.JSONArray}
     */
    void profileSet(String property, Object value);

    /**
     * 首次设置用户的一个或多个 Profile。
     * 与profileSet接口不同的是，如果之前存在，则忽略，否则，新创建
     *
     * @param properties 属性列表
     */
    void profileSetOnce(JSONObject properties);

    /**
     * 首次设置用户的一个 Profile
     * 与profileSet接口不同的是，如果之前存在，则忽略，否则，新创建
     *
     * @param property 属性名称
     * @param value 属性的值，值的类型只允许为
     * {@link String}, {@link Number}, {@link java.util.Date}, {@link Boolean}, {@link org.json.JSONArray}
     */
    void profileSetOnce(String property, Object value);

    /**
     * 给一个或多个数值类型的 Profile 增加一个数值。只能对数值型属性进行操作，若该属性
     * 未设置，则添加属性并设置默认值为 0
     *
     * @param properties 一个或多个属性集合
     */
    void profileIncrement(Map<String, ? extends Number> properties);

    /**
     * 给一个数值类型的 Profile 增加一个数值。只能对数值型属性进行操作，若该属性
     * 未设置，则添加属性并设置默认值为 0
     *
     * @param property 属性名称
     * @param value 属性的值，值的类型只允许为 {@link Number}
     */
    void profileIncrement(String property, Number value);

    /**
     * 给一个列表类型的 Profile 增加一个元素
     *
     * @param property 属性名称
     * @param value 新增的元素
     */
    void profileAppend(String property, String value);

    /**
     * 给一个列表类型的 Profile 增加一个或多个元素
     *
     * @param property 属性名称
     * @param values 新增的元素集合
     */
    void profileAppend(String property, Set<String> values);

    /**
     * 删除用户的一个 Profile
     *
     * @param property 属性名称
     */
    void profileUnset(String property);

    /**
     * 删除用户所有 Profile
     */
    void profileDelete();

    /**
     * 更新 GPS 位置信息
     *
     * @param latitude 纬度
     * @param longitude 经度
     */
    void setGPSLocation(double latitude, double longitude);

    /**
     * 更新 GPS 位置信息及对应坐标系
     *
     * @param latitude 纬度
     * @param longitude 经度
     * @param coordinate 坐标系，坐标系类型请参照 {@link SensorsDataGPSLocation.CoordinateType}
     */
    void setGPSLocation(double latitude, double longitude, final String coordinate);

    /**
     * 清除 GPS 位置信息
     */
    void clearGPSLocation();

    /**
     * 开启/关闭采集屏幕方向
     *
     * @param enable true：开启 false：关闭
     */
    void enableTrackScreenOrientation(boolean enable);

    /**
     * 恢复采集屏幕方向
     */
    void resumeTrackScreenOrientation();

    /**
     * 暂停采集屏幕方向
     */
    void stopTrackScreenOrientation();

    /**
     * 获取当前屏幕方向
     *
     * @return portrait:竖屏 landscape:横屏
     */
    String getScreenOrientation();

    /**
     * 初始化事件的计时器，计时单位为秒。
     *
     * @param eventName 事件的名称
     * @return 交叉计时的事件名称
     */
    String trackTimerStart(final String eventName);

    /**
     * 暂停事件计时器，计时单位为秒。
     *
     * @param eventName 事件的名称
     */
    void trackTimerPause(final String eventName);

    /**
     * 恢复事件计时器，计时单位为秒。
     *
     * @param eventName 事件的名称
     */
    void trackTimerResume(final String eventName);

    /**
     * 设置 Cookie，flush 的时候会设置 HTTP 的 cookie
     * 内部会 URLEncoder.encode(cookie, "UTF-8")
     *
     * @param cookie String cookie
     * @param encode boolean 是否 encode
     */
    void setCookie(final String cookie, boolean encode);

    /**
     * 获取已设置的 Cookie
     * URLDecoder.decode(Cookie, "UTF-8")
     *
     * @param decode String
     * @return String cookie
     */
    String getCookie(boolean decode);

    /**
     * 删除本地缓存的全部事件
     */
    void deleteAll();

    /**
     * 保存用户推送 ID 到用户表
     *
     * @param pushTypeKey 属性名称（例如 jgId）
     * @param pushId 推送 ID
     * 使用 profilePushId("jgId",JPushInterface.getRegistrationID(this))
     */

    void profilePushId(String pushTypeKey, String pushId);

    /**
     * 删除用户设置的 pushId
     *
     * @param pushTypeKey 属性名称（例如 jgId）
     */
    void profileUnsetPushId(String pushTypeKey);

    /**
     * 设置 item
     *
     * @param itemType item 类型
     * @param itemId item ID
     * @param properties item 相关属性
     */
    void itemSet(String itemType, String itemId, JSONObject properties);

    /**
     * 删除 item
     *
     * @param itemType item 类型
     * @param itemId item ID
     */
    void itemDelete(String itemType, String itemId);

    /**
     * 停止事件采集，注意不要随便调用，调用后会造成数据丢失。
     */
    void stopTrackThread();

    /**
     * 开启事件采集
     */
    void startTrackThread();

    /**
     * DeepLink 是否采集设备信息
     *
     * @param enable 是否开启采集设备信息
     */
    void enableDeepLinkInstallSource(boolean enable);

    /**
     * 记录 $AppDeepLinkLaunch {@link #trackDeepLinkLaunch(String, String)}}事件
     *
     * @param deepLinkUrl 唤起应用的 DeepLink 链接
     */
    void trackDeepLinkLaunch(String deepLinkUrl);

    /**
     * 记录 $AppDeepLinkLaunch 事件
     *
     * @param deepLinkUrl 唤起应用的 DeepLink 链接
     * @param oaid oaid
     */
    void trackDeepLinkLaunch(String deepLinkUrl, String oaid);

    /**
     * 注册限制性属性 key
     *
     * @param limitKeys 限制性属性 key
     */
    void registerLimitKeys(Map<String, String> limitKeys);

    /**
     * 是否开启远程配置
     *
     * @param enable true 开启，false 不开启
     */
    void enableRemoteConfig(boolean enable);
}
