/*
 * Created by dengshiwei on 2019/06/03.
 * Copyright 2015－2020 Sensors Data Inc.
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
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
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
     * 获取网络类型
     *
     * @param context Context
     * @return 网络类型
     */
    public static String networkType(Context context) {
        try {
            // 检测权限
            if (!SensorsDataUtils.checkHasPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
                return "NULL";
            }

            // Wifi
            ConnectivityManager manager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    Network network = manager.getActiveNetwork();
                    if (network != null) {
                        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
                        if (capabilities != null) {
                            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                                return "WIFI";
                            } else if (!isNetworkValid(capabilities)) {
                                return "NULL";
                            }
                        }
                    } else {
                        return "NULL";
                    }
                } else {
                    NetworkInfo networkInfo = manager.getActiveNetworkInfo();
                    if (networkInfo == null || !networkInfo.isConnected()) {
                        return "NULL";
                    }

                    networkInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
                        return "WIFI";
                    }
                }
            }

            // Mobile network
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context
                    .TELEPHONY_SERVICE);

            if (telephonyManager == null) {
                return "NULL";
            }

            int networkType = telephonyManager.getNetworkType();
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
                default:
                    return "NULL";
            }
        } catch (Exception e) {
            return "NULL";
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
            if (cm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Network network = cm.getActiveNetwork();
                    if (network != null) {
                        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                        if (capabilities != null) {
                            return isNetworkValid(capabilities);
                        }
                    }
                } else {
                    NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                    return networkInfo != null && networkInfo.isConnected();
                }
            }
            return false;
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

}
