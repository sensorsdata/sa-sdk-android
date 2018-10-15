/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Toast;

import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Sensors Analytics SDK
 */
public class SensorsDataAPI implements ISensorsDataAPI {

    /**
     * Debug 模式，用于检验数据导入是否正确。该模式下，事件会逐条实时发送到 Sensors Analytics，并根据返回值检查
     * 数据导入是否正确。
     *
     * Debug 模式的具体使用方式，请参考:
     * http://www.sensorsdata.cn/manual/debug_mode.html
     *
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
     * 网络类型
     */
    public final class NetworkType {
        public static final int TYPE_NONE = 0;//NULL
        public static final int TYPE_2G = 1;//2G
        public static final int TYPE_3G = 1 << 1;//3G
        public static final int TYPE_4G = 1 << 2;//4G
        public static final int TYPE_WIFI = 1 << 3;//WIFI
        public static final int TYPE_ALL = 0xFF;//ALL
    }

    protected boolean isShouldFlush(String networkType) {
        return (toNetworkType(networkType) & mFlushNetworkPolicy) != 0;
    }

    private int toNetworkType(String networkType) {
        if ("NULL".equals(networkType)) {
            return NetworkType.TYPE_ALL;
        } else if ("WIFI".equals(networkType)) {
            return NetworkType.TYPE_WIFI;
        } else if ("2G".equals(networkType)) {
            return NetworkType.TYPE_2G;
        } else if ("3G".equals(networkType)) {
            return NetworkType.TYPE_3G;
        } else if ("4G".equals(networkType)) {
            return NetworkType.TYPE_4G;
        }
        return NetworkType.TYPE_ALL;
    }

    /**
     * AutoTrack 默认采集的事件类型
     */
    public enum AutoTrackEventType {
        APP_START("$AppStart", 1 << 0),
        APP_END("$AppEnd", 1 << 1),
        APP_CLICK("$AppClick", 1 << 2),
        APP_VIEW_SCREEN("$AppViewScreen", 1 << 3);
        private final String eventName;
        private final int eventValue;

        public static AutoTrackEventType autoTrackEventTypeFromEventName(String eventName) {
            if (TextUtils.isEmpty(eventName)) {
                return null;
            }

            if ("$AppStart".equals(eventName)) {
                return APP_START;
            } else if ("$AppEnd".equals(eventName)) {
                return APP_END;
            } else if ("$AppClick".equals(eventName)) {
                return APP_CLICK;
            } else if ("$AppViewScreen".equals(eventName)) {
                return APP_VIEW_SCREEN;
            }

            return null;
        }

        AutoTrackEventType(String eventName, int eventValue) {
            this.eventName = eventName;
            this.eventValue = eventValue;
        }

        String getEventName() {
            return eventName;
        }

        int getEventValue() {
            return eventValue;
        }
    }

    //private
    SensorsDataAPI() {
        mContext = null;
        mMessages = null;
        mDistinctId = null;
        mLoginId = null;
        mSuperProperties = null;
        mFirstStart = null;
        mFirstDay = null;
        mFirstTrackInstallation = null;
        mFirstTrackInstallationWithCallback = null;
        mPersistentRemoteSDKConfig = null;
        mDeviceInfo = null;
        mTrackTimer = null;
        mMainProcessName = null;
        mDbAdapter = null;
    }

    SensorsDataAPI(Context context, String serverURL, DebugMode debugMode) {
        mContext = context;
        mDebugMode = debugMode;

        final String packageName = context.getApplicationContext().getPackageName();
        mAutoTrackIgnoredActivities = new ArrayList<>();
        mHeatMapActivities = new ArrayList<>();
        mAutoTrackEventTypeList = new CopyOnWriteArraySet<>();

        try {
            SensorsDataUtils.cleanUserAgent(mContext);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Bundle configBundle = null;

        try {
            SALog.init(this);
            final ApplicationInfo appInfo = context.getApplicationContext().getPackageManager()
                    .getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            configBundle = appInfo.metaData;
        } catch (final PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (null == configBundle) {
            configBundle = new Bundle();
        }

        setServerUrl(serverURL);

        if (debugMode == DebugMode.DEBUG_OFF) {
            ENABLE_LOG = configBundle.getBoolean("com.sensorsdata.analytics.android.EnableLogging",
                    false);
        } else {
            ENABLE_LOG = configBundle.getBoolean("com.sensorsdata.analytics.android.EnableLogging",
                    true);
        }
        SHOW_DEBUG_INFO_VIEW = configBundle.getBoolean("com.sensorsdata.analytics.android.ShowDebugInfoView",
                true);

        mFlushInterval = configBundle.getInt("com.sensorsdata.analytics.android.FlushInterval",
                15000);
        mFlushBulkSize = configBundle.getInt("com.sensorsdata.analytics.android.FlushBulkSize",
                100);
        mAutoTrack = configBundle.getBoolean("com.sensorsdata.analytics.android.AutoTrack",
                false);
        mHeatMapEnabled = configBundle.getBoolean("com.sensorsdata.analytics.android.HeatMap",
                false);
        mDisableDefaultRemoteConfig = configBundle.getBoolean("com.sensorsdata.analytics.android.DisableDefaultRemoteConfig",
                false);
        mEnableButterknifeOnClick = configBundle.getBoolean("com.sensorsdata.analytics.android.ButterknifeOnClick",
                false);
        String mainProcessName = SensorsDataUtils.getMainProcessName(context);
        if (TextUtils.isEmpty(mainProcessName)) {
            mMainProcessName = configBundle.getString("com.sensorsdata.analytics.android.MainProcessName");
        } else {
            mMainProcessName = mainProcessName;
        }

        mEnableAppHeatMapConfirmDialog = configBundle.getBoolean("com.sensorsdata.analytics.android.EnableHeatMapConfirmDialog",
                true);

        mDisableTrackDeviceId = configBundle.getBoolean("com.sensorsdata.analytics.android.DisableTrackDeviceId",
                false);

        mMessages = AnalyticsMessages.getInstance(mContext, packageName);

        final SharedPreferencesLoader.OnPrefsLoadedListener listener =
                new SharedPreferencesLoader.OnPrefsLoadedListener() {
                    @Override
                    public void onPrefsLoaded(SharedPreferences preferences) {
                    }
                };

        final String prefsName = "com.sensorsdata.analytics.android.sdk.SensorsDataAPI";
        final Future<SharedPreferences> storedPreferences =
                sPrefsLoader.loadPreferences(context, prefsName, listener);

        mDistinctId = new PersistentDistinctId(storedPreferences, mContext);
        mLoginId = new PersistentLoginId(storedPreferences);
        mSuperProperties = new PersistentSuperProperties(storedPreferences);
        mFirstStart = new PersistentFirstStart(storedPreferences);
        mFirstTrackInstallation = new PersistentFirstTrackInstallation(storedPreferences);
        mFirstTrackInstallationWithCallback = new PersistentFirstTrackInstallationWithCallback(storedPreferences);
        mPersistentRemoteSDKConfig = new PersistentRemoteSDKConfig(storedPreferences);

        mTrackTaskManager = TrackTaskManager.getInstance();
        mTrackTaskManagerThread = new TrackTaskManagerThread();
        new Thread(mTrackTaskManagerThread).start();

        //先从缓存中读取 SDKConfig
        applySDKConfigFromCache();

        //打开debug模式，弹出提示
        if (mDebugMode != DebugMode.DEBUG_OFF && SensorsDataUtils.isMainProcess(mContext.getApplicationContext(), mMainProcessName)) {
            if (SHOW_DEBUG_INFO_VIEW) {
                if (!isSDKDisabled()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            showDebugModeWarning();
                        }
                    }).start();
                }
            }
        }

        mFirstDay = new PersistentFirstDay(storedPreferences);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            final Application app = (Application) context.getApplicationContext();

