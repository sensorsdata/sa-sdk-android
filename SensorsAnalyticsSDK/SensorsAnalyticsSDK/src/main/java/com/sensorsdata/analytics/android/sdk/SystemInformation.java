package com.sensorsdata.analytics.android.sdk;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

/**
 * Abstracts away possibly non-present system information classes,
 * and handles permission-dependent queries for default system information.
 */
/* package */ class SystemInformation {

  public SystemInformation(Context context) {
    mContext = context;

    PackageManager packageManager = mContext.getPackageManager();

    String foundAppVersionName = null;
    Integer foundAppVersionCode = null;
    try {
      PackageInfo packageInfo = packageManager.getPackageInfo(mContext.getPackageName(), 0);
      foundAppVersionName = packageInfo.versionName;
      foundAppVersionCode = packageInfo.versionCode;
    } catch (NameNotFoundException e) {
      Log.w(LOGTAG, "System information constructed with a context that apparently doesn't exist.");
    }

    mAppVersionName = foundAppVersionName;
    mAppVersionCode = foundAppVersionCode;

    mDisplayMetrics = new DisplayMetrics();

    Display display =
        ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    display.getMetrics(mDisplayMetrics);
  }

  public String getAppVersionName() {
    return mAppVersionName;
  }

  public Integer getAppVersionCode() {
    return mAppVersionCode;
  }

  public DisplayMetrics getDisplayMetrics() {
    return mDisplayMetrics;
  }

  public String getPhoneRadioType() {
    String ret = null;

    TelephonyManager telephonyManager =
        (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    if (null != telephonyManager) {
      switch (telephonyManager.getPhoneType()) {
        case 0x00000000: // TelephonyManager.PHONE_TYPE_NONE
          ret = "none";
          break;
        case 0x00000001: // TelephonyManager.PHONE_TYPE_GSM
          ret = "gsm";
          break;
        case 0x00000002: // TelephonyManager.PHONE_TYPE_CDMA
          ret = "cdma";
          break;
        case 0x00000003: // TelephonyManager.PHONE_TYPE_SIP
          ret = "sip";
          break;
        default:
          ret = null;
      }
    }

    return ret;
  }

  // Note this is the *current*, not the canonical network, because it
  // doesn't require special permissions to access. Unreliable for CDMA phones,
  //
  public String getCurrentNetworkOperator() {
    String ret = null;

    TelephonyManager telephonyManager =
        (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    if (null != telephonyManager)
      ret = telephonyManager.getNetworkOperatorName();

    return ret;
  }


  public Boolean isWifiConnected() {
    Boolean ret = null;

    if (PackageManager.PERMISSION_GRANTED == mContext
        .checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
      ConnectivityManager connManager =
          (ConnectivityManager) this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
      ret = wifiInfo.isConnected();
    }

    return ret;
  }

  private final Context mContext;

  // Unchanging facts
  private final DisplayMetrics mDisplayMetrics;
  private final String mAppVersionName;
  private final Integer mAppVersionCode;

  private static final String LOGTAG = "SA.SysInfo";
}
