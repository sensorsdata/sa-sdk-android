/*
 * Created by wangzhuozhou on 2015/08/01.
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

package com.sensorsdata.analytics.android.sdk.exceptions;

/**
 * 网络连接错误
 */
public class ConnectErrorException extends Exception {

    private int mRetryAfter;

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

}
