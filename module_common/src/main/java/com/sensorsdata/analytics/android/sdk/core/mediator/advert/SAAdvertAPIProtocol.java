/*
 * Created by chenru on 2022/3/9 上午10:26.
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

package com.sensorsdata.analytics.android.sdk.core.mediator.advert;

import com.sensorsdata.analytics.android.sdk.deeplink.SensorsDataDeepLinkCallback;
import com.sensorsdata.analytics.android.sdk.deeplink.SensorsDataDeferredDeepLinkCallback;

import org.json.JSONObject;

public interface SAAdvertAPIProtocol {

    /**
     * 记录 $AppInstall 事件，用于在 App 首次启动时追踪渠道来源，并设置追踪渠道事件的属性。
     * 注意：如果之前使用 trackInstallation 触发的激活事件，需要继续保持原来的调用，无需改成 trackAppInstall，否则会导致激活事件数据分离。
     * 这是 Sensors Analytics 进阶功能，请参考文档 https://sensorsdata.cn/manual/track_installation.html
     *
     * @param properties 渠道追踪事件的属性
     * @param disableCallback 是否关闭这次渠道匹配的回调请求
     */
    void trackAppInstall(JSONObject properties, boolean disableCallback);

    /**
     * 记录 $AppInstall 事件，用于在 App 首次启动时追踪渠道来源，并设置追踪渠道事件的属性。
     * 注意：如果之前使用 trackInstallation 触发的激活事件，需要继续保持原来的调用，无需改成 trackAppInstall，否则会导致激活事件数据分离。
     * 这是 Sensors Analytics 进阶功能，请参考文档 https://sensorsdata.cn/manual/track_installation.html
     *
     * @param properties 渠道追踪事件的属性
     */
    void trackAppInstall(JSONObject properties);

    /**
     * 记录 $AppInstall 事件，用于在 App 首次启动时追踪渠道来源，并设置追踪渠道事件的属性。
     * 注意：如果之前使用 trackInstallation 触发的激活事件，需要继续保持原来的调用，无需改成 trackAppInstall，否则会导致激活事件数据分离。
     * 这是 Sensors Analytics 进阶功能，请参考文档 https://sensorsdata.cn/manual/track_installation.html
     */
    void trackAppInstall();

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
     * 用于在 App 首次启动时追踪渠道来源，并设置追踪渠道事件的属性。
     * 这是 Sensors Analytics 进阶功能，请参考文档 https://sensorsdata.cn/manual/track_installation.html
     *
     * @param eventName 渠道追踪事件的名称
     * @param properties 渠道追踪事件的属性
     * @param disableCallback 是否关闭这次渠道匹配的回调请求
     */
    void trackInstallation(String eventName, JSONObject properties, boolean disableCallback);

    /**
     * 用于在 App 首次启动时追踪渠道来源，并设置追踪渠道事件的属性。
     * 这是 Sensors Analytics 进阶功能，请参考文档 https://sensorsdata.cn/manual/track_installation.html
     *
     * @param eventName 渠道追踪事件的名称
     * @param properties 渠道追踪事件的属性
     */
    void trackInstallation(String eventName, JSONObject properties);

    /**
     * 用于在 App 首次启动时追踪渠道来源，并设置追踪渠道事件的属性。
     * 这是 Sensors Analytics 进阶功能，请参考文档 https://sensorsdata.cn/manual/track_installation.html
     *
     * @param eventName 渠道追踪事件的名称
     */
    void trackInstallation(String eventName);

    /**
     * 调用 track 接口，并附加渠道信息.
     *
     * @param eventName 事件的名称
     */
    void trackChannelEvent(String eventName);

    /**
     * 调用 track 接口，并附加渠道信息.
     *
     * @param eventName 事件的名称
     * @param properties 事件的属性
     */
    void trackChannelEvent(String eventName, JSONObject properties);

    /**
     * DeepLink 是否采集设备信息
     *
     * @param enable 是否开启采集设备信息
     */
    void enableDeepLinkInstallSource(boolean enable);

    /**
     * 设置 DeepLink 接口回调
     *
     * @param deepLinkCallback DeepLink 接口回调
     */
    @Deprecated
    void setDeepLinkCallback(SensorsDataDeepLinkCallback deepLinkCallback);

    /**
     * 设置 DeepLink 与 Deferred DeepLink接口回调
     *
     * @param deepLinkCallback DeepLink 与 Deferred DeepLink接口回调
     */
    void setDeepLinkCompletion(SensorsDataDeferredDeepLinkCallback deepLinkCallback);

    /**
     * 触发 DeferredDeeplink 请求
     *
     * @param params DeferredDeepLink 自定义参数
     */
    void requestDeferredDeepLink(JSONObject params);
}
