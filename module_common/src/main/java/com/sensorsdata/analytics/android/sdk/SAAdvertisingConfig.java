/*
 * Created by chenru on 2023/8/21 上午11:11.
 * Copyright 2015－2023 Sensors Data Inc.
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

import com.sensorsdata.analytics.android.sdk.encrypt.SecreteKey;

import java.util.List;

public class SAAdvertisingConfig {
    public String serverUrl;
    public List<String> eventNames;
    public SecreteKey secreteKey;

    //serverUrl  上报到 SAT 的地址
    // events  上报到 SAT 的事件列表，需填写事件名
    // secretKey  加密密钥
    public SAAdvertisingConfig(String serverUrl, List<String> eventNames, SecreteKey secretKey) {
        this.secreteKey = secretKey;
        this.serverUrl = serverUrl;
        this.eventNames = eventNames;
    }
}