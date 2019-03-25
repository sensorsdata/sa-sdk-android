/**Created by dengshiwei on 2019/03/11.
 * Copyright © 2015－2019 Sensors Data Inc. All rights reserved. */

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
