/*
 * Created by dengshiwei on 2021/07/29.
 * Copyright 2015－2021 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.autotrack;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.ScreenAutoTracker;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataActivityLifecycleCallbacks;
import com.sensorsdata.analytics.android.sdk.SensorsDataExceptionHandler;
import com.sensorsdata.analytics.android.sdk.advert.utils.ChannelUtils;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstDay;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstStart;
import com.sensorsdata.analytics.android.sdk.deeplink.DeepLinkManager;
import com.sensorsdata.analytics.android.sdk.dialog.SensorsDataDialogUtils;
import com.sensorsdata.analytics.android.sdk.util.AopUtil;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;
import com.sensorsdata.analytics.android.sdk.visual.HeatMapService;
import com.sensorsdata.analytics.android.sdk.visual.VisualizedAutoTrackService;

import org.json.JSONObject;

public class ActivityLifecycleCallbacks implements SensorsDataActivityLifecycleCallbacks.SAActivityLifecycleCallbacks, SensorsDataExceptionHandler.SAExceptionListener {
    private static final String TAG = "SA.ActivityLifecycleCallbacks";
    private static final String EVENT_TIMER = "event_timer";
    private static final String TRACK_TIMER = "track_timer";
    private static final String LIB_VERSION = "$lib_version";
    private static final String APP_VERSION = "$app_version";
    private final SensorsDataAPI mSensorsDataInstance;
    private final Context mContext;
    private final PersistentFirstStart mFirstStart;
    private final PersistentFirstDay mFirstDay;
    private boolean resumeFromBackground = false;
    private final DbAdapter mDbAdapter;
    private JSONObject activityProperty = new JSONObject();
    private final JSONObject endDataProperty = new JSONObject();
    private JSONObject mDeepLinkProperty = new JSONObject();
    private int mStartActivityCount;
    private int mStartTimerCount;
    // $AppStart 事件的时间戳
    private final String APP_START_TIME = "app_start_time";
    // $AppEnd 事件的时间戳
    private final String APP_END_TIME = "app_end_time";
    // $AppEnd 补发时触发的时间戳
    private final String APP_END_MESSAGE_TIME = "app_end_message_time";
    // $AppEnd 事件属性
    private final String APP_END_DATA = "app_end_data";
    // App 是否重置标记位
    private final String APP_RESET_STATE = "app_reset_state";
    private final String TIME = "time";
    private final String ELAPSE_TIME = "elapse_time";
    private Handler mHandler;
    /* 兼容由于在魅族手机上退到后台后，线程会被休眠，导致 $AppEnd 无法触发，造成再次打开重复发送。*/
    private long messageReceiveTime = 0L;
    private final int MESSAGE_CODE_APP_END = 0;
    private final int MESSAGE_CODE_START = 100;
    private final int MESSAGE_CODE_STOP = 200;
    private final int MESSAGE_CODE_TIMER = 300;
    /**
     * 打点时间间隔：2000 毫秒
     */
    private static final int TIME_INTERVAL = 2000;
    private boolean mDataCollectState;

    public ActivityLifecycleCallbacks(SensorsDataAPI instance, PersistentFirstStart firstStart,
                                          PersistentFirstDay firstDay, Context context) {
        this.mSensorsDataInstance = instance;
        this.mFirstStart = firstStart;
        this.mFirstDay = firstDay;
        this.mDbAdapter = DbAdapter.getInstance();
        this.mContext = context;
        mDataCollectState = SensorsDataAPI.getConfigOptions().isDataCollectEnable();
        initHandler();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        if (!SensorsDataDialogUtils.isSchemeActivity(activity)) {
            SensorsDataUtils.handleSchemeUrl(activity, activity.getIntent());
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (!SensorsDataDialogUtils.isSchemeActivity(activity)) {
            if (mStartActivityCount == 0) {
                // 第一个页面进行页面信息解析
                buildScreenProperties(activity);
            }
            sendActivityHandleMessage(MESSAGE_CODE_START);
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        try {
            buildScreenProperties(activity);
            if (mSensorsDataInstance.isAutoTrackEnabled() && !mSensorsDataInstance.isActivityAutoTrackAppViewScreenIgnored(activity.getClass())
                    && !mSensorsDataInstance.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_VIEW_SCREEN)) {
                JSONObject properties = new JSONObject();
                SensorsDataUtils.mergeJSONObject(activityProperty, properties);
                if (activity instanceof ScreenAutoTracker) {
                    ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) activity;
                    JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                    if (otherProperties != null) {
                        SensorsDataUtils.mergeJSONObject(otherProperties, properties);
                    }
                }
                // 合并渠道信息到 $AppViewScreen 事件中
                DeepLinkManager.mergeDeepLinkProperty(properties);
                DeepLinkManager.resetDeepLinkProcessor();
                JSONObject eventProperties = SADataHelper.appendLibMethodAutoTrack(properties);
                mSensorsDataInstance.trackViewScreen(SensorsDataUtils.getScreenUrl(activity), eventProperties);
            }
        } catch (Throwable e) {
            SALog.i(TAG, e);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (!SensorsDataDialogUtils.isSchemeActivity(activity)) {
            sendActivityHandleMessage(MESSAGE_CODE_STOP);
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    @Override
    public void onNewIntent(Intent intent) {

    }


    private void initHandler() {
        try {
            HandlerThread handlerThread = new HandlerThread("SENSORS_DATA_THREAD");
            handlerThread.start();
            mHandler = new Handler(handlerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    int code = msg.what;
                    switch (code) {
                        case MESSAGE_CODE_START:
                            handleStartedMessage(msg);
                            break;
                        case MESSAGE_CODE_STOP:
                            handleStoppedMessage(msg);
                            break;
                        case MESSAGE_CODE_TIMER:
                            if (mSensorsDataInstance.isAutoTrackEnabled() && isAutoTrackAppEnd()) {
                                generateAppEndData(0, 0);
                            }

                            if (mStartTimerCount > 0) {
                                mHandler.sendEmptyMessageDelayed(MESSAGE_CODE_TIMER, TIME_INTERVAL);
                            }
                            break;
                        case MESSAGE_CODE_APP_END:
                            if (messageReceiveTime != 0 && SystemClock.elapsedRealtime() - messageReceiveTime < mSensorsDataInstance.getSessionIntervalTime()) {
                                SALog.i(TAG, "$AppEnd 事件已触发。");
                                return;
                            }
                            messageReceiveTime = SystemClock.elapsedRealtime();
                            Bundle bundle = msg.getData();
                            long startTime = bundle.getLong(APP_START_TIME);
                            long endTime = bundle.getLong(APP_END_TIME);
                            String endData = bundle.getString(APP_END_DATA);
                            boolean resetState = bundle.getBoolean(APP_RESET_STATE);
                            // 如果是正常的退到后台，需要重置标记位
                            if (resetState) {
                                resetState();
                                // 对于 Unity 多进程跳转的场景，需要在判断一下
                                if (DbAdapter.getInstance().getActivityCount() <= 0) {
                                    trackAppEnd(startTime, endTime, endData);
                                }
                            } else {// 如果是补发则需要添加打点间隔，防止 $AppEnd 在 AppCrash 事件序列之前
                                endTime = endTime == 0 ? bundle.getLong(APP_END_MESSAGE_TIME) : endTime + TIME_INTERVAL;
                                trackAppEnd(startTime, endTime, endData);
                            }
                            break;
                    }
                }
            };
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    private void handleStartedMessage(Message message) {
        boolean isSessionTimeout = false;
        try {
            mStartActivityCount = mDbAdapter.getActivityCount();
            mDbAdapter.commitActivityCount(++mStartActivityCount);
            // 如果是第一个页面
            if (mStartActivityCount == 1) {
                if (mSensorsDataInstance.getConfigOptions().isSaveDeepLinkInfo()) {// 保存 utm 信息时,在 endData 中合并保存的 latestUtm 信息。
                    SensorsDataUtils.mergeJSONObject(ChannelUtils.getLatestUtmProperties(), endDataProperty);
                }
                mHandler.removeMessages(MESSAGE_CODE_APP_END);
                isSessionTimeout = isSessionTimeOut();
                if (isSessionTimeout) {
                    // 超时尝试补发 $AppEnd
                    mHandler.sendMessage(obtainAppEndMessage(false));
                    checkFirstDay();
                    // XXX: 注意内部执行顺序
                    boolean firstStart = mFirstStart.get();
                    try {
                        mSensorsDataInstance.appBecomeActive();

                        //从后台恢复，从缓存中读取 SDK 控制配置信息
                        if (resumeFromBackground) {
                            //先从缓存中读取 SDKConfig
                            mSensorsDataInstance.getRemoteManager().applySDKConfigFromCache();
                            mSensorsDataInstance.resumeTrackScreenOrientation();
//                    mSensorsDataInstance.resumeTrackTaskThread();
                        }
                        //每次启动 App，重新拉取最新的配置信息
                        mSensorsDataInstance.getRemoteManager().pullSDKConfigFromServer();
                    } catch (Exception ex) {
                        SALog.printStackTrace(ex);
                    }
                    Bundle bundle = message.getData();
                    try {
                        if (mSensorsDataInstance.isAutoTrackEnabled() && !mSensorsDataInstance.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_START)) {
                            if (firstStart) {
                                mFirstStart.commit(false);
                            }
                            JSONObject properties = new JSONObject();
                            properties.put("$resume_from_background", resumeFromBackground);
                            properties.put("$is_first_time", firstStart);
                            SensorsDataUtils.mergeJSONObject(activityProperty, properties);
                            // 合并渠道信息到 $AppStart 事件中
                            if (mDeepLinkProperty != null) {
                                SensorsDataUtils.mergeJSONObject(mDeepLinkProperty, properties);
                                mDeepLinkProperty = null;
                            }
                            // 读取 Message 中的时间戳
                            long eventTime = bundle.getLong(TIME);
                            properties.put("event_time", eventTime > 0 ? eventTime : System.currentTimeMillis());
                            mSensorsDataInstance.trackAutoEvent("$AppStart", properties);
                            SensorsDataAPI.sharedInstance().flush();
                        }
                    } catch (Exception e) {
                        SALog.i(TAG, e);
                    }
                    // 读取 Message 中的时间戳
                    long elapsedRealtime = bundle.getLong(ELAPSE_TIME);
                    try {
                        mDbAdapter.commitAppStartTime(elapsedRealtime > 0 ? elapsedRealtime : SystemClock.elapsedRealtime());   // 防止动态开启 $AppEnd 时，启动时间戳不对的问题。
                    } catch (Exception ex) {
                        // 出现异常，在重新存储一次，防止使用原有的时间戳造成时长计算错误
                        mDbAdapter.commitAppStartTime(elapsedRealtime > 0 ? elapsedRealtime : SystemClock.elapsedRealtime());
                    }

                    if (resumeFromBackground) {
                        try {
                            HeatMapService.getInstance().resume();
                            VisualizedAutoTrackService.getInstance().resume();
                        } catch (Exception e) {
                            SALog.printStackTrace(e);
                        }
                    }

                    // 下次启动时，从后台恢复
                    resumeFromBackground = true;
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
            if (isSessionTimeout) {
                mDbAdapter.commitAppStartTime(SystemClock.elapsedRealtime());
            }
        }

        try {
            if (mStartTimerCount++ == 0) {
                /*
                 * 在启动的时候开启打点，退出时停止打点，在此处可以防止两点：
                 *  1. App 在 onResume 之前 Crash，导致只有启动没有退出；
                 *  2. 多进程的情况下只会开启一个打点器；
                 */
                mHandler.sendEmptyMessage(MESSAGE_CODE_TIMER);
            }
        } catch (Exception exception) {
            SALog.printStackTrace(exception);
        }
    }

    private void handleStoppedMessage(Message message) {
        try {
            // 停止计时器，针对跨进程的情况，要停止当前进程的打点器
            mStartTimerCount--;
            if (mStartTimerCount <= 0) {
                mHandler.removeMessages(MESSAGE_CODE_TIMER);
                mStartTimerCount = 0;
            }

            mStartActivityCount = mDbAdapter.getActivityCount();
            mStartActivityCount = mStartActivityCount > 0 ? --mStartActivityCount : 0;
            mDbAdapter.commitActivityCount(mStartActivityCount);

            /*
             * 为了处理跨进程之间跳转 Crash 的情况，由于在 ExceptionHandler 中进行重置，
             * 所以会引起的计数器小于 0 的情况。
             */
            if (mStartActivityCount <= 0) {
                // 主动 flush 数据
                mSensorsDataInstance.flush();
                Bundle bundle = message.getData();
                generateAppEndData(bundle.getLong(TIME), bundle.getLong(ELAPSE_TIME));
                mHandler.sendMessageDelayed(obtainAppEndMessage(true), mSensorsDataInstance.getSessionIntervalTime());
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 发送 $AppEnd 事件
     *
     * @param endEventTime 退出时间
     * @param jsonEndData $AppEnd 事件属性
     */
    private void trackAppEnd(long startTime, long endEventTime, String jsonEndData) {
        try {
            if (mSensorsDataInstance.isAutoTrackEnabled() && isAutoTrackAppEnd() && !TextUtils.isEmpty(jsonEndData)) {
                JSONObject endDataJsonObject = new JSONObject(jsonEndData);
                long endTime = endDataJsonObject.optLong(EVENT_TIMER); // 获取结束时间戳
                long endTrackTime = endDataJsonObject.optLong(TRACK_TIMER); // 获取 $AppEnd 打点事件戳
                // 读取指定的字段，防止别人篡改写入脏属性
                JSONObject properties = new JSONObject();
                properties.put("$screen_name", endDataJsonObject.optString("$screen_name"));
                properties.put("$title", endDataJsonObject.optString("$title"));
                properties.put(LIB_VERSION, endDataJsonObject.optString(LIB_VERSION));
                properties.put(APP_VERSION, endDataJsonObject.optString(APP_VERSION));
                if (startTime > 0) {
                    properties.put("event_duration", TimeUtils.duration(startTime, endTime));
                }
                properties.put("event_time", endTrackTime == 0 ? endEventTime : endTrackTime);
                ChannelUtils.mergeUtmToEndData(endDataJsonObject, properties);
                mSensorsDataInstance.trackAutoEvent("$AppEnd", properties);
                mDbAdapter.commitAppEndData(""); // 保存的信息只使用一次就置空，防止后面状态错乱再次发送。
                mSensorsDataInstance.flush();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 存储当前的 AppEnd 事件关键信息
     */
    private void generateAppEndData(long messageTime, long endElapsedTime) {
        try {
            // 如果初始未非合规状态，则需要判断同意合规后才打点
            if (!mDataCollectState && !mSensorsDataInstance.getSAContextManager().isAppStartSuccess()) {
                return;
            }
            mDataCollectState = true;
            // 同意合规时进行打点记录
            if (SensorsDataAPI.getConfigOptions().isDataCollectEnable()) {
                long timer = messageTime == 0 ? System.currentTimeMillis() : messageTime;
                endDataProperty.put(EVENT_TIMER, endElapsedTime == 0 ? SystemClock.elapsedRealtime() : endElapsedTime);
                endDataProperty.put(TRACK_TIMER, timer);
                endDataProperty.put(APP_VERSION, AppInfoUtils.getAppVersionName(mContext));
                endDataProperty.put(LIB_VERSION, SensorsDataAPI.sharedInstance().getSDKVersion());
                // 合并 $utm 信息
                ChannelUtils.mergeUtmToEndData(ChannelUtils.getLatestUtmProperties(), endDataProperty);
                mDbAdapter.commitAppEndData(endDataProperty.toString());
                mDbAdapter.commitAppEndTime(timer);
            }
        } catch (Throwable e) {
            SALog.d(TAG, e.getMessage());
        }
    }

    /**
     * 判断是否超出 Session 时间间隔
     *
     * @return true 超时，false 未超时
     */
    private boolean isSessionTimeOut() {
        long currentTime = Math.max(System.currentTimeMillis(), 946656000000L);
        long endTrackTime = 0;
        try {
            String endData = DbAdapter.getInstance().getAppEndData();
            if (!TextUtils.isEmpty(endData)) {
                JSONObject endDataJsonObject = new JSONObject(endData);
                endTrackTime = endDataJsonObject.optLong(TRACK_TIMER); // 获取 $AppEnd 打点时间戳
            }
            if (endTrackTime == 0) {
                endTrackTime = mDbAdapter.getAppEndTime();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        boolean sessionTimeOut = Math.abs(currentTime - endTrackTime) > mSensorsDataInstance.getSessionIntervalTime();
        SALog.d(TAG, "SessionTimeOut:" + sessionTimeOut);
        return sessionTimeOut;
    }

    /**
     * 发送处理 Activity 生命周期的 Message
     *
     * @param type 消息类型
     */
    private void sendActivityHandleMessage(int type) {
        Message message = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putLong(TIME, System.currentTimeMillis());
        bundle.putLong(ELAPSE_TIME, SystemClock.elapsedRealtime());
        message.what = type;
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    /**
     * 构建 Message 对象
     *
     * @param resetState 是否重置状态
     * @return Message
     */
    private Message obtainAppEndMessage(boolean resetState) {
        Message message = Message.obtain(mHandler);
        message.what = MESSAGE_CODE_APP_END;
        Bundle bundle = new Bundle();
        bundle.putLong(APP_START_TIME, DbAdapter.getInstance().getAppStartTime());
        bundle.putLong(APP_END_TIME, DbAdapter.getInstance().getAppEndTime());
        bundle.putString(APP_END_DATA, DbAdapter.getInstance().getAppEndData());
        bundle.putLong(APP_END_MESSAGE_TIME, System.currentTimeMillis());
        bundle.putBoolean(APP_RESET_STATE, resetState);
        message.setData(bundle);
        return message;
    }

    /**
     * AppEnd 正常结束时，重置一些设置状态
     */
    private void resetState() {
        try {
            mSensorsDataInstance.stopTrackScreenOrientation();
            mSensorsDataInstance.getRemoteManager().resetPullSDKConfigTimer();
            HeatMapService.getInstance().stop();
            VisualizedAutoTrackService.getInstance().stop();
            mSensorsDataInstance.appEnterBackground();
            resumeFromBackground = true;
            mSensorsDataInstance.clearLastScreenUrl();
//            mSensorsDataInstance.stopTrackTaskThread();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 检查更新首日逻辑
     */
    private void checkFirstDay() {
        if (mFirstDay.get() == null && SensorsDataAPI.getConfigOptions().isDataCollectEnable()) {
            mFirstDay.commit(TimeUtils.formatTime(System.currentTimeMillis(), TimeUtils.YYYY_MM_DD));
        }
    }

    private boolean isAutoTrackAppEnd() {
        return !mSensorsDataInstance.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_END);
    }

    private void buildScreenProperties(Activity activity) {
        activityProperty = AopUtil.buildTitleNoAutoTrackerProperties(activity);
        SensorsDataUtils.mergeJSONObject(activityProperty, endDataProperty);
        if (!SensorsDataAPI.getConfigOptions().isDisableSDK() && isDeepLinkParseSuccess(activity)) {
            // 清除 AppEnd 中的 DeepLink 信息
            ChannelUtils.removeDeepLinkInfo(endDataProperty);
            // 合并渠道信息到 $AppStart 事件中
            if (mDeepLinkProperty == null) {
                mDeepLinkProperty = new JSONObject();
            }
            DeepLinkManager.mergeDeepLinkProperty(mDeepLinkProperty);
        }
    }

    /**
     * DeepLink 信息是否解析成功
     *
     * @param activity Activity 页面
     * @return DeepLink 是否成功解析
     */
    private boolean isDeepLinkParseSuccess(Activity activity) {
        try {
            if(!SensorsDataUtils.isUniApp() || !ChannelUtils.isDeepLinkBlackList(activity)) {
                Intent intent = activity.getIntent();
                if (intent != null && intent.getData() != null) {
                    //判断 deepLink 信息是否已处理过
                    if (!intent.getBooleanExtra(DeepLinkManager.IS_ANALYTICS_DEEPLINK, false)) {
                        if (DeepLinkManager.parseDeepLink(activity, mSensorsDataInstance.getConfigOptions().isSaveDeepLinkInfo(), mSensorsDataInstance.getDeepLinkCallback())) {
                            intent.putExtra(DeepLinkManager.IS_ANALYTICS_DEEPLINK, true);
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ex) {
            SALog.i(TAG, ex.getMessage());
        }
        return false;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        /*
         * 异常的情况会出现两种：
         * 1. 未完成 $AppEnd 事件，触发的异常，此时需要记录下 AppEndTime
         * 2. 完成了 $AppEnd 事件，下次启动时触发的异常。还未及时更新 $AppStart 的时间戳，导致计算时长偏大，所以需要重新更新启动时间戳
         */
        if (TextUtils.isEmpty(DbAdapter.getInstance().getAppEndData())) {
            DbAdapter.getInstance().commitAppStartTime(SystemClock.elapsedRealtime());
        } else {
            DbAdapter.getInstance().commitAppEndTime(System.currentTimeMillis());
        }

        if (SensorsDataAPI.getConfigOptions().isMultiProcessFlush()) {
            DbAdapter.getInstance().commitSubProcessFlushState(false);
        }

        // 注意这里要重置为 0，对于跨进程的情况，如果子进程崩溃，主进程但是没崩溃，造成统计个数异常，所以要重置为 0。
        DbAdapter.getInstance().commitActivityCount(0);
    }
}
