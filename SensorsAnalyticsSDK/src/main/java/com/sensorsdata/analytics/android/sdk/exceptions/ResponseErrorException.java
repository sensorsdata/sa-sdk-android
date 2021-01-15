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
 * Sensors Analytics 返回数据收集异常
 */
public class ResponseErrorException extends Exception {
    private int httpCode;

    public ResponseErrorException(String error, int httpCode) {
        super(error);
        this.httpCode = httpCode;
    }

    public ResponseErrorException(Throwable throwable, int httpCode) {
        super(throwable);
        this.httpCode = httpCode;
    }

    public int getHttpCode() {
        return this.httpCode;
    }
}
