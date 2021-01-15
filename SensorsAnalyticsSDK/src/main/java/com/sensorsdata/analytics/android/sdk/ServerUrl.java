/*
 * Created by wangzhuozhou on 2018/1/2.
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

package com.sensorsdata.analytics.android.sdk;

import android.net.Uri;
import android.text.TextUtils;

public class ServerUrl {
    private String url;
    private String host;
    private String project;
    private String token;
    private String baseUrl;

    private ServerUrl() {

    }

    public ServerUrl(String url) {
        this.url = url;
        if (!TextUtils.isEmpty(url)) {
            baseUrl = getBaseUrl(url);
            Uri uri = Uri.parse(url);
            try {
                host = uri.getHost();
                token = uri.getQueryParameter("token");
                project = uri.getQueryParameter("project");
            } catch (Exception e) {
                com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
            } finally {
                if (TextUtils.isEmpty(host)) {
                    host = "";
                }
                if (TextUtils.isEmpty(project)) {
                    project = "default";
                }
                if (TextUtils.isEmpty(token)) {
                    token = "";
                }
            }
        }
    }

    public String getUrl() {
        return url;
    }

    public String getHost() {
        return host;
    }

    public String getProject() {
        return project;
    }

    public String getToken() {
        return token;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public String toString() {
        return "url=" + url + "," +
                "baseUrl" + baseUrl + "," +
                "host=" + host + "," +
                "project=" + project + "," +
                "token=" + token;
    }

    public boolean check(ServerUrl serverUrl) {
        try {
            if (serverUrl != null) {
                if (getHost().equals(serverUrl.getHost()) &&
                        getProject().equals(serverUrl.getProject())) {
                    return true;
                }
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
        return false;
    }

    /**
     * 获取 BaseRUl 不包含 queryParams 的网络地址。
     *
     * @param url 数据接收地址
     * @return BaseUrl
     */
    public String getBaseUrl(String url) {
        if (!TextUtils.isEmpty(url)) {
            int pathPrefix = url.lastIndexOf("/");
            if (pathPrefix != -1) {
                return url.substring(0, pathPrefix);
            }
        }
        return "";
    }
}
