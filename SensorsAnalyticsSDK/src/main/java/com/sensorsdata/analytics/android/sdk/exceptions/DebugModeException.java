/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk.exceptions;

/**
 * Debug模式下的异常，用于指出Debug模式下的各种问题，程序不应该捕获该异常，以免屏蔽错误信息
 */
public class DebugModeException extends RuntimeException {

    public DebugModeException(String error) {
        super(error);
    }

    public DebugModeException(Throwable throwable) {
        super(throwable);
    }

}
