/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.sensorsdata.analytics.android.sdk.util.SensorsDataTimer;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by 王灼洲 on 2017/4/12
 */

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class SensorsDataActivityLifecycleCallbacks extends ContentObserver implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "SA.LifecycleCallbacks";
    private static final SimpleDateFormat mIsFirstDayDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private boolean resumeFromBackground = false;
    private final Object mActivityLifecycleCallbacksLock = new Object();
    private final SensorsDataAPI mSensorsDataInstance;
    private final PersistentFirstStart mFirstStart;
    private final PersistentFirstDay mFirstDay;
    private Context mContext;
    private static CountDownTimer mCountDownTimer;
    private static DbAdapter mDbAdapter;
    private static final String EVENT_TIMER = "event_timer";
    public SensorsDataActivityLifecycleCallbacks(SensorsDataAPI instance, PersistentFirstStart firstStart,
                                                 PersistentFirstDay firstDay, Context context, String packageName) {
        super(new Handler());
        this.mSensorsDataInstance = instance;
        this.mFirstStart = firstStart;
        this.mFirstDay = firstDay;
        this.mDbAdapter = new DbAdapter(context, packageName);

        initCountDownTimer();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        try {
            Uri uri = null;
            if (activity != null) {
                Intent intent = activity.getIntent();
                if (intent != null) {
                    uri = intent.getData();
                }
            }
            if (uri != null) {
                String host = uri.getHost();
                if ("heatmap".equals(host)) {
                    String featureCode = uri.getQueryParameter("feature_code");
                    String postUrl = uri.getQueryParameter("url");
                    showOpenHeatMapDialog(activity, featureCode, postUrl);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        try {
            synchronized (mActivityLifecycleCallbacksLock) {
                mContext = activity.getApplicationContext();
                boolean isAutoTrackEnabled = mSensorsDataInstance.isAutoTrackEnabled();
                if (!isAutoTrackEnabled) {
                    if (mFirstDay.get() == null) {
                        mFirstDay.commit(mIsFirstDayDateFormat.format(System.currentTimeMillis()));
                    }
                    //每次启动 App，重新拉取最新的配置信息
                    mSensorsDataInstance.pullSDKConfigFromServer();
                    return;
                }

                double timeDiff = System.currentTimeMillis() - mDbAdapter.getAppPausedTime();
                SALog.d(TAG, "timeDiff:" + timeDiff);
                if (timeDiff > mDbAdapter.getSessionIntervalTime()) {
                    if (!mDbAdapter.getAppEndState()) {
                        trackAppEnd();
                    }
                }

                if (mDbAdapter.getAppEndState()) {
                    mDbAdapter.commitAppEndState(false);

                    if (mFirstDay.get() == null) {
                        mFirstDay.commit(mIsFirstDayDateFormat.format(System.currentTimeMillis()));
                    }

                    // XXX: 注意内部执行顺序
                    boolean firstStart = mFirstStart.get();

                    try {
                        mSensorsDataInstance.appBecomeActive();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //从后台恢复，从缓存中读取 SDK 控制配置信息
                    if (resumeFromBackground) {
                        //先从缓存中读取 SDKConfig
                        mSensorsDataInstance.applySDKConfigFromCache();
                        mSensorsDataInstance.resumeTrackScreenOrientation();
                        mSensorsDataInstance.resumeTrackTaskThread();
                    }
                    //每次启动 App，重新拉取最新的配置信息
                    mSensorsDataInstance.pullSDKConfigFromServer();


                    try {
                        if (!mSensorsDataInstance.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_START)) {
                            if (firstStart) {
                                mFirstStart.commit(false);
                            }
                            JSONObject properties = new JSONObject();
                            properties.put("$resume_from_background", resumeFromBackground);
                            properties.put("$is_first_time", firstStart);
                            SensorsDataUtils.getScreenNameAndTitleFromActivity(properties, activity);

                            mSensorsDataInstance.track("$AppStart", properties);
                        }

                        if (!mSensorsDataInstance.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_END)) {
                            mDbAdapter.commitAppStartTime(SystemClock.elapsedRealtime());
                            mSensorsDataInstance.trackTimer("$AppEnd", TimeUnit.SECONDS);
                        }
                    } catch (Exception e) {
                        SALog.i(TAG, e);
                    }

                    if (resumeFromBackground) {
                        try {
                            HeatMapService.getInstance().resume();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // 下次启动时，从后台恢复
                    resumeFromBackground = true;

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResumed(final Activity activity) {
        try {
            mDbAdapter.commitAppStart(true);

            boolean mShowAutoTrack = true;
            boolean isAutoTrackEnabled = mSensorsDataInstance.isAutoTrackEnabled();
            if (mSensorsDataInstance.isActivityAutoTrackAppViewScreenIgnored(activity.getClass())) {
                mShowAutoTrack = false;
            }

            if (isAutoTrackEnabled && mShowAutoTrack && !mSensorsDataInstance.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_VIEW_SCREEN)) {
                try {
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

                        mSensorsDataInstance.trackViewScreen(screenUrl, properties);
                    } else {
                        SensorsDataAutoTrackAppViewScreenUrl autoTrackAppViewScreenUrl = activity.getClass().getAnnotation(SensorsDataAutoTrackAppViewScreenUrl.class);
                        if (autoTrackAppViewScreenUrl != null) {
                            String screenUrl = autoTrackAppViewScreenUrl.url();
                            if (TextUtils.isEmpty(screenUrl)) {
                                screenUrl = activity.getClass().getCanonicalName();
                            }
                            mSensorsDataInstance.trackViewScreen(screenUrl, properties);
                        } else {
                            mSensorsDataInstance.track("$AppViewScreen", properties);
                        }
                    }
                } catch (Exception e) {
                    SALog.i(TAG, e);
                }
            }

            if (isAutoTrackEnabled && !mSensorsDataInstance.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_END)) {
                SensorsDataTimer.getInstance().timer(new Runnable() {
                    @Override
                    public void run() {
                        generateAppEndData(activity);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        boolean isAutoTrackEnabled = mSensorsDataInstance.isAutoTrackEnabled();
        if (!isAutoTrackEnabled) {
            return;
        }
        try {
            synchronized (mActivityLifecycleCallbacksLock) {
                mCountDownTimer.start();
                // cancel TimerTask of current Activity
                SensorsDataTimer.getInstance().cancleTimerTask();
                // store $AppEnd data
                generateAppEndData(activity);
                mDbAdapter.commitAppPausedTime(System.currentTimeMillis());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        try {
            if (mDbAdapter.getAppStartUri().equals(uri)) {
                boolean isAppStart = mDbAdapter.getAppStart();
                if (isAppStart) {
                    if (mCountDownTimer != null) {
                        mCountDownTimer.cancel();
                    }
                }
            } else if (mDbAdapter.getIntervalTimeUri().equals(uri)) {
                initCountDownTimer();
            } else if (mDbAdapter.getAppEndStateUri().equals(uri)) {
                if (mDbAdapter.getAppEndState()) {
                    mSensorsDataInstance.flush(3000);
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *  send $AppEnd 事件
     */
    private void trackAppEnd(){
        if (mDbAdapter.getAppEndState()) {
            return;
        }
        try {
            mSensorsDataInstance.stopTrackScreenOrientation();
            mSensorsDataInstance.resetPullSDKConfigTimer();
            HeatMapService.getInstance().stop();
            mSensorsDataInstance.appEnterBackground();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mContext != null) {
            if (mSensorsDataInstance.isAutoTrackEnabled()) {
                try {
                    if (!mSensorsDataInstance.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_END)) {
                        String jsonEndData = mDbAdapter.getAppEndData();
                        JSONObject endDataJsonObject = null;
                        if (!TextUtils.isEmpty(jsonEndData)) {
                            endDataJsonObject = new JSONObject(jsonEndData);
                            if (endDataJsonObject.has(EVENT_TIMER)) {
                                long startTime = mDbAdapter.getAppStartTime();
                                long endTime = endDataJsonObject.getLong(EVENT_TIMER);
                                EventTimer eventTimer = new EventTimer(TimeUnit.SECONDS, startTime, endTime);
                                SALog.d(TAG,"startTime:" + startTime + "--endTime:" + endTime + "--event_duration:" + eventTimer.duration());
                                mSensorsDataInstance.trackTimer("$AppEnd", eventTimer);
                                endDataJsonObject.remove(EVENT_TIMER);
                            }
                        }
                        JSONObject properties = new JSONObject();
                        if (endDataJsonObject != null) {
                            properties = new JSONObject(endDataJsonObject.toString());
                        }

                        mSensorsDataInstance.clearLastScreenUrl();
                        properties.put("event_time", mDbAdapter.getAppPausedTime());
                        mSensorsDataInstance.track("$AppEnd", properties);
                    }
                } catch (Exception e) {
                    SALog.i(TAG, e);
                }
            }
        }
        try {
            mDbAdapter.commitAppEndState(true);
            // 下次启动时，从后台恢复
            resumeFromBackground = true;
            mSensorsDataInstance.stopTrackTaskThread();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *  存储当前的 AppEnd 事件关键信息
     * @param activity
     * @return
     */
    private void generateAppEndData(Activity activity){
        JSONObject properties = new JSONObject();
        try {
            SensorsDataUtils.getScreenNameAndTitleFromActivity(properties, activity);
            properties.put(EVENT_TIMER, SystemClock.elapsedRealtime());
            mDbAdapter.commitAppEndData(properties.toString());
            mDbAdapter.commitAppPausedTime(System.currentTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initCountDownTimer(){
        mCountDownTimer = new CountDownTimer(mDbAdapter.getSessionIntervalTime(),10*1000) {
            @Override
            public void onTick(long l) {
                Log.d(TAG,"time:" + l);
            }

            @Override
            public void onFinish() {
                Log.d(TAG,"timeFinish");
                trackAppEnd();
            }
        };
    }

    private void showOpenHeatMapDialog(final Activity context, final String featureCode, final String postUrl) {
        try {
            if (!SensorsDataAPI.sharedInstance().isAppHeatMapConfirmDialogEnabled()) {
                HeatMapService.getInstance().start(context, featureCode, postUrl);
                return;
            }

            boolean isWifi = false;
            try {
                String networkType = SensorsDataUtils.networkType(context);
                if (networkType.equals("WIFI")) {
                    isWifi = true;
                }
            } catch (Exception e) {

            }

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("提示");
            if (isWifi) {
                builder.setMessage("正在连接 APP 点击分析");
            } else {
                builder.setMessage("正在连接 APP 点击分析，建议在 WiFi 环境下使用");
            }
            builder.setCancelable(false);
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.setPositiveButton("继续", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    HeatMapService.getInstance().start(context, featureCode, postUrl);
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();

            try {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
