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
import com.sensorsdata.analytics.android.sdk.core.business.SAPropertyManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.core.mediator.SAModuleManager;
import com.sensorsdata.analytics.android.sdk.internal.beans.LimitKey;
import com.sensorsdata.analytics.android.sdk.jsbridge.AppWebViewInterface;
import com.sensorsdata.analytics.android.sdk.plugin.encrypt.SAStoreManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SensorsDataUtils {

    private static final String SHARED_PREF_APP_VERSION = "sensorsdata.app.version";
    public static final String COMMAND_HARMONYOS_VERSION = "getprop hw_sc.build.platform.version";

    private static boolean isUniApp = false;
    private static String androidID = "";
    private static boolean isAndroidIDEnabled = true;
    private static boolean isOAIDEnabled = true;
    private static final List<String> mInvalidAndroidId = new ArrayList<String>() {
        {
            add("9774d56d682e549c");
            add("0123456789abcdef");
            add("0000000000000000");
        }
    };
    private static final String TAG = "SA.SensorsDataUtils";

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
            if (!isAndroidIDEnabled) {
                SALog.i(TAG, "SensorsData getAndroidID is disabled");
                return "";
            }
            if (SAPropertyManager.getInstance().isLimitKey(LimitKey.ANDROID_ID)) {
                return SAPropertyManager.getInstance().getLimitValue(LimitKey.ANDROID_ID);
            }
            if (TextUtils.isEmpty(androidID)) {
                SALog.i(TAG, "SensorsData getIdentifier");
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

    /*
     * 谨慎使用
     */
    public static void showUpWebView(Context context, Object x5WebView, JSONObject properties, boolean isSupportJellyBean, boolean enableVerify) {
        try {
            SALog.i(TAG, "SensorsDataUtils.showUpWebView called.x5WebView = " + x5WebView + ", isSupportJellyBean = " + isSupportJellyBean + ", enableVerify = " + enableVerify);
            if (Build.VERSION.SDK_INT < 17 && !isSupportJellyBean) {
                SALog.d(TAG, "For applications targeted to API level JELLY_BEAN or below, this feature NOT SUPPORTED");
                return;
            }

            if (x5WebView == null) {
                return;
            }
            try {
                Class<?> clazz = x5WebView.getClass();
                try {
                    Method getSettingsMethod = clazz.getMethod("getSettings");
                    Object settings = getSettingsMethod.invoke(x5WebView);
                    if (settings != null) {
                        Method setJavaScriptEnabledMethod = settings.getClass().getMethod("setJavaScriptEnabled", boolean.class);
                        setJavaScriptEnabledMethod.invoke(settings, true);
                    }
                } catch (Exception e) {
                    //ignore
                }
                Method addJavascriptInterface = clazz.getMethod("addJavascriptInterface", Object.class, String.class);
                addJavascriptInterface.invoke(x5WebView, new AppWebViewInterface(context, properties, enableVerify), "SensorsData_APP_JS_Bridge");
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
            SAModuleManager.getInstance().invokeModuleFunction(Modules.Visual.MODULE_NAME, Modules.Visual.METHOD_ADD_VISUAL_JAVASCRIPTINTERFACE, x5WebView);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 是否开启 AndroidID 采集
     *
     * @param enabled true 开启，false 关闭
     */
    public static void enableAndroidId(boolean enabled) {
        isAndroidIDEnabled = enabled;
    }

    /**
     * 是否开启 OAID 采集
     *
     * @param enabled true 开启，false 关闭
     */
    public static void enableOAID(boolean enabled) {
        isOAIDEnabled = enabled;
    }

    public static boolean isOAIDEnabled() {
        return isOAIDEnabled;
    }
}
