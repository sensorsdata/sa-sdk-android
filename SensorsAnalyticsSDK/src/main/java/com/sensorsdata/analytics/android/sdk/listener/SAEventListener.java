/*
 * Created by dengshiwei on 2020/03/20.
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

package com.sensorsdata.analytics.android.sdk.listener;

import org.json.JSONObject;

public interface SAEventListener {
    /**
     * 事件回调接口
     * @param jsonObject JSONObject
     */
    void trackEvent(JSONObject jsonObject);

    /**
     * 登录回调接口
     */
    void login();

    /**
     * 登出回调接口
     */
    void logout();

    /**
     * identify 接口回调
     */
    void identify();

    /**
     * resetAnonymousId 接口回调
     */
    void resetAnonymousId();
}
