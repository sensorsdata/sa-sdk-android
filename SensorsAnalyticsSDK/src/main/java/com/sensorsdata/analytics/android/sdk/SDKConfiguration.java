package com.sensorsdata.analytics.android.sdk;

public class SDKConfiguration {
    /**
     * config 版本号
     */
    private String v;

    /**
     * 是否关闭 debug 模式
     */
    private boolean disableDebugMode;

    /**
     * 是否关闭 SDK
     */
    private boolean disableSDK;

    public SDKConfiguration() {
        this.disableDebugMode = false;
        this.disableSDK = false;
    }

    public String getV() {
        return v;
    }

    public void setV(String v) {
        this.v = v;
    }

    public boolean isDisableDebugMode() {
        return disableDebugMode;
    }

    public void setDisableDebugMode(boolean disableDebugMode) {
        this.disableDebugMode = disableDebugMode;
    }

    public boolean isDisableSDK() {
        return disableSDK;
    }

    public void setDisableSDK(boolean disableSDK) {
        this.disableSDK = disableSDK;
    }

    @Override
    public String toString() {
        return "{ v=" + v + ", disableDebugMode=" + disableDebugMode + ", disableSDK=" + disableSDK + "}";
    }
}
