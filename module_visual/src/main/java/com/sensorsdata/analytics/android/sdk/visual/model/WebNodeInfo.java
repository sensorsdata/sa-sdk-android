package com.sensorsdata.analytics.android.sdk.visual.model;
/*
 * Created by zhangxiangwei on 2020/03/05.
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

import java.util.List;

public class WebNodeInfo extends NodeInfo {

    private String url;

    private WebNodeInfo(List<? extends CommonNode> webNodes, List<AlertInfo> alertInfos, String title, String url, Status status) {
        this.webNodes = webNodes;
        this.alertInfos = alertInfos;
        this.title = title;
        this.url = url;
        this.status = status;
    }

    public static WebNodeInfo createAlertInfo(List<AlertInfo> list) {
        return new WebNodeBuilder().setAlertInfo(list).setStatus(Status.FAILURE).create();
    }

    public static WebNodeInfo createNodesInfo(List<? extends CommonNode> webNodes) {
        return new WebNodeBuilder().setWebNodes(webNodes).setStatus(Status.SUCCESS).create();
    }

    public static WebNodeInfo createPageInfo(String title, String url) {
        return new WebNodeBuilder().setUrl(url).setTitle(title).create();
    }

    public static class WebNodeBuilder extends NodeInfo.Builder<WebNodeInfo> {
        private String url;

        WebNodeBuilder setUrl(String url) {
            this.url = url;
            return this;
        }

        @Override
        WebNodeInfo create() {
            return new WebNodeInfo(webNodes, alertInfos, title, url, status);
        }
    }

    public String getUrl() {
        return url;
    }


}
