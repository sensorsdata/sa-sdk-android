package com.sensorsdata.analytics.android.sdk.visual.model;

import java.util.List;

public class FlutterNodeInfo extends NodeInfo {

    private final String screen_name;
    private final String flutter_lib_version;

    private FlutterNodeInfo(List<? extends CommonNode> webNodes, List<NodeInfo.AlertInfo> alertInfos, String title, String screen_name, String flutter_lib_version, NodeInfo.Status status) {
        this.webNodes = webNodes;
        this.alertInfos = alertInfos;
        this.title = title;
        this.screen_name = screen_name;
        this.flutter_lib_version = flutter_lib_version;
        this.status = status;
    }

    public static FlutterNodeInfo createAlertInfo(List<AlertInfo> list) {
        return new FlutterNodeInfo.FlutterNodeBuilder().setAlertInfo(list).setStatus(Status.FAILURE).create();
    }

    public static FlutterNodeInfo createNodesInfo(List<? extends CommonNode> webNodes) {
        return new FlutterNodeInfo.FlutterNodeBuilder().setWebNodes(webNodes).setStatus(Status.SUCCESS).create();
    }

    public static FlutterNodeInfo createPageInfo(String title, String screen_name, String flutter_lib_version) {
        return new FlutterNodeBuilder().setScreen_name(screen_name).setFlutter_lib_version(flutter_lib_version).setTitle(title).create();
    }

    public static class FlutterNodeBuilder extends NodeInfo.Builder<FlutterNodeInfo> {

        private String screen_name;
        private String flutter_lib_version;

        FlutterNodeInfo.FlutterNodeBuilder setScreen_name(String screen_name) {
            this.screen_name = screen_name;
            return this;
        }

        FlutterNodeInfo.FlutterNodeBuilder setFlutter_lib_version(String flutter_lib_version) {
            this.flutter_lib_version = flutter_lib_version;
            return this;
        }

        @Override
        FlutterNodeInfo create() {
            return new FlutterNodeInfo(webNodes, alertInfos, title, screen_name, flutter_lib_version, status);
        }
    }

    public String getScreen_name() {
        return screen_name;
    }

    public String getFlutter_lib_version() {
        return flutter_lib_version;
    }
}
