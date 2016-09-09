package com.sensorsdata.analytics.android.sdk.exceptions;

/**
 * EventName, Properties Key/Value格式错误
 */
public class InvalidDataException extends Exception {

    public InvalidDataException(String error) {
        super(error);
    }

    public InvalidDataException(Throwable throwable) {
        super(throwable);
    }

}
