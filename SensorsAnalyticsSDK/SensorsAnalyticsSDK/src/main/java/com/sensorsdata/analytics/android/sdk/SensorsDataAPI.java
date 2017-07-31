package com.sensorsdata.analytics.android.sdk;

import com.sensorsdata.analytics.android.sdk.aop.AopConstants;
import com.sensorsdata.analytics.android.sdk.aop.AopThreadPool;
import com.sensorsdata.analytics.android.sdk.aop.AopUtil;
import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Sensors Analytics SDK
 */
public class SensorsDataAPI {

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
        public static final int TYPE_2G = 1 << 0;//2G
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
        APP_START("$AppStart"),
        APP_END("$AppEnd"),
        APP_CLICK("$AppClick"),
        APP_VIEW_SCREEN("$AppViewScreen");
        private final String eventName;

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

        AutoTrackEventType(String eventName) {
            this.eventName = eventName;
        }

        String getEventName() {
            return eventName;
        }
    }

    SensorsDataAPI(Context context, String serverURL, String configureURL,
                   String vtrackServerURL, DebugMode debugMode) {
        mContext = context;

        final String packageName = context.getApplicationContext().getPackageName();

        mAutoTrackIgnoredActivities = new ArrayList<>();

        try {
            SensorsDataUtils.cleanUserAgent(mContext);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            SALog.init(this);
            final ApplicationInfo appInfo = context.getApplicationContext().getPackageManager()
                    .getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            Bundle configBundle = appInfo.metaData;
            if (null == configBundle) {
                configBundle = new Bundle();
            }

            if (debugMode.isDebugMode()) {
                Uri serverURI = Uri.parse(serverURL);

                int pathPrefix = serverURI.getPath().lastIndexOf('/');
                String newPath = serverURI.getPath().substring(0, pathPrefix) + "/debug";

                // 将 URI Path 中末尾的部分替换成 '/debug'
                mServerUrl = serverURI.buildUpon().path(newPath).build().toString();
            } else {
                mServerUrl = serverURL;
            }

            // 若 Configure Url 为 '/api/vtrack/config' 或 '/config'，则补齐 SDK 类型
            Uri configureURI = Uri.parse(configureURL);
            if (configureURI.getPath().equals("/api/vtrack/config") || configureURI.getPath().equals
                    ("/api/vtrack/config/") || configureURI.getPath().equals("/config") || configureURI
                    .getPath().equals("/config/")) {
                mConfigureUrl = configureURI.buildUpon().appendPath("Android.conf").build().toString();
            } else {
                mConfigureUrl = configureURL;
            }

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
            mEnableVTrack = configBundle.getBoolean("com.sensorsdata.analytics.android.VTrack",
                    true);
            mEnableAndroidId = configBundle.getBoolean("com.sensorsdata.analytics.android.AndroidId",
                    false);
            mEnableButterknifeOnClick = configBundle.getBoolean("com.sensorsdata.analytics.android.ButterknifeOnClick",
                    false);
            mMainProcessName = configBundle.getString("com.sensorsdata.analytics.android.MainProcessName");

            mDebugMode = debugMode;
            //打开debug模式，弹出提示
            if (mDebugMode != DebugMode.DEBUG_OFF && SensorsDataUtils.isMainProcess(mContext.getApplicationContext(), mMainProcessName)) {
                showDebugModeWarning();
            } else {
                SHOW_DEBUG_INFO_VIEW = false;
            }

            if (Build.VERSION.SDK_INT >= VTRACK_SUPPORTED_MIN_API
                    && mEnableVTrack) {
                String resourcePackageName =
                        configBundle.getString("com.sensorsdata.analytics.android.ResourcePackageName");
                if (null == resourcePackageName) {
                    resourcePackageName = context.getPackageName();
                }

                mVTrack = new ViewCrawler(mContext, resourcePackageName);
            } else {
                SALog.d(TAG, "VTrack is not supported on this Android OS Version");
                mVTrack = new VTrackUnsupported();
            }

            if (vtrackServerURL != null) {
                mVTrack.setVTrackServer(vtrackServerURL);
            }
        } catch (final PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Can't configure SensorsDataAPI with package name " + packageName,
                    e);
        }

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

        mDistinctId = new PersistentDistinctId(storedPreferences);
        if (mEnableAndroidId) {
            try {
                String androidId = SensorsDataUtils.getAndroidID(mContext);
                if (SensorsDataUtils.isValidAndroidId(androidId)) {
                    identify(androidId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mLoginId = new PersistentLoginId(storedPreferences);
        mSuperProperties = new PersistentSuperProperties(storedPreferences);
        mFirstStart = new PersistentFirstStart(storedPreferences);
        mFirstTrackInstallation = new PersistentFirstTrackInstallation(storedPreferences);
        mFirstDay = new PersistentFirstDay(storedPreferences);
        if (mFirstDay.get() == null) {
            mFirstDay.commit(mIsFirstDayDateFormat.format(System.currentTimeMillis()));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            final Application app = (Application) context.getApplicationContext();
            app.registerActivityLifecycleCallbacks(new SensorsDataActivityLifecycleCallbacks(this, mFirstStart, mMainProcessName));
        }

        SALog.d(TAG, String.format(Locale.CHINA, "Initialized the instance of Sensors Analytics SDK with server"
                        + " url '%s', configure url '%s' flush interval %d ms, debugMode: %s", mServerUrl,
                mConfigureUrl, mFlushInterval, debugMode));

        mAutoTrackEventTypeList = new ArrayList<>();

        final Map<String, Object> deviceInfo = new HashMap<>();

        {
            deviceInfo.put("$lib", "Android");
            deviceInfo.put("$lib_version", VERSION);
            deviceInfo.put("$os", "Android");
            deviceInfo.put("$os_version",
                    Build.VERSION.RELEASE == null ? "UNKNOWN" : Build.VERSION.RELEASE);
            deviceInfo
                    .put("$manufacturer", Build.MANUFACTURER == null ? "UNKNOWN" : Build.MANUFACTURER);
            deviceInfo.put("$model", Build.MODEL == null ? "UNKNOWN" : Build.MODEL);
            try {
                final PackageManager manager = mContext.getPackageManager();
                final PackageInfo info = manager.getPackageInfo(mContext.getPackageName(), 0);
                deviceInfo.put("$app_version", info.versionName);
            } catch (final Exception e) {
                SALog.d(TAG, "Exception getting app version name", e);
            }
            final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            deviceInfo.put("$screen_height", displayMetrics.heightPixels);
            deviceInfo.put("$screen_width", displayMetrics.widthPixels);

            TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context
                    .TELEPHONY_SERVICE);
            String operatorString = telephonyManager.getSimOperator();

            if (!TextUtils.isEmpty(operatorString)) {
                deviceInfo.put("$carrier", SensorsDataUtils.operatorToCarrier(operatorString));
            }

            String androidID = SensorsDataUtils.getAndroidID(mContext);
            if (!TextUtils.isEmpty(androidID)) {
                deviceInfo.put("$device_id", androidID);
            }
        }

        mDeviceInfo = Collections.unmodifiableMap(deviceInfo);
        mTrackTimer = new HashMap<>();

        mVTrack.startUpdates();
        if (mEnableVTrack) {
            mMessages.checkConfigure(new DecideMessages(mVTrack));
        }
    }

    /**
     * 获取SensorsDataAPI单例
     *
     * @param context App的Context
     * @return SensorsDataAPI单例
     */
    public static SensorsDataAPI sharedInstance(Context context) {
        if (null == context) {
            return null;
        }
        synchronized (sInstanceMap) {
            final Context appContext = context.getApplicationContext();
            SensorsDataAPI instance = sInstanceMap.get(appContext);

            if (null == instance) {
                SALog.d(TAG, "The static method sharedInstance(context, serverURL, configureURL, "
                        + "vtrackServerURL, debugMode) should be called before calling sharedInstance()");
            }
            return instance;
        }
    }

    /**
     * 初始化并获取SensorsDataAPI单例
     *
     * @param context      App 的 Context
     * @param serverURL    用于收集事件的服务地址
     * @param configureUrl 用于获取SDK配置的服务地址
     * @param debugMode    Debug模式,
     *                     {@link com.sensorsdata.analytics.android.sdk.SensorsDataAPI.DebugMode}
     * @return SensorsDataAPI单例
     */
    public static SensorsDataAPI sharedInstance(Context context, String serverURL, String
            configureUrl, DebugMode debugMode) {
        if (null == context) {
            return null;
        }

        synchronized (sInstanceMap) {
            final Context appContext = context.getApplicationContext();

            SensorsDataAPI instance = sInstanceMap.get(appContext);
            if (null == instance && ConfigurationChecker.checkBasicConfiguration(appContext)) {
                instance = new SensorsDataAPI(appContext, serverURL, configureUrl, null, debugMode);
                sInstanceMap.put(appContext, instance);
            }

            return instance;
        }
    }

    /**
     * 初始化并获取SensorsDataAPI单例（打开可视化埋点功能）
     *
     * @param context         App的Context
     * @param serverURL       用于收集事件的服务地址
     * @param configureURL    用于获取SDK配置的服务地址
     * @param vtrackServerURL 可视化埋点的WebServer地址
     * @param debugMode       Debug模式,
     *                        {@link com.sensorsdata.analytics.android.sdk.SensorsDataAPI.DebugMode}
     * @return SensorsDataAPI单例
     */
    public static SensorsDataAPI sharedInstance(Context context, String serverURL,
        String configureURL, String vtrackServerURL, DebugMode debugMode) {
        if (null == context) {
            return null;
        }

        synchronized (sInstanceMap) {
            final Context appContext = context.getApplicationContext();

            SensorsDataAPI instance = sInstanceMap.get(appContext);
            if (null == instance && ConfigurationChecker.checkBasicConfiguration(appContext)) {
                instance = new SensorsDataAPI(appContext, serverURL, configureURL, vtrackServerURL,
                        debugMode);
                sInstanceMap.put(appContext, instance);
            }

            return instance;
        }
    }

    public static SensorsDataAPI sharedInstance() {
        synchronized (sInstanceMap) {
            if (sInstanceMap.size() > 0) {
                Iterator<SensorsDataAPI> iterator = sInstanceMap.values().iterator();
                if (iterator.hasNext()) {
                    return iterator.next();
                }
            }
            return null;
        }
    }

    /**
     * 设置是否开启 log
     * @param enable boolean
     */
    public void enableLog(boolean enable) {
        this.ENABLE_LOG = enable;
    }

    public long getMaxCacheSize() {
        return mMaxCacheSize;
    }

    /**
     * 设置本地缓存上限值，单位 byte，默认为 32MB：32 * 1024 * 1024
     * @param maxCacheSize 单位 byte
     */
    public void setMaxCacheSize(long maxCacheSize) {
        if (maxCacheSize > 0) {
            this.mMaxCacheSize = maxCacheSize;
        }
    }

    /**
     * 设置 flush 时网络发送策略，默认 3G、4G、WI-FI 环境下都会尝试 flush
     * @param networkType int 网络类型
     */
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
    public int getFlushInterval() {
        return mFlushInterval;
    }

    /**
     * 设置两次数据发送的最小时间间隔
     *
     * @param flushInterval 时间间隔，单位毫秒
     */
    public void setFlushInterval(int flushInterval) {
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
    public int getFlushBulkSize() {
        return mFlushBulkSize;
    }

    /**
     * 设置本地缓存日志的最大条目数
     *
     * @param flushBulkSize 缓存数目
     */
    public void setFlushBulkSize(int flushBulkSize) {
        mFlushBulkSize = flushBulkSize;
    }

    /**
     * 允许 App 连接可视化埋点管理界面
     *
     * 调用这个方法，允许 App 连接可视化埋点管理界面并设置可视化埋点。建议用户只在 DEBUG 编译模式下，打开该选项。
     */
    public void enableEditingVTrack() {
        mVTrack.enableEditingVTrack();
    }

    /**
     * 屏蔽某个 Activity 的可视化埋点功能
     *
     * @param canonicalName Activity 的 Canonical Name
     */
    public void disableActivityForVTrack(String canonicalName) {
        if (canonicalName != null) {
            mVTrack.disableActivity(canonicalName);
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
     */
    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList) {
        mAutoTrack = true;
        if (eventTypeList == null || eventTypeList.size() == 0) {
            return;
        }

        mAutoTrackEventTypeList.clear();
        mAutoTrackEventTypeList.addAll(eventTypeList);
    }

    /**
     * 关闭 AutoTrack 中的部分事件
     * @param eventTypeList AutoTrackEventType 类型 List
     */
    public void disableAutoTrack(List<AutoTrackEventType> eventTypeList) {
        if (eventTypeList == null || eventTypeList.size() == 0) {
            return;
        }

        if (mAutoTrackEventTypeList == null) {
            return;
        }

        for (AutoTrackEventType autoTrackEventType: eventTypeList) {
            if (autoTrackEventType != null) {
                if (mAutoTrackEventTypeList.contains(autoTrackEventType)) {
                    mAutoTrackEventTypeList.remove(autoTrackEventType);
                }
            }
        }

        if (mAutoTrackEventTypeList.size() == 0) {
            mAutoTrack = false;
        }
    }

    /**
     * 关闭 AutoTrack 中的某个事件
     * @param autoTrackEventType AutoTrackEventType 类型
     */
    public void disableAutoTrack(AutoTrackEventType autoTrackEventType) {
        if (autoTrackEventType == null) {
            return;
        }

        if (mAutoTrackEventTypeList == null) {
            return;
        }

        if (mAutoTrackEventTypeList.contains(autoTrackEventType)) {
            mAutoTrackEventTypeList.remove(autoTrackEventType);
        }

        if (mAutoTrackEventTypeList.size() == 0) {
            mAutoTrack = false;
        }
    }

    /**
     * 是否开启 AutoTrack
     * @return true: 开启 AutoTrack; false：没有开启 AutoTrack
     */
    public boolean isAutoTrackEnabled() {
        return mAutoTrack;
    }

    public boolean isButterknifeOnClickEnabled() {
        return mEnableButterknifeOnClick;
    }

    /**
     * 是否开启自动追踪 Fragment 的 $AppViewScreen 事件
     * 默认不开启
     */
    public void trackFragmentAppViewScreen() {
        this.mTrackFragmentAppViewScreen = true;
    }

    public boolean isTrackFragmentAppViewScreenEnabled() {
        return this.mTrackFragmentAppViewScreen;
    }

    /**
     * 开启 AutoTrack 支持 React Native
     */
    public void enableReactNativeAutoTrack() {
        this.mEnableReactNativeAutoTrack = true;
    }

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
    @Deprecated
    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    public void showUpWebView(WebView webView, boolean isSupportJellyBean) {
        showUpWebView(webView, isSupportJellyBean, null);
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
    public void showUpWebView(WebView webView, boolean isSupportJellyBean, JSONObject properties) {
        if (Build.VERSION.SDK_INT < 17 && !isSupportJellyBean) {
            SALog.d(TAG, "For applications targeted to API level JELLY_BEAN or below, this feature NOT SUPPORTED");
            return;
        }

        if (webView != null) {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.addJavascriptInterface(new AppWebViewInterface(mContext, properties), "SensorsData_APP_JS_Bridge");
        }
    }

    /**
     * 指定哪些 activity 不被AutoTrack
     *
     * 指定activity的格式为：activity.getClass().getCanonicalName()
     *
     * @param activitiesList  activity列表
     */
    public void ignoreAutoTrackActivities(List<Class<?>> activitiesList) {
        if (activitiesList == null || activitiesList.size() == 0) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        for (Class<?> activity : activitiesList) {
            if (activity != null && !mAutoTrackIgnoredActivities.contains(activity.hashCode())) {
                mAutoTrackIgnoredActivities.add(activity.hashCode());
            }
        }
    }

    /**
     * 指定某个 activity 不被 AutoTrack
     * @param activity Activity
     */
    public void ignoreAutoTrackActivity(Class<?> activity) {
        if (activity == null) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        if (!mAutoTrackIgnoredActivities.contains(activity.hashCode())) {
            mAutoTrackIgnoredActivities.add(activity.hashCode());
        }
    }

    /**
     * 判断 AutoTrack 时，某个 Activity 的 $AppViewScreen 是否被过滤
     * 如果过滤的话，会过滤掉 Activity 的 $AppViewScreen 事件
     * @param activity Activity
     * @return Activity 是否被过滤
     */
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
     * 判断 AutoTrack 时，某个 Activity 的 $AppClick 是否被过滤
     * 如果过滤的话，会过滤掉 Activity 的 $AppClick 事件
     * @param activity Activity
     * @return Activity 是否被过滤
     */
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

    private List<AutoTrackEventType> mAutoTrackEventTypeList;

    /**
     * 过滤掉 AutoTrack 的某个事件类型
     * @param autoTrackEventType AutoTrackEventType
     */
    @Deprecated
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
    public boolean isAutoTrackEventTypeIgnored(AutoTrackEventType eventType) {
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
    public void ignoreView(View view) {
        if (view != null) {
            view.setTag(R.id.sensors_analytics_tag_view_ignored, "1");
        }
    }

    /**
     * 设置View属性
     *
     * @param view       要设置的View
     * @param properties 要设置的View的属性
     */
    public void setViewProperties(View view, JSONObject properties) {
        if (view == null || properties == null) {
            return;
        }

        view.setTag(R.id.sensors_analytics_tag_view_properties, properties);
    }

    private List<Class> mIgnoredViewTypeList = new ArrayList<>();

    public List<Class> getIgnoredViewTypeList() {
        if (mIgnoredViewTypeList == null) {
            mIgnoredViewTypeList = new ArrayList<>();
        }

        return mIgnoredViewTypeList;
    }

    /**
     * 忽略某一类型的 View
     *
     * @param viewType Class
     */
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
    public String getAnonymousId() {
        synchronized (mDistinctId) {
            return mDistinctId.get();
        }
    }

    /**
     * 重置默认匿名id
     */
    public void resetAnonymousId() {
        synchronized (mDistinctId) {
            if (mEnableAndroidId) {
                String androidId = SensorsDataUtils.getAndroidID(mContext);
                if (SensorsDataUtils.isValidAndroidId(androidId)) {
                    mDistinctId.commit(androidId);
                    return;
                }
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
    public void identify(String distinctId) {
        try {
            assertDistinctId(distinctId);
            synchronized (mDistinctId) {
                mDistinctId.commit(distinctId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 登录，设置当前用户的 loginId
     *
     * @param loginId 当前用户的 loginId，不能为空，且长度不能大于255
     */
    public void login(String loginId) {
        try {
            assertDistinctId(loginId);
            synchronized (mLoginId) {
                if (!loginId.equals(mLoginId.get())) {
                    mLoginId.commit(loginId);
                    if (!loginId.equals(getAnonymousId())) {
                        trackEvent(EventType.TRACK_SIGNUP, "$SignUp", null, getAnonymousId());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 注销，清空当前用户的 loginId
     */
    public void logout() {
        synchronized (mLoginId) {
            mLoginId.commit(null);
        }
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
    public void trackSignUp(String newDistinctId, JSONObject properties) {
        try {
            String originalDistinctId = getDistinctId();
            identify(newDistinctId);

            trackEvent(EventType.TRACK_SIGNUP, "$SignUp", properties, originalDistinctId);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    public void trackSignUp(String newDistinctId) {
        try {
            String originalDistinctId = getDistinctId();
            identify(newDistinctId);

            trackEvent(EventType.TRACK_SIGNUP, "$SignUp", null, originalDistinctId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 用于在 App 首次启动时追踪渠道来源，并设置追踪渠道事件的属性。
     *
     * 这是 Sensors Analytics 进阶功能，请参考文档 https://sensorsdata.cn/manual/track_installation.html
     *
     * @param eventName  渠道追踪事件的名称
     * @param properties 渠道追踪事件的属性
     */
    public void trackInstallation(String eventName, JSONObject properties) {
        try {
            boolean firstTrackInstallation = mFirstTrackInstallation.get();
            if (firstTrackInstallation) {
                try {
                    if (properties == null) {
                        properties = new JSONObject();
                    }

                    if (!SensorsDataUtils.hasUtmProperties(properties)) {
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
                                    properties.put(entry.getValue(), utmValue);
                                }
                            }
                        }
                    }

                    if (!SensorsDataUtils.hasUtmProperties(properties)) {
                        String installSource = String.format("android_id=%s##imei=%s##mac=%s",
                                SensorsDataUtils.getAndroidID(mContext),
                                SensorsDataUtils.getIMEI(mContext),
                                SensorsDataUtils.getMacAddress());
                        properties.put("$ios_install_source", installSource);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // 先发送 track
                trackEvent(EventType.TRACK, eventName, properties, null);

                // 再发送 profile_set_once
                trackEvent(EventType.PROFILE_SET_ONCE, null, properties, null);

                mFirstTrackInstallation.commit(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 调用track接口，追踪一个带有属性的事件
     *
     * @param eventName  事件的名称
     * @param properties 事件的属性
     */
    public void track(String eventName, JSONObject properties) {
        try {
            trackEvent(EventType.TRACK, eventName, properties, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 与 {@link #track(String, org.json.JSONObject)} 类似，无事件属性
     *
     * @param eventName 事件的名称
     */
    public void track(String eventName) {
        try {
            trackEvent(EventType.TRACK, eventName, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化事件的计时器，默认计时单位为毫秒。
     *
     * 详细用法请参考 trackTimer(String, TimeUnit)
     *
     * @param eventName 事件的名称
     */
    @Deprecated
    public void trackTimer(final String eventName) {
        try {
            trackTimer(eventName, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    public void trackTimer(final String eventName, final TimeUnit timeUnit) {
        try {
            assertKey(eventName);
            synchronized (mTrackTimer) {
                mTrackTimer.put(eventName, new EventTimer(timeUnit));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化事件的计时器，默认计时单位为毫秒。
     *
     * 详细用法请参考 trackTimerBegin(String, TimeUnit)
     *
     * @param eventName 事件的名称
     */
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
    public void trackTimerBegin(final String eventName, final TimeUnit timeUnit) {
        trackTimer(eventName, timeUnit);
    }

    /**
     * 停止事件计时器
     * @param eventName 事件的名称
     * @param properties 事件的属性
     */
    public void trackTimerEnd(final String eventName, JSONObject properties) {
        track(eventName, properties);
    }

    /**
     * 停止事件计时器
     * @param eventName 事件的名称
     */
    public void trackTimerEnd(final String eventName) {
        track(eventName);
    }

    /**
     * 清除所有事件计时器
     */
    public void clearTrackTimer() {
        synchronized (mTrackTimer) {
            mTrackTimer.clear();
        }
    }

    /**
     * 获取LastScreenUrl
     * @return String
     */
    public String getLastScreenUrl() {
        return mLastScreenUrl;
    }

    protected String getMainProcessName() {
        return mMainProcessName;
    }

    /**
     * 获取LastScreenTrackProperties
     * @return JSONObject
     */
    public JSONObject getLastScreenTrackProperties() {
        return mLastScreenTrackProperties;
    }

    /**
     * Track 进入页面事件 ($AppViewScreen)
     * @param url String
     * @param properties JSONObject
     */
    public void trackViewScreen(String url, JSONObject properties) {
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
        } catch (JSONException e) {
            SALog.i(TAG, "trackViewScreen:" + e);
        }
    }

    /**
     * Track Activity 进入页面事件($AppViewScreen)
     * @param activity activity Activity，当前 Activity
     */
    public void trackViewScreen(Activity activity) {
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
            SALog.i(TAG, "trackViewScreen:" + e);
        }
    }

    public void trackViewScreen(android.app.Fragment fragment) {
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
                    String activityTitle = AopUtil.getActivityTitle(activity);
                    if (!TextUtils.isEmpty(activityTitle)) {
                        properties.put(AopConstants.TITLE, activityTitle);
                    }
                    screenName = String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), fragmentName);
                }
            }

            properties.put(AopConstants.SCREEN_NAME, screenName);
            track("$AppViewScreen", properties);
        } catch (Exception e) {
            SALog.i(TAG, "trackViewScreen:" + e);
        }
    }

    public void trackViewScreen(android.support.v4.app.Fragment fragment) {
        try {
            if (fragment == null) {
                return;
            }

            JSONObject properties = new JSONObject();
            String fragmentName = fragment.getClass().getCanonicalName();
            String screenName = fragmentName;

            Activity activity = fragment.getActivity();
            if (activity != null) {
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AopConstants.TITLE, activityTitle);
                }
                screenName = String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), fragmentName);
            }

            properties.put(AopConstants.SCREEN_NAME, screenName);
            track("$AppViewScreen", properties);
        } catch (Exception e) {
            SALog.i(TAG, "trackViewScreen:" + e);
        }
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
    public void flush() {
        mMessages.flush();
    }

    /**
     * 以阻塞形式将所有本地缓存的日志发送到 Sensors Analytics，该方法不能在 UI 线程调用。
     */
    public void flushSync() {
        mMessages.sendData();
    }

    /**
     * 获取事件公共属性
     *
     * @return 当前所有Super属性
     */
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
    public void registerSuperProperties(JSONObject superProperties) {
        try {
            if (superProperties == null) {
                return;
            }
            assertPropertyTypes(EventType.REGISTER_SUPER_PROPERTIES, superProperties);
            synchronized (mSuperProperties) {
                JSONObject properties = mSuperProperties.get();
                SensorsDataUtils.mergeJSONObject(superProperties, properties);
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
    public void profileSet(JSONObject properties) {
        try {
            trackEvent(EventType.PROFILE_SET, null, properties, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置用户的一个Profile，如果之前存在，则覆盖，否则，新创建
     *
     * @param property 属性名称
     * @param value    属性的值，值的类型只允许为
     *                 {@link java.lang.String}, {@link java.lang.Number}, {@link java.util.Date}, {@link java.util.List}
     */
    public void profileSet(String property, Object value) {
        try {
            trackEvent(EventType.PROFILE_SET, null, new JSONObject().put(property, value), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 首次设置用户的一个或多个Profile。
     * 与profileSet接口不同的是，如果之前存在，则忽略，否则，新创建
     *
     * @param properties 属性列表
     */
    public void profileSetOnce(JSONObject properties)  {
        try {
            trackEvent(EventType.PROFILE_SET_ONCE, null, properties, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 首次设置用户的一个Profile
     * 与profileSet接口不同的是，如果之前存在，则忽略，否则，新创建
     *
     * @param property 属性名称
     * @param value    属性的值，值的类型只允许为
     *                 {@link java.lang.String}, {@link java.lang.Number}, {@link java.util.Date}, {@link java.util.List}
     */
    public void profileSetOnce(String property, Object value) {
        try {
            trackEvent(EventType.PROFILE_SET_ONCE, null, new JSONObject().put(property, value), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 给一个或多个数值类型的Profile增加一个数值。只能对数值型属性进行操作，若该属性
     * 未设置，则添加属性并设置默认值为0
     *
     * @param properties 一个或多个属性集合
     */
    public void profileIncrement(Map<String, ? extends Number> properties) {
        try {
            trackEvent(EventType.PROFILE_INCREMENT, null, new JSONObject(properties), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 给一个数值类型的Profile增加一个数值。只能对数值型属性进行操作，若该属性
     * 未设置，则添加属性并设置默认值为0
     *
     * @param property 属性名称
     * @param value    属性的值，值的类型只允许为 {@link java.lang.Number}
     */
    public void profileIncrement(String property, Number value) {
        try {
            trackEvent(EventType.PROFILE_INCREMENT, null, new JSONObject().put(property, value), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 给一个列表类型的Profile增加一个元素
     *
     * @param property 属性名称
     * @param value    新增的元素
     */
    public void profileAppend(String property, String value) {
        try {
            final JSONArray append_values = new JSONArray();
            append_values.put(value);
            final JSONObject properties = new JSONObject();
            properties.put(property, append_values);
            trackEvent(EventType.PROFILE_APPEND, null, properties, null);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 给一个列表类型的Profile增加一个或多个元素
     *
     * @param property 属性名称
     * @param values   新增的元素集合
     */
    public void profileAppend(String property, Set<String> values) {
        try {
            final JSONArray append_values = new JSONArray();
            for (String value : values) {
                append_values.put(value);
            }
            final JSONObject properties = new JSONObject();
            properties.put(property, append_values);
            trackEvent(EventType.PROFILE_APPEND, null, properties, null);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除用户的一个Profile
     *
     * @param property 属性名称
     */
    public void profileUnset(String property) {
        try {
            trackEvent(EventType.PROFILE_UNSET, null, new JSONObject().put(property, true), null);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除用户所有Profile
     */
    public void profileDelete() {
        try {
            trackEvent(EventType.PROFILE_DELETE, null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean isDebugMode() {
        return mDebugMode.isDebugMode();
    }

    boolean isDebugWriteData() {
        return mDebugMode.isDebugWriteData();
    }

    String getServerUrl() {
        return mServerUrl;
    }

    String getConfigureUrl() {
        return mConfigureUrl;
    }

    private void showDebugModeWarning() {
        try {
            if (mDebugMode == DebugMode.DEBUG_OFF) {
                return;
            }
            String info = null;
            if (mDebugMode == DebugMode.DEBUG_ONLY) {
                info = "现在您打开了神策SDK的'DEBUG_ONLY'模式，此模式下只校验数据但不导入数据，数据出错时会以 Toast 的方式提示开发者，请上线前一定使用 DEBUG_OFF 模式。";
            } else if (mDebugMode == DebugMode.DEBUG_AND_TRACK) {
                info = "现在您打开了神策SDK的'DEBUG_AND_TRACK'模式，此模式下校验数据并且导入数据，数据出错时会以 Toast 的方式提示开发者，请上线前一定使用 DEBUG_OFF 模式。";
            }

            Toast.makeText(mContext, info, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void trackEventFromH5(String eventInfo) {
        try {
            if (TextUtils.isEmpty(eventInfo)) {
                return;
            }

            JSONObject eventObject = new JSONObject(eventInfo);
            if (!TextUtils.isEmpty(getLoginId())) {
                eventObject.put("distinct_id", getLoginId());
            } else {
                eventObject.put("distinct_id", getAnonymousId());
            }

            try {
                Random random = new Random();
                eventObject.put("_track_id", random.nextInt());
            } catch (Exception e) {
                //ignore
            }

            String type = eventObject.getString("type");
            EventType eventType = EventType.valueOf(type.toUpperCase());

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

                //是否首日访问
                propertiesObject.put("$is_first_day", isFirstDay());

                if (propertiesObject.has("$is_first_time")) {
                    propertiesObject.remove("$is_first_time");
                }

                if (propertiesObject.has("_nocache")) {
                    propertiesObject.remove("_nocache");
                }
            }

            mMessages.enqueueEventMessage(type, eventObject);
        } catch (Exception e) {
            //ignore
            e.printStackTrace();
        }
    }

    private void trackEvent(final EventType eventType, final String eventName, final JSONObject properties, final String
            originalDistinctId) throws InvalidDataException {
        AopThreadPool.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (eventType.isTrack()) {
                        assertKey(eventName);
                    }
                    assertPropertyTypes(eventType, properties);

                    final long now = System.currentTimeMillis();

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
                        JSONObject sendProperties = null;

                        if (eventType.isTrack()) {
                            sendProperties = new JSONObject(mDeviceInfo);

                            synchronized (mSuperProperties) {
                                JSONObject superProperties = mSuperProperties.get();
                                SensorsDataUtils.mergeJSONObject(superProperties, sendProperties);
                            }

                            // 当前网络状况
                            String networkType = SensorsDataUtils.networkType(mContext);
                            sendProperties.put("$wifi", networkType.equals("WIFI"));
                            sendProperties.put("$network_type", networkType);
                        } else if (eventType.isProfile()) {
                            sendProperties = new JSONObject();
                        } else {
                            return;
                        }

                        if (null != properties) {
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
                        dataObj.put("time", now);
                        dataObj.put("type", eventType.getEventType());
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

                        // $binding_depolyed为true或者无该属性时，isDepolyed为true
                        final boolean isDepolyed = sendProperties.optBoolean("$binding_depolyed", true);

                        // 若$binding_depolyed为true，则删除这些属性
                        if (sendProperties.has("$binding_depolyed")) {
                            libProperties.put("$lib_method", "vtrack");
                            libProperties.put("$lib_detail", sendProperties.get("$binding_trigger_id").toString());

                            // 可视化埋点的事件
                            if (mVTrack instanceof DebugTracking) {
                                // Deep clone the event
                                JSONObject debugDataObj = new JSONObject(dataObj.toString());
                                ((DebugTracking) mVTrack).reportTrack(debugDataObj);
                            }

                            sendProperties.remove("$binding_path");
                            sendProperties.remove("$binding_depolyed");
                            sendProperties.remove("$binding_trigger_id");
                        } else {
                            libProperties.put("$lib_method", "code");

                            String libDetail = null;
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
                        }

                        if (isDepolyed) {
                            mMessages.enqueueEventMessage(eventType.getEventType(), dataObj);
                            SALog.i(TAG, "track event:\n" + JSONUtils.formatJson(dataObj.toString()));
                        }
                    } catch (JSONException e) {
                        throw new InvalidDataException("Unexpected property");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private boolean isFirstDay() {
        String firstDay = mFirstDay.get();
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

                if (value instanceof String && !key.startsWith("$") && ((String) value).length() > 8191) {
                    SALog.d(TAG, "The property value is too long. [key='" + key
                            + "', value='" + value.toString() + "']");
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
    static final String VERSION = "1.7.14";

    static Boolean ENABLE_LOG = false;
    static Boolean SHOW_DEBUG_INFO_VIEW = true;

    private static final Pattern KEY_PATTERN = Pattern.compile(
            "^((?!^distinct_id$|^original_id$|^time$|^properties$|^id$|^first_id$|^second_id$|^users$|^events$|^event$|^user_id$|^date$|^datetime$)[a-zA-Z_$][a-zA-Z\\d_$]{0,99})$",
            Pattern.CASE_INSENSITIVE);

    // Maps each token to a singleton SensorsDataAPI instance
    private static final Map<Context, SensorsDataAPI> sInstanceMap = new HashMap<>();
    private static final SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();

    // Configures
  /* SensorsAnalytics 地址 */
    private final String mServerUrl;
    /* 可视化埋点配置地址 */
    private final String mConfigureUrl;
    /* Debug模式选项 */
    private final DebugMode mDebugMode;
    /* Flush时间间隔 */
    private int mFlushInterval;
    /* Flush数据量阈值 */
    private int mFlushBulkSize;
    /* SDK 自动采集事件 */
    private boolean mAutoTrack;
    /* SDK 开启可视化埋点功能 */
    private boolean mEnableVTrack;
    /* AndroidId 作为默认匿名Id */
    private boolean mEnableAndroidId;
    /* 上个页面的Url*/
    private String mLastScreenUrl;
    private JSONObject mLastScreenTrackProperties;
    private boolean mEnableButterknifeOnClick;
    /* $AppViewScreen 事件是否支持 Fragment*/
    private boolean mTrackFragmentAppViewScreen;
    private boolean mEnableReactNativeAutoTrack;

    private final Context mContext;
    private final AnalyticsMessages mMessages;
    private final PersistentDistinctId mDistinctId;
    private final PersistentLoginId mLoginId;
    private final PersistentSuperProperties mSuperProperties;
    private final PersistentFirstStart mFirstStart;
    private final PersistentFirstDay mFirstDay;
    private final PersistentFirstTrackInstallation mFirstTrackInstallation;
    private final Map<String, Object> mDeviceInfo;
    private final Map<String, EventTimer> mTrackTimer;
    private List<Integer> mAutoTrackIgnoredActivities;
    private int mFlushNetworkPolicy = NetworkType.TYPE_3G | NetworkType.TYPE_4G | NetworkType.TYPE_WIFI;
    private final String mMainProcessName;
    private long mMaxCacheSize = 32 * 1024 * 1024; //default 32MB

    private final VTrack mVTrack;

    private static final SimpleDateFormat mIsFirstDayDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private static final String TAG = "SA.SensorsDataAPI";
}
