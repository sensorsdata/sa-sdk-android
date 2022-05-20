package com.sensorsdata.analytics.android.sdk.network;

import com.sensorsdata.analytics.android.sdk.SALog;

import java.io.Serializable;

public class HttpConfig implements Serializable {
    private final static String TAG = "SA." + HttpConfig.class.getSimpleName();
    /**
     * 网络连接超时单位是毫秒
     */
    private int connectionTimeout = 30 * 1000;
    /**
     * 网络读取超时但是是毫秒
     */
    private int readTimeout = 30 * 1000;

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        if (connectionTimeout < 1000) {
            SALog.i(TAG, "connectionTimeout minimum value is 1000ms");
            this.connectionTimeout = 1000;
            return;
        }
        this.connectionTimeout = connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        if (readTimeout < 1000) {
            SALog.i(TAG, "readTimeout minimum value is 1000ms");
            this.readTimeout = 1000;
            return;
        }
        this.readTimeout = readTimeout;
    }
}
