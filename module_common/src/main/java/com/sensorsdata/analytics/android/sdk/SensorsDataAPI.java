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
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;

import com.sensorsdata.analytics.android.sdk.core.business.SAPropertyManager;
import com.sensorsdata.analytics.android.sdk.core.business.exposure.SAExposureData;
import com.sensorsdata.analytics.android.sdk.core.business.timer.EventTimer;
import com.sensorsdata.analytics.android.sdk.core.business.timer.EventTimerManager;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.core.mediator.SAModuleManager;
import com.sensorsdata.analytics.android.sdk.core.rpc.SensorsDataContentObserver;
import com.sensorsdata.analytics.android.sdk.core.tasks.TrackTaskManagerThread;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentSuperProperties;
import com.sensorsdata.analytics.android.sdk.deeplink.SensorsDataDeepLinkCallback;
import com.sensorsdata.analytics.android.sdk.deeplink.SensorsDataDeferredDeepLinkCallback;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.monitor.TrackMonitor;
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
        try {
            if (isSDKDisabled()) {
                return new SensorsDataAPIEmptyImplementation();
            }

            if (null == context) {
                return new SensorsDataAPIEmptyImplementation();
            }

            final Context appContext = context.getApplicationContext();
            SensorsDataAPI instance = sInstanceMap.get(appContext);

            if (null == instance) {
                SALog.i(TAG, "The static method sharedInstance(context, serverURL, debugMode) should be called before calling sharedInstance()");
                return new SensorsDataAPIEmptyImplementation();
            }
            return instance;
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return new SensorsDataAPIEmptyImplementation();
        }
    }

    /**
     * 初始化神策 SDK
     *
     * @param context App 的 Context
     * @param saConfigOptions SDK 的配置项
     */
    public static void startWithConfigOptions(Context context, SAConfigOptions saConfigOptions) {
        try {
            if (context == null || saConfigOptions == null) {
                throw new NullPointerException("Context、SAConfigOptions can not be null");
            }
            SensorsDataAPI sensorsDataAPI = getInstance(context, DebugMode.DEBUG_OFF, saConfigOptions);
            if (!sensorsDataAPI.mSDKConfigInit) {
                sensorsDataAPI.applySAConfigOptions();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private synchronized static SensorsDataAPI getInstance(Context context, DebugMode debugMode,
                                              SAConfigOptions saConfigOptions) {
        if (null == context) {
            return new SensorsDataAPIEmptyImplementation();
        }

        final Context appContext = context.getApplicationContext();
        SensorsDataAPI instance = sInstanceMap.get(appContext);
        if (null == instance) {
            instance = new SensorsDataAPI(context, saConfigOptions, debugMode);
            sInstanceMap.put(appContext, instance);
        }
        return instance;
    }

    private static SensorsDataAPI getSDKInstance() {
        if (sInstanceMap.size() > 0) {
            Iterator<SensorsDataAPI> iterator = sInstanceMap.values().iterator();
            if (iterator.hasNext()) {
                return iterator.next();
            }
        }
        return new SensorsDataAPIEmptyImplementation();
    }

    public static SensorsDataAPI sharedInstance() {
        try {
            if (isSDKDisabled()) {
                return new SensorsDataAPIEmptyImplementation();
            }

            return getSDKInstance();
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return new SensorsDataAPIEmptyImplementation();
        }
    }

    /**
     * 关闭 SDK
     */
    public static void disableSDK() {
        SALog.i(TAG, "call static function disableSDK");
        try {
            final SensorsDataAPI sensorsDataAPI = getSDKInstance();
            if (sensorsDataAPI instanceof SensorsDataAPIEmptyImplementation ||
                    getConfigOptions() == null) {
                return;
            }
            if (!SensorsDataContentObserver.State.DISABLE_SDK.isObserverCalled) {
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
            if (!SensorsDataContentObserver.State.DISABLE_SDK.isObserverCalled) {
                sensorsDataAPI.getSAContextManager().getContext().getContentResolver().notifyChange(DbParams.getInstance().getDisableSDKUri(), null);
            }
            SensorsDataContentObserver.State.DISABLE_SDK.isObserverCalled = false;
            SensorsDataContentObserver.State.DISABLE_SDK.isDid = true;
            SensorsDataContentObserver.State.ENABLE_SDK.isDid = false;
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
                    getConfigOptions() == null) {
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
                //重新请求采集控制
                sensorsDataAPI.mSAContextManager.getRemoteManager().pullSDKConfigFromServer();
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }

            if (!SensorsDataContentObserver.State.ENABLE_SDK.isObserverCalled) {
                sensorsDataAPI.getSAContextManager().getContext().getContentResolver().notifyChange(DbParams.getInstance().getEnableSDKUri(), null);
            }
            SensorsDataContentObserver.State.ENABLE_SDK.isObserverCalled = false;
            SensorsDataContentObserver.State.ENABLE_SDK.isDid = true;
            SensorsDataContentObserver.State.DISABLE_SDK.isDid = false;
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
                if (mSAContextManager.getOrientationDetector() == null) {
                    mSAContextManager.setOrientationDetector(new SensorsDataScreenOrientationDetector(mInternalConfigs.context, SensorManager.SENSOR_DELAY_NORMAL));
                }
                mSAContextManager.getOrientationDetector().enable();
                mSAContextManager.getOrientationDetector().setState(true);
            } else {
                if (mSAContextManager.getOrientationDetector() != null) {
                    mSAContextManager.getOrientationDetector().disable();
                    mSAContextManager.getOrientationDetector().setState(false);
                    mSAContextManager.setOrientationDetector(null);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void resumeTrackScreenOrientation() {
        try {
            if (mSAContextManager.getOrientationDetector() != null) {
                mSAContextManager.getOrientationDetector().enable();
                mSAContextManager.getOrientationDetector().setState(true);
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public void stopTrackScreenOrientation() {
        try {
            if (mSAContextManager.getOrientationDetector() != null) {
                mSAContextManager.getOrientationDetector().disable();
                mSAContextManager.getOrientationDetector().setState(false);
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public String getScreenOrientation() {
        try {
            if (mSAContextManager.getOrientationDetector() != null) {
                return mSAContextManager.getOrientationDetector().getOrientation();
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
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_ENABLE_AUTO_TRACK, eventTypeList);
    }

    @Override
    public void disableAutoTrack(List<AutoTrackEventType> eventTypeList) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_DISABLE_AUTO_TRACK, eventTypeList);
    }

    @Override
    public void disableAutoTrack(AutoTrackEventType autoTrackEventType) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_DISABLE_AUTO_TRACK, autoTrackEventType);
    }

    @Override
    public boolean isAutoTrackEnabled() {
        try {
            if (isSDKDisabled()) {
                return false;
            }
            Boolean isAutoTrack = SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_IS_AUTOTRACK_ENABLED);
            return isAutoTrack != null && isAutoTrack;
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return false;
        }
    }

    @Override
    public void trackFragmentAppViewScreen() {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_TRACK_FRAGMENT_APPVIEWSCREEN);
    }

    @Override
    public boolean isTrackFragmentAppViewScreenEnabled() {
        Boolean isAutoTrackFragment = SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_IS_TRACK_FRAGMENT_APPVIEWSCREEN_ENABLED);
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
        SAModuleManager.getInstance().invokeModuleFunction(Modules.WebView.MODULE_NAME, Modules.WebView.METHOD_SHOWUP_WEBVIEW,
                webView, properties, isSupportJellyBean, enableVerify);
    }

    @Override
    @Deprecated
    public void showUpWebView(WebView webView, boolean isSupportJellyBean, JSONObject properties) {
        showUpWebView(webView, properties, isSupportJellyBean, false);
    }

    @Override
    @Deprecated
    public void showUpX5WebView(Object x5WebView, JSONObject properties, boolean isSupportJellyBean, boolean enableVerify) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.WebView.MODULE_NAME, Modules.WebView.METHOD_SHOWUP_X5WEBVIEW,
                x5WebView, properties, isSupportJellyBean, enableVerify);
    }

    @Override
    public void showUpX5WebView(Object x5WebView, boolean enableVerify) {
        showUpX5WebView(x5WebView, null, true, enableVerify);
    }

    @Override
    public void showUpX5WebView(Object x5WebView) {
        showUpX5WebView(x5WebView, false);
    }

    @Override
    public void ignoreAutoTrackActivities(List<Class<?>> activitiesList) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_IGNORE_AUTOTRACK_ACTIVITIES, activitiesList);
    }

    @Override
    public void resumeAutoTrackActivities(List<Class<?>> activitiesList) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_RESUME_AUTOTRACK_ACTIVITIES, activitiesList);
    }

    @Override
    public void ignoreAutoTrackActivity(Class<?> activity) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_IGNORE_AUTOTRACK_ACTIVITY, activity);
    }

    @Override
    public void resumeAutoTrackActivity(Class<?> activity) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_RESUME_AUTOTRACK_ACTIVITY, activity);
    }

    @Override
    public void enableAutoTrackFragment(Class<?> fragment) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_ENABLE_AUTOTRACK_FRAGMENT, fragment);
    }

    @Override
    public void enableAutoTrackFragments(List<Class<?>> fragmentsList) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_ENABLE_AUTOTRACK_FRAGMENTS, fragmentsList);
    }

    @Override
    public boolean isActivityAutoTrackAppViewScreenIgnored(Class<?> activity) {
        Boolean isAutoTrackActivity = SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME,
                Modules.AutoTrack.METHOD_IS_ACTIVITY_AUTOTRACK_APPVIEWSCREEN_IGNORED, activity);
        return isAutoTrackActivity != null && isAutoTrackActivity;
    }

    @Override
    public boolean isFragmentAutoTrackAppViewScreen(Class<?> fragment) {
        Boolean isAutoTrackFragment = SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME,
                Modules.AutoTrack.METHOD_IS_FRAGMENT_AUTOTRACK_APPVIEWSCREEN, fragment);
        return isAutoTrackFragment != null && isAutoTrackFragment;
    }

    @Override
    public void ignoreAutoTrackFragments(List<Class<?>> fragmentList) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME,
                Modules.AutoTrack.METHOD_IGNORE_AUTOTRACK_FRAGMENTS, fragmentList);
    }

    @Override
    public void ignoreAutoTrackFragment(Class<?> fragment) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME,
                Modules.AutoTrack.METHOD_IGNORE_AUTOTRACK_FRAGMENT, fragment);
    }

    @Override
    public void resumeIgnoredAutoTrackFragments(List<Class<?>> fragmentList) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME,
                Modules.AutoTrack.METHOD_RESUME_IGNORED_AUTOTRACK_FRAGMENTS, fragmentList);
    }

    @Override
    public void resumeIgnoredAutoTrackFragment(Class<?> fragment) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME,
                Modules.AutoTrack.METHOD_RESUME_IGNORED_AUTOTRACK_FRAGMENT, fragment);
    }

    @Override
    public boolean isActivityAutoTrackAppClickIgnored(Class<?> activity) {
        Boolean isAutoTrackActivity = SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME,
                Modules.AutoTrack.METHOD_IS_ACTIVITY_AUTOTRACK_APPCLICK_IGNORED, activity);
        return isAutoTrackActivity != null && isAutoTrackActivity;
    }

    @Override
    public boolean isAutoTrackEventTypeIgnored(AutoTrackEventType eventType) {
        Boolean isAutoTrackEventType = SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME,
                Modules.AutoTrack.METHOD_IS_AUTOTRACK_EVENT_TYPE_IGNORED, eventType);
        return isAutoTrackEventType != null && isAutoTrackEventType;
    }

    @Override
    public boolean isAutoTrackEventTypeIgnored(int autoTrackEventType) {
        Boolean isAutoTrackEventType = SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME,
                Modules.AutoTrack.METHOD_IS_AUTOTRACK_EVENT_TYPE_IGNORED, autoTrackEventType);
        return isAutoTrackEventType != null && isAutoTrackEventType;
    }

    @Override
    public void setViewID(View view, String viewID) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME,
                Modules.AutoTrack.METHOD_SET_VIEW_ID, view, viewID);
    }

    @Override
    public void setViewID(android.app.Dialog view, String viewID) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME,
                Modules.AutoTrack.METHOD_SET_VIEW_ID, view, viewID);
    }

    @Override
    public void setViewID(Object alertDialog, String viewID) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME,
                Modules.AutoTrack.METHOD_SET_VIEW_ID, alertDialog, viewID);
    }

    @Override
    public void setViewActivity(View view, Activity activity) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME,
                Modules.AutoTrack.METHOD_SET_VIEW_ACTIVITY, view, activity);
    }

    @Override
    public void setViewFragmentName(View view, String fragmentName) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME,
                Modules.AutoTrack.METHOD_SET_VIEW_FRAGMENT_NAME, view, fragmentName);
    }

    @Override
    public void ignoreView(View view) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME,
                Modules.AutoTrack.METHOD_IGNORE_VIEW, view);
    }

    @Override
    public void ignoreView(View view, boolean ignore) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME,
                Modules.AutoTrack.METHOD_IGNORE_VIEW, view, ignore);
    }

    @Override
    public void setViewProperties(View view, JSONObject properties) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME,
                Modules.AutoTrack.METHOD_SET_VIEW_PROPERTIES, view, properties);
    }

    @Override
    public List<Class<?>> getIgnoredViewTypeList() {
        try {
            return SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME,
                    Modules.AutoTrack.METHOD_GET_IGNORED_VIEW_TYPE_LIST);
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return null;
        }
    }

    @Override
    public void ignoreViewType(Class<?> viewType) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME,
                Modules.AutoTrack.METHOD_IGNORE_VIEW_TYPE, viewType);
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
            SensorsDataContentObserver.State.LOGIN.isDid = true;
            SensorsDataContentObserver.State.LOGOUT.isDid = false;
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
    public void resetAnonymousIdentity(final String anonymousId) {
        if (mTrackTaskManager != null) {
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        mSAContextManager.getUserIdentityAPI().resetAnonymousIdentity(anonymousId);
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            });
        }
    }

    @Override
    public void trackInstallation(String eventName, JSONObject properties, final boolean disableCallback) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.Advert.MODULE_NAME, Modules.Advert.METHOD_TRACK_INSTALLATION, eventName, properties, disableCallback);
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
        SAModuleManager.getInstance().invokeModuleFunction(Modules.Advert.MODULE_NAME,
                Modules.Advert.METHOD_TRACK_CHANNEL_EVENT, eventName, properties);
    }

    @Override
    public void track(final String eventName, final JSONObject properties) {
        try {
            final JSONObject cloneProperties = JSONUtils.cloneJsonObject(properties);
            JSONUtils.mergeDistinctProperty(JSONUtils.cloneJsonObject(getDynamicProperty()), cloneProperties);
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject _properties = SAModuleManager.getInstance().invokeModuleFunction(Modules.Advert.MODULE_NAME,
                                Modules.Advert.METHOD_MERGE_CHANNEL_EVENT_PROPERTIES, eventName, cloneProperties);
                        if (_properties == null) {
                            _properties = cloneProperties;
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
                        JSONObject _properties = SAModuleManager.getInstance().invokeModuleFunction(Modules.Advert.MODULE_NAME,
                                Modules.Advert.METHOD_MERGE_CHANNEL_EVENT_PROPERTIES, eventName, cloneProperties);
                        if (_properties == null) {
                            _properties = cloneProperties;
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
        return SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_GET_LAST_SCREENURL);
    }

    @Override
    public void clearReferrerWhenAppEnd() {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_CLEAR_REFERRER_WHEN_APPEND);
    }

    @Override
    public void clearLastScreenUrl() {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_CLEAR_LAST_SCREENURL);
    }

    @Override
    public JSONObject getLastScreenTrackProperties() {
        return SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_GET_LAST_SCREEN_TRACK_PROPERTIES);
    }

    @Override
    @Deprecated
    public void trackViewScreen(final String url, final JSONObject properties) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_TRACK_VIEW_SCREEN, url, properties);
    }

    @Override
    public void trackViewScreen(final Activity activity) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_TRACK_VIEW_SCREEN, activity);
    }

    @Override
    public void trackViewScreen(final Object fragment) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_TRACK_VIEW_SCREEN, fragment);
    }

    @Override
    public void trackViewAppClick(View view) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_TRACK_VIEW_APPCLICK, view);
    }

    @Override
    public void trackViewAppClick(final View view, final JSONObject properties) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.AutoTrack.MODULE_NAME, Modules.AutoTrack.METHOD_TRACK_VIEW_APPCLICK, view, properties);
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
        SAModuleManager.getInstance().invokeModuleFunction(Modules.Advert.MODULE_NAME,
                Modules.Advert.METHOD_SET_DEEPLINK_CALLBACK, deepLinkCallback);
    }

    @Override
    public void setDeepLinkCompletion(SensorsDataDeferredDeepLinkCallback callback) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.Advert.MODULE_NAME,
                Modules.Advert.METHOD_SET_DEEPLINK_COMPLETION, callback);
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
                return new JSONObject(JSONUtils.cloneJsonObject(PersistentLoader.getInstance().getSuperPropertiesPst().get()).toString());
            } catch (Exception e) {
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
                        PersistentLoader.getInstance().getSuperPropertiesPst().commit(JSONUtils.mergeSuperJSONObject(cloneSuperProperties,
                                JSONUtils.cloneJsonObject(PersistentLoader.getInstance().getSuperPropertiesPst().get())));
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
                    JSONObject superProperties = JSONUtils.cloneJsonObject(PersistentLoader.getInstance().getSuperPropertiesPst().get());
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
                    SAModuleManager.getInstance().invokeModuleFunction(Modules.Visual.MODULE_NAME, Modules.Visual.METHOD_REQUEST_VISUAL_CONFIG);
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }

            mOriginServerUrl = serverUrl;
            if (TextUtils.isEmpty(serverUrl)) {
                mServerUrl = serverUrl;
                mSAConfigOptions.mServerUrl = mServerUrl;
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
            mSAConfigOptions.mServerUrl = mServerUrl;
            TrackMonitor.getInstance().callSetServerUrl();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
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
        SAModuleManager.getInstance().invokeModuleFunction(Modules.Advert.MODULE_NAME,
                Modules.Advert.METHOD_ENABLE_DEEPLINK_INSTALL_SOURCE, enable);
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
        SAModuleManager.getInstance().invokeModuleFunction(Modules.Advert.MODULE_NAME,
                Modules.Advert.METHOD_TRACK_DEEPLINK_LAUNCH, deepLinkUrl, oaid);
    }

    @Override
    public void requestDeferredDeepLink(final JSONObject params) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.Advert.MODULE_NAME,
                Modules.Advert.METHOD_REQUEST_DEFERRED_DEEPLINK, params);
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
     * 设置曝光 view 唯一标记位，一般只在列表复用的情况下使用
     *
     * @param view 被标记的 view
     * @param exposureIdentifier 被标记 view 的唯一标记位
     */
    @Override
    public void setExposureIdentifier(View view, String exposureIdentifier) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.Exposure.MODULE_NAME, Modules.Exposure.METHOD_SET_EXPOSURE_IDENTIFIER, view, exposureIdentifier);
    }

    /**
     * 曝光 view 标记
     *
     * @param view 被标记的 view
     * @param exposureData 曝光配置
     */
    @Override
    public void addExposureView(View view, SAExposureData exposureData) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.Exposure.MODULE_NAME, Modules.Exposure.METHOD_ADD_EXPOSURE_VIEW, view, exposureData);
    }

    /**
     * 曝光 view 标记取消
     *
     * @param view 被标记的 view
     * @param identifier 被标记的 view 的唯一标识
     */
    @Override
    public void removeExposureView(View view, String identifier) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.Exposure.MODULE_NAME, Modules.Exposure.METHOD_REMOVE_EXPOSURE_VIEW, view, identifier);
    }

    /**
     * 曝光 view 标记取消
     *
     * @param view 被标记的 view
     */
    @Override
    public void removeExposureView(View view) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.Exposure.MODULE_NAME, Modules.Exposure.METHOD_REMOVE_EXPOSURE_VIEW, view);
    }

    @Override
    public void updateExposureProperties(View view, JSONObject properties) {
        SAModuleManager.getInstance().invokeModuleFunction(Modules.Exposure.MODULE_NAME, Modules.Exposure.METHOD_UPDATE_EXPOSURE_PROPERTIES, view, properties);
    }

    @Override
    public void registerLimitKeys(Map<String, String> limitKeys) {
        SAPropertyManager.getInstance().registerLimitKeys(limitKeys);
    }

    @Override
    public void enableRemoteConfig(boolean enable) {
        try {
            if (mSAContextManager != null) {
                mInternalConfigs.isRemoteConfigEnabled = enable;
                if (mSAContextManager.getRemoteManager() != null) {
                    mSAContextManager.getRemoteManager().pullSDKConfigFromServer();
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
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