            SensorsDataActivityLifecycleCallbacks lifecycleCallbacks =
                    new SensorsDataActivityLifecycleCallbacks(this, mFirstStart, mFirstDay, context, packageName);
            app.registerActivityLifecycleCallbacks(lifecycleCallbacks);
            context.getContentResolver().registerContentObserver(Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + DbAdapter.Table.APPSTARTED.getName()),
                    false, lifecycleCallbacks);
            context.getContentResolver().registerContentObserver(Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + DbAdapter.Table.SESSIONINTERVALTIME.getName()),
                    false, lifecycleCallbacks);
            context.getContentResolver().registerContentObserver(Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + DbAdapter.Table.APPENDSTATE.getName()),
                    false, lifecycleCallbacks);
        }

        if (debugMode != DebugMode.DEBUG_OFF) {
            Log.i(TAG, String.format(Locale.CHINA, "Initialized the instance of Sensors Analytics SDK with server"
                            + " url '%s', flush interval %d ms, debugMode: %s", mServerUrl, mFlushInterval, debugMode));
        }

        final Map<String, Object> deviceInfo = new HashMap<>();

        {
            deviceInfo.put("$lib", "Android");
            deviceInfo.put("$lib_version", VERSION);
            deviceInfo.put("$os", "Android");
            deviceInfo.put("$os_version",
                    Build.VERSION.RELEASE == null ? "UNKNOWN" : Build.VERSION.RELEASE);
            deviceInfo
                    .put("$manufacturer", SensorsDataUtils.getManufacturer());
            if (TextUtils.isEmpty(Build.MODEL)) {
                deviceInfo.put("$model", "UNKNOWN");
            } else {
                deviceInfo.put("$model", Build.MODEL.trim());
            }
            try {
                final PackageManager manager = mContext.getPackageManager();
                final PackageInfo info = manager.getPackageInfo(mContext.getPackageName(), 0);
                deviceInfo.put("$app_version", info.versionName);
            } catch (final Exception e) {
                if (debugMode != DebugMode.DEBUG_OFF) {
                    Log.i(TAG, "Exception getting app version name", e);
                }
            }
            final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            deviceInfo.put("$screen_height", displayMetrics.heightPixels);
            deviceInfo.put("$screen_width", displayMetrics.widthPixels);

            try {
                WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
                if (Build.VERSION.SDK_INT >= 17) {
                    Point point = new Point();
                    if (windowManager != null) {
                        windowManager.getDefaultDisplay().getRealSize(point);
                        deviceInfo.put("$screen_height", point.y);
                    }
                }
            } catch (Exception e) {
                deviceInfo.put("$screen_height", displayMetrics.heightPixels);
            }

            String carrier = SensorsDataUtils.getCarrier(mContext);
            if (!TextUtils.isEmpty(carrier)) {
                deviceInfo.put("$carrier", carrier);
            }

            if (!mDisableTrackDeviceId) {
                String androidID = SensorsDataUtils.getAndroidID(mContext);
                if (!TextUtils.isEmpty(androidID)) {
                    deviceInfo.put("$device_id", androidID);
                }
            }

            Integer zone_offset = SensorsDataUtils.getZoneOffset();
            if (zone_offset != null){
                //deviceInfo.put("$timezone_offset", zone_offset);
            }
        }

        ArrayList<String> autoTrackFragments = SensorsDataUtils.getAutoTrackFragments(context);
        if (autoTrackFragments.size() > 0) {
            mAutoTrackFragments = new CopyOnWriteArraySet<>();
            for(String fragment : autoTrackFragments) {
                mAutoTrackFragments.add(fragment.hashCode());
            }
        }
        mDeviceInfo = Collections.unmodifiableMap(deviceInfo);
        mTrackTimer = new HashMap<>();
        mDbAdapter = new DbAdapter(context, packageName);
    }

    /**
     * 获取SensorsDataAPI单例
     *
     * @param context App的Context
     * @return SensorsDataAPI单例
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
                Log.i(TAG, "The static method sharedInstance(context, serverURL, debugMode) should be called before calling sharedInstance()");
                return new SensorsDataAPIEmptyImplementation();
            }
            return instance;
        }
    }


    /**
     * 初始化并获取SensorsDataAPI单例
     *
     * @param context      App 的 Context
     * @param serverURL    用于收集事件的服务地址
     * @param debugMode    Debug模式,
     *                     {@link com.sensorsdata.analytics.android.sdk.SensorsDataAPI.DebugMode}
     * @return SensorsDataAPI单例
     */
    public static SensorsDataAPI sharedInstance(Context context, String serverURL, DebugMode debugMode) {
        if (null == context) {
            return new SensorsDataAPIEmptyImplementation();
        }

        synchronized (sInstanceMap) {
            final Context appContext = context.getApplicationContext();

            SensorsDataAPI instance = sInstanceMap.get(appContext);
            if (null == instance && ConfigurationChecker.checkBasicConfiguration(appContext)) {
                instance = new SensorsDataAPI(appContext, serverURL, debugMode);
                sInstanceMap.put(appContext, instance);
            }

            if (instance != null) {
                return instance;
            } else {
                return new SensorsDataAPIEmptyImplementation();
            }
        }
    }

    public static SensorsDataAPI sharedInstance() {
        if (isSDKDisabled()) {
            return new SensorsDataAPIEmptyImplementation();
        }

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

    /**
     * 返回是否关闭了 SDK
     * @return true：关闭；false：没有关闭
     */
    public static boolean isSDKDisabled() {
        if (mSDKRemoteConfig == null) {
            return false;
        }

        return mSDKRemoteConfig.isDisableSDK();
    }

    /**
     * 更新 SensorsDataSDKRemoteConfig
     * @param sdkRemoteConfig SensorsDataSDKRemoteConfig 在线控制 SDK 的配置
     * @param effectImmediately 是否立即生效
     */
    private void setSDKRemoteConfig(SensorsDataSDKRemoteConfig sdkRemoteConfig, boolean effectImmediately) {
        try {
            if (sdkRemoteConfig.isDisableSDK()) {
                SensorsDataSDKRemoteConfig cachedConfig = SensorsDataUtils.toSDKRemoteConfig(mPersistentRemoteSDKConfig.get());
                if (!cachedConfig.isDisableSDK()) {
                    track("DisableSensorsDataSDK");
                }
            }
            mPersistentRemoteSDKConfig.commit(sdkRemoteConfig.toJson().toString());
            if (effectImmediately) {
                mSDKRemoteConfig = sdkRemoteConfig;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void pullSDKConfigFromServer() {
        if (mDisableDefaultRemoteConfig) {
            return;
        }

        if (mPullSDKConfigCountDownTimer != null) {
            mPullSDKConfigCountDownTimer.cancel();
            mPullSDKConfigCountDownTimer = null;
        }

        mPullSDKConfigCountDownTimer = new CountDownTimer(120 * 1000, 30 * 1000) {
            @Override
            public void onTick(long l) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        InputStreamReader in = null;
                        HttpURLConnection urlConnection = null;
                        try {
                            if (TextUtils.isEmpty(mServerUrl)) {
                                return;
                            }

                            URL url = null;
                            String configUrl = null;
                            int pathPrefix = mServerUrl.lastIndexOf("/");
                            if (pathPrefix != -1) {
                                configUrl = mServerUrl.substring(0, pathPrefix);
                                String configVersion = null;
                                if (mSDKRemoteConfig != null) {
                                    configVersion = mSDKRemoteConfig.getV();
                                }

                                configUrl = configUrl + "/config/Android.conf";

                                if (configVersion != null) {
                                    configUrl = configUrl + "?v=" + configVersion;
                                }
                            }

                            url = new URL(configUrl);
                            urlConnection = (HttpURLConnection) url.openConnection();
                            if (urlConnection == null) {
                                return;
                            }
                            int responseCode = urlConnection.getResponseCode();

                            //配置没有更新
                            if (responseCode == 304) {
                                resetPullSDKConfigTimer();
                                return;
                            }

                            if (responseCode == 200) {
                                resetPullSDKConfigTimer();

                                in = new InputStreamReader(urlConnection.getInputStream());
                                BufferedReader bufferedReader = new BufferedReader(in);
                                StringBuilder result = new StringBuilder();
                                String data;
                                while ((data = bufferedReader.readLine()) != null) {
                                    result.append(data);
                                }
                                SensorsDataSDKRemoteConfig sdkRemoteConfig = SensorsDataUtils.toSDKRemoteConfig(result.toString());
                                setSDKRemoteConfig(sdkRemoteConfig, false);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                if (in != null) {
                                    in.close();
                                }

                                if (urlConnection != null) {
                                    urlConnection.disconnect();
                                }
                            } catch (Exception e) {
                                //ignored
                            }
                        }
                    }
                }).start();
            }

            @Override
            public void onFinish() {
            }
        };
        mPullSDKConfigCountDownTimer.start();
    }

    /**
     * 每次启动 App 时，最多尝试三次
     */
    private CountDownTimer mPullSDKConfigCountDownTimer;

    protected void resetPullSDKConfigTimer() {
        try {
            if (mPullSDKConfigCountDownTimer != null) {
                mPullSDKConfigCountDownTimer.cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mPullSDKConfigCountDownTimer = null;
        }
    }

    /**
     * 从本地缓存中读取最新的 SDK 配置信息
     */
    protected void applySDKConfigFromCache() {
        try {
            SensorsDataSDKRemoteConfig sdkRemoteConfig = SensorsDataUtils.toSDKRemoteConfig(mPersistentRemoteSDKConfig.get());

            if (sdkRemoteConfig == null) {
                sdkRemoteConfig = new SensorsDataSDKRemoteConfig();
            }

            //关闭 debug 模式
            if (sdkRemoteConfig.isDisableDebugMode()) {
                disableDebugMode();
            }

            //开启关闭 AutoTrack
            List<SensorsDataAPI.AutoTrackEventType> autoTrackEventTypeList = sdkRemoteConfig.getAutoTrackEventTypeList();
            if (autoTrackEventTypeList != null) {
                enableAutoTrack(autoTrackEventTypeList);
            }

            if (sdkRemoteConfig.isDisableSDK()) {
                try {
                    flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            mSDKRemoteConfig = sdkRemoteConfig;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 返回预置属性
     * @return JSONObject 预置属性
     */
    @Override
    public JSONObject getPresetProperties() {
        JSONObject properties = new JSONObject();
        try {
            properties.put("$app_version", mDeviceInfo.get("$app_version"));
            properties.put("$lib", "Android");
            properties.put("$lib_version", VERSION);
            properties.put("$manufacturer", mDeviceInfo.get("$manufacturer"));
            properties.put("$model", mDeviceInfo.get("$model"));
            properties.put("$os", "Android");
            properties.put("$os_version", mDeviceInfo.get("$os_version"));
            properties.put("$screen_height", mDeviceInfo.get("$screen_height"));
            properties.put("$screen_width", mDeviceInfo.get("$screen_width"));
            String networkType = SensorsDataUtils.networkType(mContext);
            properties.put("$wifi", networkType.equals("WIFI"));
            properties.put("$network_type", networkType);
            properties.put("$carrier", mDeviceInfo.get("$carrier"));
            properties.put("$is_first_day", isFirstDay());
            if (mDeviceInfo.containsKey("$device_id")) {
                properties.put("$device_id", mDeviceInfo.get("$device_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return properties;
    }

    /**
     * 设置当前 serverUrl
     * @param serverUrl 当前 serverUrl
     */
    @Override
    public void setServerUrl(String serverUrl) {
        try {
            mOriginServerUrl = serverUrl;
            if (TextUtils.isEmpty(serverUrl) || mDebugMode == DebugMode.DEBUG_OFF) {
                mServerUrl = serverUrl;
            } else {
                Uri serverURI = Uri.parse(serverUrl);

                int pathPrefix = serverURI.getPath().lastIndexOf('/');
                String hostServer = serverURI.getHost();
                if (!TextUtils.isEmpty(hostServer) && hostServer.contains("_")) {
                    Log.i(TAG, "Server url " + serverUrl + " contains '_' is not recommend，" +
                            "see details: https://en.wikipedia.org/wiki/Hostname");
                }
                if (pathPrefix != -1) {
                    String newPath = serverURI.getPath().substring(0, pathPrefix) + "/debug";

                    // 将 URI Path 中末尾的部分替换成 '/debug'
                    mServerUrl = serverURI.buildUpon().path(newPath).build().toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置是否开启 log
     * @param enable boolean
     */
    @Override
    public void enableLog(boolean enable) {
        this.ENABLE_LOG = enable;
    }

    @Override
    public long getMaxCacheSize() {
        return mMaxCacheSize;
    }

    /**
     * 设置本地缓存上限值，单位 byte，默认为 32MB：32 * 1024 * 1024
     * @param maxCacheSize 单位 byte
     */
    @Override
    public void setMaxCacheSize(long maxCacheSize) {
        if (maxCacheSize > 0) {
            //防止设置的值太小导致事件丢失
            if (maxCacheSize < 16 * 1024 * 1024) {
                maxCacheSize = 16 * 1024 * 1024;
            }
            this.mMaxCacheSize = maxCacheSize;
        }
    }

    /**
     * 设置 flush 时网络发送策略，默认 3G、4G、WI-FI 环境下都会尝试 flush
     * @param networkType int 网络类型
     */
    @Override
    public void setFlushNetworkPolicy(int networkType) {
        mFlushNetworkPolicy = networkType;
    }

    /**
     * 两次数据发送的最小时间间隔，单位毫秒
     *
     * 默认值为15 * 1000毫秒
     * 在每次调用track、signUp以及profileSet等接口的时候，都会检查如下条件，以判断是否向服务器上传数据:
     *
     * 1. 是否是WIFI/3G/4G网络条件
     * 2. 是否满足发送条件之一:
     * 1) 与上次发送的时间间隔是否大于 flushInterval
     * 2) 本地缓存日志数目是否大于 flushBulkSize
     *
     * 如果满足这两个条件，则向服务器发送一次数据；如果不满足，则把数据加入到队列中，等待下次检查时把整个队列的内
     * 容一并发送。需要注意的是，为了避免占用过多存储，队列最多只缓存20MB数据。
     *
     * @return 返回时间间隔，单位毫秒
     */
    @Override
    public int getFlushInterval() {
        return mFlushInterval;
    }

    /**
     * 设置两次数据发送的最小时间间隔
     *
     * @param flushInterval 时间间隔，单位毫秒
     */
    @Override
    public void setFlushInterval(int flushInterval) {
        if (flushInterval < 5 * 1000) {
            flushInterval = 5 * 1000;
        }
        mFlushInterval = flushInterval;
    }

    /**
     * 返回本地缓存日志的最大条目数
     *
     * 默认值为100条
     * 在每次调用track、signUp以及profileSet等接口的时候，都会检查如下条件，以判断是否向服务器上传数据:
     *
     * 1. 是否是WIFI/3G/4G网络条件
     * 2. 是否满足发送条件之一:
     * 1) 与上次发送的时间间隔是否大于 flushInterval
     * 2) 本地缓存日志数目是否大于 flushBulkSize
     *
     * 如果满足这两个条件，则向服务器发送一次数据；如果不满足，则把数据加入到队列中，等待下次检查时把整个队列的内
     * 容一并发送。需要注意的是，为了避免占用过多存储，队列最多只缓存32MB数据。
     *
     * @return 返回本地缓存日志的最大条目数
     */
    @Override
    public int getFlushBulkSize() {
        return mFlushBulkSize;
    }

    /**
     * 设置本地缓存日志的最大条目数
     *
     * @param flushBulkSize 缓存数目
     */
    @Override
    public void setFlushBulkSize(int flushBulkSize) {
        mFlushBulkSize = flushBulkSize;
    }

    /**
     * 设置 App 切换到后台与下次事件的事件间隔
     *
     * 默认值为 30*1000 毫秒
     *
     * 若 App 在后台超过设定事件，则认为当前 Session 结束，发送 $AppEnd 事件
     *
     */
    @Override
    public void setSessionIntervalTime(int sessionIntervalTime) {
        if(mDbAdapter == null){
            SALog.i(TAG, "The static method sharedInstance(context, serverURL, debugMode) should be called before calling sharedInstance()");
            return;
        }

        if (sessionIntervalTime < 10 * 1000 || sessionIntervalTime > 5 * 60 * 1000) {
            Log.i(TAG, "SessionIntervalTime:" + sessionIntervalTime + " is invalid, session interval time is between 10s and 300s.");
            return;
        }

        mDbAdapter.commitSessionIntervalTime(sessionIntervalTime);
    }

    /**
     * 设置 App 切换到后台与下次事件的事件间隔
     *
     * 默认值为 30*1000 毫秒
     *
     * 若 App 在后台超过设定事件，则认为当前 Session 结束，发送 $AppEnd 事件
     *
     * @return 返回设置的 SessionIntervalTime ，默认是 30 * 1000 毫秒
     */
    @Override
    public int getSessionIntervalTime() {
        if(mDbAdapter == null){
            SALog.i(TAG, "The static method sharedInstance(context, serverURL, debugMode) should be called before calling sharedInstance()");
            return 30 * 1000;
        }

        return mDbAdapter.getSessionIntervalTime();
    }

    /**
     * 更新 GPS 位置信息
     * @param latitude 纬度
     * @param longitude 经度
     */
    @Override
    public void setGPSLocation(double latitude, double longitude) {
        try {
            if (mGPSLocation == null) {
                mGPSLocation = new SensorsDataGPSLocation();
            }

            mGPSLocation.setLatitude((long)(latitude * Math.pow(10, 6)));
            mGPSLocation.setLongitude((long)(longitude * Math.pow(10, 6)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 清楚 GPS 位置信息
     */
    @Override
    public void clearGPSLocation() {
        mGPSLocation = null;
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
            e.printStackTrace();
        }
    }

    @Override
    public void resumeTrackScreenOrientation() {
        try {
            if (mOrientationDetector != null) {
                mOrientationDetector.enable();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopTrackScreenOrientation() {
        try {
            if (mOrientationDetector != null) {
                mOrientationDetector.disable();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getScreenOrientation() {
        try {
            if (mOrientationDetector != null) {
                return mOrientationDetector.getOrientation();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setCookie(String cookie, boolean encode) {
        try {
            if (encode) {
                this.mCookie = URLEncoder.encode(cookie, "UTF-8");
            } else {
                this.mCookie = cookie;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getCookie(boolean decode) {
        try {
            if (decode) {
                return URLDecoder.decode(this.mCookie, "UTF-8");
            } else {
                return this.mCookie;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * 打开 SDK 自动追踪
     *
     * 该功能自动追踪 App 的一些行为，例如 SDK 初始化、App 启动（$AppStart） / 关闭（$AppEnd）、
     * 进入页面（$AppViewScreen）等等，具体信息请参考文档:
     * https://sensorsdata.cn/manual/android_sdk.html
     *
     * 该功能仅在 API 14 及以上版本中生效，默认关闭
     */
    @Deprecated
    @Override
    public void enableAutoTrack() {
        List<AutoTrackEventType> eventTypeList = new ArrayList<>();
        eventTypeList.add(AutoTrackEventType.APP_START);
        eventTypeList.add(AutoTrackEventType.APP_END);
        eventTypeList.add(AutoTrackEventType.APP_VIEW_SCREEN);
        enableAutoTrack(eventTypeList);
    }

    /**
     * 打开 SDK 自动追踪
     *
     * 该功能自动追踪 App 的一些行为，指定哪些 AutoTrack 事件被追踪，具体信息请参考文档:
     * https://sensorsdata.cn/manual/android_sdk.html
     *
     * 该功能仅在 API 14 及以上版本中生效，默认关闭
     *
     * @param eventTypeList 开启 AutoTrack 的事件列表
     */
    @Override
    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList) {
        try {
            mAutoTrack = true;
            if (eventTypeList == null) {
                eventTypeList = new ArrayList<>();
            }

            mAutoTrackEventTypeList.clear();
            mAutoTrackEventTypeList.addAll(eventTypeList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭 AutoTrack 中的部分事件
     * @param eventTypeList AutoTrackEventType 类型 List
     */
    @Override
    public void disableAutoTrack(List<AutoTrackEventType> eventTypeList) {
        if (eventTypeList == null || eventTypeList.size() == 0) {
            return;
        }

        if (mAutoTrackEventTypeList == null) {
            return;
        }
        try {
            mAutoTrackEventTypeList.removeAll(eventTypeList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mAutoTrackEventTypeList.size() == 0) {
            mAutoTrack = false;
        }
    }

    /**
     * 关闭 AutoTrack 中的某个事件
     * @param autoTrackEventType AutoTrackEventType 类型
     */
    @Override
    public void disableAutoTrack(AutoTrackEventType autoTrackEventType) {
        if (autoTrackEventType == null) {
            return;
        }

        if (mAutoTrackEventTypeList == null) {
            return;
        }

        try {
            if (mAutoTrackEventTypeList.contains(autoTrackEventType)) {
                mAutoTrackEventTypeList.remove(autoTrackEventType);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mAutoTrackEventTypeList.size() == 0) {
            mAutoTrack = false;
        }
    }

    /**
     * 自动收集 App Crash 日志，该功能默认是关闭的
     */
    @Override
    public void trackAppCrash() {
        SensorsDataExceptionHandler.init();
    }

    // Package-level access. Used (at least) by GCMReceiver
    // when OS-level events occur.
    /* package */ interface InstanceProcessor {
        public void process(SensorsDataAPI m);
    }

    /* package */ static void allInstances(InstanceProcessor processor) {
        synchronized (sInstanceMap) {
            for (final SensorsDataAPI instance : sInstanceMap.values()) {
                processor.process(instance);
            }
        }
    }

    /**
     * 是否开启 AutoTrack
     * @return true: 开启 AutoTrack; false：没有开启 AutoTrack
     */
    @Override
    public boolean isAutoTrackEnabled() {
        if (isSDKDisabled()) {
            return false;
        }

        if (mSDKRemoteConfig != null) {
            if (mSDKRemoteConfig.getAutoTrackMode() == 0) {
                return false;
            } else if (mSDKRemoteConfig.getAutoTrackMode() > 0) {
                return true;
            }
        }

        return mAutoTrack;
    }

    @Override
    public boolean isButterknifeOnClickEnabled() {
        return mEnableButterknifeOnClick;
    }

    /**
     * 是否开启自动追踪 Fragment 的 $AppViewScreen 事件
     * 默认不开启
     */
    @Override
    public void trackFragmentAppViewScreen() {
        this.mTrackFragmentAppViewScreen = true;
    }

    @Override
    public boolean isTrackFragmentAppViewScreenEnabled() {
        return this.mTrackFragmentAppViewScreen;
    }

    /**
     * 开启 AutoTrack 支持 React Native
     */
    @Override
    public void enableReactNativeAutoTrack() {
        this.mEnableReactNativeAutoTrack = true;
    }

    @Override
    public boolean isReactNativeAutoTrackEnabled() {
        return this.mEnableReactNativeAutoTrack;
    }

    /**
     * 向WebView注入本地方法, 将distinctId传递给当前的WebView
     *
     * @param webView 当前WebView
     * @param isSupportJellyBean 是否支持API level 16及以下的版本。
     * 因为API level 16及以下的版本, addJavascriptInterface有安全漏洞,请谨慎使用
     */
    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    public void showUpWebView(WebView webView, boolean isSupportJellyBean) {
        showUpWebView(webView, isSupportJellyBean, null);
    }

    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    public void showUpWebView(WebView webView, boolean isSupportJellyBean, boolean enableVerify) {
        showUpWebView(webView, null, isSupportJellyBean, enableVerify);
    }

    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    public void showUpWebView(WebView webView, JSONObject properties, boolean isSupportJellyBean, boolean enableVerify) {
        if (Build.VERSION.SDK_INT < 17 && !isSupportJellyBean) {
            SALog.d(TAG, "For applications targeted to API level JELLY_BEAN or below, this feature NOT SUPPORTED");
            return;
        }

        if (webView != null) {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.addJavascriptInterface(new AppWebViewInterface(mContext, properties, enableVerify), "SensorsData_APP_JS_Bridge");
        }
    }

    /**
     * 向WebView注入本地方法, 将distinctId传递给当前的WebView
     *
     * @param webView 当前WebView
     * @param isSupportJellyBean 是否支持API level 16及以下的版本。
     *                           因为API level 16及以下的版本, addJavascriptInterface有安全漏洞,请谨慎使用
     * @param properties 用户自定义属性
     */
    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    public void showUpWebView(WebView webView, boolean isSupportJellyBean, JSONObject properties) {
        showUpWebView(webView, properties, isSupportJellyBean, false);
    }

    @Override
    public void showUpX5WebView(Object x5WebView, boolean enableVerify) {
        try {
            if (x5WebView == null) {
                return;
            }

            Class clazz = x5WebView.getClass();
            Method addJavascriptInterface = clazz.getMethod("addJavascriptInterface", Object.class, String.class);
            if (addJavascriptInterface == null) {
                return;
            }

            addJavascriptInterface.invoke(x5WebView, new AppWebViewInterface(mContext, null, enableVerify), "SensorsData_APP_JS_Bridge");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void showUpX5WebView(Object x5WebView) {
        showUpX5WebView(x5WebView, false);
    }

    /**
     * 指定哪些 activity 不被AutoTrack
     *
     * 指定activity的格式为：activity.getClass().getCanonicalName()
     *
     * @param activitiesList  activity列表
     */
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

    /**
     * 恢复不被 AutoTrack 的 activity
     * @param activitiesList
     */
    @Override
    public void resumeAutoTrackActivities(List<Class<?>> activitiesList) {
        if (activitiesList == null || activitiesList.size() == 0) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null){
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
            e.printStackTrace();
        }
    }

    /**
     * 指定某个 activity 不被 AutoTrack
     * @param activity Activity
     */
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
            e.printStackTrace();
        }
    }

    /**
     * 恢复不被 AutoTrack 的 activity
     * @param activity
     */
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
            e.printStackTrace();
        }
    }

    /**
     * 指定 fragment 被 AutoTrack 采集
     * @param fragment Fragment
     */
    @Override
    public void enableAutoTrackFragment(Class<?> fragment) {
        if (fragment == null) {
            return;
        }

        if (mAutoTrackFragments == null) {
            mAutoTrackFragments = new CopyOnWriteArraySet<>();
        }

        try {
            mAutoTrackFragments.add(fragment.hashCode());
            mAutoTrackFragments.add(fragment.getCanonicalName().hashCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 指定 fragments 被 AutoTrack 采集
     * @param fragmentsList Fragment 集合
     */
    @Override
    public void enableAutoTrackFragments(List<Class<?>> fragmentsList) {
        if (fragmentsList == null || fragmentsList.size() == 0) {
            return;
        }

        if (mAutoTrackFragments == null) {
            mAutoTrackFragments = new CopyOnWriteArraySet<>();
        }

        try {
            for (Class fragment : fragmentsList) {
                mAutoTrackFragments.add(fragment.hashCode());
                mAutoTrackFragments.add(fragment.getCanonicalName().hashCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 指定 fragment 被 AutoTrack 采集
     * @param fragmentName,Fragment 名称，使用 包名 + 类名，建议直接通过 Class.getCanonicalName 获取
     */
    @Override
    public void enableAutoTrackFragment(String fragmentName) {
        if (TextUtils.isEmpty(fragmentName)) {
            return;
        }

        if (mAutoTrackFragments == null) {
            mAutoTrackFragments = new CopyOnWriteArraySet<>();
        }

        try {
            mAutoTrackFragments.add(fragmentName.hashCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断 AutoTrack 时，某个 Activity 的 $AppViewScreen 是否被过滤
     * 如果过滤的话，会过滤掉 Activity 的 $AppViewScreen 事件
     * @param activity Activity
     * @return Activity 是否被过滤
     */
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

        if (activity.getAnnotation(SensorsDataIgnoreTrackAppViewScreen.class) != null) {
            return true;
        }

        return false;
    }

    /**
     * 判断 AutoTrack 时，某个 Activity 的 $AppViewScreen 是否被采集
     * @param fragment Fragment
     * @return
     */
    @Override
    public boolean isFragmentAutoTrackAppViewScreen(Class<?> fragment) {
        if (fragment == null) {
            return false;
        }
        try {
            if (mAutoTrackFragments != null && mAutoTrackFragments.size() > 0) {
                if (mAutoTrackFragments.contains(fragment.hashCode())
                        || mAutoTrackFragments.contains(fragment.getCanonicalName().hashCode())) {
                    return true;
                } else {
                    return false;
                }
            }

            if (fragment.getClass().getAnnotation(SensorsDataIgnoreTrackAppViewScreen.class) != null) {
                return false;
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        return true;
    }

    /**
     * 判断 AutoTrack 时，某个 Activity 的 $AppClick 是否被过滤
     * 如果过滤的话，会过滤掉 Activity 的 $AppClick 事件
     * @param activity Activity
     * @return Activity 是否被过滤
     */
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

        if (activity.getAnnotation(SensorsDataIgnoreTrackAppClick.class) != null) {
            return true;
        }

        return false;
    }

    private Set<AutoTrackEventType> mAutoTrackEventTypeList;

    /**
     * 过滤掉 AutoTrack 的某个事件类型
     * @param autoTrackEventType AutoTrackEventType
     */
    @Deprecated
    @Override
    public void ignoreAutoTrackEventType(AutoTrackEventType autoTrackEventType) {
        if (autoTrackEventType == null) {
            return;
        }

        if (mAutoTrackEventTypeList.contains(autoTrackEventType)) {
            mAutoTrackEventTypeList.remove(autoTrackEventType);
        }
    }

    /**
     * 过滤掉 AutoTrack 的某些事件类型
     * @param eventTypeList AutoTrackEventType List
     */
    @Deprecated
    @Override
    public void ignoreAutoTrackEventType(List<AutoTrackEventType> eventTypeList) {
        if (eventTypeList == null) {
            return;
        }

        for (AutoTrackEventType eventType: eventTypeList) {
            if (eventType != null && mAutoTrackEventTypeList.contains(eventType)) {
                mAutoTrackEventTypeList.remove(eventType);
            }
        }
    }

    /**
     * 判断 某个 AutoTrackEventType 是否被忽略
     * @param eventType AutoTrackEventType
     * @return true 被忽略; false 没有被忽略
     */
    @Override
    public boolean isAutoTrackEventTypeIgnored(AutoTrackEventType eventType) {
        if (mSDKRemoteConfig != null) {
            if (mSDKRemoteConfig.getAutoTrackMode() != -1) {
                if (mSDKRemoteConfig.getAutoTrackMode() == 0) {
                    return true;
                }
                return mSDKRemoteConfig.isAutoTrackEventTypeIgnored(eventType);
            }
        }
        if (eventType != null  && !mAutoTrackEventTypeList.contains(eventType)) {
            return true;
        }
        return false;
    }

    /**
     * 设置界面元素ID
     *
     * @param view   要设置的View
     * @param viewID String 给这个View的ID
     */
    @Override
    public void setViewID(View view, String viewID) {
        if (view != null && !TextUtils.isEmpty(viewID)) {
            view.setTag(R.id.sensors_analytics_tag_view_id, viewID);
        }
    }

    /**
     * 设置界面元素ID
     *
     * @param view   要设置的View
     * @param viewID String 给这个View的ID
     */
    @Override
    public void setViewID(android.app.Dialog view, String viewID) {
        try {
            if (view != null && !TextUtils.isEmpty(viewID)) {
                if (view.getWindow() != null) {
                    view.getWindow().getDecorView().setTag(R.id.sensors_analytics_tag_view_id, viewID);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置界面元素ID
     *
     * @param view   要设置的View
     * @param viewID String 给这个View的ID
     */
    @Override
    public void setViewID(android.support.v7.app.AlertDialog view, String viewID) {
        try {
            if (view != null && !TextUtils.isEmpty(viewID)) {
                if (view.getWindow() != null) {
                    view.getWindow().getDecorView().setTag(R.id.sensors_analytics_tag_view_id, viewID);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置 View 所属 Activity
     *
     * @param view   要设置的View
     * @param activity Activity View 所属 Activity
     */
    @Override
    public void setViewActivity(View view, Activity activity) {
        try {
            if (view == null || activity == null) {
                return;
            }
            view.setTag(R.id.sensors_analytics_tag_view_activity, activity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置 View 所属 Fragment 名称
     *
     * @param view   要设置的View
     * @param fragmentName String View 所属 Fragment 名称
     */
    @Override
    public void setViewFragmentName(View view, String fragmentName) {
        try {
            if (view == null || TextUtils.isEmpty(fragmentName)) {
                return;
            }
            view.setTag(R.id.sensors_analytics_tag_view_fragment_name2, fragmentName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 忽略View
     *
     * @param view 要忽略的View
     */
    @Override
    public void ignoreView(View view) {
        if (view != null) {
            view.setTag(R.id.sensors_analytics_tag_view_ignored, "1");
        }
    }

    @Override
    public void ignoreView(View view, boolean ignore) {
        if (view != null) {
            view.setTag(R.id.sensors_analytics_tag_view_ignored, ignore ? "1": "0");
        }
    }

    /**
     * 设置View属性
     *
     * @param view       要设置的View
     * @param properties 要设置的View的属性
     */
    @Override
    public void setViewProperties(View view, JSONObject properties) {
        if (view == null || properties == null) {
            return;
        }

        view.setTag(R.id.sensors_analytics_tag_view_properties, properties);
    }

    private List<Class> mIgnoredViewTypeList = new ArrayList<>();

    @Override
    public List<Class> getIgnoredViewTypeList() {
        if (mIgnoredViewTypeList == null) {
            mIgnoredViewTypeList = new ArrayList<>();
        }

        return mIgnoredViewTypeList;
    }

    /**
     * 返回设置 AutoTrack 的 Fragments 集合，如果没有设置则返回 null.
     * @return
     */
    @Override
    public Set<Integer> getAutoTrackFragments() {
        return mAutoTrackFragments;
    }

    /**
     * 忽略某一类型的 View
     *
     * @param viewType Class
     */
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
    public boolean isHeatMapActivity(Class<?> activity) {
        try {
            if (mHeatMapActivities.size() == 0) {
                return true;
            }

            if (mHeatMapActivities.contains(activity.hashCode())) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    @Override
    public void addHeatMapActivities(List<Class<?>> activitiesList) {
        try {
            if (activitiesList == null || activitiesList.size() == 0) {
                return;
            }

            for (Class<?> activity: activitiesList) {
                if (activity != null) {
                    if (!mHeatMapActivities.contains(activity.hashCode())) {
                        mHeatMapActivities.add(activity.hashCode());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isHeatMapEnabled() {
        return mHeatMapEnabled;
    }

    protected boolean isAppHeatMapConfirmDialogEnabled() {
        return mEnableAppHeatMapConfirmDialog;
    }

    @Override
    public void enableAppHeatMapConfirmDialog(boolean enable) {
        this.mEnableAppHeatMapConfirmDialog = enable;
    }

    /**
     * 开启 HeatMap，$AppClick 事件将会采集控件的 viewPath
     */
    @Override
    public void enableHeatMap() {
        mHeatMapEnabled = true;
    }

    /**
     * 获取当前用户的distinctId
     *
     * 若调用前未调用 {@link #identify(String)} 设置用户的 distinctId，SDK 会调用 {@link java.util.UUID} 随机生成
     * UUID，作为用户的 distinctId
     *
     * 该方法已不推荐使用，请参考 {@link #getAnonymousId()}
     *
     * @return 当前用户的distinctId
     */
    @Deprecated
    @Override
    public String getDistinctId() {
        synchronized (mDistinctId) {
            return mDistinctId.get();
        }
    }

    /**
     * 获取当前用户的匿名id
     *
     * 若调用前未调用 {@link #identify(String)} 设置用户的匿名id，SDK 会调用 {@link java.util.UUID} 随机生成
     * UUID，作为用户的匿名id
     *
     * @return 当前用户的匿名id
     */
    @Override
    public String getAnonymousId() {
        synchronized (mDistinctId) {
            return mDistinctId.get();
        }
    }

    /**
     * 重置默认匿名id
     */
    @Override
    public void resetAnonymousId() {
        synchronized (mDistinctId) {
            String androidId = SensorsDataUtils.getAndroidID(mContext);
            if (SensorsDataUtils.isValidAndroidId(androidId)) {
                mDistinctId.commit(androidId);
                return;
            }
            mDistinctId.commit(UUID.randomUUID().toString());
        }
    }

    /**
     * 获取当前用户的 loginId
     *
     * 若调用前未调用 {@link #login(String)} 设置用户的 loginId，会返回null
     *
     * @return 当前用户的 loginId
     */
    @Override
    public String getLoginId() {
        synchronized (mLoginId) {
            return mLoginId.get();
        }
    }

    /**
     * 设置当前用户的distinctId。一般情况下，如果是一个注册用户，则应该使用注册系统内
     * 的user_id，如果是个未注册用户，则可以选择一个不会重复的匿名ID，如设备ID等，如果
     * 客户没有调用identify，则使用SDK自动生成的匿名ID
     *
     * @param distinctId 当前用户的distinctId，仅接受数字、下划线和大小写字母
     */
    @Override
    public void identify(final String distinctId) {
        try {
            assertDistinctId(distinctId);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (mDistinctId) {
                        mDistinctId.commit(distinctId);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 登录，设置当前用户的 loginId
     *
     * @param loginId 当前用户的 loginId，不能为空，且长度不能大于255
     */
    @Override
    public void login(final String loginId) {
        login(loginId, null);
    }

    /**
     * 登录，设置当前用户的 loginId
     *
     * @param loginId 当前用户的 loginId，不能为空，且长度不能大于255
     * @param properties 用户登录属性
     */
    @Override
    public void login(final String loginId , final JSONObject properties) {
        try {
            assertDistinctId(loginId);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (mLoginId) {
                        if (!loginId.equals(mLoginId.get())) {
                            mLoginId.commit(loginId);
                            if (!loginId.equals(getAnonymousId())) {
                                trackEvent(EventType.TRACK_SIGNUP, "$SignUp", properties, getAnonymousId());
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 注销，清空当前用户的 loginId
     */
    @Override
    public void logout() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (mLoginId) {
                        mLoginId.commit(null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 记录第一次登录行为
     *
     * 这个接口是一个较为复杂的功能，请在使用前先阅读相关说明:
     * http://www.sensorsdata.cn/manual/track_signup.html
     * 并在必要时联系我们的技术支持人员。
     * 该方法已不推荐使用，可以具体参考 {@link #login(String)} 方法
     *
     * @param newDistinctId 用户完成注册后生成的注册ID
     * @param properties    事件的属性
     */
    @Deprecated
    @Override
    public void trackSignUp(final String newDistinctId, final JSONObject properties) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    String originalDistinctId = getDistinctId();

                    synchronized (mDistinctId) {
                        mDistinctId.commit(newDistinctId);
                    }

                    trackEvent(EventType.TRACK_SIGNUP, "$SignUp", properties, originalDistinctId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 与 {@link #trackSignUp(String, org.json.JSONObject)} 类似，无事件属性
     *
     * 这个接口是一个较为复杂的功能，请在使用前先阅读相关说明:
     * http://www.sensorsdata.cn/manual/track_signup.html，
     * 并在必要时联系我们的技术支持人员。
     * 该方法已不推荐使用，可以具体参考 {@link #login(String)} 方法
     *
     * @param newDistinctId 用户完成注册后生成的注册ID
     */
    @Deprecated
    @Override
    public void trackSignUp(final String newDistinctId) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    String originalDistinctId = getDistinctId();
                    synchronized (mDistinctId) {
                        mDistinctId.commit(newDistinctId);
                    }

                    trackEvent(EventType.TRACK_SIGNUP, "$SignUp", null, originalDistinctId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 用于在 App 首次启动时追踪渠道来源，并设置追踪渠道事件的属性。
     *
     * 这是 Sensors Analytics 进阶功能，请参考文档 https://sensorsdata.cn/manual/track_installation.html
     *
     * @param eventName  渠道追踪事件的名称
     * @param properties 渠道追踪事件的属性
     * @param disableCallback 是否关闭这次渠道匹配的回调请求
     */
    @Override
    public void trackInstallation(final String eventName, final JSONObject properties, final boolean disableCallback) {
        //只在主进程触发 trackInstallation
        final JSONObject _properties;
        if (properties != null) {
            _properties = properties;
        } else {
            _properties = new JSONObject();
        }

        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!SensorsDataUtils.isMainProcess(mContext, mMainProcessName)) {
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    boolean firstTrackInstallation;
                    if (disableCallback) {
                        firstTrackInstallation = mFirstTrackInstallationWithCallback.get();
                    } else {
                        firstTrackInstallation = mFirstTrackInstallation.get();
                    }
                    if (firstTrackInstallation) {
                        try {
                            if (!SensorsDataUtils.hasUtmProperties(_properties)) {
                                Map<String, String> utmMap = new HashMap<>();
                                utmMap.put("SENSORS_ANALYTICS_UTM_SOURCE", "$utm_source");
                                utmMap.put("SENSORS_ANALYTICS_UTM_MEDIUM", "$utm_medium");
                                utmMap.put("SENSORS_ANALYTICS_UTM_TERM", "$utm_term");
                                utmMap.put("SENSORS_ANALYTICS_UTM_CONTENT", "$utm_content");
                                utmMap.put("SENSORS_ANALYTICS_UTM_CAMPAIGN", "$utm_campaign");

                                for (Map.Entry<String, String> entry : utmMap.entrySet()) {
                                    if (entry != null) {
                                        String utmValue = SensorsDataUtils.getApplicationMetaData(mContext, entry.getKey());
                                        if (!TextUtils.isEmpty(utmValue)) {
                                            _properties.put(entry.getValue(), utmValue);
                                        }
                                    }
                                }
                            }

                            if (!SensorsDataUtils.hasUtmProperties(_properties)) {
                                String installSource = String.format("android_id=%s##imei=%s##mac=%s",
                                        SensorsDataUtils.getAndroidID(mContext),
                                        SensorsDataUtils.getIMEI(mContext),
                                        SensorsDataUtils.getMacAddress(mContext));
                                _properties.put("$ios_install_source", installSource);
                            }

                            if (disableCallback) {
                                _properties.put("$ios_install_disable_callback", disableCallback);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // 先发送 track
                        trackEvent(EventType.TRACK, eventName, _properties, null);

                        // 再发送 profile_set_once
                        JSONObject profileProperties = new JSONObject();
                        if (_properties != null) {
                            profileProperties = new JSONObject(_properties.toString());
                        }
                        profileProperties.put("$first_visit_time", new java.util.Date());
                        trackEvent(EventType.PROFILE_SET_ONCE, null, profileProperties, null);

                        if (disableCallback) {
                            mFirstTrackInstallationWithCallback.commit(false);
                        } else {
                            mFirstTrackInstallation.commit(false);
                        }
                    }
                    flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 用于在 App 首次启动时追踪渠道来源，并设置追踪渠道事件的属性。
     *
     * 这是 Sensors Analytics 进阶功能，请参考文档 https://sensorsdata.cn/manual/track_installation.html
     *
     * @param eventName  渠道追踪事件的名称
     * @param properties 渠道追踪事件的属性
     */
    @Override
    public void trackInstallation(String eventName, JSONObject properties) {
        trackInstallation(eventName, properties, false);
    }

    /**
     * 用于在 App 首次启动时追踪渠道来源，并设置追踪渠道事件的属性。
     *
     * 这是 Sensors Analytics 进阶功能，请参考文档 https://sensorsdata.cn/manual/track_installation.html
     *
     * @param eventName  渠道追踪事件的名称
     */
    @Override
    public void trackInstallation(String eventName) {
        trackInstallation(eventName, null, false);
    }

    /**
     * 调用track接口，追踪一个带有属性的事件
     *
     * @param eventName  事件的名称
     * @param properties 事件的属性
     */
    @Override
    public void track(final String eventName, final JSONObject properties) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.TRACK, eventName, properties, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 与 {@link #track(String, org.json.JSONObject)} 类似，无事件属性
     *
     * @param eventName 事件的名称
     */
    @Override
    public void track(final String eventName) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.TRACK, eventName, null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 初始化事件的计时器，默认计时单位为毫秒。
     *
     * 详细用法请参考 trackTimer(String, TimeUnit)
     *
     * @param eventName 事件的名称
     */
    @Deprecated
    @Override
    public void trackTimer(final String eventName) {
        trackTimer(eventName, TimeUnit.MILLISECONDS);
    }

    /**
     * 初始化事件的计时器。
     *
     * 若需要统计某个事件的持续时间，先在事件开始时调用 trackTimer("Event") 记录事件开始时间，该方法并不会真正发
     * 送事件；随后在事件结束时，调用 track("Event", properties)，SDK 会追踪 "Event" 事件，并自动将事件持续时
     * 间记录在事件属性 "event_duration" 中。
     *
     * 多次调用 trackTimer("Event") 时，事件 "Event" 的开始时间以最后一次调用时为准。
     *
     * @param eventName 事件的名称
     * @param timeUnit  计时结果的时间单位
     */
    @Deprecated
    @Override
    public void trackTimer(final String eventName, final TimeUnit timeUnit) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    assertKey(eventName);
                    synchronized (mTrackTimer) {
                        mTrackTimer.put(eventName, new EventTimer(timeUnit));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    /**
     * 初始化事件的计时器。
     *
     * 若需要统计某个事件的持续时间，先在事件开始时调用 trackTimer("Event") 记录事件开始时间，该方法并不会真正发
     * 送事件；随后在事件结束时，调用 track("Event", properties)，SDK 会追踪 "Event" 事件，并自动将事件持续时
     * 间记录在事件属性 "event_duration" 中。
     *
     * 多次调用 trackTimer("Event") 时，事件 "Event" 的开始时间以最后一次调用时为准。
     *
     * @param eventName 事件的名称
     * @param eventTimer 自定义事件计时器
     */
    @Deprecated
    @Override
    public void trackTimer(final String eventName, final EventTimer eventTimer) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    assertKey(eventName);
                    synchronized (mTrackTimer) {
                        mTrackTimer.put(eventName, eventTimer);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    /**
     * 删除指定时间的计时器
     *
     * @param eventName 事件名称
     */
    @Override
    public void removeTimer(final String eventName) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    assertKey(eventName);
                    synchronized (mTrackTimer) {
                        mTrackTimer.remove(eventName);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    /**
     * 初始化事件的计时器，计时单位为秒。
     *
     * @param eventName 事件的名称
     */
    @Override
    public void trackTimerStart(String eventName) {
        trackTimerBegin(eventName, TimeUnit.SECONDS);
    }

    /**
     * 初始化事件的计时器，默认计时单位为毫秒。
     *
     * 详细用法请参考 trackTimerBegin(String, TimeUnit)
     *
     * @param eventName 事件的名称
     */
    @Override
    @Deprecated
    public void trackTimerBegin(final String eventName) {
        trackTimer(eventName);
    }

    /**
     * 初始化事件的计时器。
     *
     * 若需要统计某个事件的持续时间，先在事件开始时调用 trackTimerBegin("Event") 记录事件开始时间，该方法并不会真正发
     * 送事件；随后在事件结束时，调用 track("Event", properties)，SDK 会追踪 "Event" 事件，并自动将事件持续时
     * 间记录在事件属性 "event_duration" 中。
     *
     * 多次调用 trackTimerBegin("Event") 时，事件 "Event" 的开始时间以最后一次调用时为准。
     *
     * @param eventName 事件的名称
     * @param timeUnit  计时结果的时间单位
     */
    @Override
    @Deprecated
    public void trackTimerBegin(final String eventName, final TimeUnit timeUnit) {
        trackTimer(eventName, timeUnit);
    }

    /**
     * 停止事件计时器
     * @param eventName 事件的名称
     * @param properties 事件的属性
     */
    @Override
    public void trackTimerEnd(final String eventName, final JSONObject properties) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.TRACK, eventName, properties, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 停止事件计时器
     * @param eventName 事件的名称
     */
    @Override
    public void trackTimerEnd(final String eventName) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.TRACK, eventName, null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 清除所有事件计时器
     */
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
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 获取LastScreenUrl
     * @return String
     */
    @Override
    public String getLastScreenUrl() {
        return mLastScreenUrl;
    }

    /**
     * App 退出或进到后台时清空 referrer，默认情况下不清空
     */
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
    public String getMainProcessName() {
        return mMainProcessName;
    }

    /**
     * 获取LastScreenTrackProperties
     * @return JSONObject
     */
    @Override
    public JSONObject getLastScreenTrackProperties() {
        return mLastScreenTrackProperties;
    }

    /**
     * Track 进入页面事件 ($AppViewScreen)
     * @param url String
     * @param properties JSONObject
     */
    @Override
    public void trackViewScreen(final String url, final JSONObject properties) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!TextUtils.isEmpty(url) || properties != null) {
                        JSONObject trackProperties = new JSONObject();
                        mLastScreenTrackProperties = properties;

                        if (!TextUtils.isEmpty(mLastScreenUrl)) {
                            trackProperties.put("$referrer", mLastScreenUrl);
                        }

                        trackProperties.put("$url", url);
                        mLastScreenUrl = url;
                        if (properties != null) {
                            SensorsDataUtils.mergeJSONObject(properties, trackProperties);
                        }
                        track("$AppViewScreen", trackProperties);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Track Activity 进入页面事件($AppViewScreen)
     * @param activity activity Activity，当前 Activity
     */
    @Override
    public void trackViewScreen(final Activity activity) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (activity == null) {
                        return;
                    }

                    JSONObject properties = new JSONObject();
                    properties.put("$screen_name", activity.getClass().getCanonicalName());
                    SensorsDataUtils.getScreenNameAndTitleFromActivity(properties, activity);

                    if (activity instanceof ScreenAutoTracker) {
                        ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) activity;

                        String screenUrl = screenAutoTracker.getScreenUrl();
                        JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                        if (otherProperties != null) {
                            SensorsDataUtils.mergeJSONObject(otherProperties, properties);
                        }

                        trackViewScreen(screenUrl, properties);
                    } else {
                        track("$AppViewScreen", properties);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void trackViewScreen(final android.app.Fragment fragment) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (fragment == null) {
                        return;
                    }

                    JSONObject properties = new JSONObject();
                    String fragmentName = fragment.getClass().getCanonicalName();
                    String screenName = fragmentName;

                    if (Build.VERSION.SDK_INT >= 11) {
                        Activity activity = fragment.getActivity();
                        if (activity != null) {
                            String activityTitle = SensorsDataUtils.getActivityTitle(activity);
                            if (!TextUtils.isEmpty(activityTitle)) {
                                properties.put("$title", activityTitle);
                            }
                            screenName = String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), fragmentName);
                        }
                    }

                    properties.put("$screen_name", screenName);
                    track("$AppViewScreen", properties);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void trackViewScreen(final android.support.v4.app.Fragment fragment) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (fragment == null) {
                        return;
                    }

                    JSONObject properties = new JSONObject();
                    String fragmentName = fragment.getClass().getCanonicalName();
                    String screenName = fragmentName;

                    Activity activity = fragment.getActivity();
                    if (activity != null) {
                        String activityTitle = SensorsDataUtils.getActivityTitle(activity);
                        if (!TextUtils.isEmpty(activityTitle)) {
                            properties.put("$title", activityTitle);
                        }
                        screenName = String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), fragmentName);
                    }

                    properties.put("$screen_name", screenName);
                    track("$AppViewScreen", properties);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void trackViewPager(final ViewPager viewPager) {
        if (viewPager == null) {
            return;
        }
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                SensorsDataAutoTrackHelper.trackViewOnClick(viewPager);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    /**
     * app进入后台
     * 遍历mTrackTimer
     * eventAccumulatedDuration = eventAccumulatedDuration + System.currentTimeMillis() - startTime
     */
    protected void appEnterBackground() {
        synchronized (mTrackTimer) {
            try {
                Iterator iter = mTrackTimer.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    if (entry != null) {
                        if ("$AppEnd".equals(entry.getKey().toString())) {
                            continue;
                        }
                        EventTimer eventTimer = (EventTimer) entry.getValue();
                        if (eventTimer != null) {
                            long eventAccumulatedDuration = eventTimer.getEventAccumulatedDuration() + SystemClock.elapsedRealtime() - eventTimer.getStartTime();
                            eventTimer.setEventAccumulatedDuration(eventAccumulatedDuration);
                            eventTimer.setStartTime(SystemClock.elapsedRealtime());
                        }
                    }
                }
            } catch (Exception e) {
                SALog.i(TAG, "appEnterBackground error:" + e.getMessage());
            }
        }
    }

    /**
     * app从后台恢复
     * 遍历mTrackTimer
     * startTime = System.currentTimeMillis()
     */
    protected void appBecomeActive() {
        synchronized (mTrackTimer) {
            try {
                Iterator iter = mTrackTimer.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    if (entry != null) {
                        EventTimer eventTimer = (EventTimer) entry.getValue();
                        if (eventTimer != null) {
                            eventTimer.setStartTime(SystemClock.elapsedRealtime());
                        }
                    }
                }
            } catch (Exception e) {
                SALog.i(TAG, "appBecomeActive error:" + e.getMessage());
            }
        }
    }

    /**
     * 将所有本地缓存的日志发送到 Sensors Analytics.
     */
    @Override
    public void flush() {
        mMessages.flush();
    }

    /**
     * 延迟指定毫秒数将所有本地缓存的日志发送到 Sensors Analytics.
     * @param timeDelayMills 延迟毫秒数
     */
    public void flush(long timeDelayMills) {
        mMessages.flush(timeDelayMills);
    }

    /**
     * 以阻塞形式将所有本地缓存的日志发送到 Sensors Analytics，该方法不能在 UI 线程调用。
     */
    @Override
    public void flushSync() {
        mMessages.sendData();
    }

    /**
     * 注册事件动态公共属性
     *
     * @param dynamicSuperProperties 事件动态公共属性回调接口
     */
    @Override
    public void registerDynamicSuperProperties(SensorsDataDynamicSuperProperties dynamicSuperProperties) {
        mDynamicSuperProperties = dynamicSuperProperties;
    }

    /**
     * 删除本地缓存的全部事件
     */
    @Override
    public void deleteAll() {
        mMessages.deleteAll();
    }

    /**
     * 获取事件公共属性
     *
     * @return 当前所有Super属性
     */
    @Override
    public JSONObject getSuperProperties() {
        synchronized (mSuperProperties) {
            return mSuperProperties.get();
        }
    }

    /**
     * 注册所有事件都有的公共属性
     *
     * @param superProperties 事件公共属性
     */
    @Override
    public void registerSuperProperties(JSONObject superProperties) {
        try {
            if (superProperties == null) {
                return;
            }
            assertPropertyTypes(EventType.REGISTER_SUPER_PROPERTIES, superProperties);
            synchronized (mSuperProperties) {
                JSONObject properties = mSuperProperties.get();
                SensorsDataUtils.mergeSuperJSONObject(superProperties, properties);
                mSuperProperties.commit(properties);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除事件公共属性
     *
     * @param superPropertyName 事件属性名称
     */
    @Override
    public void unregisterSuperProperty(String superPropertyName) {
        try {
            synchronized (mSuperProperties) {
                JSONObject superProperties = mSuperProperties.get();
                superProperties.remove(superPropertyName);
                mSuperProperties.commit(superProperties);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除所有事件公共属性
     */
    @Override
    public void clearSuperProperties() {
        synchronized (mSuperProperties) {
            mSuperProperties.commit(new JSONObject());
        }
    }

    /**
     * 设置用户的一个或多个Profile。
     * Profile如果存在，则覆盖；否则，新创建。
     *
     * @param properties 属性列表
     */
    @Override
    public void profileSet(final JSONObject properties) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_SET, null, properties, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 设置用户的一个Profile，如果之前存在，则覆盖，否则，新创建
     *
     * @param property 属性名称
     * @param value    属性的值，值的类型只允许为
     *                 {@link java.lang.String}, {@link java.lang.Number}, {@link java.util.Date}, {@link java.util.List}
     */
    @Override
    public void profileSet(final String property, final Object value) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_SET, null, new JSONObject().put(property, value), null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 首次设置用户的一个或多个Profile。
     * 与profileSet接口不同的是，如果之前存在，则忽略，否则，新创建
     *
     * @param properties 属性列表
     */
    @Override
    public void profileSetOnce(final JSONObject properties)  {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_SET_ONCE, null, properties, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 首次设置用户的一个Profile
     * 与profileSet接口不同的是，如果之前存在，则忽略，否则，新创建
     *
     * @param property 属性名称
     * @param value    属性的值，值的类型只允许为
     *                 {@link java.lang.String}, {@link java.lang.Number}, {@link java.util.Date}, {@link java.util.List}
     */
    @Override
    public void profileSetOnce(final String property, final Object value) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_SET_ONCE, null, new JSONObject().put(property, value), null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 给一个或多个数值类型的Profile增加一个数值。只能对数值型属性进行操作，若该属性
     * 未设置，则添加属性并设置默认值为0
     *
     * @param properties 一个或多个属性集合
     */
    @Override
    public void profileIncrement(final Map<String, ? extends Number> properties) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_INCREMENT, null, new JSONObject(properties), null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 给一个数值类型的Profile增加一个数值。只能对数值型属性进行操作，若该属性
     * 未设置，则添加属性并设置默认值为0
     *
     * @param property 属性名称
     * @param value    属性的值，值的类型只允许为 {@link java.lang.Number}
     */
    @Override
    public void profileIncrement(final String property, final Number value) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_INCREMENT, null, new JSONObject().put(property, value), null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 给一个列表类型的Profile增加一个元素
     *
     * @param property 属性名称
     * @param value    新增的元素
     */
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
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 给一个列表类型的Profile增加一个或多个元素
     *
     * @param property 属性名称
     * @param values   新增的元素集合
     */
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
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 删除用户的一个Profile
     *
     * @param property 属性名称
     */
    @Override
    public void profileUnset(final String property) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_UNSET, null, new JSONObject().put(property, true), null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 删除用户所有Profile
     */
    @Override
    public void profileDelete() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent(EventType.PROFILE_DELETE, null, null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean isDebugMode() {
        return mDebugMode.isDebugMode();
    }

    boolean isDebugWriteData() {
        return mDebugMode.isDebugWriteData();
    }

    private void disableDebugMode() {
        mDebugMode = DebugMode.DEBUG_OFF;
        enableLog(false);
        mServerUrl = mOriginServerUrl;
    }

    String getServerUrl() {
        return mServerUrl;
    }

    private void showDebugModeWarning() {
        try {
            Looper.prepare();
            if (mDebugMode == DebugMode.DEBUG_OFF) {
                return;
            }

            if (TextUtils.isEmpty(getServerUrl())) {
                return;
            }

            String info = null;
            if (mDebugMode == DebugMode.DEBUG_ONLY) {
                info = "现在您打开了 SensorsData SDK 的 'DEBUG_ONLY' 模式，此模式下只校验数据但不导入数据，数据出错时会以 Toast 的方式提示开发者，请上线前一定使用 DEBUG_OFF 模式。";
            } else if (mDebugMode == DebugMode.DEBUG_AND_TRACK) {
                info = "现在您打开了神策 SensorsData SDK 的 'DEBUG_AND_TRACK' 模式，此模式下校验数据并且导入数据，数据出错时会以 Toast 的方式提示开发者，请上线前一定使用 DEBUG_OFF 模式。";
            }

            Toast.makeText(mContext, info, Toast.LENGTH_LONG).show();
            Looper.loop();
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    protected boolean _trackEventFromH5(String eventInfo) {
        try {
            if (TextUtils.isEmpty(eventInfo)) {
                return false;
            }
            JSONObject eventObject = new JSONObject(eventInfo);

            String serverUrl = eventObject.optString("server_url");
            if (!TextUtils.isEmpty(serverUrl)) {
                if (!(new ServerUrl(serverUrl).check(new ServerUrl(mServerUrl)))) {
                    return false;
                }
                trackEventFromH5(eventInfo);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;

    }

    @Override
    public void trackEventFromH5(String eventInfo) {
        try {
            if (TextUtils.isEmpty(eventInfo)) {
                return;
            }

            JSONObject eventObject = new JSONObject(eventInfo);
            String type = eventObject.getString("type");
            EventType eventType = EventType.valueOf(type.toUpperCase());

            String distinctIdKey = "distinct_id";
            if (eventType == EventType.TRACK_SIGNUP) {
                distinctIdKey = "original_id";
            }
            if (!TextUtils.isEmpty(getLoginId())) {
                eventObject.put(distinctIdKey, getLoginId());
            } else {
                eventObject.put(distinctIdKey, getAnonymousId());
            }

            eventObject.put("time", System.currentTimeMillis());

            try {
                Random random = new Random();
                eventObject.put("_track_id", random.nextInt());
            } catch (Exception e) {
                //ignore
            }

            JSONObject propertiesObject = eventObject.optJSONObject("properties");
            if (propertiesObject == null) {
                propertiesObject = new JSONObject();
            }

            JSONObject libObject = eventObject.optJSONObject("lib");
            if (libObject != null) {
                if (mDeviceInfo.containsKey("$app_version")) {
                    libObject.put("$app_version", mDeviceInfo.get("$app_version"));
                }

                //update lib $app_version from super properties
                JSONObject superProperties = mSuperProperties.get();
                if (superProperties != null) {
                    if (superProperties.has("$app_version")) {
                        libObject.put("$app_version", superProperties.get("$app_version"));
                    }
                }
            }

            if (eventType.isTrack()) {
                if (mDeviceInfo != null) {
                    for (Map.Entry<String, Object> entry : mDeviceInfo.entrySet()) {
                        String key = entry.getKey();
                        if (!TextUtils.isEmpty(key)) {
                            if ("$lib".equals(key) || "$lib_version".equals(key)) {
                                continue;
                            }
                            propertiesObject.put(entry.getKey(), entry.getValue());
                        }
                    }
                }

                // 当前网络状况
                String networkType = SensorsDataUtils.networkType(mContext);
                propertiesObject.put("$wifi", networkType.equals("WIFI"));
                propertiesObject.put("$network_type", networkType);

                // SuperProperties
                synchronized (mSuperProperties) {
                    JSONObject superProperties = mSuperProperties.get();
                    SensorsDataUtils.mergeJSONObject(superProperties, propertiesObject);
                }

                try {
                    if (mDynamicSuperProperties != null) {
                        JSONObject dynamicSuperProperties = mDynamicSuperProperties.getDynamicSuperProperties();
                        if (dynamicSuperProperties != null) {
                            SensorsDataUtils.mergeJSONObject(dynamicSuperProperties, propertiesObject);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //是否首日访问
                if (eventType.isTrack()) {
                    propertiesObject.put("$is_first_day", isFirstDay());
                }

                if (propertiesObject.has("$is_first_time")) {
                    propertiesObject.remove("$is_first_time");
                }
            }

            if (eventObject.has("_nocache")) {
                eventObject.remove("_nocache");
            }
            if (eventObject.has("server_url")) {
                eventObject.remove("server_url");
            }

            if (propertiesObject.has("$project")) {
                eventObject.put("project", propertiesObject.optString("$project"));
                propertiesObject.remove("$project");
            }

            if (propertiesObject.has("$token")) {
                eventObject.put("token", propertiesObject.optString("$token"));
                propertiesObject.remove("$token");
            }

            if (eventType == EventType.TRACK_SIGNUP) {
                String loginId = eventObject.getString("distinct_id");
                synchronized (mLoginId) {
                    if (!loginId.equals(mLoginId.get())) {
                        mLoginId.commit(loginId);
                        if (!loginId.equals(getAnonymousId())) {
                            mMessages.enqueueEventMessage(type, eventObject);
                        }
                    }
                }
            } else {
                mMessages.enqueueEventMessage(type, eventObject);
            }
            SALog.i(TAG, "track event:\n" + JSONUtils.formatJson(eventObject.toString()));
        } catch (Exception e) {
            //ignore
            e.printStackTrace();
        }
    }

    private void trackEvent(final EventType eventType, final String eventName, final JSONObject properties, final String
            originalDistinctId) {
        final EventTimer eventTimer;
        if (eventName != null) {
            synchronized (mTrackTimer) {
                eventTimer = mTrackTimer.get(eventName);
                mTrackTimer.remove(eventName);
            }
        } else {
            eventTimer = null;
        }

        try {
            if (eventType.isTrack()) {
                assertKey(eventName);
            }
            assertPropertyTypes(eventType, properties);

            try {
                JSONObject sendProperties;

                if (eventType.isTrack()) {
                    sendProperties = new JSONObject(mDeviceInfo);

                    //之前可能会因为没有权限无法获取运营商信息，检测再次获取
                    try {
                        if (TextUtils.isEmpty(sendProperties.optString("$carrier"))) {
                            String carrier = SensorsDataUtils.getCarrier(mContext);
                            if (!TextUtils.isEmpty(carrier)) {
                                sendProperties.put("$carrier", carrier);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    synchronized (mSuperProperties) {
                        JSONObject superProperties = mSuperProperties.get();
                        SensorsDataUtils.mergeJSONObject(superProperties, sendProperties);
                    }

                    try {
                        if (mDynamicSuperProperties != null) {
                            JSONObject dynamicSuperProperties = mDynamicSuperProperties.getDynamicSuperProperties();
                            if (dynamicSuperProperties != null) {
                                SensorsDataUtils.mergeJSONObject(dynamicSuperProperties, sendProperties);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // 当前网络状况
                    String networkType = SensorsDataUtils.networkType(mContext);
                    sendProperties.put("$wifi", networkType.equals("WIFI"));
                    sendProperties.put("$network_type", networkType);


                    // GPS
                    try {
                        if (mGPSLocation != null) {
                            sendProperties.put("$latitude", mGPSLocation.getLatitude());
                            sendProperties.put("$longitude", mGPSLocation.getLongitude());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // 屏幕方向
                    try {
                        String screenOrientation = getScreenOrientation();
                        if (!TextUtils.isEmpty(screenOrientation)) {
                            sendProperties.put("$screen_orientation", screenOrientation);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (eventType.isProfile()) {
                    sendProperties = new JSONObject();
                } else {
                    return;
                }

                String libDetail = null;
                long eventTime = System.currentTimeMillis();
                if (null != properties) {
                    try {
                        if (properties.has("$lib_detail")) {
                            libDetail = properties.getString("$lib_detail");
                            properties.remove("$lib_detail");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        if ("$AppEnd".equals(eventName)) {
                            eventTime = properties.getLong("event_time");
                            properties.remove("event_time");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    SensorsDataUtils.mergeJSONObject(properties, sendProperties);
                }

                if (null != eventTimer) {
                    try {
                        Double duration = Double.valueOf(eventTimer.duration());
                        if (duration > 0) {
                            sendProperties.put("event_duration", duration);
                        }
                    } catch (Exception e) {
                        //ignore
                        e.printStackTrace();
                    }
                }

                JSONObject libProperties = new JSONObject();
                libProperties.put("$lib", "Android");
                libProperties.put("$lib_version", VERSION);

                if (mDeviceInfo.containsKey("$app_version")) {
                    libProperties.put("$app_version", mDeviceInfo.get("$app_version"));
                }

                //update lib $app_version from super properties
                JSONObject superProperties = mSuperProperties.get();
                if (superProperties != null) {
                    if (superProperties.has("$app_version")) {
                        libProperties.put("$app_version", superProperties.get("$app_version"));
                    }
                }

                final JSONObject dataObj = new JSONObject();

                try {
                    Random random = new Random();
                    dataObj.put("_track_id", random.nextInt());
                } catch (Exception e) {
                    //ignore
                }

                dataObj.put("time", eventTime);
                dataObj.put("type", eventType.getEventType());

                try {
                    if (sendProperties.has("$project")) {
                        dataObj.put("project", sendProperties.optString("$project"));
                        sendProperties.remove("$project");
                    }

                    if (sendProperties.has("$token")) {
                        dataObj.put("token", sendProperties.optString("$token"));
                        sendProperties.remove("$token");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                dataObj.put("properties", sendProperties);
                if (!TextUtils.isEmpty(getLoginId())) {
                    dataObj.put("distinct_id", getLoginId());
                } else {
                    dataObj.put("distinct_id", getAnonymousId());
                }
                dataObj.put("lib", libProperties);

                if (eventType == EventType.TRACK) {
                    dataObj.put("event", eventName);
                    //是否首日访问
                    sendProperties.put("$is_first_day", isFirstDay());
                } else if (eventType == EventType.TRACK_SIGNUP) {
                    dataObj.put("event", eventName);
                    dataObj.put("original_id", originalDistinctId);
                }

                libProperties.put("$lib_method", "code");

                if (mAutoTrack && properties != null) {
                    if (AutoTrackEventType.APP_VIEW_SCREEN.getEventName().equals(eventName) ||
                            AutoTrackEventType.APP_CLICK.getEventName().equals(eventName) ||
                            AutoTrackEventType.APP_START.getEventName().equals(eventName) ||
                            AutoTrackEventType.APP_END.getEventName().equals(eventName)) {
                        AutoTrackEventType trackEventType = AutoTrackEventType.autoTrackEventTypeFromEventName(eventName);
                        if (trackEventType != null) {
                            if (mAutoTrackEventTypeList.contains(trackEventType)) {
                                if (properties.has("$screen_name")) {
                                    String screenName = properties.getString("$screen_name");
                                    if (!TextUtils.isEmpty(screenName)) {
                                        String screenNameArray[] = screenName.split("\\|");
                                        if (screenNameArray.length > 0) {
                                            libDetail = String.format("%s##%s##%s##%s", screenNameArray[0], "", "", "");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (TextUtils.isEmpty(libDetail)) {
                    StackTraceElement[] trace = (new Exception()).getStackTrace();
                    if (trace.length > 2) {
                        StackTraceElement traceElement = trace[2];
                        libDetail = String.format("%s##%s##%s##%s", traceElement
                                        .getClassName(), traceElement.getMethodName(), traceElement.getFileName(),
                                traceElement.getLineNumber());
                    }
                }

                libProperties.put("$lib_detail", libDetail);

                //防止用户自定义事件以及公共属性可能会加$device_id属性，导致覆盖sdk原始的$device_id属性值
                if (sendProperties.has("$device_id")){//由于profileSet等类型事件没有$device_id属性，故加此判断
                    if (mDeviceInfo.containsKey("$device_id")) {
                        sendProperties.put("$device_id", mDeviceInfo.get("$device_id"));
                    }
                }

                mMessages.enqueueEventMessage(eventType.getEventType(), dataObj);
                SALog.i(TAG, "track event:\n" + JSONUtils.formatJson(dataObj.toString()));
            } catch (JSONException e) {
                throw new InvalidDataException("Unexpected property");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopTrackTaskThread() {
        mTrackTaskManagerThread.setStop(true);
    }

    public void resumeTrackTaskThread() {
        mTrackTaskManagerThread = new TrackTaskManagerThread();
        new Thread(mTrackTaskManagerThread).start();
    }

    private boolean isFirstDay() {
        String firstDay = mFirstDay.get();
        if (firstDay == null) {
            return true;
        }
        String current = mIsFirstDayDateFormat.format(System.currentTimeMillis());
        return firstDay.equals(current);
    }

    private void assertPropertyTypes(EventType eventType, JSONObject properties) throws
            InvalidDataException {
        if (properties == null) {
            return;
        }

        for (Iterator iterator = properties.keys(); iterator.hasNext(); ) {
            String key = (String) iterator.next();

            // Check Keys
            assertKey(key);

            try {
                Object value = properties.get(key);

                if (!(value instanceof String || value instanceof Number || value
                        instanceof JSONArray || value instanceof Boolean || value instanceof Date)) {
                    throw new InvalidDataException("The property value must be an instance of "
                            + "String/Number/Boolean/JSONArray. [key='" + key + "', value='" + value.toString()
                            + "']");
                }

                if ("app_crashed_reason".equals(key)) {
                    if (value instanceof String && ((String) value).length() > 8191 * 2) {
                        properties.put(key, ((String) value).substring(0, 8191 * 2) + "$");
                        SALog.d(TAG, "The property value is too long. [key='" + key
                                + "', value='" + value.toString() + "']");
                    }
                } else {
                    if (value instanceof String && ((String) value).length() > 8191) {
                        properties.put(key, ((String) value).substring(0, 8191) + "$");
                        SALog.d(TAG, "The property value is too long. [key='" + key
                                + "', value='" + value.toString() + "']");
                    }
                }
            } catch (JSONException e) {
                throw new InvalidDataException("Unexpected property key. [key='" + key + "']");
            }
        }
    }

    private void assertKey(String key) throws InvalidDataException {
        if (null == key || key.length() < 1) {
            throw new InvalidDataException("The key is empty.");
        }
        if (!(KEY_PATTERN.matcher(key).matches())) {
            throw new InvalidDataException("The key '" + key + "' is invalid.");
        }
    }

    private void assertDistinctId(String key) throws InvalidDataException {
        if (key == null || key.length() < 1) {
            throw new InvalidDataException("The distinct_id or original_id or login_id is empty.");
        }
        if (key.length() > 255) {
            throw new InvalidDataException("The max length of distinct_id or original_id or login_id is 255.");
        }
    }

    // 可视化埋点功能最低API版本
    static final int VTRACK_SUPPORTED_MIN_API = 16;

    // SDK版本
    static final String VERSION = "2.1.2";

    static Boolean ENABLE_LOG = false;
    static Boolean SHOW_DEBUG_INFO_VIEW = true;

    private static final Pattern KEY_PATTERN = Pattern.compile(
            "^((?!^distinct_id$|^original_id$|^time$|^properties$|^id$|^first_id$|^second_id$|^users$|^events$|^event$|^user_id$|^date$|^datetime$)[a-zA-Z_$][a-zA-Z\\d_$]{0,99})$",
            Pattern.CASE_INSENSITIVE);

    // Maps each token to a singleton SensorsDataAPI instance
    private static final Map<Context, SensorsDataAPI> sInstanceMap = new HashMap<>();
    private static final SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();
    private static SensorsDataSDKRemoteConfig mSDKRemoteConfig;
    private static SensorsDataGPSLocation mGPSLocation;

    // Configures
  /* SensorsAnalytics 地址 */
    private String mServerUrl;
    private String mOriginServerUrl;
    /* Debug模式选项 */
    private DebugMode mDebugMode;
    /* Flush时间间隔 */
    private int mFlushInterval;
    /* Flush数据量阈值 */
    private int mFlushBulkSize;
    /* SDK 自动采集事件 */
    private boolean mAutoTrack;
    private boolean mHeatMapEnabled;
    /* 上个页面的Url*/
    private String mLastScreenUrl;
    private JSONObject mLastScreenTrackProperties;
    private boolean mEnableButterknifeOnClick;
    /* $AppViewScreen 事件是否支持 Fragment*/
    private boolean mTrackFragmentAppViewScreen;
    private boolean mEnableReactNativeAutoTrack;
    private boolean mClearReferrerWhenAppEnd = false;
    private boolean mEnableAppHeatMapConfirmDialog = true;
    private boolean mDisableDefaultRemoteConfig = false;
    private boolean mDisableTrackDeviceId = false;

    private final Context mContext;
    private final AnalyticsMessages mMessages;
    private final PersistentDistinctId mDistinctId;
    private final PersistentLoginId mLoginId;
    private final PersistentSuperProperties mSuperProperties;
    private final PersistentFirstStart mFirstStart;
    private final PersistentFirstDay mFirstDay;
    private final PersistentFirstTrackInstallation mFirstTrackInstallation;
    private final PersistentFirstTrackInstallationWithCallback mFirstTrackInstallationWithCallback;
    private final PersistentRemoteSDKConfig mPersistentRemoteSDKConfig;
    private final Map<String, Object> mDeviceInfo;
    private final Map<String, EventTimer> mTrackTimer;
    private List<Integer> mAutoTrackIgnoredActivities;
    private List<Integer> mHeatMapActivities;
    private Set<Integer> mAutoTrackFragments;
    private int mFlushNetworkPolicy = NetworkType.TYPE_3G | NetworkType.TYPE_4G | NetworkType.TYPE_WIFI;
    private final String mMainProcessName;
    private long mMaxCacheSize = 32 * 1024 * 1024; //default 32MB
    private String mCookie;
    private TrackTaskManager mTrackTaskManager;
    private TrackTaskManagerThread mTrackTaskManagerThread;

    private SensorsDataScreenOrientationDetector mOrientationDetector;

    private SensorsDataDynamicSuperProperties mDynamicSuperProperties;
    private static DbAdapter mDbAdapter;
    private static final SimpleDateFormat mIsFirstDayDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private static final String TAG = "SA.SensorsDataAPI";
}
