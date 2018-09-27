/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk.exceptions;

/**
 * Sensors Analytics 返回数据收集异常
 */
public class ResponseErrorException extends Exception {

    public ResponseErrorException(String error) {
        super(error);
    }

    public ResponseErrorException(Throwable throwable) {
        super(throwable);
    }

}
