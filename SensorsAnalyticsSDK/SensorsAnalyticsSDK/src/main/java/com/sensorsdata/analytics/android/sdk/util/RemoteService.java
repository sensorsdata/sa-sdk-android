package com.sensorsdata.analytics.android.sdk.util;

import android.content.Context;
import org.apache.http.NameValuePair;

import java.io.IOException;
import java.util.List;

public interface RemoteService {

  boolean isOnline(Context context);

  byte[] performRequest(String endpointUrl, List<NameValuePair> params)
      throws ServiceUnavailableException, IOException;

  class ServiceUnavailableException extends Exception {

    private static final long serialVersionUID = -5438240981096654973L;

    public ServiceUnavailableException(String message, String strRetryAfter) {
      super(message);
      int retry;
      try {
        retry = Integer.parseInt(strRetryAfter);
      } catch (NumberFormatException e) {
        retry = 0;
      }
      mRetryAfter = retry;
    }

    public int getRetryAfter() {
      return mRetryAfter;
    }

    private final int mRetryAfter;
  }
}
