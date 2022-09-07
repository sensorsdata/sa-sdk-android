/*
 * Created by wangzhuozhou on 2015/08/01.
 * Copyright 2015－2022 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.plugin.encrypt.SAStoreManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SensorsDataUtils {

    private static final String marshmallowMacAddress = "02:00:00:00:00:00";
    private static final String SHARED_PREF_APP_VERSION = "sensorsdata.app.version";
    public static final String COMMAND_HARMONYOS_VERSION = "getprop hw_sc.build.platform.version";

    private static final Set<String> mPermissionGrantedSet = new HashSet<>();
    private static final Map<String, String> deviceUniqueIdentifiersMap = new HashMap<>();

    private static boolean isUniApp = false;
    private static String androidID = "";
    private static String mCurrentCarrier = null;
    private static final Map<String, String> sCarrierMap = new HashMap<>();

    private static final List<String> mInvalidAndroidId = new ArrayList<String>() {
        {
            add("9774d56d682e549c");
            add("0123456789abcdef");
            add("0000000000000000");
        }
    };
    private static final String TAG = "SA.SensorsDataUtils";

    /**
     * 此方法谨慎修改
     * 插件配置 disableCarrier 会修改此方法
     * 获取运营商信息
     *
     * @param context Context
     * @return 运营商信息
     */
    public static String getOperator(Context context) {
        try {
            if (TextUtils.isEmpty(mCurrentCarrier) && SensorsDataUtils.checkHasPermission(context, Manifest.permission.READ_PHONE_STATE)) {
                try {
                    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context
                            .TELEPHONY_SERVICE);
                    if (telephonyManager != null) {
                        SALog.i(TAG, "SensorsData getCarrier");
                        String operator = telephonyManager.getSimOperator();
                        if (!TextUtils.isEmpty(operator)) {
                            mCurrentCarrier = operatorToCarrier(context, operator, telephonyManager);
                            return mCurrentCarrier;
                        }
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        } catch (Error error) {
            //针对酷派 B770 机型抛出的 IncompatibleClassChangeError 错误进行捕获
            SALog.i(TAG, error.toString());
        }
        return mCurrentCarrier;
    }

    /**
     * 获取 Activity 的 title
     *
     * @param activity Activity
     * @return Activity 的 title
     */
    public static String getActivityTitle(Activity activity) {
        try {
            if (activity != null) {
                try {
                    String activityTitle = null;

                    if (Build.VERSION.SDK_INT >= 11) {
                        String toolbarTitle = SensorsDataUtils.getToolbarTitle(activity);
                        if (!TextUtils.isEmpty(toolbarTitle)) {
                            activityTitle = toolbarTitle;
                        }
                    }

                    if (TextUtils.isEmpty(activityTitle)) {
                        activityTitle = activity.getTitle().toString();
                    }

                    if (TextUtils.isEmpty(activityTitle)) {
                        PackageManager packageManager = activity.getPackageManager();
                        if (packageManager != null) {
                            ActivityInfo activityInfo = packageManager.getActivityInfo(activity.getComponentName(), 0);
                            if (!TextUtils.isEmpty(activityInfo.loadLabel(packageManager))) {
                                activityTitle = activityInfo.loadLabel(packageManager).toString();
                            }
                        }
                    }

                    return activityTitle;
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return null;
        }
    }

    /**
     * 根据 operator，获取本地化运营商信息
     *
     * @param context context
     * @param operator sim operator
     * @param telephonyManager TelephonyManager
     * @return local carrier name
     */
    private static String operatorToCarrier(Context context, String operator, TelephonyManager telephonyManager) {
        try {
            if (sCarrierMap.containsKey(operator)) {
                return sCarrierMap.get(operator);
            }

            if (TextUtils.isEmpty(operator)) {
                return getCarrierName(context, telephonyManager);
            }

            // init default carrier
            initDefaultCarrier(context);
            if (sCarrierMap.containsKey(operator)) {
                return sCarrierMap.get(operator);
            }

            String carrier = getCarrierFromJsonObject(context.getString(R.string.sensors_analytics_carrier), operator);
            if (TextUtils.isEmpty(carrier)) {
                carrier = getCarrierFromJsonObject(context.getString(R.string.sensors_analytics_carrier1), operator);
                if (TextUtils.isEmpty(carrier)) {
                    carrier = getCarrierFromJsonObject(context.getString(R.string.sensors_analytics_carrier2), operator);
                }
            }
            if (TextUtils.isEmpty(carrier)) {
                String carrierName = getCarrierName(context, telephonyManager);
                sCarrierMap.put(operator, carrierName);
                return carrierName;
            } else {
                sCarrierMap.put(operator, carrier);
                return carrier;
            }
        } catch (Throwable e) {
            SALog.i(TAG, e.getMessage());
        }
        return getCarrierName(context, telephonyManager);
    }

    private static String getCarrierName(Context context, TelephonyManager telephonyManager) {
        String alternativeName = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            CharSequence tmpCarrierName = telephonyManager.getSimCarrierIdName();
            if (!TextUtils.isEmpty(tmpCarrierName)) {
                alternativeName = tmpCarrierName.toString();
            }
        }
        if (TextUtils.isEmpty(alternativeName)) {
            if (telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY) {
                alternativeName = telephonyManager.getSimOperatorName();
            } else {
                alternativeName = context.getString(R.string.sensors_analytics_carrier_unknown);
            }
        }
        return alternativeName;
    }

    private static void initDefaultCarrier(Context context) {
        if (sCarrierMap.size() == 0) {
            String mobile = context.getString(R.string.sensors_analytics_carrier_mobile);
            sCarrierMap.put("46000", mobile);
            sCarrierMap.put("46002", mobile);
            sCarrierMap.put("46007", mobile);
            sCarrierMap.put("46008", mobile);

            String unicom = context.getString(R.string.sensors_analytics_carrier_unicom);
            sCarrierMap.put("46001", unicom);
            sCarrierMap.put("46006", unicom);
            sCarrierMap.put("46009", unicom);
            sCarrierMap.put("46010", unicom);

            String telecom = context.getString(R.string.sensors_analytics_carrier_telecom);
            sCarrierMap.put("46003", telecom);
            sCarrierMap.put("46005", telecom);
            sCarrierMap.put("46011", telecom);

            String satellite = context.getString(R.string.sensors_analytics_carrier_satellite);
            sCarrierMap.put("46004", satellite);

            String tietong = context.getString(R.string.sensors_analytics_carrier_tietong);
            sCarrierMap.put("46020", tietong);
        }
    }

    private static String getCarrierFromJsonObject(String carrierJson, String mccMnc) {
        if (carrierJson == null || TextUtils.isEmpty(mccMnc)) {
            return "";
        }
        try {
            JSONObject jsonObject = new JSONObject(carrierJson);
            String carrier = jsonObject.optString(mccMnc);
            if (!TextUtils.isEmpty(carrier)) {
                sCarrierMap.put(mccMnc, carrier);
                return carrier;
            }
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
        return "";
    }

    public static String getToolbarTitle(Activity activity) {
        try {
            String canonicalName = SnapCache.getInstance().getCanonicalName(activity.getClass());
            if ("com.tencent.connect.common.AssistActivity".equals(canonicalName)) {
                if (!TextUtils.isEmpty(activity.getTitle())) {
                    return activity.getTitle().toString();
                }
                return null;
            }
            ActionBar actionBar = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                actionBar = activity.getActionBar();
            }
            if (actionBar != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    if (!TextUtils.isEmpty(actionBar.getTitle())) {
                        return actionBar.getTitle().toString();
                    }
                }
            } else {
                try {
                    Class<?> appCompatActivityClass = compatActivity();
                    if (appCompatActivityClass != null && appCompatActivityClass.isInstance(activity)) {
                        Method method = activity.getClass().getMethod("getSupportActionBar");
                        Object supportActionBar = method.invoke(activity);
                        if (supportActionBar != null) {
                            method = supportActionBar.getClass().getMethod("getTitle");
                            CharSequence charSequence = (CharSequence) method.invoke(supportActionBar);
                            if (charSequence != null) {
                                return charSequence.toString();
                            }
                        }
                    }
                } catch (Exception e) {
                    //ignored
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    private static Class<?> compatActivity() {
        Class<?> appCompatActivityClass = null;
        try {
            appCompatActivityClass = ReflectUtil.getClassByName("android.support.v7.app.AppCompatActivity");
        } catch (Exception e) {
            //ignored
        }
        if (appCompatActivityClass == null) {
            try {
                appCompatActivityClass = ReflectUtil.getClassByName("androidx.appcompat.app.AppCompatActivity");
            } catch (Exception e) {
                //ignored
            }
        }
        return appCompatActivityClass;
    }

    /**
     * 检测权限
     *
     * @param context Context
     * @param permission 权限名称
     * @return true:已允许该权限; false:没有允许该权限
     */
    public static boolean checkHasPermission(Context context, String permission) {
        try {
            if (mPermissionGrantedSet.contains(permission)) {
                return true;
            }
            Class<?> contextCompat = null;
            try {
                contextCompat = Class.forName("android.support.v4.content.ContextCompat");
            } catch (Exception e) {
                //ignored
            }

            if (contextCompat == null) {
                try {
                    contextCompat = Class.forName("androidx.core.content.ContextCompat");
                } catch (Exception e) {
                    //ignored
                }
            }

            if (contextCompat == null) {
                mPermissionGrantedSet.add(permission);
                return true;
            }

            Method checkSelfPermissionMethod = contextCompat.getMethod("checkSelfPermission", Context.class, String.class);
            int result = (int) checkSelfPermissionMethod.invoke(null, new Object[]{context, permission});
            if (result != PackageManager.PERMISSION_GRANTED) {
                SALog.i(TAG, "You can fix this by adding the following to your AndroidManifest.xml file:\n"
                        + "<uses-permission android:name=\"" + permission + "\" />");
                return false;
            }
            mPermissionGrantedSet.add(permission);
            return true;
        } catch (Exception e) {
            SALog.i(TAG, e.toString());
            return true;
        }
    }

    /**
     * 此方法谨慎修改
     * 插件配置 disableIMEI 会修改此方法
     * 获取IMEI
     *
     * @param context Context
     * @return IMEI
     */
    @SuppressLint({"MissingPermission", "HardwareIds"})
    public static String getInternationalIdentifier(Context context) {
        String imei = "";
        try {
            if (deviceUniqueIdentifiersMap.containsKey("IMEI")) {
                imei = deviceUniqueIdentifiersMap.get("IMEI");
            }
            if (TextUtils.isEmpty(imei) && hasReadPhoneStatePermission(context)) {
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (tm != null) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                        if (tm.hasCarrierPrivileges()) {
                            imei = tm.getImei();
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        imei = tm.getImei();
                    } else {
                        imei = tm.getDeviceId();
                    }
                    deviceUniqueIdentifiersMap.put("IMEI", imei);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return imei;
    }

    /**
     * 获取设备标识
     *
     * @param context Context
     * @return 设备标识
     */
    public static String getInternationalIdOld(Context context) {
        return getPhoneIdentifier(context, -1);
    }

    /**
     * 获取设备标识
     *
     * @param context Context
     * @param number 卡槽位置
     * @return 设备标识
     */
    public static String getSlot(Context context, int number) {
        return getPhoneIdentifier(context, number);
    }

    /**
     * 获取设备标识
     *
     * @param context Context
     * @return 设备标识
     */
    public static String getEquipmentIdentifier(Context context) {
        return getPhoneIdentifier(context, -2);
    }

    /**
     * 获取设备唯一标识
     *
     * @param context Context
     * @param number 卡槽
     * @return 设备唯一标识
     */
    @SuppressLint({"MissingPermission", "HardwareIds"})
    private static String getPhoneIdentifier(Context context, int number) {
        String deviceId = "";
        try {
            String deviceIDKey = "deviceID" + number;
            if (deviceUniqueIdentifiersMap.containsKey(deviceIDKey)) {
                deviceId = deviceUniqueIdentifiersMap.get(deviceIDKey);
            }
            if (TextUtils.isEmpty(deviceId) && hasReadPhoneStatePermission(context)) {
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (tm != null) {
                    if (number == -1) {
                        deviceId = tm.getDeviceId();
                    } else if (number == -2 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        deviceId = tm.getMeid();
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        deviceId = tm.getDeviceId(number);
                    }
                    deviceUniqueIdentifiersMap.put(deviceIDKey, deviceId);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return deviceId;
    }

    private static boolean hasReadPhoneStatePermission(Context context) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            if (!SensorsDataUtils.checkHasPermission(context, Manifest.permission.READ_PRECISE_PHONE_STATE)) {
                SALog.i(TAG, "Don't have permission android.permission.READ_PRECISE_PHONE_STATE,getDeviceID failed");
                return false;
            }
        } else if (!SensorsDataUtils.checkHasPermission(context, Manifest.permission.READ_PHONE_STATE)) {
            SALog.i(TAG, "Don't have permission android.permission.READ_PHONE_STATE,getDeviceID failed");
            return false;
        }
        return true;
    }

    /**
     * 此方法谨慎修改
     * 插件配置 disableAndroidID 会修改此方法
     * 获取 Android ID
     *
     * @param context Context
     * @return androidID
     */
    @SuppressLint("HardwareIds")
    public static String getIdentifier(Context context) {
        try {
            if (TextUtils.isEmpty(androidID)) {
                androidID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
                if (!isValidAndroidId(androidID)) {
                    androidID = "";
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return androidID;
    }

    private static String getMacAddressByInterface() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if ("wlan0".equalsIgnoreCase(nif.getName())) {
                    byte[] macBytes = nif.getHardwareAddress();
                    if (macBytes == null) {
                        return "";
                    }

                    StringBuilder res1 = new StringBuilder();
                    for (byte b : macBytes) {
                        res1.append(String.format("%02X:", b));
                    }

                    if (res1.length() > 0) {
                        res1.deleteCharAt(res1.length() - 1);
                    }
                    return res1.toString();
                }
            }

        } catch (Exception e) {
            //ignore
        }
        return null;
    }

    /**
     * 此方法谨慎修改
     * 插件配置 disableMacAddress 会修改此方法
     * 获取手机的 Mac 地址
     *
     * @param context Context
     * @return String 当前手机的 Mac 地址
     */
    @SuppressLint("MissingPermission")
    public static String getMediaAddress(Context context) {
        String macAddress = "";
        try {
            if (deviceUniqueIdentifiersMap.containsKey("macAddress")) {
                macAddress = deviceUniqueIdentifiersMap.get("macAddress");
            }
            if (TextUtils.isEmpty(macAddress) && checkHasPermission(context, Manifest.permission.ACCESS_WIFI_STATE)) {
                WifiManager wifiMan = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wifiMan != null) {
                    SALog.i(TAG, "SensorsData getMacAddress");
                    WifiInfo wifiInfo = wifiMan.getConnectionInfo();
                    if (wifiInfo != null) {
                        macAddress = wifiInfo.getMacAddress();
                        if (!TextUtils.isEmpty(macAddress)) {
                            if (marshmallowMacAddress.equals(macAddress)) {
                                macAddress = getMacAddressByInterface();
                                if (macAddress == null) {
                                    macAddress = marshmallowMacAddress;
                                }
                            }
                            deviceUniqueIdentifiersMap.put("macAddress", macAddress);
                        }
                    }
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return macAddress;
    }

    public static boolean isValidAndroidId(String androidId) {
        if (TextUtils.isEmpty(androidId)) {
            return false;
        }

        return !mInvalidAndroidId.contains(androidId.toLowerCase(Locale.getDefault()));
    }

    /**
     * 检查版本是否经过升级
     *
     * @param context context
     * @param currVersion 当前 SDK 版本
     * @return true，老版本升级到新版本。false，当前已是最新版本
     */
    public static boolean checkVersionIsNew(Context context, String currVersion) {
        try {
            String localVersion = SAStoreManager.getInstance().getString(SHARED_PREF_APP_VERSION, "");
            if (!TextUtils.isEmpty(currVersion) && !currVersion.equals(localVersion)) {
                SAStoreManager.getInstance().setString(SHARED_PREF_APP_VERSION, currVersion);
                return true;
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
            return true;
        }
        return false;
    }

    /**
     * 解析 Activity 的 Intent 中是否包含 DebugMode、点击图、可视化全埋点的 uri 信息并显示 Dialog。
     * 此方法用来辅助完善 Dialog 的展示，通常用在配置了神策 scheme 的 Activity 页面中的 onNewIntent 方法中，
     * 并且此 Activity 的 launchMode 为 singleTop 或者 singleTask 或者为 singleInstance。
     *
     * @param activity activity
     * @param intent intent
     */
    public static void handleSchemeUrl(Activity activity, Intent intent) {
        SASchemeHelper.handleSchemeUrl(activity, intent);
    }

    /**
     * only for RN SDK, it will be removed
     */
    @Deprecated
    public static void mergeJSONObject(final JSONObject source, JSONObject dest) {
        try {
            JSONUtils.mergeJSONObject(source, dest);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    public static void initUniAppStatus() {
        try {
            Class.forName("io.dcloud.application.DCloudApplication");
            isUniApp = true;
        } catch (ClassNotFoundException e) {
            // ignore
        }
    }

    public static boolean isUniApp() {
        return isUniApp;
    }
}
