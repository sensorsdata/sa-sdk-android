/*
 * Created by chenru on 2022/4/24 下午3:54(format year/.
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

package com.sensorsdata.analytics.android.sdk.deeplink;

/**
 * DeepLink Callback
 */
public interface SensorsDataDeferredDeepLinkCallback {
    /**
     * @param saDeepLinkObject 请求数据相关数据实体
     * @return isJumpSuccess 是否成功跳转到指定页面
     */
    boolean onReceive(SADeepLinkObject saDeepLinkObject);
}