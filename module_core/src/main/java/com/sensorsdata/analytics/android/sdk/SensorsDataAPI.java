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
package com.sensorsdata.analytics.android.sdk;

import static com.sensorsdata.analytics.android.sdk.util.Base64Coder.CHARSET_UTF8;

import android.app.Activity;
import android.content.Context;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;

import com.sensorsdata.analytics.android.sdk.core.SAModuleManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.ModuleConstants;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;
import com.sensorsdata.analytics.android.sdk.deeplink.SensorsDataDeferredDeepLinkCallback;
import com.sensorsdata.analytics.android.sdk.deeplink.SensorsDataDeepLinkCallback;
import com.sensorsdata.analytics.android.sdk.useridentity.LoginIDAndKey;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventTimer;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.internal.rpc.SensorsDataContentObserver;
import com.sensorsdata.analytics.android.sdk.monitor.TrackMonitor;
import com.sensorsdata.analytics.android.sdk.remote.BaseSensorsDataSDKRemoteManager;
import com.sensorsdata.analytics.android.sdk.util.AopUtil;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;
import com.sensorsdata.analytics.android.sdk.visual.property.VisualPropertiesManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Sensors Analytics SDK
 */
public class SensorsDataAPI extends AbstractSensorsDataAPI {
    // 可视化埋点功能最低 API 版本
    public static final int VTRACK_SUPPORTED_MIN_API = 16;
    // SDK 版本，此属性插件会进行访问，谨慎修改
    static final String VERSION = BuildConfig.SDK_VERSION;
    // 此属性插件会进行访问，谨慎删除。当前 SDK 版本所需插件最低版本号，设为空，意为没有任何限制
    static final String MIN_PLUGIN_VERSION = BuildConfig.MIN_PLUGIN_VERSION;
    /**
     * 插件版本号，插件会用到此属性，请谨慎修改
     */
    static String ANDROID_PLUGIN_VERSION = "";

    //private
    SensorsDataAPI() {
        super();
    }

    SensorsDataAPI(Context context, SAConfigOptions configOptions, DebugMode debugMode) {
        super(context, configOptions, debugMode);
    }

    /**
     * 获取 SensorsDataAPI 单例
     *
     * @param context App的Context
     * @return SensorsDataAPI 单例
     */
    public static SensorsDataAPI sharedInstance(Context context) {
        if (isSDKDisabled()) {
            return new SensorsDataAPIEmptyImplementation();
        }

        if (null == context) {
            return new SensorsDataAPIEmptyImplementation();
        }

        synchronized (sInstanceMap) {
            final Context appContext = context.getApplicationContext();
            SensorsDataAPI instance = sInstanceMap.get(appContext);

            if (null == instance) {
                SALog.i(TAG, "The static method sharedInstance(context, serverURL, debugMode) should be called before calling sharedInstance()");
                return new SensorsDataAPIEmptyImplementation();
            }
            return instance;
        }
    }

    /**
     * 初始化神策 SDK
     *
     * @param context App 的 Context
     * @param saConfigOptions SDK 的配置项
     */
    public static void startWithConfigOptions(Context context, SAConfigOptions saConfigOptions) {
        if (context == null || saConfigOptions == null) {
            throw new NullPointerException("Context、SAConfigOptions 不可以为 null");
        }
        SensorsDataAPI sensorsDataAPI = getInstance(context, DebugMode.DEBUG_OFF, saConfigOptions);
        if (!sensorsDataAPI.mSDKConfigInit) {
            sensorsDataAPI.applySAConfigOptions();
        }
    }

    private static SensorsDataAPI getInstance(Context context, DebugMode debugMode,
                                              SAConfigOptions saConfigOptions) {
        if (null == context) {
            return new SensorsDataAPIEmptyImplementation();
        }

        synchronized (sInstanceMap) {
            final Context appContext = context.getApplicationContext();
            SensorsDataAPI instance = sInstanceMap.get(appContext);
            if (null == instance) {
                instance = new SensorsDataAPI(appContext, saConfigOptions, debugMode);
                sInstanceMap.put(appContext, instance);
                if (context instanceof Activity) {
                    instance.delayExecution((Activity) context);
                }
            }
            return instance;
        }
    }

    private static SensorsDataAPI getSDKInstance() {
        synchronized (sInstanceMap) {
            if (sInstanceMap.size() > 0) {
                Iterator<SensorsDataAPI> iterator = sInstanceMap.values().iterator();
                if (iterator.hasNext()) {
                    return iterator.next();
                }
            }
            return new SensorsDataAPIEmptyImplementation();
        }
    }

    public static SensorsDataAPI sharedInstance() {
        if (isSDKDisabled()) {
            return new SensorsDataAPIEmptyImplementation();
        }

        return getSDKInstance();
    }

