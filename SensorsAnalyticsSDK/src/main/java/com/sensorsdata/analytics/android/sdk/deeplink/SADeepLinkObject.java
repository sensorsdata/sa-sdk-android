package com.sensorsdata.analytics.android.sdk.deeplink;

import java.io.Serializable;

public class SADeepLinkObject implements Serializable {
    /**
     * 链接设置的 App 内参数
     */
    private String mParams;
    /**
     * 链接设置的 归因数据
     */
    private String mChannels;
    /**
     * 是否请求成功
     */
    private boolean success;
    /**
     * 请求时长
     */
    private long mAppAwakePassedTime;

    public SADeepLinkObject(String params, String channels, boolean success, long appAwakePassedTime) {
        this.mParams = params;
        this.mChannels = channels;
        this.success = success;
        this.mAppAwakePassedTime = appAwakePassedTime;
    }

    public String getParams() {
        return mParams;
    }

    public String getChannels() {
        return mChannels;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getAppAwakePassedTime() {
        return mAppAwakePassedTime;
    }
}
