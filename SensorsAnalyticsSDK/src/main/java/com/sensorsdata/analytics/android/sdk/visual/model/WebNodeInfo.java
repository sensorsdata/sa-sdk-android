package com.sensorsdata.analytics.android.sdk.visual.model;
/*
 * Created by zhangxiangwei on 2020/03/05.
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

import java.util.List;

public class WebNodeInfo {

    private List<WebNode> webNodes;
    private List<AlertInfo> alertInfos;
    private String title;
    private String url;
    private Status status;

    private WebNodeInfo(List<WebNode> webNodes, List<AlertInfo> alertInfos, String title, String url, Status status) {
        this.webNodes = webNodes;
        this.alertInfos = alertInfos;
        this.title = title;
        this.url = url;
        this.status = status;
    }

    public static WebNodeInfo createWebAlertInfo(List<AlertInfo> list) {
        return new Builder().setAlertInfo(list).setStatus(Status.FAILURE).create();
    }

    public static WebNodeInfo createWebNodesInfo(List<WebNode> webNodes) {
        return new Builder().setWebNodes(webNodes).setStatus(Status.SUCCESS).create();
    }

    public static WebNodeInfo createPageInfo(String title, String url) {
        return new Builder().setTitle(title).setUrl(url).create();
    }

    public static class Builder {
        private List<WebNode> webNodes;
        private List<AlertInfo> alertInfos;
        private String title;
        private String url;
        private Status status;

        Builder setWebNodes(List<WebNode> webNodes) {
            this.webNodes = webNodes;
            return this;
        }

        Builder setAlertInfo(List<AlertInfo> alertInfos) {
            this.alertInfos = alertInfos;
            return this;
        }

        Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        Builder setStatus(Status status) {
            this.status = status;
            return this;
        }

        WebNodeInfo create() {
            return new WebNodeInfo(webNodes, alertInfos, title, url, status);
        }
    }

    public List<WebNode> getWebNodes() {
        return webNodes;
    }

    public Status getStatus() {
        return status;
    }

    public List<AlertInfo> getAlertInfos() {
        return alertInfos;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public static class AlertInfo {
        public String title;
        public String message;
        public String linkText;
        public String linkUrl;

        public AlertInfo(String title, String message, String linkText, String linkUrl) {
            this.title = title;
            this.message = message;
            this.linkText = linkText;
            this.linkUrl = linkUrl;
        }
    }

    public enum Status {
        SUCCESS,
        FAILURE
    }

}