    /**
     * 关闭 SDK
     */
    public static void disableSDK() {
        SALog.i(TAG, "call static function disableSDK");
        try {
            final SensorsDataAPI sensorsDataAPI = sharedInstance();
            if (sensorsDataAPI instanceof SensorsDataAPIEmptyImplementation ||
                    getConfigOptions() == null ||
                    getConfigOptions().isDisableSDK) {
                return;
            }
            final boolean isFromObserver = !SensorsDataContentObserver.isDisableFromObserver;
            sensorsDataAPI.transformTaskQueue(new Runnable() {
                @Override
                public void run() {
                    if (isFromObserver) {
                        sensorsDataAPI.trackInternal("$AppDataTrackingClose", null);
                    }
                }
            });
            //禁止网络
            if (sensorsDataAPI.isNetworkRequestEnable()) {
                sensorsDataAPI.enableNetworkRequest(false);
                isChangeEnableNetworkFlag = true;
            } else {
                isChangeEnableNetworkFlag = false;
            }
            //关闭网络监听
            sensorsDataAPI.unregisterNetworkListener();
            sensorsDataAPI.clearTrackTimer();
            SAModuleManager.getInstance().setModuleState(false);
            DbAdapter.getInstance().commitAppStartTime(0);
            getConfigOptions().disableSDK(true);
            //关闭日志
            SALog.setDisableSDK(true);
            if (!SensorsDataContentObserver.isDisableFromObserver) {
                sensorsDataAPI.getContext().getContentResolver().notifyChange(DbParams.getInstance().getDisableSDKUri(), null);
            }
            SensorsDataContentObserver.isDisableFromObserver = false;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 开启 SDK
     */
    public static void enableSDK() {
        SALog.i(TAG, "call static function enableSDK");
        try {
            SensorsDataAPI sensorsDataAPI = getSDKInstance();
            if (sensorsDataAPI instanceof SensorsDataAPIEmptyImplementation ||
                    getConfigOptions() == null ||
                    !getConfigOptions().isDisableSDK) {
                return;
            }
            getConfigOptions().disableSDK(false);
            SAModuleManager.getInstance().setModuleState(true);
            try {
                //开启日志
                SALog.setDisableSDK(false);
                sensorsDataAPI.enableLog(SALog.isLogEnabled());
                SALog.i(TAG, "enableSDK, enable log");
                if (sensorsDataAPI.mFirstDay.get() == null) {
                    sensorsDataAPI.mFirstDay.commit(TimeUtils.formatTime(System.currentTimeMillis(), TimeUtils.YYYY_MM_DD));
                }
                sensorsDataAPI.delayInitTask();
                //开启网络请求
                if (isChangeEnableNetworkFlag) {
                    sensorsDataAPI.enableNetworkRequest(true);
                    isChangeEnableNetworkFlag = false;
                }
                //重新请求可视化全埋点
                if (SensorsDataAPI.getConfigOptions().isVisualizedPropertiesEnabled()) {
                    VisualPropertiesManager.getInstance().requestVisualConfig();
                }
                //重新请求采集控制
                sensorsDataAPI.getRemoteManager().pullSDKConfigFromServer();
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }

            if (!SensorsDataContentObserver.isEnableFromObserver) {
                sensorsDataAPI.getContext().getContentResolver().notifyChange(DbParams.getInstance().getEnableSDKUri(), null);
            }
            SensorsDataContentObserver.isEnableFromObserver = false;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 返回预置属性
     *
     * @return JSONObject 预置属性
     */
    @Override
    public JSONObject getPresetProperties() {
        JSONObject properties = new JSONObject();
        try {
            properties = mSAContextManager.getPresetProperties();
            properties.put("$is_first_day", isFirstDay(System.currentTimeMillis()));
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return properties;
    }

    @Override
    public JSONObject getIdentities() {
        return mUserIdentityAPI.getIdentities();
    }

    @Override
    public void enableLog(boolean enable) {
        SALog.setEnableLog(enable);
    }

    @Override
    public long getMaxCacheSize() {
        return mSAConfigOptions.mMaxCacheSize;
    }

    @Override
    public void setMaxCacheSize(long maxCacheSize) {
        mSAConfigOptions.setMaxCacheSize(maxCacheSize);
    }

    @Override
    public void setFlushNetworkPolicy(int networkType) {
        mSAConfigOptions.setNetworkTypePolicy(networkType);
    }

    int getFlushNetworkPolicy() {
        return mSAConfigOptions.mNetworkTypePolicy;
    }

    @Override
    public int getFlushInterval() {
        return mSAConfigOptions.mFlushInterval;
    }

    @Override
    public void setFlushInterval(int flushInterval) {
        mSAConfigOptions.setFlushInterval(flushInterval);
    }

    @Override
    public int getFlushBulkSize() {
        return mSAConfigOptions.mFlushBulkSize;
    }

    @Override
    public void setFlushBulkSize(int flushBulkSize) {
        if (flushBulkSize < 0) {
            SALog.i(TAG, "The value of flushBulkSize is invalid");
        }
        mSAConfigOptions.setFlushBulkSize(flushBulkSize);
    }

    @Override
    public int getSessionIntervalTime() {
        return mSessionTime;
    }

    @Override
    public void setSessionIntervalTime(int sessionIntervalTime) {
        if (DbAdapter.getInstance() == null) {
            SALog.i(TAG, "The static method sharedInstance(context, serverURL, debugMode) should be called before calling sharedInstance()");
            return;
        }

        if (sessionIntervalTime < 10 * 1000 || sessionIntervalTime > 5 * 60 * 1000) {
            SALog.i(TAG, "SessionIntervalTime:" + sessionIntervalTime + " is invalid, session interval time is between 10s and 300s.");
            return;
        }
        if (sessionIntervalTime != mSessionTime) {
            mSessionTime = sessionIntervalTime;
            DbAdapter.getInstance().commitSessionIntervalTime(sessionIntervalTime);
        }
    }

    @Override
    public void setGPSLocation(final double latitude, final double longitude) {
        setGPSLocation(latitude, longitude, null);
    }

    @Override
    public void setGPSLocation(final double latitude, final double longitude, final String coordinate) {
        try {
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (mGPSLocation == null) {
                            mGPSLocation = new SensorsDataGPSLocation();
                        }
                        mGPSLocation.setLatitude((long) (latitude * Math.pow(10, 6)));
                        mGPSLocation.setLongitude((long) (longitude * Math.pow(10, 6)));
                        mGPSLocation.setCoordinate(SADataHelper.assertPropertyValue(coordinate));
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void clearGPSLocation() {
        try {
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    mGPSLocation = null;
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void enableTrackScreenOrientation(boolean enable) {
        try {
            if (enable) {
                if (mOrientationDetector == null) {
                    mOrientationDetector = new SensorsDataScreenOrientationDetector(mContext, SensorManager.SENSOR_DELAY_NORMAL);
                }
                mOrientationDetector.enable();
            } else {
                if (mOrientationDetector != null) {
                    mOrientationDetector.disable();
                    mOrientationDetector = null;
                }
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public void resumeTrackScreenOrientation() {
        try {
            if (mOrientationDetector != null) {
                mOrientationDetector.enable();
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public void stopTrackScreenOrientation() {
        try {
            if (mOrientationDetector != null) {
                mOrientationDetector.disable();
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public String getScreenOrientation() {
        try {
            if (mOrientationDetector != null) {
                return mOrientationDetector.getOrientation();
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
        return null;
    }

    @Override
    public void setCookie(String cookie, boolean encode) {
        try {
            if (encode) {
                this.mCookie = URLEncoder.encode(cookie, CHARSET_UTF8);
            } else {
                this.mCookie = cookie;
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public String getCookie(boolean decode) {
        try {
            if (decode) {
                return URLDecoder.decode(this.mCookie, CHARSET_UTF8);
            } else {
                return this.mCookie;
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
            return null;
        }

    }

    @Override
    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList) {
        try {
            if (eventTypeList == null || eventTypeList.isEmpty()) {
                return;
            }
            this.mAutoTrack = true;
            for (AutoTrackEventType autoTrackEventType : eventTypeList) {
                mSAConfigOptions.setAutoTrackEventType(mSAConfigOptions.mAutoTrackEventType | autoTrackEventType.eventValue);
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public void disableAutoTrack(List<AutoTrackEventType> eventTypeList) {
        try {
            if (eventTypeList == null) {
                return;
            }

            if (mSAConfigOptions.mAutoTrackEventType == 0) {
                return;
            }

            for (AutoTrackEventType autoTrackEventType : eventTypeList) {
                if ((mSAConfigOptions.mAutoTrackEventType | autoTrackEventType.eventValue) == mSAConfigOptions.mAutoTrackEventType) {
                    mSAConfigOptions.setAutoTrackEventType(mSAConfigOptions.mAutoTrackEventType ^ autoTrackEventType.eventValue);
                }
            }

            if (mSAConfigOptions.mAutoTrackEventType == 0) {
                this.mAutoTrack = false;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void disableAutoTrack(AutoTrackEventType autoTrackEventType) {
        try {
            if (autoTrackEventType == null) {
                return;
            }

            if (mSAConfigOptions.mAutoTrackEventType == 0) {
                return;
            }

            int union = mSAConfigOptions.mAutoTrackEventType | autoTrackEventType.eventValue;
            if (union == autoTrackEventType.eventValue) {
                mSAConfigOptions.setAutoTrackEventType(0);
            } else {
                mSAConfigOptions.setAutoTrackEventType(autoTrackEventType.eventValue ^ union);
            }

            if (mSAConfigOptions.mAutoTrackEventType == 0) {
                this.mAutoTrack = false;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public boolean isAutoTrackEnabled() {
        if (isSDKDisabled()) {
            return false;
        }

        if (mRemoteManager != null) {
            Boolean isAutoTrackEnabled = mRemoteManager.isAutoTrackEnabled();
            if (isAutoTrackEnabled != null) {
                return isAutoTrackEnabled;
            }
        }
        return mAutoTrack;
    }

    @Override
    public void trackFragmentAppViewScreen() {
        mFragmentAPI.trackFragmentAppViewScreen();
    }

    @Override
    public boolean isTrackFragmentAppViewScreenEnabled() {
        return mFragmentAPI.isTrackFragmentAppViewScreenEnabled();
    }

    @Override
    public void showUpWebView(WebView webView, boolean isSupportJellyBean) {
        showUpWebView(webView, isSupportJellyBean, null);
    }

    @Override
    public void showUpWebView(WebView webView, boolean isSupportJellyBean, boolean enableVerify) {
        showUpWebView(webView, null, isSupportJellyBean, enableVerify);
    }

    @Override
    @Deprecated
    public void showUpWebView(WebView webView, JSONObject properties, boolean isSupportJellyBean, boolean enableVerify) {
        if (Build.VERSION.SDK_INT < 17 && !isSupportJellyBean) {
            SALog.d(TAG, "For applications targeted to API level JELLY_BEAN or below, this feature NOT SUPPORTED");
            return;
        }

        if (webView != null) {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.addJavascriptInterface(new AppWebViewInterface(mContext, properties, enableVerify, webView), "SensorsData_APP_JS_Bridge");
            SensorsDataAutoTrackHelper.addWebViewVisualInterface(webView);
        }
    }

    @Override
    @Deprecated
    public void showUpWebView(WebView webView, boolean isSupportJellyBean, JSONObject properties) {
        showUpWebView(webView, properties, isSupportJellyBean, false);
    }

    @Override
    @Deprecated
    public void showUpX5WebView(Object x5WebView, JSONObject properties, boolean isSupportJellyBean, boolean enableVerify) {
        try {
            if (Build.VERSION.SDK_INT < 17 && !isSupportJellyBean) {
                SALog.d(TAG, "For applications targeted to API level JELLY_BEAN or below, this feature NOT SUPPORTED");
                return;
            }

            if (x5WebView == null) {
                return;
            }

            Class<?> clazz = x5WebView.getClass();
            Method addJavascriptInterface = clazz.getMethod("addJavascriptInterface", Object.class, String.class);
            if (addJavascriptInterface == null) {
                return;
            }
            addJavascriptInterface.invoke(x5WebView, new AppWebViewInterface(mContext, properties, enableVerify), "SensorsData_APP_JS_Bridge");
            SensorsDataAutoTrackHelper.addWebViewVisualInterface((View) x5WebView);
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public void showUpX5WebView(Object x5WebView, boolean enableVerify) {
        try {
            if (x5WebView == null) {
                return;
            }

            Class<?> clazz = x5WebView.getClass();
            Method addJavascriptInterface = clazz.getMethod("addJavascriptInterface", Object.class, String.class);
            if (addJavascriptInterface == null) {
                return;
            }
            addJavascriptInterface.invoke(x5WebView, new AppWebViewInterface(mContext, null, enableVerify, (View) x5WebView), "SensorsData_APP_JS_Bridge");
            SensorsDataAutoTrackHelper.addWebViewVisualInterface((View) x5WebView);
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public void showUpX5WebView(Object x5WebView) {
        showUpX5WebView(x5WebView, false);
    }

    @Override
    public void ignoreAutoTrackActivities(List<Class<?>> activitiesList) {
        if (activitiesList == null || activitiesList.size() == 0) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        int hashCode;
        for (Class<?> activity : activitiesList) {
            if (activity != null) {
                hashCode = activity.hashCode();
                if (!mAutoTrackIgnoredActivities.contains(hashCode)) {
                    mAutoTrackIgnoredActivities.add(hashCode);
                }
            }
        }
    }

    @Override
    public void resumeAutoTrackActivities(List<Class<?>> activitiesList) {
        if (activitiesList == null || activitiesList.size() == 0) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        try {
            int hashCode;
            for (Class activity : activitiesList) {
                if (activity != null) {
                    hashCode = activity.hashCode();
                    if (mAutoTrackIgnoredActivities.contains(hashCode)) {
                        mAutoTrackIgnoredActivities.remove(Integer.valueOf(hashCode));
                    }
                }
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public void ignoreAutoTrackActivity(Class<?> activity) {
        if (activity == null) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        try {
            int hashCode = activity.hashCode();
            if (!mAutoTrackIgnoredActivities.contains(hashCode)) {
                mAutoTrackIgnoredActivities.add(hashCode);
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public void resumeAutoTrackActivity(Class<?> activity) {
        if (activity == null) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        try {
            int hashCode = activity.hashCode();
            if (mAutoTrackIgnoredActivities.contains(hashCode)) {
                mAutoTrackIgnoredActivities.remove(Integer.valueOf(hashCode));
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public void enableAutoTrackFragment(Class<?> fragment) {
        mFragmentAPI.enableAutoTrackFragment(fragment);
    }

    @Override
    public void enableAutoTrackFragments(List<Class<?>> fragmentsList) {
        mFragmentAPI.enableAutoTrackFragments(fragmentsList);
    }

    @Override
    public boolean isActivityAutoTrackAppViewScreenIgnored(Class<?> activity) {
        if (activity == null) {
            return false;
        }
        if (mAutoTrackIgnoredActivities != null &&
                mAutoTrackIgnoredActivities.contains(activity.hashCode())) {
            return true;
        }

        if (activity.getAnnotation(SensorsDataIgnoreTrackAppViewScreenAndAppClick.class) != null) {
            return true;
        }

        return activity.getAnnotation(SensorsDataIgnoreTrackAppViewScreen.class) != null;
    }

    @Override
    public boolean isFragmentAutoTrackAppViewScreen(Class<?> fragment) {
        return mFragmentAPI.isFragmentAutoTrackAppViewScreen(fragment);
    }

    @Override
    public void ignoreAutoTrackFragments(List<Class<?>> fragmentList) {
        mFragmentAPI.ignoreAutoTrackFragments(fragmentList);
    }

    @Override
    public void ignoreAutoTrackFragment(Class<?> fragment) {
        mFragmentAPI.ignoreAutoTrackFragment(fragment);
    }

    @Override
    public void resumeIgnoredAutoTrackFragments(List<Class<?>> fragmentList) {
        mFragmentAPI.resumeIgnoredAutoTrackFragments(fragmentList);
    }

    @Override
    public void resumeIgnoredAutoTrackFragment(Class<?> fragment) {
        mFragmentAPI.resumeIgnoredAutoTrackFragment(fragment);
    }

    @Override
    public boolean isActivityAutoTrackAppClickIgnored(Class<?> activity) {
        if (activity == null) {
            return false;
        }
        if (mAutoTrackIgnoredActivities != null &&
                mAutoTrackIgnoredActivities.contains(activity.hashCode())) {
            return true;
        }

        if (activity.getAnnotation(SensorsDataIgnoreTrackAppViewScreenAndAppClick.class) != null) {
            return true;
        }

        return activity.getAnnotation(SensorsDataIgnoreTrackAppClick.class) != null;

    }

    @Override
    public boolean isAutoTrackEventTypeIgnored(AutoTrackEventType eventType) {
        if (eventType == null) {
            return false;
        }
        return isAutoTrackEventTypeIgnored(eventType.eventValue);
    }

    @Override
    public boolean isAutoTrackEventTypeIgnored(int autoTrackEventType) {
        if (mRemoteManager != null) {
            Boolean isIgnored = mRemoteManager.isAutoTrackEventTypeIgnored(autoTrackEventType);
            if (isIgnored != null) {
                if (isIgnored) {
                    SALog.i(TAG, "remote config: " + AutoTrackEventType.autoTrackEventName(autoTrackEventType) + " is ignored by remote config");
                }
                return isIgnored;
            }
        }

        return (mSAConfigOptions.mAutoTrackEventType | autoTrackEventType) != mSAConfigOptions.mAutoTrackEventType;
    }

    @Override
    public void setViewID(View view, String viewID) {
        if (view != null && !TextUtils.isEmpty(viewID)) {
            view.setTag(R.id.sensors_analytics_tag_view_id, viewID);
        }
    }

    @Override
    public void setViewID(android.app.Dialog view, String viewID) {
        try {
            if (view != null && !TextUtils.isEmpty(viewID)) {
                if (view.getWindow() != null) {
                    view.getWindow().getDecorView().setTag(R.id.sensors_analytics_tag_view_id, viewID);
                }
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public void setViewID(Object alertDialog, String viewID) {
        try {
            if (alertDialog == null) {
                return;

            }

            Class<?> supportAlertDialogClass = null;
            Class<?> androidXAlertDialogClass = null;
            Class<?> currentAlertDialogClass;
            try {
                supportAlertDialogClass = Class.forName("android.support.v7.app.AlertDialog");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXAlertDialogClass = Class.forName("androidx.appcompat.app.AlertDialog");
            } catch (Exception e) {
                //ignored
            }

            if (supportAlertDialogClass != null) {
                currentAlertDialogClass = supportAlertDialogClass;
            } else {
                currentAlertDialogClass = androidXAlertDialogClass;
            }

            if (currentAlertDialogClass == null) {
                return;
            }

            if (!currentAlertDialogClass.isInstance(alertDialog)) {
                return;
            }

            if (!TextUtils.isEmpty(viewID)) {
                Method getWindowMethod = alertDialog.getClass().getMethod("getWindow");
                if (getWindowMethod == null) {
                    return;
                }

                Window window = (Window) getWindowMethod.invoke(alertDialog);
                if (window != null) {
                    window.getDecorView().setTag(R.id.sensors_analytics_tag_view_id, viewID);
                }
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public void setViewActivity(View view, Activity activity) {
        try {
            if (view == null || activity == null) {
                return;
            }
            view.setTag(R.id.sensors_analytics_tag_view_activity, activity);
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public void setViewFragmentName(View view, String fragmentName) {
        try {
            if (view == null || TextUtils.isEmpty(fragmentName)) {
                return;
            }
            view.setTag(R.id.sensors_analytics_tag_view_fragment_name2, fragmentName);
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public void ignoreView(View view) {
        if (view != null) {
            view.setTag(R.id.sensors_analytics_tag_view_ignored, "1");
        }
    }

    @Override
    public void ignoreView(View view, boolean ignore) {
        if (view != null) {
            view.setTag(R.id.sensors_analytics_tag_view_ignored, ignore ? "1" : "0");
        }
    }

    @Override
    public void setViewProperties(View view, JSONObject properties) {
        if (view == null || properties == null) {
            return;
        }

        view.setTag(R.id.sensors_analytics_tag_view_properties, properties);
    }

    @Override
    public List<Class> getIgnoredViewTypeList() {
        if (mIgnoredViewTypeList == null) {
            mIgnoredViewTypeList = new ArrayList<>();
        }

        return mIgnoredViewTypeList;
    }

    @Override
    public void ignoreViewType(Class viewType) {
        if (viewType == null) {
            return;
        }

        if (mIgnoredViewTypeList == null) {
            mIgnoredViewTypeList = new ArrayList<>();
        }

        if (!mIgnoredViewTypeList.contains(viewType)) {
            mIgnoredViewTypeList.add(viewType);
        }
    }

    @Override
    public boolean isVisualizedAutoTrackActivity(Class<?> activity) {
        try {
            if (activity == null) {
                return false;
            }
            if (mVisualizedAutoTrackActivities.size() == 0) {
                return true;
            }
            if (mVisualizedAutoTrackActivities.contains(activity.hashCode())) {
                return true;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return false;
    }

    @Override
    public void addVisualizedAutoTrackActivity(Class<?> activity) {
        try {
            if (activity == null) {
                return;
            }
            mVisualizedAutoTrackActivities.add(activity.hashCode());
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void addVisualizedAutoTrackActivities(List<Class<?>> activitiesList) {
        try {
            if (activitiesList == null || activitiesList.size() == 0) {
                return;
            }

            for (Class<?> activity : activitiesList) {
                if (activity != null) {
                    int hashCode = activity.hashCode();
                    if (!mVisualizedAutoTrackActivities.contains(hashCode)) {
                        mVisualizedAutoTrackActivities.add(hashCode);
                    }
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public boolean isVisualizedAutoTrackEnabled() {
        return mSAConfigOptions.mVisualizedEnabled || mSAConfigOptions.mVisualizedPropertiesEnabled;
    }

    @Override
    public boolean isHeatMapActivity(Class<?> activity) {
        try {
            if (activity == null) {
                return false;
            }
            if (mHeatMapActivities.size() == 0) {
                return true;
            }
            if (mHeatMapActivities.contains(activity.hashCode())) {
                return true;
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
        return false;
    }

    @Override
    public void addHeatMapActivity(Class<?> activity) {
        try {
            if (activity == null) {
                return;
            }

            mHeatMapActivities.add(activity.hashCode());
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public void addHeatMapActivities(List<Class<?>> activitiesList) {
        try {
            if (activitiesList == null || activitiesList.size() == 0) {
                return;
            }

            for (Class<?> activity : activitiesList) {
                if (activity != null) {
                    int hashCode = activity.hashCode();
                    if (!mHeatMapActivities.contains(hashCode)) {
                        mHeatMapActivities.add(hashCode);
                    }
                }
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public boolean isHeatMapEnabled() {
        return mSAConfigOptions.mHeatMapEnabled;
    }

    @Override
    public String getDistinctId() {
        try {
            return mUserIdentityAPI.getDistinctId();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return "";
    }

    @Override
    public String getAnonymousId() {
        try {
            return mUserIdentityAPI.getAnonymousId();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return "";
    }

    @Override
    public void resetAnonymousId() {
        try {
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    mUserIdentityAPI.resetAnonymousId();
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public String getLoginId() {
        try {
            return mUserIdentityAPI.getLoginId();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return "";
    }

    @Override
    public void identify(final String distinctId) {
        try {
            SADataHelper.assertDistinctId(distinctId);
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        mUserIdentityAPI.identify(distinctId);
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void login(final String loginId) {
        login(loginId, null);
    }

    @Override
    public void login(final String loginId, final JSONObject properties) {
        loginWithKey(LoginIDAndKey.LOGIN_ID_KEY_DEFAULT, loginId, properties);
    }

    @Override
    public void loginWithKey(final String loginIDKey, final String loginId) {
        loginWithKey(loginIDKey, loginId, null);
    }

    @Override
    public void loginWithKey(final String loginIDKey, final String loginId, final JSONObject properties) {
        try {
            //区分是否由 Observer 发送过来
            if (SensorsDataContentObserver.isLoginFromObserver) {
                SensorsDataContentObserver.isLoginFromObserver = false;
                return;
            }
            synchronized (mLoginIdLock) {
                final JSONObject cloneProperties = JSONUtils.cloneJsonObject(properties);
                //临时逻辑，保存同以前一致，避免主线程 getLoginID 不同步
                if (!LoginIDAndKey.isInValidLogin(loginIDKey, loginId, mUserIdentityAPI.getIdentitiesInstance().getLoginIDKey(), mUserIdentityAPI.getIdentitiesInstance().getLoginId(), getAnonymousId())) {
                    mUserIdentityAPI.updateLoginId(loginIDKey, loginId);
                }
                mTrackTaskManager.addTrackEventTask(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (mUserIdentityAPI.loginWithKeyBack(loginIDKey, loginId)) {
                                //1、进行登录事件发送
                                trackEvent(EventType.TRACK_SIGNUP, "$SignUp", cloneProperties, getAnonymousId());
                            }
                        } catch (Exception e) {
                            SALog.printStackTrace(e);
                        }
                    }
                });
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void logout() {
        synchronized (mLoginIdLock) {
            mUserIdentityAPI.updateLoginId(null, null);
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        mUserIdentityAPI.logout();
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            });
        }
    }

    @Override
    public void bind(final String key, final String value) {
        try {
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (mUserIdentityAPI.bindBack(key, value)) {
                            trackEvent(EventType.TRACK_ID_BIND, BIND_ID, null, getAnonymousId());
                        }
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void unbind(final String key, final String value) {
        try {
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (mUserIdentityAPI.unbindBack(key, value)) {
                            trackEvent(EventType.TRACK_ID_UNBIND, UNBIND_ID, null, getAnonymousId());
                        }
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void trackInstallation(String eventName, JSONObject properties, final boolean disableCallback) {
        if (SAModuleManager.getInstance().hasModuleByName(ModuleConstants.ModuleName.ADVERT_NAME)) {
            try {
                SAModuleManager.getInstance().getAdvertModuleService().trackInstallation(eventName, properties, disableCallback);
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    @Override
    public void trackInstallation(String eventName, JSONObject properties) {
        trackInstallation(eventName, properties, false);
    }

    @Override
    public void trackInstallation(String eventName) {
        trackInstallation(eventName, null, false);
    }

    @Override
    public void trackAppInstall(JSONObject properties, final boolean disableCallback) {
        trackInstallation("$AppInstall", properties, disableCallback);
    }

    @Override
    public void trackAppInstall(JSONObject properties) {
        trackAppInstall(properties, false);
    }

    @Override
    public void trackAppInstall() {
        trackAppInstall(null, false);
    }

    @Override
    public void trackChannelEvent(String eventName) {
        trackChannelEvent(eventName, null);
    }

    @Override
    public void trackChannelEvent(final String eventName, final JSONObject properties) {
        if (SAModuleManager.getInstance().hasModuleByName(ModuleConstants.ModuleName.ADVERT_NAME)) {
            try {
                SAModuleManager.getInstance().getAdvertModuleService().trackChannelEvent(eventName, properties);
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    @Override
    public void track(final String eventName, final JSONObject properties) {
        try {
            final JSONObject cloneProperties = JSONUtils.cloneJsonObject(properties);
            final JSONObject dynamicProperty = getDynamicProperty();
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    JSONObject _properties = cloneProperties;
                    if (SAModuleManager.getInstance().hasModuleByName(ModuleConstants.ModuleName.ADVERT_NAME)) {
                        _properties = SAModuleManager.getInstance().getAdvertModuleService().mergeChannelEventProperties(eventName, cloneProperties);
                    }
                    trackEvent(EventType.TRACK, eventName, _properties, dynamicProperty, getDistinctId(), getLoginId(), null);
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void track(final String eventName) {
        track(eventName, null);
    }

    @Deprecated
    @Override
    public void trackTimer(final String eventName, final TimeUnit timeUnit) {
        final long startTime = SystemClock.elapsedRealtime();
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    SADataHelper.assertEventName(eventName);
                    synchronized (mTrackTimer) {
                        mTrackTimer.put(eventName, new EventTimer(timeUnit, startTime));
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void removeTimer(final String eventName) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    SADataHelper.assertEventName(eventName);
                    synchronized (mTrackTimer) {
                        mTrackTimer.remove(eventName);
                    }
                } catch (Exception e) {
                    com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public String trackTimerStart(String eventName) {
        try {
            final String eventNameRegex = String.format("%s_%s_%s", eventName, UUID.randomUUID().toString().replace("-", "_"), "SATimer");
            trackTimer(eventNameRegex, TimeUnit.SECONDS);
            trackTimer(eventName, TimeUnit.SECONDS);
            return eventNameRegex;
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return "";
    }

    @Override
    public void trackTimerPause(String eventName) {
        trackTimerState(eventName, true);
    }

    @Override
    public void trackTimerResume(String eventName) {
        trackTimerState(eventName, false);
    }

    @Override
    public void trackTimerEnd(final String eventName, final JSONObject properties) {
        final long endTime = SystemClock.elapsedRealtime();
        try {
            final JSONObject cloneProperties = JSONUtils.cloneJsonObject(properties);
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    if (eventName != null) {
                        synchronized (mTrackTimer) {
                            EventTimer eventTimer = mTrackTimer.get(eventName);
                            if (eventTimer != null) {
                                eventTimer.setEndTime(endTime);
                            }
                        }
                    }
                    try {
                        JSONObject _properties = cloneProperties;
                        if (SAModuleManager.getInstance().hasModuleByName(ModuleConstants.ModuleName.ADVERT_NAME)) {
                            _properties = SAModuleManager.getInstance().getAdvertModuleService().mergeChannelEventProperties(eventName, cloneProperties);
                        }
                        trackEvent(EventType.TRACK, eventName, _properties, null);
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void trackTimerEnd(final String eventName) {
        trackTimerEnd(eventName, null);
    }

    @Override
    public void clearTrackTimer() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (mTrackTimer) {
                        mTrackTimer.clear();
                    }
                } catch (Exception e) {
                    com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public String getLastScreenUrl() {
        return mLastScreenUrl;
    }

    @Override
    public void clearReferrerWhenAppEnd() {
        mClearReferrerWhenAppEnd = true;
    }

    @Override
    public void clearLastScreenUrl() {
        if (mClearReferrerWhenAppEnd) {
            mLastScreenUrl = null;
        }
    }

    @Override
    public JSONObject getLastScreenTrackProperties() {
        return mLastScreenTrackProperties;
    }

    @Override
    @Deprecated
    public void trackViewScreen(final String url, final JSONObject properties) {
        try {
            final JSONObject cloneProperties = JSONUtils.cloneJsonObject(properties);
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!TextUtils.isEmpty(url) || cloneProperties != null) {
                            String currentUrl = url;
                            JSONObject trackProperties = new JSONObject();
                            mLastScreenTrackProperties = cloneProperties;

                            if (mLastScreenUrl != null) {
                                trackProperties.put("$referrer", mLastScreenUrl);
                            }

                            mReferrerScreenTitle = mCurrentScreenTitle;
                            if (cloneProperties != null) {
                                if (cloneProperties.has("$title")) {
                                    mCurrentScreenTitle = cloneProperties.getString("$title");
                                } else {
                                    mCurrentScreenTitle = null;
                                }
                                if (cloneProperties.has("$url")) {
                                    currentUrl = cloneProperties.optString("$url");
                                }
                            }
                            trackProperties.put("$url", currentUrl);
                            mLastScreenUrl = currentUrl;
                            if (cloneProperties != null) {
                                SensorsDataUtils.mergeJSONObject(cloneProperties, trackProperties);
                            }
                            trackEvent(EventType.TRACK, "$AppViewScreen", trackProperties, null);
                        }
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void trackViewScreen(final Activity activity) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (activity == null) {
                        return;
                    }
                    JSONObject properties = AopUtil.buildTitleAndScreenName(activity);
                    trackViewScreen(SensorsDataUtils.getScreenUrl(activity), properties);
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void trackViewScreen(final Object fragment) {
        if (fragment == null) {
            return;
        }

        Class<?> supportFragmentClass = null;
        Class<?> appFragmentClass = null;
        Class<?> androidXFragmentClass = null;

        try {
            try {
                supportFragmentClass = Class.forName("android.support.v4.app.Fragment");
            } catch (Exception e) {
                //ignored
            }

            try {
                appFragmentClass = Class.forName("android.app.Fragment");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXFragmentClass = Class.forName("androidx.fragment.app.Fragment");
            } catch (Exception e) {
                //ignored
            }
        } catch (Exception e) {
            //ignored
        }

        if (!(supportFragmentClass != null && supportFragmentClass.isInstance(fragment)) &&
                !(appFragmentClass != null && appFragmentClass.isInstance(fragment)) &&
                !(androidXFragmentClass != null && androidXFragmentClass.isInstance(fragment))) {
            return;
        }

        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject properties = new JSONObject();
                    String screenName = fragment.getClass().getCanonicalName();

                    String title = null;

                    if (fragment.getClass().isAnnotationPresent(SensorsDataFragmentTitle.class)) {
                        SensorsDataFragmentTitle sensorsDataFragmentTitle = fragment.getClass().getAnnotation(SensorsDataFragmentTitle.class);
                        if (sensorsDataFragmentTitle != null) {
                            title = sensorsDataFragmentTitle.title();
                        }
                    }

                    if (Build.VERSION.SDK_INT >= 11) {
                        Activity activity = null;
                        try {
                            Method getActivityMethod = fragment.getClass().getMethod("getActivity");
                            if (getActivityMethod != null) {
                                activity = (Activity) getActivityMethod.invoke(fragment);
                            }
                        } catch (Exception e) {
                            //ignored
                        }
                        if (activity != null) {
                            if (TextUtils.isEmpty(title)) {
                                title = SensorsDataUtils.getActivityTitle(activity);
                            }
                            screenName = String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), screenName);
                        }
                    }

                    if (!TextUtils.isEmpty(title)) {
                        properties.put(AopConstants.TITLE, title);
                    }
                    properties.put("$screen_name", screenName);
                    if (fragment instanceof ScreenAutoTracker) {
                        ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) fragment;
                        JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                        if (otherProperties != null) {
                            SensorsDataUtils.mergeJSONObject(otherProperties, properties);
                        }
                    }
                    trackViewScreen(SensorsDataUtils.getScreenUrl(fragment), properties);
                } catch (Exception e) {
                    com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void trackViewAppClick(View view) {
        trackViewAppClick(view, null);
    }

    @Override
    public void trackViewAppClick(final View view, JSONObject properties) {
        if (view == null) {
            return;
        }
        try {
            JSONObject cloneProperties = JSONUtils.cloneJsonObject(properties);
            if (cloneProperties == null) {
                cloneProperties = new JSONObject();
            }
            if (AopUtil.injectClickInfo(view, cloneProperties, true)) {
                Activity activity = AopUtil.getActivityFromContext(view.getContext(), view);
                trackInternal(AopConstants.APP_CLICK_EVENT_NAME, cloneProperties, AopUtil.addViewPathProperties(activity, view, cloneProperties));
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void flush() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mMessages.flush();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }

    /**
     * 将所有本地缓存的日志发送到 Sensors Analytics.
     */
    @Override
    public void flushSync() {
        flush();
    }

    @Override
    public void flushScheduled() {
        try {
            mMessages.flushScheduled();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void registerDynamicSuperProperties(SensorsDataDynamicSuperProperties dynamicSuperProperties) {
        mDynamicSuperPropertiesCallBack = dynamicSuperProperties;
    }

    @Override
    public void setTrackEventCallBack(SensorsDataTrackEventCallBack trackEventCallBack) {
        mTrackEventCallBack = trackEventCallBack;
    }

    @Override
    @Deprecated()
    public void setDeepLinkCallback(SensorsDataDeepLinkCallback deepLinkCallback) {
        if (SAModuleManager.getInstance().hasModuleByName(ModuleConstants.ModuleName.ADVERT_NAME)) {
            try {
                SAModuleManager.getInstance().getAdvertModuleService().setDeepLinkCallback(deepLinkCallback);
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    @Override
    public void setDeepLinkCompletion(SensorsDataDeferredDeepLinkCallback callback) {
        if (null != callback && SAModuleManager.getInstance().hasModuleByName(ModuleConstants.ModuleName.ADVERT_NAME)) {
            try {
                SAModuleManager.getInstance().getAdvertModuleService().setDeepLinkCompletion(callback);
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    @Override
    public void stopTrackThread() {
        if (mTrackTaskManagerThread != null && !mTrackTaskManagerThread.isStopped()) {
            mTrackTaskManagerThread.stop();
            SALog.i(TAG, "Data collection thread has been stopped");
        }
    }

    @Override
    public void startTrackThread() {
        if (mTrackTaskManagerThread == null || mTrackTaskManagerThread.isStopped()) {
            mTrackTaskManagerThread = new TrackTaskManagerThread();
            new Thread(mTrackTaskManagerThread).start();
            SALog.i(TAG, "Data collection thread has been started");
        }
    }

    @Override
    @Deprecated
    public void enableDataCollect() {
        try {
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    if (!mSAConfigOptions.isDataCollectEnable) {
                        mContext.getContentResolver().notifyChange(DbParams.getInstance().getDataCollectUri(), null);
                    }
                    mSAConfigOptions.isDataCollectEnable = true;
                    // 同意合规时重新判断当前进程是否主进程
                    mIsMainProcess = AppInfoUtils.isMainProcess(mContext, null);
                    mUserIdentityAPI.enableDataCollect(mSAContextManager.getAndroidId());
                    mTrackTaskManager.setDataCollectEnable(true);
                    // 同意合规时更新首日首次
                    if (mFirstDay.get() == null) {
                        mFirstDay.commit(TimeUtils.formatTime(System.currentTimeMillis(), TimeUtils.YYYY_MM_DD));
                    }
                    try {
                        TrackMonitor.getInstance().callEnableDataCollect();
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            });
            flush();
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    @Override
    public void deleteAll() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                mMessages.deleteAll();
            }
        });
    }

    @Override
    public JSONObject getSuperProperties() {
        synchronized (mSuperProperties) {
            try {
                return new JSONObject(mSuperProperties.get().toString());
            } catch (JSONException e) {
                SALog.printStackTrace(e);
                return new JSONObject();
            }
        }
    }

    @Override
    public void registerSuperProperties(final JSONObject superProperties) {
        try {
            final JSONObject cloneSuperProperties = JSONUtils.cloneJsonObject(superProperties);
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (cloneSuperProperties == null) {
                            return;
                        }
                        synchronized (mSuperProperties) {
                            JSONObject properties = mSuperProperties.get();
                            mSuperProperties.commit(SensorsDataUtils.mergeSuperJSONObject(cloneSuperProperties, properties));
                        }
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void unregisterSuperProperty(final String superPropertyName) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (mSuperProperties) {
                        JSONObject superProperties = mSuperProperties.get();
                        superProperties.remove(superPropertyName);
                        mSuperProperties.commit(superProperties);
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void clearSuperProperties() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                synchronized (mSuperProperties) {
                    mSuperProperties.commit(new JSONObject());
                }
            }
        });
    }

    @Override
    public void profileSet(final JSONObject properties) {
        try {
            final JSONObject cloneProperties = JSONUtils.cloneJsonObject(properties);
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        trackEvent(EventType.PROFILE_SET, null, cloneProperties, null);
                    } catch (Exception e) {
                        com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
                    }
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void profileSet(final String property, final Object value) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_SET, null, new JSONObject().put(property, value), null);
                } catch (Exception e) {
                    com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void profileSetOnce(final JSONObject properties) {
        try {
            final JSONObject cloneProperties = JSONUtils.cloneJsonObject(properties);
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        trackEvent(EventType.PROFILE_SET_ONCE, null, cloneProperties, null);
                    } catch (Exception e) {
                        com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
                    }
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void profileSetOnce(final String property, final Object value) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_SET_ONCE, null, new JSONObject().put(property, value), null);
                } catch (Exception e) {
                    com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void profileIncrement(final Map<String, ? extends Number> properties) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_INCREMENT, null, new JSONObject(properties), null);
                } catch (Exception e) {
                    com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void profileIncrement(final String property, final Number value) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_INCREMENT, null, new JSONObject().put(property, value), null);
                } catch (Exception e) {
                    com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void profileAppend(final String property, final String value) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONArray append_values = new JSONArray();
                    append_values.put(value);
                    final JSONObject properties = new JSONObject();
                    properties.put(property, append_values);
                    trackEvent(EventType.PROFILE_APPEND, null, properties, null);
                } catch (Exception e) {
                    com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void profileAppend(final String property, final Set<String> values) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONArray append_values = new JSONArray();
                    for (String value : values) {
                        append_values.put(value);
                    }
                    final JSONObject properties = new JSONObject();
                    properties.put(property, append_values);
                    trackEvent(EventType.PROFILE_APPEND, null, properties, null);
                } catch (Exception e) {
                    com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void profileUnset(final String property) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_UNSET, null, new JSONObject().put(property, true), null);
                } catch (Exception e) {
                    com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void profileDelete() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_DELETE, null, null, null);
                } catch (Exception e) {
                    com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public boolean isDebugMode() {
        return mDebugMode.isDebugMode();
    }

    @Override
    public boolean isNetworkRequestEnable() {
        return mEnableNetworkRequest;
    }

    @Override
    public void enableNetworkRequest(boolean isRequest) {
        this.mEnableNetworkRequest = isRequest;
    }

    @Override
    public void setServerUrl(String serverUrl) {
        setServerUrl(serverUrl, false);
    }

    @Override
    public void setServerUrl(final String serverUrl, boolean isRequestRemoteConfig) {
        try {
            //请求远程配置
            if (isRequestRemoteConfig && mRemoteManager != null) {
                try {
                    mRemoteManager.requestRemoteConfig(BaseSensorsDataSDKRemoteManager.RandomTimeType.RandomTimeTypeWrite, false);
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
            //请求可视化全埋点自定义属性配置
            if (!TextUtils.equals(serverUrl, mOriginServerUrl) && SensorsDataAPI.getConfigOptions().isVisualizedPropertiesEnabled()) {
                try {
                    VisualPropertiesManager.getInstance().requestVisualConfig();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }

            mOriginServerUrl = serverUrl;
            if (TextUtils.isEmpty(serverUrl)) {
                mServerUrl = serverUrl;
                SALog.i(TAG, "Server url is null or empty.");
                return;
            }

            final Uri serverURI = Uri.parse(serverUrl);
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    String hostServer = serverURI.getHost();
                    if (!TextUtils.isEmpty(hostServer) && hostServer.contains("_")) {
                        SALog.i(TAG, "Server url " + serverUrl + " contains '_' is not recommend，" +
                                "see details: https://en.wikipedia.org/wiki/Hostname");
                    }
                }
            });

            if (mDebugMode != DebugMode.DEBUG_OFF) {
                String uriPath = serverURI.getPath();
                if (TextUtils.isEmpty(uriPath)) {
                    return;
                }

                int pathPrefix = uriPath.lastIndexOf('/');
                if (pathPrefix != -1) {
                    String newPath = uriPath.substring(0, pathPrefix) + "/debug";
                    // 将 URI Path 中末尾的部分替换成 '/debug'
                    mServerUrl = serverURI.buildUpon().path(newPath).build().toString();
                }
            } else {
                mServerUrl = serverUrl;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void trackEventFromH5(String eventInfo, boolean enableVerify) {
        try {
            if (TextUtils.isEmpty(eventInfo)) {
                return;
            }

            JSONObject eventObject = new JSONObject(eventInfo);
            if (enableVerify) {
                String serverUrl = eventObject.optString("server_url");
                if (!TextUtils.isEmpty(serverUrl)) {
                    if (!(new ServerUrl(serverUrl).check(new ServerUrl(mServerUrl)))) {
                        return;
                    }
                } else {
                    //防止 H5 集成的 JS SDK 版本太老，没有发 server_url
                    return;
                }
            }
            trackEventFromH5(eventInfo);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void trackEventFromH5(final String eventInfo) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                trackEventH5(eventInfo);
            }
        });
    }

    @Override
    public void profilePushId(final String pushTypeKey, final String pushId) {
        transformTaskQueue(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!SADataHelper.assertPropertyKey(pushTypeKey)) {
                        return;
                    }
                    String distinctId = getDistinctId();
                    String distinctPushId = distinctId + pushId;
                    String spDistinctPushId = DbAdapter.getInstance().getPushId("distinctId_" + pushTypeKey);
                    if (!TextUtils.equals(spDistinctPushId, distinctPushId)) {
                        profileSet(pushTypeKey, pushId);
                        DbAdapter.getInstance().commitPushID("distinctId_" + pushTypeKey, distinctPushId);
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void profileUnsetPushId(final String pushTypeKey) {
        transformTaskQueue(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!SADataHelper.assertPropertyKey(pushTypeKey)) {
                        return;
                    }
                    String distinctId = getDistinctId();
                    String key = "distinctId_" + pushTypeKey;
                    String spDistinctPushId = DbAdapter.getInstance().getPushId(key);
                    if (!TextUtils.isEmpty(spDistinctPushId) &&
                            spDistinctPushId.startsWith(distinctId)) {
                        profileUnset(pushTypeKey);
                        DbAdapter.getInstance().removePushId(key);
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void itemSet(final String itemType, final String itemId, final JSONObject properties) {
        try {
            final JSONObject cloneProperties = JSONUtils.cloneJsonObject(properties);
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    trackItemEvent(itemType, itemId, EventType.ITEM_SET.getEventType(), System.currentTimeMillis(), cloneProperties);
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void itemDelete(final String itemType, final String itemId) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                trackItemEvent(itemType, itemId, EventType.ITEM_DELETE.getEventType(), System.currentTimeMillis(), null);
            }
        });
    }

    @Override
    public void enableDeepLinkInstallSource(boolean enable) {
        if (SAModuleManager.getInstance().hasModuleByName(ModuleConstants.ModuleName.ADVERT_NAME)) {
            try {
                SAModuleManager.getInstance().getAdvertModuleService().enableDeepLinkInstallSource(enable);
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    /**
     * 不能动位置，因为 SF 反射获取使用
     *
     * @return ServerUrl
     */
    @Override
    public String getServerUrl() {
        return mServerUrl;
    }

    @Override
    public void trackDeepLinkLaunch(String deepLinkUrl) {
        trackDeepLinkLaunch(deepLinkUrl, null);
    }

    @Override
    public void trackDeepLinkLaunch(final String deepLinkUrl, final String oaid) {
        if (SAModuleManager.getInstance().hasModuleByName(ModuleConstants.ModuleName.ADVERT_NAME)) {
            try {
                SAModuleManager.getInstance().getAdvertModuleService().trackDeepLinkLaunch(deepLinkUrl, oaid);
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    @Override
    public void requestDeferredDeepLink(final JSONObject params) {
        if (SAModuleManager.getInstance().hasModuleByName(ModuleConstants.ModuleName.ADVERT_NAME)) {
            try {
                SAModuleManager.getInstance().getAdvertModuleService().requestDeferredDeepLink(params);
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    /**
     * 获取 SDK 的版本号
     *
     * @return SDK 的版本号
     */
    public String getSDKVersion() {
        return VERSION;
    }


    /**
     * Debug 模式，用于检验数据导入是否正确。该模式下，事件会逐条实时发送到 Sensors Analytics，并根据返回值检查
     * 数据导入是否正确。
     * Debug 模式的具体使用方式，请参考:
     * http://www.sensorsdata.cn/manual/debug_mode.html
     * Debug 模式有三种：
     * DEBUG_OFF - 关闭DEBUG模式
     * DEBUG_ONLY - 打开DEBUG模式，但该模式下发送的数据仅用于调试，不进行数据导入
     * DEBUG_AND_TRACK - 打开DEBUG模式，并将数据导入到SensorsAnalytics中
     */
    public enum DebugMode {
        DEBUG_OFF(false, false),
        DEBUG_ONLY(true, false),
        DEBUG_AND_TRACK(true, true);

        private final boolean debugMode;
        private final boolean debugWriteData;

        DebugMode(boolean debugMode, boolean debugWriteData) {
            this.debugMode = debugMode;
            this.debugWriteData = debugWriteData;
        }

        boolean isDebugMode() {
            return debugMode;
        }

        boolean isDebugWriteData() {
            return debugWriteData;
        }
    }

    /**
     * AutoTrack 默认采集的事件类型
     */
    public enum AutoTrackEventType {
        APP_START(1),
        APP_END(1 << 1),
        APP_CLICK(1 << 2),
        APP_VIEW_SCREEN(1 << 3);
        private final int eventValue;

        AutoTrackEventType(int eventValue) {
            this.eventValue = eventValue;
        }

        static AutoTrackEventType autoTrackEventTypeFromEventName(String eventName) {
            if (TextUtils.isEmpty(eventName)) {
                return null;
            }

            switch (eventName) {
                case "$AppStart":
                    return APP_START;
                case "$AppEnd":
                    return APP_END;
                case "$AppClick":
                    return APP_CLICK;
                case "$AppViewScreen":
                    return APP_VIEW_SCREEN;
                default:
                    break;
            }

            return null;
        }

        static String autoTrackEventName(int eventType) {
            switch (eventType) {
                case 1:
                    return "$AppStart";
                case 2:
                    return "$AppEnd";
                case 4:
                    return "$AppClick";
                case 8:
                    return "$AppViewScreen";
                default:
                    return "";
            }
        }

        static boolean isAutoTrackType(String eventName) {
            if (!TextUtils.isEmpty(eventName)) {
                switch (eventName) {
                    case "$AppStart":
                    case "$AppEnd":
                    case "$AppClick":
                    case "$AppViewScreen":
                        return true;
                    default:
                        break;
                }
            }
            return false;
        }

        int getEventValue() {
            return eventValue;
        }
    }

    /**
     * 网络类型
     */
    public final class NetworkType {
        public static final int TYPE_NONE = 0;//NULL
        public static final int TYPE_2G = 1;//2G
        public static final int TYPE_3G = 1 << 1;//3G
        public static final int TYPE_4G = 1 << 2;//4G
        public static final int TYPE_WIFI = 1 << 3;//WIFI
        public static final int TYPE_5G = 1 << 4;//5G
        public static final int TYPE_ALL = 0xFF;//ALL
    }
}