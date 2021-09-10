/*
 * Created by dengshiwei on 2019/06/03.
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

package com.sensorsdata.analytics.android.sdk.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class NetworkUtils {

    /**
     * HTTP 状态码 307
     */
    private static final int HTTP_307 = 307;

    /**
     * 缓存的网络状态
     */
    private static String networkType;

    private static final String TAG = "SA.NetworkUtils";

    private static SABroadcastReceiver mReceiver;
    private static SANetworkCallbackImpl networkCallback;

    /**
     * 获取网络类型
     *
     * @param context Context
     * @return 网络类型
     */
    public static String networkType(Context context) {
        try {
            //小米特殊机型冷启动时获取不到 Network，为 NULL 字符串时需要重新尝试获取
            if (!TextUtils.isEmpty(networkType) && !"NULL".equals(networkType)) {
                return networkType;
            }

            // 检测权限
            if (!SensorsDataUtils.checkHasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
                networkType = "NULL";
                return networkType;
            }

            ConnectivityManager connectivityManager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                // 网络不可用返回 NULL
                if (!isNetworkAvailable(connectivityManager)) {
                    networkType = "NULL";
                    return networkType;
                }
                // WIFI 网络
                if (isWiFiNetwork(connectivityManager)) {
                    networkType = "WIFI";
                    return networkType;
                }
            }
            //读取移动网络类型
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            networkType = mobileNetworkType(context, telephonyManager, connectivityManager);
            return networkType;
        } catch (Exception e) {
            SALog.printStackTrace(e);
            networkType = "NULL";
            return networkType;
        }
    }

    /**
     * 是否有可用网络
     *
     * @param context Context
     * @return true：网络可用，false：网络不可用
     */
    @SuppressLint("WrongConstant")
    public static boolean isNetworkAvailable(Context context) {
        // 检测权限
        if (!SensorsDataUtils.checkHasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
            return false;
        }
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            return isNetworkAvailable(cm);
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return false;
        }
    }

    /**
     * 判断指定网络类型是否可以上传数据
     *
     * @param networkType 网络类型
     * @param flushNetworkPolicy 上传策略
     * @return true：可以上传，false：不可以上传
     */
    public static boolean isShouldFlush(String networkType, int flushNetworkPolicy) {
        return (toNetworkType(networkType) & flushNetworkPolicy) != 0;
    }

    private static int toNetworkType(String networkType) {
        if ("NULL".equals(networkType)) {
            return SensorsDataAPI.NetworkType.TYPE_ALL;
        } else if ("WIFI".equals(networkType)) {
            return SensorsDataAPI.NetworkType.TYPE_WIFI;
        } else if ("2G".equals(networkType)) {
            return SensorsDataAPI.NetworkType.TYPE_2G;
        } else if ("3G".equals(networkType)) {
            return SensorsDataAPI.NetworkType.TYPE_3G;
        } else if ("4G".equals(networkType)) {
            return SensorsDataAPI.NetworkType.TYPE_4G;
        } else if ("5G".equals(networkType)) {
            return SensorsDataAPI.NetworkType.TYPE_5G;
        }
        return SensorsDataAPI.NetworkType.TYPE_ALL;
    }

    @SuppressLint({"NewApi", "WrongConstant"})
    public static boolean isNetworkValid(NetworkCapabilities capabilities) {
        if (capabilities != null) {
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    || capabilities.hasTransport(7)  //目前已知在车联网行业使用该标记作为网络类型（TBOX 网络类型）
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                    || capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        }
        return false;
    }

    public static boolean needRedirects(int responseCode) {
        return responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HTTP_307;
    }

    public static String getLocation(HttpURLConnection connection, String path) throws MalformedURLException {
        if (connection == null || TextUtils.isEmpty(path)) {
            return null;
        }
        String location = connection.getHeaderField("Location");
        if (TextUtils.isEmpty(location)) {
            location = connection.getHeaderField("location");
        }
        if (TextUtils.isEmpty(location)) {
            return null;
        }
        if (!(location.startsWith("http://") || location
                .startsWith("https://"))) {
            //某些时候会省略host，只返回后面的path，所以需要补全url
            URL originUrl = new URL(path);
            location = originUrl.getProtocol() + "://"
                    + originUrl.getHost() + location;
        }
        return location;
    }

    public static void registerNetworkListener(Context context) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                if (mReceiver == null) {
                    mReceiver = new SABroadcastReceiver();
                }
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                context.registerReceiver(mReceiver, intentFilter);
                SALog.i(TAG, "Register BroadcastReceiver");
            } else {
                if (networkCallback == null) {
                    networkCallback = new SANetworkCallbackImpl();
                }
                NetworkRequest request = new NetworkRequest.Builder().build();
                ConnectivityManager connectivityManager = (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivityManager != null) {
                    connectivityManager.registerNetworkCallback(request, networkCallback);
                    SALog.i(TAG, "Register ConnectivityManager");
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void unregisterNetworkListener(Context context) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                if (mReceiver == null) {
                    return;
                }
                context.unregisterReceiver(mReceiver);
                SALog.i(TAG, "unregisterReceiver BroadcastReceiver");
            } else {
                if (networkCallback == null) {
                    return;
                }
                ConnectivityManager connectivityManager = (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivityManager != null) {
                    connectivityManager.unregisterNetworkCallback(networkCallback);
                    SALog.i(TAG, "unregister ConnectivityManager");
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void cleanNetworkTypeCache() {
        networkType = null;
    }

    /**
     * 判断网络是否可用
     *
     * @param connectivityManager ConnectivityManager
     * @return true：可用；false：不可用
     */
    private static boolean isNetworkAvailable(ConnectivityManager connectivityManager) {
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network != null) {
                    NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                    if (capabilities != null) {
                        return isNetworkValid(capabilities);
                    }
                }
            } else {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                return networkInfo != null && networkInfo.isConnected();
            }
        }
        return false;
    }

    /**
     * 判断当前网络是否是 wifi
     *
     * @param connectivityManager ConnectivityManager
     * @return true：是 wifi；false：不是 wifi
     */
    private static boolean isWiFiNetwork(ConnectivityManager connectivityManager) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                if (capabilities != null) {
                    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                }
            }
        } else {
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return networkInfo != null && networkInfo.isConnectedOrConnecting();
        }
        return false;
    }

    /**
     * 获取当前移动网络类型
     *
     * @param telephonyManager TelephonyManager
     * @param connectivityManager ConnectivityManager
     * @return 移动网络类型
     */
    @SuppressLint("MissingPermission")
    private static String mobileNetworkType(Context context, TelephonyManager telephonyManager, ConnectivityManager connectivityManager) {
        // Mobile network
        int networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        if (telephonyManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    && (SensorsDataUtils.checkHasPermission(context, Manifest.permission.READ_PHONE_STATE) || telephonyManager.hasCarrierPrivileges())) {
                networkType = telephonyManager.getDataNetworkType();
            } else {
                try {
                    networkType = telephonyManager.getNetworkType();
                } catch (Exception ex) {
                    SALog.printStackTrace(ex);
                }
            }
        }

        if (networkType == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // 在 Android 11 平台上，没有 READ_PHONE_STATE 权限时
                return "NULL";
            }

            if (connectivityManager != null) {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null) {
                    networkType = networkInfo.getSubtype();
                }
            }
        }

        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
            case TelephonyManager.NETWORK_TYPE_IWLAN:
            case 19:  //目前已知有车机客户使用该标记作为 4G 网络类型 TelephonyManager.NETWORK_TYPE_LTE_CA:
                return "4G";
            case TelephonyManager.NETWORK_TYPE_NR:
                return "5G";
        }
        return "NULL";
    }

    private static class SABroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                NetworkUtils.cleanNetworkTypeCache();
                SensorsDataAPI.sharedInstance().flushSync();
                SALog.i(TAG, "SABroadcastReceiver is receiving ConnectivityManager.CONNECTIVITY_ACTION broadcast");
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static class SANetworkCallbackImpl extends ConnectivityManager.NetworkCallback {

        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            NetworkUtils.cleanNetworkTypeCache();
            SensorsDataAPI.sharedInstance().flushSync();
            SALog.i(TAG, "onAvailable is calling");
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            NetworkUtils.cleanNetworkTypeCache();
            SALog.i(TAG, "onCapabilitiesChanged is calling");
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            NetworkUtils.cleanNetworkTypeCache();
            SALog.i(TAG, "onLost is calling");
        }
    }
}