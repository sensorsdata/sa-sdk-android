package com.sensorsdata.analytics.android.sdk;

/**
 * 在因为网络或者不可预知的问题导致数据无法发送时，SDK会抛出此异常，用户应当捕获并处理
 */
public class SensorsDataException extends Exception {

  public SensorsDataException(String error) {
    super(error);
  }

  public SensorsDataException(Throwable throwable) {
    super(throwable);
  }

}
