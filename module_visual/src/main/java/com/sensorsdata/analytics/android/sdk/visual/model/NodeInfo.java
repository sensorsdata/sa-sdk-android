package com.sensorsdata.analytics.android.sdk.visual.model;

import java.util.List;

public class NodeInfo {

    protected List<? extends CommonNode> webNodes;
    protected List<NodeInfo.AlertInfo> alertInfos;
    protected String title;
    protected NodeInfo.Status status;

    public List<? extends CommonNode> getNodes() {
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

    public static class Builder<T extends NodeInfo> {
        protected List<? extends CommonNode> webNodes;
        protected List<NodeInfo.AlertInfo> alertInfos;
        protected String title;
        protected NodeInfo.Status status;

        Builder<T> setWebNodes(List<? extends CommonNode> webNodes) {
            this.webNodes = webNodes;
            return this;
        }

        Builder<T> setAlertInfo(List<WebNodeInfo.AlertInfo> alertInfos) {
            this.alertInfos = alertInfos;
            return this;
        }

        Builder<T> setTitle(String title) {
            this.title = title;
            return this;
        }

        Builder<T> setStatus(WebNodeInfo.Status status) {
            this.status = status;
            return this;
        }

        T create() {
            return null;
        }
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
