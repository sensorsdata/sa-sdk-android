package com.sensorsdata.analytics.android.sdk;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;

public class SSConfig {

  public static final String VERSION = "0.0.1";

  /* package */ static final String REFERRER_PREFS_NAME =
      "com.sensorsdata.analytics.android.sdk.ReferralInfo";

  public static boolean DEBUG = false;

  // Instances are safe to store, since they're immutable and always the same.
  public static SSConfig getInstance(Context context) {
    synchronized (sInstanceLock) {
      if (null == sInstance) {
        final Context appContext = context.getApplicationContext();
        sInstance = readConfig(appContext);
      }
    }

    return sInstance;
  }

  /* package */ SSConfig(Bundle metaData, Context context) {
    DEBUG = metaData
        .getBoolean("com.sensorsdata.analytics.android.sdk.config.DebugMode", false);

    mBulkSize = metaData
        .getInt("com.sensorsdata.analytics.android.sdk.config.BulkSize", 100); // 100
        // records default
    mFlushInterval = metaData.getInt("com.sensorsdata.analytics.android.sdk.SSConfig.FlushInterval",
        60 * 1000); // one minute default
    mMinimumDatabaseLimit = metaData
        .getInt("com.sensorsdata.analytics.android.sdk.config.MinimumDatabaseLimit",
            20 * 1024 * 1024); // 20 Mb

    mServerUrl = metaData.getString("com.sensorsdata.analytics.android.sdk.config.ServerUrl");
    if (null == mServerUrl) {
      Log.e(LOGTAG, "com.sensorsdata.analytics.android.sdk.config.ServerUrl MUST BE setting");
    }

    if (DEBUG) {
      Log.v(LOGTAG, "SensorsData configured with:\n" +
              "    BulkSize " + getBulkSize() + "\n" +
              "    FlushInterval " + getFlushInterval() + "\n" +
              "    MinimumDatabaseLimit " + getMinimumDatabaseLimit() + "\n" +
              "    DebugMode " + DEBUG + "\n" +
              "    ServerUrl " + getServerUrl() + "\n");
    }
  }

  // Max size of queue before we require a flush. Must be below the limit the service will accept.
  public int getBulkSize() {
    return mBulkSize;
  }

  // Target max milliseconds between flushes. This is advisory.
  public int getFlushInterval() {
    return mFlushInterval;
  }

  public int getMinimumDatabaseLimit() {
    return mMinimumDatabaseLimit;
  }

  // Preferred URL for tracking events
  public String getServerUrl() {
    return mServerUrl;
  }

  ///////////////////////////////////////////////

  // Package access for testing only- do not call directly in library code
    /* package */
  static SSConfig readConfig(Context appContext) {
    final String packageName = appContext.getPackageName();
    try {
      final ApplicationInfo appInfo = appContext.getPackageManager()
          .getApplicationInfo(packageName, PackageManager.GET_META_DATA);
      Bundle configBundle = appInfo.metaData;
      if (null == configBundle) {
        configBundle = new Bundle();
      }
      return new SSConfig(configBundle, appContext);
    } catch (final NameNotFoundException e) {
      throw new RuntimeException("Can't configure SensorsData with package name " + packageName, e);
    }
  }

  private final int mBulkSize;
  private final int mFlushInterval;
  private final int mMinimumDatabaseLimit;
  private final String mServerUrl;

  private static SSConfig sInstance;
  private static final Object sInstanceLock = new Object();
  private static final String LOGTAG = "SA.Conf";
}
