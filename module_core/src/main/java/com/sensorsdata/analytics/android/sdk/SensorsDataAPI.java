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
import android.webkit.WebView;

import com.sensorsdata.analytics.android.sdk.core.SAModuleManager;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.core.business.timer.EventTimer;
import com.sensorsdata.analytics.android.sdk.core.business.timer.EventTimerManager;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.core.mediator.ModuleConstants;
import com.sensorsdata.analytics.android.sdk.core.mediator.visual.SAVisual;
import com.sensorsdata.analytics.android.sdk.core.rpc.SensorsDataContentObserver;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentSuperProperties;
import com.sensorsdata.analytics.android.sdk.deeplink.SensorsDataDeepLinkCallback;
import com.sensorsdata.analytics.android.sdk.deeplink.SensorsDataDeferredDeepLinkCallback;
import com.sensorsdata.analytics.android.sdk.plugin.property.SAPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.plugin.property.impl.SAPresetPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.remote.BaseSensorsDataSDKRemoteManager;
import com.sensorsdata.analytics.android.sdk.useridentity.LoginIDAndKey;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
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
    public static String ANDROID_PLUGIN_VERSION = "";

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
            throw new NullPointerException("Context、SAConfigOptions can not be null");
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
                instance = new SensorsDataAPI(context, saConfigOptions, debugMode);
                sInstanceMap.put(appContext, instance);
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
            if (isFromObserver) {
                sensorsDataAPI.track("$AppDataTrackingClose");
                sensorsDataAPI.flush();
            }
            //关闭网络监听
            sensorsDataAPI.unregisterNetworkListener(sensorsDataAPI.getSAContextManager().getContext());
            sensorsDataAPI.clearTrackTimer();
            SAModuleManager.getInstance().setModuleState(false);
            DbAdapter.getInstance().commitAppStartTime(0);
            getConfigOptions().disableSDK(true);
            //关闭日志
            SALog.setDisableSDK(true);
            if (!SensorsDataContentObserver.isDisableFromObserver) {
                sensorsDataAPI.getSAContextManager().getContext().getContentResolver().notifyChange(DbParams.getInstance().getDisableSDKUri(), null);
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
                if (PersistentLoader.getInstance().getFirstDayPst().get() == null) {
                    PersistentLoader.getInstance().getFirstDayPst().commit(TimeUtils.formatTime(System.currentTimeMillis(), TimeUtils.YYYY_MM_DD));
                }
                sensorsDataAPI.delayInitTask(sensorsDataAPI.getSAContextManager().getContext());
                //重新请求采集控制
                sensorsDataAPI.mSAContextManager.getRemoteManager().pullSDKConfigFromServer();
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }

            if (!SensorsDataContentObserver.isEnableFromObserver) {
                sensorsDataAPI.getSAContextManager().getContext().getContentResolver().notifyChange(DbParams.getInstance().getEnableSDKUri(), null);
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
            SAPropertyPlugin presetPlugin = mSAContextManager.getPluginManager().getPropertyPlugin(SAPresetPropertyPlugin.class.getName());
            if (presetPlugin instanceof SAPresetPropertyPlugin) {
                properties = ((SAPresetPropertyPlugin) presetPlugin).getPresetProperties();
            }
            properties.put("$is_first_day", getSAContextManager().isFirstDay(System.currentTimeMillis()));
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return properties;
    }

    @Override
    public JSONObject getIdentities() {
        try {
            JSONObject identities = mSAContextManager.getUserIdentityAPI().getIdentities();
            if (identities != null) {
                return new JSONObject(identities.toString());
            }
            return null;
        } catch (JSONException e) {
            SALog.printStackTrace(e);
            return null;
        }
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
        return mInternalConfigs.sessionTime;
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
        if (sessionIntervalTime != mInternalConfigs.sessionTime) {
            mInternalConfigs.sessionTime = sessionIntervalTime;
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
                        if (mInternalConfigs.gpsLocation == null) {
                            mInternalConfigs.gpsLocation = new SensorsDataGPSLocation();
                        }
                        mInternalConfigs.gpsLocation.setLatitude((long) (latitude * Math.pow(10, 6)));
                        mInternalConfigs.gpsLocation.setLongitude((long) (longitude * Math.pow(10, 6)));
                        mInternalConfigs.gpsLocation.setCoordinate(SADataHelper.assertPropertyValue(coordinate));
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
                    mInternalConfigs.gpsLocation = null;
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
                    mOrientationDetector = new SensorsDataScreenOrientationDetector(mInternalConfigs.context, SensorManager.SENSOR_DELAY_NORMAL);
                }
                mOrientationDetector.enable();
            } else {
                if (mOrientationDetector != null) {
                    mOrientationDetector.disable();
                    mOrientationDetector = null;
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
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
                mInternalConfigs.cookie = URLEncoder.encode(cookie, CHARSET_UTF8);
            } else {
                mInternalConfigs.cookie = cookie;
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public String getCookie(boolean decode) {
        try {
            if (decode) {
                return URLDecoder.decode(mInternalConfigs.cookie, CHARSET_UTF8);
            } else {
                return mInternalConfigs.cookie;
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
            return null;
        }

    }

    @Override
    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("enableAutoTrack", eventTypeList);
    }

    @Override
    public void disableAutoTrack(List<AutoTrackEventType> eventTypeList) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("disableAutoTrack", eventTypeList);
    }

    @Override
    public void disableAutoTrack(AutoTrackEventType autoTrackEventType) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("disableAutoTrack", autoTrackEventType);
    }

    @Override
    public boolean isAutoTrackEnabled() {
        try {
            if (isSDKDisabled()) {
                return false;
            }
            Boolean isAutoTrack = SAModuleManager.getInstance().invokeAutoTrackFunction("isAutoTrackEnabled");
            return isAutoTrack != null && isAutoTrack;
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return false;
        }
    }

    @Override
    public void trackFragmentAppViewScreen() {
        SAModuleManager.getInstance().invokeAutoTrackFunction("trackFragmentAppViewScreen");
    }

    @Override
    public boolean isTrackFragmentAppViewScreenEnabled() {
        Boolean isAutoTrackFragment = SAModuleManager.getInstance().invokeAutoTrackFunction("isTrackFragmentAppViewScreenEnabled");
        return isAutoTrackFragment != null && isAutoTrackFragment;
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
            SALog.i(TAG, "For applications targeted to API level JELLY_BEAN or below, this feature NOT SUPPORTED");
            return;
        }

        if (webView != null) {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.addJavascriptInterface(new AppWebViewInterface(mSAContextManager.getContext(), properties, enableVerify, webView), "SensorsData_APP_JS_Bridge");
            SAVisual.addVisualJavascriptInterface(webView);
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
            addJavascriptInterface.invoke(x5WebView, new AppWebViewInterface(mSAContextManager.getContext(), properties, enableVerify), "SensorsData_APP_JS_Bridge");
            SAVisual.addVisualJavascriptInterface((View) x5WebView);
        } catch (Exception e) {
            SALog.printStackTrace(e);
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
            addJavascriptInterface.invoke(x5WebView, new AppWebViewInterface(mInternalConfigs.context, null, enableVerify, (View) x5WebView), "SensorsData_APP_JS_Bridge");
            SAVisual.addVisualJavascriptInterface((View) x5WebView);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void showUpX5WebView(Object x5WebView) {
        showUpX5WebView(x5WebView, false);
    }

    @Override
    public void ignoreAutoTrackActivities(List<Class<?>> activitiesList) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("ignoreAutoTrackActivities", activitiesList);
    }

    @Override
    public void resumeAutoTrackActivities(List<Class<?>> activitiesList) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("resumeAutoTrackActivities", activitiesList);
    }

    @Override
    public void ignoreAutoTrackActivity(Class<?> activity) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("ignoreAutoTrackActivity", activity);
    }

    @Override
    public void resumeAutoTrackActivity(Class<?> activity) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("resumeAutoTrackActivity", activity);
    }

    @Override
    public void enableAutoTrackFragment(Class<?> fragment) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("enableAutoTrackFragment", fragment);
    }

    @Override
    public void enableAutoTrackFragments(List<Class<?>> fragmentsList) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("enableAutoTrackFragments", fragmentsList);
    }

    @Override
    public boolean isActivityAutoTrackAppViewScreenIgnored(Class<?> activity) {
        Boolean isAutoTrackActivity = SAModuleManager.getInstance().invokeAutoTrackFunction("isActivityAutoTrackAppViewScreenIgnored", activity);
        return isAutoTrackActivity != null && isAutoTrackActivity;
    }

    @Override
    public boolean isFragmentAutoTrackAppViewScreen(Class<?> fragment) {
        Boolean isAutoTrackFragment = SAModuleManager.getInstance().invokeAutoTrackFunction("isFragmentAutoTrackAppViewScreen", fragment);
        return isAutoTrackFragment != null && isAutoTrackFragment;
    }

    @Override
    public void ignoreAutoTrackFragments(List<Class<?>> fragmentList) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("ignoreAutoTrackFragments", fragmentList);
    }

    @Override
    public void ignoreAutoTrackFragment(Class<?> fragment) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("ignoreAutoTrackFragment", fragment);
    }

    @Override
    public void resumeIgnoredAutoTrackFragments(List<Class<?>> fragmentList) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("resumeIgnoredAutoTrackFragments", fragmentList);
    }

    @Override
    public void resumeIgnoredAutoTrackFragment(Class<?> fragment) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("resumeIgnoredAutoTrackFragment", fragment);
    }

    @Override
    public boolean isActivityAutoTrackAppClickIgnored(Class<?> activity) {
        Boolean isAutoTrackActivity = SAModuleManager.getInstance().invokeAutoTrackFunction("isActivityAutoTrackAppClickIgnored", activity);
        return isAutoTrackActivity != null && isAutoTrackActivity;
    }

    @Override
    public boolean isAutoTrackEventTypeIgnored(AutoTrackEventType eventType) {
        Boolean isAutoTrackEventType = SAModuleManager.getInstance().invokeAutoTrackFunction("isAutoTrackEventTypeIgnored", eventType);
        return isAutoTrackEventType != null && isAutoTrackEventType;
    }

    @Override
    public boolean isAutoTrackEventTypeIgnored(int autoTrackEventType) {
        Boolean isAutoTrackEventType = SAModuleManager.getInstance().invokeAutoTrackFunction("isAutoTrackEventTypeIgnored", autoTrackEventType);
        return isAutoTrackEventType != null && isAutoTrackEventType;
    }

    @Override
    public void setViewID(View view, String viewID) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("setViewID", view, viewID);
    }

    @Override
    public void setViewID(android.app.Dialog view, String viewID) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("setViewID", view, viewID);
    }

    @Override
    public void setViewID(Object alertDialog, String viewID) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("setViewID", alertDialog, viewID);
    }

    @Override
    public void setViewActivity(View view, Activity activity) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("setViewActivity", view, activity);
    }

    @Override
    public void setViewFragmentName(View view, String fragmentName) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("setViewFragmentName", view, fragmentName);
    }

    @Override
    public void ignoreView(View view) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("ignoreView", view);
    }

    @Override
    public void ignoreView(View view, boolean ignore) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("ignoreView", view, ignore);
    }

    @Override
    public void setViewProperties(View view, JSONObject properties) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("setViewProperties", view, properties);
    }

    @Override
    public List<Class<?>> getIgnoredViewTypeList() {
        try {
            return SAModuleManager.getInstance().invokeAutoTrackFunction("getIgnoredViewTypeList");
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return null;
        }
    }

    @Override
    public void ignoreViewType(Class<?> viewType) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("ignoreViewType", viewType);
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
            SALog.printStackTrace(e);
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
            SALog.printStackTrace(e);
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
            SALog.printStackTrace(e);
        }
    }

    @Override
    public boolean isHeatMapEnabled() {
        return mSAConfigOptions.mHeatMapEnabled;
    }

    @Override
    public String getDistinctId() {
        try {
            return mSAContextManager.getUserIdentityAPI().getDistinctId();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return "";
    }

    @Override
    public String getAnonymousId() {
        try {
            return mSAContextManager.getUserIdentityAPI().getAnonymousId();
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
                    mSAContextManager.getUserIdentityAPI().resetAnonymousId();
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public String getLoginId() {
        try {
            return mSAContextManager.getUserIdentityAPI().getLoginId();
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
                        mSAContextManager.getUserIdentityAPI().identify(distinctId);
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
                if (!LoginIDAndKey.isInValidLogin(loginIDKey, loginId, mSAContextManager.getUserIdentityAPI().getIdentitiesInstance().getLoginIDKey(), mSAContextManager.getUserIdentityAPI().getIdentitiesInstance().getLoginId(), getAnonymousId())) {
                    mSAContextManager.getUserIdentityAPI().updateLoginId(loginIDKey, loginId);
                }
                mTrackTaskManager.addTrackEventTask(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (mSAContextManager.getUserIdentityAPI().loginWithKeyBack(loginIDKey, loginId)) {
                                mSAContextManager.trackEvent(new InputData().setEventName("$SignUp").setEventType(EventType.TRACK_SIGNUP).setProperties(cloneProperties));
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
            mSAContextManager.getUserIdentityAPI().updateLoginId(null, null);
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        mSAContextManager.getUserIdentityAPI().logout();
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
                        if (mSAContextManager.getUserIdentityAPI().bindBack(key, value)) {
                            mSAContextManager.trackEvent(new InputData().setEventName(BIND_ID).setEventType(EventType.TRACK_ID_BIND));
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
                        if (mSAContextManager.getUserIdentityAPI().unbindBack(key, value)) {
                            mSAContextManager.trackEvent(new InputData().setEventName(UNBIND_ID).setEventType(EventType.TRACK_ID_UNBIND));
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
            JSONUtils.mergeDistinctProperty(JSONUtils.cloneJsonObject(getDynamicProperty()), cloneProperties);
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    JSONObject _properties = cloneProperties;
                    if (SAModuleManager.getInstance().hasModuleByName(ModuleConstants.ModuleName.ADVERT_NAME)) {
                        _properties = SAModuleManager.getInstance().getAdvertModuleService().mergeChannelEventProperties(eventName, cloneProperties);
                    }
                    mSAContextManager.trackEvent(new InputData().setEventName(eventName).setEventType(EventType.TRACK).setProperties(_properties));
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
                    EventTimerManager.getInstance().addEventTimer(eventName, new EventTimer(timeUnit, startTime));
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
                    EventTimerManager.getInstance().removeTimer(eventName);
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
    public void trackTimerPause(final String eventName) {
        final long startTime = SystemClock.elapsedRealtime();
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                EventTimerManager.getInstance().updateTimerState(eventName, startTime, true);
            }
        });
    }

    @Override
    public void trackTimerResume(final String eventName) {
        final long startTime = SystemClock.elapsedRealtime();
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                EventTimerManager.getInstance().updateTimerState(eventName, startTime, false);
            }
        });
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
                        EventTimerManager.getInstance().updateEndTime(eventName, endTime);
                    }
                    try {
                        JSONObject _properties = cloneProperties;
                        if (SAModuleManager.getInstance().hasModuleByName(ModuleConstants.ModuleName.ADVERT_NAME)) {
                            _properties = SAModuleManager.getInstance().getAdvertModuleService().mergeChannelEventProperties(eventName, cloneProperties);
                        }
                        mSAContextManager.trackEvent(new InputData().setEventName(eventName).setEventType(EventType.TRACK).setProperties(_properties));
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
                EventTimerManager.getInstance().clearTimers();
            }
        });
    }

    @Override
    public String getLastScreenUrl() {
        return SAModuleManager.getInstance().invokeAutoTrackFunction("getLastScreenUrl");
    }

    @Override
    public void clearReferrerWhenAppEnd() {
        SAModuleManager.getInstance().invokeAutoTrackFunction("clearReferrerWhenAppEnd");
    }

    @Override
    public void clearLastScreenUrl() {
        SAModuleManager.getInstance().invokeAutoTrackFunction("clearLastScreenUrl");
    }

    @Override
    public JSONObject getLastScreenTrackProperties() {
        return SAModuleManager.getInstance().invokeAutoTrackFunction("getLastScreenTrackProperties");
    }

    @Override
    @Deprecated
    public void trackViewScreen(final String url, final JSONObject properties) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("trackViewScreen", url, properties);
    }

    @Override
    public void trackViewScreen(final Activity activity) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("trackViewScreen", activity);
    }

    @Override
    public void trackViewScreen(final Object fragment) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("trackViewScreen", fragment);
    }

    @Override
    public void trackViewAppClick(View view) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("trackViewAppClick", view);
    }

    @Override
    public void trackViewAppClick(final View view, final JSONObject properties) {
        SAModuleManager.getInstance().invokeAutoTrackFunction("trackViewAppClick", view, properties);
    }

    @Override
    public void flush() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mSAContextManager.getAnalyticsMessages().flush();
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
            mSAContextManager.getAnalyticsMessages().flushScheduled();
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
        mInternalConfigs.sensorsDataTrackEventCallBack = trackEventCallBack;
    }

    public void registerPropertyPlugin(final SAPropertyPlugin plugin) {
        if (plugin != null) {
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    mSAContextManager.getPluginManager().registerPropertyPlugin(plugin);
                }
            });
        }
    }

    public void unregisterPropertyPlugin(final SAPropertyPlugin plugin) {
        if (plugin != null) {
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    mSAContextManager.getPluginManager().unregisterPropertyPlugin(plugin);
                }
            });
        }
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
    public void deleteAll() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                mSAContextManager.getAnalyticsMessages().deleteAll();
            }
        });
    }

    @Override
    public JSONObject getSuperProperties() {
        synchronized (PersistentSuperProperties.class) {
            try {
                return new JSONObject(PersistentLoader.getInstance().getSuperPropertiesPst().get().toString());
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
                        JSONObject properties = PersistentLoader.getInstance().getSuperPropertiesPst().get();
                        PersistentLoader.getInstance().getSuperPropertiesPst().commit(JSONUtils.mergeSuperJSONObject(cloneSuperProperties, properties));
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
                    JSONObject superProperties = PersistentLoader.getInstance().getSuperPropertiesPst().get();
                    superProperties.remove(superPropertyName);
                    PersistentLoader.getInstance().getSuperPropertiesPst().commit(superProperties);
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
                PersistentLoader.getInstance().getSuperPropertiesPst().commit(new JSONObject());
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
                        mSAContextManager.trackEvent(new InputData().setEventType(EventType.PROFILE_SET).setProperties(cloneProperties));
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
    public void profileSet(final String property, final Object value) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mSAContextManager.trackEvent(new InputData().setEventType(EventType.PROFILE_SET).setProperties(new JSONObject().put(property, value)));
                } catch (Exception e) {
                    SALog.printStackTrace(e);
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
                        mSAContextManager.trackEvent(
                                new InputData().setEventType(EventType.PROFILE_SET_ONCE).setProperties(cloneProperties));
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
    public void profileSetOnce(final String property, final Object value) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mSAContextManager.trackEvent(new InputData().setEventType(EventType.PROFILE_SET_ONCE)
                            .setProperties(new JSONObject().put(property, value)));
                } catch (Exception e) {
                    SALog.printStackTrace(e);
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
                    mSAContextManager.trackEvent(new InputData().setEventType(EventType.PROFILE_INCREMENT)
                            .setProperties(new JSONObject(properties)));
                } catch (Exception e) {
                    SALog.printStackTrace(e);
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
                    mSAContextManager.trackEvent(new InputData().setEventType(EventType.PROFILE_INCREMENT)
                            .setProperties(new JSONObject().put(property, value)));
                } catch (Exception e) {
                    SALog.printStackTrace(e);
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
                    mSAContextManager.trackEvent(new InputData().setEventType(EventType.PROFILE_APPEND)
                            .setProperties(properties));
                } catch (Exception e) {
                    SALog.printStackTrace(e);
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
                    mSAContextManager.trackEvent(new InputData().setEventType(EventType.PROFILE_APPEND)
                            .setProperties(properties));
                } catch (Exception e) {
                    SALog.printStackTrace(e);
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
                    mSAContextManager.trackEvent(new InputData().setEventType(EventType.PROFILE_UNSET)
                            .setProperties(new JSONObject().put(property, true)));
                } catch (Exception e) {
                    SALog.printStackTrace(e);
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
                    mSAContextManager.trackEvent(new InputData().setEventType(EventType.PROFILE_DELETE));
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public boolean isDebugMode() {
        return mInternalConfigs.debugMode.isDebugMode();
    }

    @Override
    public boolean isNetworkRequestEnable() {
        return mInternalConfigs.isNetworkRequestEnable;
    }

    @Override
    public void enableNetworkRequest(boolean isRequest) {
        mInternalConfigs.isNetworkRequestEnable = isRequest;
    }

    @Override
    public void setServerUrl(String serverUrl) {
        setServerUrl(serverUrl, false);
    }

    @Override
    public void setServerUrl(final String serverUrl, boolean isRequestRemoteConfig) {
        try {
            //请求远程配置
            if (isRequestRemoteConfig && mSAContextManager.getRemoteManager() != null) {
                try {
                    mSAContextManager.getRemoteManager().requestRemoteConfig(BaseSensorsDataSDKRemoteManager.RandomTimeType.RandomTimeTypeWrite, false);
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
            //请求可视化全埋点自定义属性配置
            if (!TextUtils.equals(serverUrl, mOriginServerUrl)) {
                try {
                    SAVisual.requestVisualConfig();
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

            if (mInternalConfigs.debugMode != DebugMode.DEBUG_OFF) {
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
                mSAContextManager.trackEvent(new InputData().setExtras(eventInfo));
            }
        });
    }

    @Override
    public void profilePushId(final String pushTypeKey, final String pushId) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
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
        mTrackTaskManager.addTrackEventTask(new Runnable() {
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
                    mSAContextManager.trackEvent(new InputData().
                            setItemId(itemId).setItemType(itemType).setEventType(EventType.ITEM_SET).setProperties(cloneProperties));
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
                mSAContextManager.trackEvent(new InputData().
                        setItemId(itemId).setItemType(itemType).setEventType(EventType.ITEM_DELETE));
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

        public boolean isDebugMode() {
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
        public final int eventValue;

        AutoTrackEventType(int eventValue) {
            this.eventValue = eventValue;
        }

        public static String autoTrackEventName(int eventType) {
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