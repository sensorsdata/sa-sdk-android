/*
 * Created by chenru on 2022/3/16 下午2:03.
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

import com.sensorsdata.analytics.android.sdk.core.mediator.protocol.SAModuleProtocol;

import org.json.JSONObject;

public interface SAAdvertModuleProtocol extends SAModuleProtocol, SAAdvertAPIProtocol {

    /**
     * 合并渠道事件属性
     * @param eventName 事件名称
     * @param properties 事件自定义属性
     * @return 合并后的自定义属性
     */
    JSONObject mergeChannelEventProperties(String eventName, JSONObject properties);

    /**
     * 获取渠道 latest 属性
     * @return latest 属性
     */
    JSONObject getLatestUtmProperties();

    /**
     * 清除 deeplink utm 属性
     * @param properties 需清除的属性
     */
    void removeDeepLinkInfo(JSONObject properties);

    /**
     * 修改请求 DeferredDeeplink 标记
     * @param isRequest 标记值
     */
    void commitRequestDeferredDeeplink(boolean isRequest);
}
