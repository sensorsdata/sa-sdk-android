/*
 * Created by dengshiwei on 2019/03/11.
 * Copyright 2015－2019 Sensors Data Inc.
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

public final class SAConfigOptions {
    /**
     * 请求配置地址，默认从 ServerUrl 解析
     */
    private String remoteConfigUrl;

    /**
     * 数据上报服务器地址
     */
    private String serverUrl;

    /**
     * 私有构造函数
     */
    private SAConfigOptions(){}

    /**
     * 获取 SAOptionsConfig 实例
     * @param serverUrl，数据上报服务器地址
     */
    public SAConfigOptions(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    /**
     * 设置远程配置请求地址
     * @param remoteConfigUrl，远程配置请求地址
     * @return SAOptionsConfig
     */
    public SAConfigOptions setRemoteConfigUrl(String remoteConfigUrl) {
        this.remoteConfigUrl = remoteConfigUrl;
        return this;
    }

    /**
     * 设置数据上报地址
     * @param serverUrl，数据上报地址
     * @return SAOptionsConfig
     */
    public SAConfigOptions setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
        return this;
    }

    String getRemoteConfigUrl() {
        return remoteConfigUrl;
    }

    String getServerUrl() {
        return serverUrl;
    }
}
