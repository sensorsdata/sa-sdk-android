package com.sensorsdata.analytics.android.sdk.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import java.util.Locale;
import java.util.UUID;

public final class SensorsDataUtils {

  private static SharedPreferences getSharedPreferences(Context context) {
    final String sharedPrefsName = SHARED_PREF_EDITS_FILE;
    return context.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
  }

  public static String getDeviceID(Context context) {
    final SharedPreferences preferences = getSharedPreferences(context);
    String storedDeviceID = preferences.getString(SHARED_PREF_DEVICE_ID_KEY, null);

    if (storedDeviceID == null) {
      storedDeviceID = UUID.randomUUID().toString();
      final SharedPreferences.Editor editor = preferences.edit();
      editor.putString(SHARED_PREF_DEVICE_ID_KEY, storedDeviceID);
      editor.apply();
    }

    return storedDeviceID;
  }

  public static boolean isInEmulator() {
    if (!Build.HARDWARE.equals("goldfish")) {
      return false;
    }

    if (!Build.BRAND.startsWith("generic")) {
      return false;
    }

    if (!Build.DEVICE.startsWith("generic")) {
      return false;
    }

    if (!Build.PRODUCT.contains("sdk")) {
      return false;
    }

    if (!Build.MODEL.toLowerCase(Locale.US).contains("sdk")) {
      return false;
    }

    return true;
  }

  private static final String SHARED_PREF_EDITS_FILE = "sensorsdata";
  private static final String SHARED_PREF_DEVICE_ID_KEY = "sensorsdata.device.id";

  private static final String LOGTAG = "SA.SensorsDataUtils";
}
