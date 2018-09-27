/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk.exceptions;

/**
 * 网络连接错误
 */
public class ConnectErrorException extends Exception {

    public ConnectErrorException(String message) {
        super(message);
        mRetryAfter = 30 * 1000;
    }

    public ConnectErrorException(String message, String strRetryAfter) {
        super(message);
        try {
            mRetryAfter = Integer.parseInt(strRetryAfter);
        } catch (NumberFormatException e) {
            mRetryAfter = 0;
        }
    }

    public ConnectErrorException(Throwable throwable) {
        super(throwable);
    }

    public int getRetryAfter() {
        return mRetryAfter;
    }

    private int mRetryAfter;

}
