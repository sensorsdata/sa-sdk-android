/*
 * Created by dengshiwei on 2020/10/20.
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

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.core.business.DefaultAppState;
import com.sensorsdata.analytics.android.sdk.core.business.SAPropertyManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.core.mediator.SAModuleManager;
import com.sensorsdata.analytics.android.sdk.core.rpc.SensorsDataContentObserver;
import com.sensorsdata.analytics.android.sdk.core.tasks.ThreadNameConstants;
import com.sensorsdata.analytics.android.sdk.core.tasks.TrackTaskManagerThread;
import com.sensorsdata.analytics.android.sdk.data.SAProviderHelper;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.exceptions.SensorsDataExceptionHandler;
import com.sensorsdata.analytics.android.sdk.internal.beans.InternalConfigOptions;
import com.sensorsdata.analytics.android.sdk.jsbridge.H5Helper;
import com.sensorsdata.analytics.android.sdk.listener.SAEventListener;
import com.sensorsdata.analytics.android.sdk.listener.SAFunctionListener;
import com.sensorsdata.analytics.android.sdk.listener.SAJSListener;
import com.sensorsdata.analytics.android.sdk.monitor.SensorsDataLifecycleMonitorManager;
import com.sensorsdata.analytics.android.sdk.monitor.TrackMonitor;
import com.sensorsdata.analytics.android.sdk.remote.SensorsDataRemoteManager;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.AppStateTools;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

abstract class AbstractSensorsDataAPI implements ISensorsDataAPI {
    protected static final String TAG = "SA.SensorsDataAPI";
    // SDK版本
    static final String VERSION = BuildConfig.SDK_VERSION;
    // Maps each token to a singleton SensorsDataAPI instance
    protected static final Map<Context, SensorsDataAPI> sInstanceMap = new ConcurrentHashMap<>();
    /* 远程配置 */
    protected static SAConfigOptions mSAConfigOptions;
    protected InternalConfigOptions mInternalConfigs;
    protected SAContextManager mSAContextManager;
    protected final Object mLoginIdLock = new Object();
    /* SensorsAnalytics 地址 */
    protected String mServerUrl;
    protected String mOriginServerUrl;
    /* SDK 配置是否初始化 */
    protected boolean mSDKConfigInit;
    protected List<Integer> mHeatMapActivities;
    protected List<Integer> mVisualizedAutoTrackActivities;
    protected TrackTaskManager mTrackTaskManager;
    protected TrackTaskManagerThread mTrackTaskManagerThread;
    protected SensorsDataDynamicSuperProperties mDynamicSuperPropertiesCallBack;

    public AbstractSensorsDataAPI(Context context, SAConfigOptions configOptions, SensorsDataAPI.DebugMode debugMode) {
        mInternalConfigs = new InternalConfigOptions();
        mInternalConfigs.context = context;
        setDebugMode(debugMode);
        mHeatMapActivities = new ArrayList<>();
        mVisualizedAutoTrackActivities = new ArrayList<>();
        try {
            mSAConfigOptions = configOptions.clone();
            PersistentLoader.preInit(context);
            mTrackTaskManager = TrackTaskManager.getInstance();
            mTrackTaskManagerThread = new TrackTaskManagerThread();
            new Thread(mTrackTaskManagerThread, ThreadNameConstants.THREAD_TASK_QUEUE).start();
            SensorsDataExceptionHandler.init();
            // 1. init config
            initSAConfig(mSAConfigOptions.mServerUrl);
            // 2. init context manager
            mSAContextManager = new SAContextManager((SensorsDataAPI) this, mInternalConfigs);
            registerLifecycleCallbacks((SensorsDataAPI) this, context);
            delayInitTask(context.getApplicationContext());
            /* 迁移数据，并删除老的数据库 */
            SAProviderHelper.getInstance(context).migratingDB(context.getApplicationContext());
            enableTrackScreenOrientation(mSAConfigOptions.mTrackScreenOrientationEnabled);
            if (SALog.isLogEnabled()) {
                SALog.i(TAG, String.format(TimeUtils.SDK_LOCALE, "Initialized the instance of Sensors Analytics SDK with server"
                        + " url '%s', flush interval %d ms, debugMode: %s", mServerUrl, mSAConfigOptions.mFlushInterval, debugMode));
            }
            SensorsDataUtils.initUniAppStatus();
        } catch (Throwable ex) {
            SALog.i(TAG, ex.getMessage());
        }
    }

    protected AbstractSensorsDataAPI() {

    }

    /**
     * 返回采集控制是否关闭了 SDK
     *
     * @return true：关闭；false：没有关闭
     */
    private static boolean isSDKDisabledByRemote() {
        boolean isSDKDisabled = SensorsDataRemoteManager.isSDKDisabledByRemote();
        if (isSDKDisabled) {
            SALog.i(TAG, "remote config: SDK is disabled");
        }
        return isSDKDisabled;
    }

    /**
     * 返回本地是否关闭了 SDK
     *
     * @return true：关闭；false：没有关闭
     */
    private static boolean isSDKDisableByLocal() {
        if (mSAConfigOptions == null) {
            SALog.i(TAG, "SAConfigOptions is null");
            return true;
        }
        return mSAConfigOptions.isDisableSDK;
    }

    /**
     * 返回是否关闭了 SDK
     *
     * @return true：关闭；false：没有关闭
     */
    public static boolean isSDKDisabled() {
        return isSDKDisableByLocal() || isSDKDisabledByRemote();
    }

    /**
     * SDK 事件回调监听，目前用于弹窗业务
     *
     * @param eventListener 事件监听
     */
    public void addEventListener(SAEventListener eventListener) {
        mSAContextManager.addEventListener(eventListener);
    }

    /**
     * 移除 SDK 事件回调监听
     *
     * @param eventListener 事件监听
     */
    public void removeEventListener(SAEventListener eventListener) {
        mSAContextManager.removeEventListener(eventListener);
    }

    /**
     * 监听 JS 消息
     *
     * @param listener JS 监听
     */
    public void addSAJSListener(SAJSListener listener) {
        H5Helper.addSAJSListener(listener);
    }

    /**
     * 移除 JS 消息
     *
     * @param listener JS 监听
     */
    public void removeSAJSListener(SAJSListener listener) {
        H5Helper.removeSAJSListener(listener);
    }

    /**
     * SDK 函数回调监听
     *
     * @param functionListener 事件监听
     */
    public void addFunctionListener(final SAFunctionListener functionListener) {
        //在事件队列中操作了 mFunctionListenerList，此处也需要放在事件队列中
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                TrackMonitor.getInstance().addFunctionListener(functionListener);
            }
        });
    }

    /**
     * 移除 SDK 事件回调监听
     *
     * @param functionListener 事件监听
     */
    public void removeFunctionListener(final SAFunctionListener functionListener) {
        try {
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    TrackMonitor.getInstance().removeFunctionListener(functionListener);
                }
            });
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    public static SAConfigOptions getConfigOptions() {
        return mSAConfigOptions;
    }

    public SensorsDataAPI.DebugMode getDebugMode() {
        return mInternalConfigs.debugMode;
    }

    public void setDebugMode(SensorsDataAPI.DebugMode debugMode) {
        mInternalConfigs.debugMode = debugMode;
        if (debugMode == SensorsDataAPI.DebugMode.DEBUG_OFF) {
            enableLog(false);
            SALog.setDebug(false);
            mServerUrl = mOriginServerUrl;
        } else {
            enableLog(true);
            SALog.setDebug(true);
            setServerUrl(mOriginServerUrl);
        }
    }

    public boolean isDisableDefaultRemoteConfig() {
        return mInternalConfigs.isDefaultRemoteConfigEnable;
    }

    public SAContextManager getSAContextManager() {
        return mSAContextManager;
    }

    void registerNetworkListener(final Context context) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                NetworkUtils.registerNetworkListener(context);
            }
        });
    }

    void unregisterNetworkListener(final Context context) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                NetworkUtils.unregisterNetworkListener(context);
            }
        });
    }

    protected void initSAConfig(String serverURL) {
        Bundle configBundle = AppInfoUtils.getAppInfoBundle(mInternalConfigs.context);
        if (mSAConfigOptions == null) {
            this.mSDKConfigInit = false;
            mSAConfigOptions = new SAConfigOptions(serverURL);
        } else {
            this.mSDKConfigInit = true;
        }

        if (mSAConfigOptions.mInvokeLog) {
            enableLog(mSAConfigOptions.mLogEnabled);
        } else {
            enableLog(configBundle.getBoolean("com.sensorsdata.analytics.android.EnableLogging",
                    mInternalConfigs.debugMode != SensorsDataAPI.DebugMode.DEBUG_OFF));
        }
        SALog.setDisableSDK(mSAConfigOptions.isDisableSDK);

        setServerUrl(serverURL);
        if (mSAConfigOptions.mEnableTrackAppCrash) {
            SensorsDataExceptionHandler.enableAppCrash();
        }

        if (mSAConfigOptions.mFlushInterval == 0) {
            mSAConfigOptions.setFlushInterval(configBundle.getInt("com.sensorsdata.analytics.android.FlushInterval",
                    15000));
        }

        if (mSAConfigOptions.mFlushBulkSize == 0) {
            mSAConfigOptions.setFlushBulkSize(configBundle.getInt("com.sensorsdata.analytics.android.FlushBulkSize",
                    100));
        }

        if (mSAConfigOptions.mMaxCacheSize == 0) {
            mSAConfigOptions.setMaxCacheSize(32 * 1024 * 1024L);
        }

        if (!mSAConfigOptions.mInvokeHeatMapEnabled) {
            mSAConfigOptions.mHeatMapEnabled = configBundle.getBoolean("com.sensorsdata.analytics.android.HeatMap",
                    false);
        }

        if (!mSAConfigOptions.mInvokeVisualizedEnabled) {
            mSAConfigOptions.mVisualizedEnabled = configBundle.getBoolean("com.sensorsdata.analytics.android.VisualizedAutoTrack",
                    false);
        }
        mInternalConfigs.saConfigOptions = mSAConfigOptions;
        mInternalConfigs.isShowDebugView = configBundle.getBoolean("com.sensorsdata.analytics.android.ShowDebugInfoView",
                true);

        mInternalConfigs.isDefaultRemoteConfigEnable = configBundle.getBoolean("com.sensorsdata.analytics.android.DisableDefaultRemoteConfig",
                false);

        mInternalConfigs.isMainProcess = AppInfoUtils.isMainProcess(mInternalConfigs.context, configBundle);
        mInternalConfigs.isTrackDeviceId = configBundle.getBoolean("com.sensorsdata.analytics.android.DisableTrackDeviceId", false);

        if (mSAConfigOptions.getLimitKeys() != null) {
            SAPropertyManager.getInstance().registerLimitKeys(mSAConfigOptions.getLimitKeys());
        }
    }

    protected void applySAConfigOptions() {
        if (mSAConfigOptions.mEnableTrackAppCrash) {
            SensorsDataExceptionHandler.enableAppCrash();
        }

        if (mSAConfigOptions.mInvokeLog) {
            enableLog(mSAConfigOptions.mLogEnabled);
        }

        enableTrackScreenOrientation(mSAConfigOptions.mTrackScreenOrientationEnabled);

        //由于自定义属性依赖于可视化全埋点，所以只要开启自定义属性，默认打开可视化全埋点功能
        if (!mSAConfigOptions.mVisualizedEnabled && mSAConfigOptions.mVisualizedPropertiesEnabled) {
            SALog.i(TAG, "The VisualizedProperties is enabled, and visualizedEnable is false");
            mSAConfigOptions.enableVisualizedAutoTrack(true);
        }
    }

    /**
     * 读取动态公共属性
     *
     * @return 动态公共属性
     */
    public JSONObject getDynamicProperty() {
        JSONObject dynamicProperty = null;
        try {
            if (mDynamicSuperPropertiesCallBack != null) {
                dynamicProperty = mDynamicSuperPropertiesCallBack.getDynamicSuperProperties();
                SADataHelper.assertPropertyTypes(dynamicProperty);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return dynamicProperty;
    }

    /**
     * 注册 ActivityLifecycleCallbacks
     */
    private void registerLifecycleCallbacks(SensorsDataAPI sensorsDataAPI, Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                final Application app = (Application) context.getApplicationContext();
                SensorsDataLifecycleMonitorManager.getInstance().addActivityLifeCallback(AppStateTools.getInstance());
                AppStateTools.getInstance().addAppStateListener(new DefaultAppState(sensorsDataAPI));
                AppStateTools.getInstance().delayInit(context);
                /* 防止并发问题注册一定要在 {@link SensorsDataActivityLifecycleCallbacks#addActivityLifecycleCallbacks(SensorsDataActivityLifecycleCallbacks.SAActivityLifecycleCallbacks)} 之后执行 */
                app.registerActivityLifecycleCallbacks(SensorsDataLifecycleMonitorManager.getInstance().getCallback());
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 注册 ContentObserver 监听
     */
    private void registerObserver(Context context) {
        // 注册跨进程业务的 ContentObserver 监听
        SensorsDataContentObserver contentObserver = new SensorsDataContentObserver();
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.registerContentObserver(DbParams.getInstance().getSessionTimeUri(), false, contentObserver);
        contentResolver.registerContentObserver(DbParams.getInstance().getLoginIdUri(), false, contentObserver);
        contentResolver.registerContentObserver(DbParams.getInstance().getDisableSDKUri(), false, contentObserver);
        contentResolver.registerContentObserver(DbParams.getInstance().getEnableSDKUri(), false, contentObserver);
        contentResolver.registerContentObserver(DbParams.getInstance().getUserIdentities(), false, contentObserver);
    }

    /**
     * 延迟初始化任务
     */
    protected void delayInitTask(final Context context) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    registerObserver(context);
                    registerNetworkListener(context);
                } catch (Exception ex) {
                    SALog.printStackTrace(ex);
                }
            }
        });
    }
}
