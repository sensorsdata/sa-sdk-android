package com.sensorsdata.analytics.android.sdk.exceptions;

/**
 * 内存超限
 */
public class QueueLimitExceededException extends Exception {

    public QueueLimitExceededException(String error) {
        super(error);
    }

    public QueueLimitExceededException(Throwable throwable) {
        super(throwable);
    }

}
