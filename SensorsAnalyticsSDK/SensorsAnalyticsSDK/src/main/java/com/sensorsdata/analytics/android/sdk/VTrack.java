package com.sensorsdata.analytics.android.sdk;


import org.json.JSONArray;

/**
 * SDK内部接口
 **/
public interface VTrack {

    // 获取 VTrack 配置
    void startUpdates();

    // 连接 VTrack 编辑器
    void enableEditingVTrack();

    // 屏蔽部分 Activity 的可是化埋点功能
    void disableActivity(String canonicalName);

    // 设置 VTrack 配置
    void setEventBindings(JSONArray bindings);

    // 设置 VTrack WebServer 地址
    void setVTrackServer(String serverUrl);

}
