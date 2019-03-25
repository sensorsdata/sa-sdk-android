/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */

package com.sensorsdata.analytics.android.sdk;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
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
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.widget.Toast;

import com.sensorsdata.analytics.android.sdk.data.DbAdapter;
import com.sensorsdata.analytics.android.sdk.data.DbParams;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstDay;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstStart;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataTimer;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by 王灼洲 on 2017/4/12
 */

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class SensorsDataActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "SA.LifecycleCallbacks";
    private SimpleDateFormat mIsFirstDayDateFormat;
    private Context mContext;
    private boolean resumeFromBackground = false;
    private final SensorsDataAPI mSensorsDataInstance;
    private final PersistentFirstStart mFirstStart;
    private final PersistentFirstDay mFirstDay;
    private CountDownTimer mCountDownTimer;
    private DbAdapter mDbAdapter;
    private JSONObject activityProperty = new JSONObject();
    private JSONObject endDataProperty = new JSONObject();
    private boolean isAutoTrackEnabled;
    private static final String EVENT_TIMER = "event_timer";
    public SensorsDataActivityLifecycleCallbacks(SensorsDataAPI instance, PersistentFirstStart firstStart,
                                                 PersistentFirstDay firstDay, Context context) {
        this.mSensorsDataInstance = instance;
        this.mFirstStart = firstStart;
        this.mFirstDay = firstDay;
        this.mContext = context;
        this.mDbAdapter = DbAdapter.getInstance();
        try {
            mIsFirstDayDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        if (Looper.myLooper() == null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    initTimerAndObserver();
                    Looper.loop();
                }
            }).start();
        } else {
            initTimerAndObserver();
        }
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
                } else if ("debugmode".equals(host)) {
                    String infoId = uri.getQueryParameter("info_id");
                    showDebugModeSelectDialog(activity, infoId);
                }
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        try {
            isAutoTrackEnabled = mSensorsDataInstance.isAutoTrackEnabled();
            if (!isAutoTrackEnabled) {
                if (mFirstDay.get() == null) {
                    if (mIsFirstDayDateFormat == null) {
                        mIsFirstDayDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    }
                    mFirstDay.commit(mIsFirstDayDateFormat.format(System.currentTimeMillis()));
                }
                //先从缓存中读取 SDKConfig
                mSensorsDataInstance.applySDKConfigFromCache();
                //每次启动 App，重新拉取最新的配置信息
                mSensorsDataInstance.pullSDKConfigFromServer();
                return;
            }

            SensorsDataUtils.getScreenNameAndTitleFromActivity(activityProperty, activity);
            SensorsDataUtils.mergeJSONObject(activityProperty, endDataProperty);
            boolean sessionTimeOut = Math.abs(System.currentTimeMillis() - mDbAdapter.getAppPausedTime()) > mDbAdapter.getSessionIntervalTime();
            SALog.d(TAG, "SessionTimeOut:" + sessionTimeOut);
            if (sessionTimeOut && !mDbAdapter.getAppEndState()) {
                trackAppEnd();
            }

            if (sessionTimeOut || mDbAdapter.getAppEndState()) {
                mDbAdapter.commitAppEndState(false);

                if (mFirstDay.get() == null) {
                    if (mIsFirstDayDateFormat == null) {
                        mIsFirstDayDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    }
                    mFirstDay.commit(mIsFirstDayDateFormat.format(System.currentTimeMillis()));
                }

                // XXX: 注意内部执行顺序
                boolean firstStart = mFirstStart.get();

                try {
                    mSensorsDataInstance.appBecomeActive();
                } catch (Exception e) {
                    com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
                }

                //从后台恢复，从缓存中读取 SDK 控制配置信息
                if (resumeFromBackground) {
                    //先从缓存中读取 SDKConfig
                    mSensorsDataInstance.applySDKConfigFromCache();
                    mSensorsDataInstance.resumeTrackScreenOrientation();
                    mSensorsDataInstance.resumeTrackTaskThread();
                    isAutoTrackEnabled = mSensorsDataInstance.isAutoTrackEnabled();
                }
                //每次启动 App，重新拉取最新的配置信息
                mSensorsDataInstance.pullSDKConfigFromServer();

                try {
                    if (isAutoTrackEnabled) {
                        if (!mSensorsDataInstance.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_START)) {
                            if (firstStart) {
                                mFirstStart.commit(false);
                            }
                            JSONObject properties = new JSONObject();
                            properties.put("$resume_from_background", resumeFromBackground);
                            properties.put("$is_first_time", firstStart);
                            SensorsDataUtils.mergeJSONObject(activityProperty, properties);
                            mSensorsDataInstance.track("$AppStart", properties);
                        }

                        if (!mSensorsDataInstance.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_END)) {
                            mDbAdapter.commitAppStartTime(SystemClock.elapsedRealtime());
                            mSensorsDataInstance.trackTimer("$AppEnd", TimeUnit.SECONDS);
                        }
                    }
                } catch (Exception e) {
                    SALog.i(TAG, e);
                }

                if (resumeFromBackground) {
                    try {
                        HeatMapService.getInstance().resume();
                    } catch (Exception e) {
                        com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
                    }
                }

                // 下次启动时，从后台恢复
                resumeFromBackground = true;

            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public void onActivityResumed(final Activity activity) {
        try {
            mDbAdapter.commitAppStart(true);

            boolean mShowAutoTrack = true;
            if (mSensorsDataInstance.isActivityAutoTrackAppViewScreenIgnored(activity.getClass())) {
                mShowAutoTrack = false;
            }

            if (isAutoTrackEnabled && mShowAutoTrack && !mSensorsDataInstance.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_VIEW_SCREEN)) {
                try {
                    JSONObject properties = new JSONObject();
                    SensorsDataUtils.mergeJSONObject(activityProperty, properties);
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
                        generateAppEndData();
                    }
                }, 500, 1000);
            }
            SensorsDataTimer.getInstance().timer(new Runnable() {
                @Override
                public void run() {
                    mSensorsDataInstance.flushDataSync();
                }
            }, 15000, 15000);

        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (!isAutoTrackEnabled) {
            return;
        }
        try {
            mCountDownTimer.start();
            mDbAdapter.commitAppStart(false);
            // cancel TimerTask of current Activity
            SensorsDataTimer.getInstance().cancelTimerTask();
            generateAppEndData();
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        mSensorsDataInstance.flushDataSync();
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
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
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }

        if (isAutoTrackEnabled) {
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
        try {
            mDbAdapter.commitAppEndState(true);
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    /**
     *  存储当前的 AppEnd 事件关键信息
     */
    private void generateAppEndData(){
        try {
            endDataProperty.put(EVENT_TIMER, SystemClock.elapsedRealtime());
            mDbAdapter.commitAppEndData(endDataProperty.toString());
            mDbAdapter.commitAppPausedTime(System.currentTimeMillis());
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    private void initTimerAndObserver() {
        initCountDownTimer();
        registerObserver();
    }

    private void initCountDownTimer(){
        mCountDownTimer = new CountDownTimer(mDbAdapter.getSessionIntervalTime(),10*1000) {
            @Override
            public void onTick(long l) {
                SALog.d(TAG,"time:" + l);
            }

            @Override
            public void onFinish() {
                SALog.d(TAG,"timeFinish");
                trackAppEnd();
                resumeFromBackground = true;
                mSensorsDataInstance.stopTrackTaskThread();
            }
        };
    }

    private void registerObserver(){
        final SensorsActivityStateObserver activityStateObserver = new SensorsActivityStateObserver(new Handler(Looper.myLooper()));
        mContext.getContentResolver().registerContentObserver(DbParams.getInstance().getAppStartUri(), false, activityStateObserver);
        mContext.getContentResolver().registerContentObserver(DbParams.getInstance().getSessionTimeUri(),false, activityStateObserver);
        mContext.getContentResolver().registerContentObserver(DbParams.getInstance().getAppEndStateUri(),false, activityStateObserver);
    }

    private void showDebugModeSelectDialog(final Activity activity, final String infoId) {
        try {
            DebugModeSelectDialog dialog = new DebugModeSelectDialog(activity, mSensorsDataInstance.getDebugMode());
            dialog.setCanceledOnTouchOutside(false);
            dialog.setOnDebugModeDialogClickListener(new DebugModeSelectDialog.OnDebugModeViewClickListener() {
                @Override
                public void onCancel(Dialog dialog) {
                    dialog.cancel();
                }

                @Override
                public void setDebugMode(Dialog dialog, SensorsDataAPI.DebugMode debugMode) {
                    mSensorsDataInstance.setDebugMode(debugMode);
                    dialog.cancel();
                }
            });
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    //如果当前的调试模式不是 DebugOff ,则发送匿名或登录 ID 给服务端
                    String serverUrl = mSensorsDataInstance.getServerUrl();
                    SensorsDataAPI.DebugMode mCurrentDebugMode = mSensorsDataInstance.getDebugMode();
                    if (!TextUtils.isEmpty(serverUrl) && !TextUtils.isEmpty(infoId) && mCurrentDebugMode != SensorsDataAPI.DebugMode.DEBUG_OFF) {
                        new SendDebugIdThread(serverUrl, mSensorsDataInstance.getCurrentDistinctId(), infoId).start();
                    }
                    String currentDebugToastMsg = "";
                    if (mCurrentDebugMode == SensorsDataAPI.DebugMode.DEBUG_OFF) {
                        currentDebugToastMsg = "已关闭调试模式，请重新扫描二维码进行开启";
                    } else if (mCurrentDebugMode == SensorsDataAPI.DebugMode.DEBUG_ONLY) {
                        currentDebugToastMsg = "开启调试模式，校验数据，但不进行数据导入；关闭 App 进程后，将自动关闭调试模式";
                    } else if (mCurrentDebugMode == SensorsDataAPI.DebugMode.DEBUG_AND_TRACK) {
                        currentDebugToastMsg = "开启调试模式，校验数据，并将数据导入到神策分析中；关闭 App 进程后，将自动关闭调试模式";
                    }
                    Toast.makeText(activity, currentDebugToastMsg, Toast.LENGTH_LONG).show();
                    SALog.info(TAG,"您当前的调试模式是："+mCurrentDebugMode, null);
                }
            });
            dialog.show();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
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
                SALog.printStackTrace(e);
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
                com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private class SensorsActivityStateObserver extends ContentObserver {

        /**
         * Creates a content observer.
         *
         * @param handler The handler to run {@link #onChange} on, or null if none.
         */
        SensorsActivityStateObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            try {
                if (DbParams.getInstance().getAppStartUri().equals(uri)) {
                    if (mCountDownTimer != null) {
                        mCountDownTimer.cancel();
                    }
                } else if (DbParams.getInstance().getSessionTimeUri().equals(uri)) {
                    initCountDownTimer();
                } else if (DbParams.getInstance().getAppEndStateUri().equals(uri)) {
                    mSensorsDataInstance.flush(3000);
                }
            }catch (Exception e) {
                com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
            }
        }
    }

    class SendDebugIdThread extends Thread {
        private String distinctId;
        private String infoId;
        private String serverUrl;
        SendDebugIdThread(String serverUrl, String distinctId, String infoId) {
            this.distinctId = distinctId;
            this.infoId = infoId;
            this.serverUrl = serverUrl;
        }
        @Override
        public void run() {
            super.run();
            sendHttpRequest(serverUrl, false);
        }

        private void sendHttpRequest(String serverUrl, boolean isRedirects) {
            ByteArrayOutputStream out = null;
            OutputStream out2 = null;
            BufferedOutputStream bout = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(String.format(serverUrl+"&info_id=%s", infoId));
                SALog.info(TAG, String.format("DebugMode URL:%s",url), null);
                connection = (HttpURLConnection) url.openConnection();
                if (connection == null) {
                    SALog.info(TAG, String.format("can not connect %s,shouldn't happen", url.toString()), null);
                    return;
                }
                connection.setInstanceFollowRedirects(false);
                out = new ByteArrayOutputStream();
                OutputStreamWriter writer = new OutputStreamWriter(out);
                String requestBody = "{\"distinct_id\": \"" + distinctId + "\"}";
                writer.write(requestBody);
                writer.flush();
                SALog.info(TAG,String.format("DebugMode request body : %s", requestBody), null);
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-type", "text/plain");

                out2 = connection.getOutputStream();
                bout = new BufferedOutputStream(out2);
                bout.write(out.toString().getBytes("UTF-8"));
                bout.flush();
                out.close();
                int responseCode = connection.getResponseCode();
                SALog.info(TAG,String.format(Locale.CHINA, "后端的响应码是:%d", responseCode), null);
                if (!isRedirects && SensorsDataHttpURLConnectionHelper.needRedirects(responseCode)) {
                    String location = SensorsDataHttpURLConnectionHelper.getLocation(connection, serverUrl);
                    if (!TextUtils.isEmpty(location)) {
                        closeStream(out, out2, bout, connection);
                        sendHttpRequest(location, true);
                    }
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            } finally {
                closeStream(out, out2, bout, connection);
            }
        }

        private void closeStream(ByteArrayOutputStream out, OutputStream out2, BufferedOutputStream bout, HttpURLConnection connection) {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
            if (out2 != null) {
                try {
                    out2.close();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
            if (bout != null) {
                try {
                    bout.close();
                } catch (Exception e){
                    SALog.printStackTrace(e);
                }
            }
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        }
    }
}
