/*
 * Created by wangzhuozhou on 2017/4/12.
 * Copyright 2015－2020 Sensors Data Inc.
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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.data.DbAdapter;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstDay;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstStart;
import com.sensorsdata.analytics.android.sdk.deeplink.DeepLinkManager;
import com.sensorsdata.analytics.android.sdk.util.AopUtil;
import com.sensorsdata.analytics.android.sdk.util.ChannelUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;
import com.sensorsdata.analytics.android.sdk.visual.HeatMapService;
import com.sensorsdata.analytics.android.sdk.visual.VisualizedAutoTrackService;

import org.json.JSONObject;

import java.util.Locale;

import static com.sensorsdata.analytics.android.sdk.deeplink.DeepLinkManager.IS_ANALYTICS_DEEPLINK;


@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class SensorsDataActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "SA.LifecycleCallbacks";
    private static final String EVENT_TIMER = "event_timer";
    private static final String TRACK_TIMER = "track_timer";
    private static final String LIB_VERSION = "$lib_version";
    private static final String APP_VERSION = "$app_version";
    private final SensorsDataAPI mSensorsDataInstance;
    private final PersistentFirstStart mFirstStart;
    private final PersistentFirstDay mFirstDay;
    private Context mContext;
    private boolean resumeFromBackground = false;
    private DbAdapter mDbAdapter;
    private JSONObject activityProperty = new JSONObject();
    private JSONObject endDataProperty = new JSONObject();
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
    // App 版本号
    private String app_version;
    // SDK 版本号
    private String lib_version;
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

    SensorsDataActivityLifecycleCallbacks(SensorsDataAPI instance, PersistentFirstStart firstStart,
                                          PersistentFirstDay firstDay, Context context) {
        this.mSensorsDataInstance = instance;
        this.mFirstStart = firstStart;
        this.mFirstDay = firstDay;
        this.mContext = context;
        this.mDbAdapter = DbAdapter.getInstance();
        try {
            final PackageManager manager = mContext.getPackageManager();
            final PackageInfo info = manager.getPackageInfo(mContext.getPackageName(), 0);
            app_version = info.versionName;
            lib_version = SensorsDataAPI.VERSION;
        } catch (final Exception e) {
            SALog.i(TAG, "Exception getting version name = ", e);
        }
        initHandler();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        SensorsDataUtils.handleSchemeUrl(activity, activity.getIntent());
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (mStartActivityCount == 0) {
            // 第一个页面进行页面信息解析
            buildScreenProperties(activity);
        }
        sendActivityHandleMessage(MESSAGE_CODE_START);
    }

    @Override
    public void onActivityResumed(final Activity activity) {
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
                // 合并 utm 属性到 properties 中
                DeepLinkManager.mergeDeepLinkProperty(properties);
                DeepLinkManager.resetDeepLinkProcessor();
                mSensorsDataInstance.trackViewScreen(SensorsDataUtils.getScreenUrl(activity), properties);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
        sendActivityHandleMessage(MESSAGE_CODE_STOP);
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
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
                            if (messageReceiveTime != 0 && SystemClock.elapsedRealtime() - messageReceiveTime < mSensorsDataInstance.mSessionTime) {
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
                                long messageTime = bundle.getLong(APP_END_MESSAGE_TIME);
                                endTime = endTime == 0 ? messageTime : endTime + TIME_INTERVAL;
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
        try {
            mStartActivityCount = mDbAdapter.getActivityCount();
            mDbAdapter.commitActivityCount(++mStartActivityCount);
            // 如果是第一个页面
            if (mStartActivityCount == 1) {
                if (mSensorsDataInstance.isSaveDeepLinkInfo()) {// 保存 utm 信息时,在 endData 中合并保存的 latestUtm 信息。
                    SensorsDataUtils.mergeJSONObject(ChannelUtils.getLatestUtmProperties(), endDataProperty);
                }
                mHandler.removeMessages(MESSAGE_CODE_APP_END);
                boolean sessionTimeOut = isSessionTimeOut();
                if (sessionTimeOut) {
                    // 超时尝试补发 $AppEnd
                    mHandler.sendMessage(obtainAppEndMessage(false));
                    checkFirstDay();
                    // XXX: 注意内部执行顺序
                    boolean firstStart = mFirstStart.get();

                    try {
                        mSensorsDataInstance.appBecomeActive();
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }

                    //从后台恢复，从缓存中读取 SDK 控制配置信息
                    if (resumeFromBackground) {
                        //先从缓存中读取 SDKConfig
                        mSensorsDataInstance.getRemoteManager().applySDKConfigFromCache();
                        mSensorsDataInstance.resumeTrackScreenOrientation();
//                    mSensorsDataInstance.resumeTrackTaskThread();
                    }
                    //每次启动 App，重新拉取最新的配置信息
                    mSensorsDataInstance.getRemoteManager().pullSDKConfigFromServer();

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
                            // 合并 utm 属性到 properties 中
                            if (mDeepLinkProperty != null) {
                                SensorsDataUtils.mergeJSONObject(mDeepLinkProperty, properties);
                                mDeepLinkProperty = null;
                            }
                            // 读取 Message 中的时间戳
                            long eventTime = bundle.getLong(TIME);
                            properties.put("event_time", eventTime > 0 ? eventTime : System.currentTimeMillis());
                            mSensorsDataInstance.trackInternal("$AppStart", properties);
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

            if (mStartTimerCount++ == 0) {
                /*
                 * 在启动的时候开启打点，退出时停止打点，在此处可以防止两点：
                 *  1. App 在 onResume 之前 Crash，导致只有启动没有退出；
                 *  2. 多进程的情况下只会开启一个打点器；
                 */
                mHandler.sendEmptyMessage(MESSAGE_CODE_TIMER);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private void handleStoppedMessage(Message message) {
        try {
            // 停止计时器，针对跨进程的情况，要停止当前进程的打点器
            mStartTimerCount--;
            if (mStartTimerCount == 0) {
                mHandler.removeMessages(MESSAGE_CODE_TIMER);
            }

            mStartActivityCount = mDbAdapter.getActivityCount();
            mStartActivityCount = mStartActivityCount > 0 ? --mStartActivityCount : 0;
            mDbAdapter.commitActivityCount(mStartActivityCount);

            /*
             * 为了处理跨进程之间跳转 Crash 的情况，由于在 ExceptionHandler 中进行重置，
             * 所以会引起的计数器小于 0 的情况。
             */
            if (mStartActivityCount <= 0) {
                Bundle bundle = message.getData();
                generateAppEndData(bundle.getLong(TIME), bundle.getLong(ELAPSE_TIME));
                mHandler.sendMessageDelayed(obtainAppEndMessage(true), mSensorsDataInstance.mSessionTime);
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 发送 $AppEnd 事件
     *
     * @param pausedTime 退出时间
     * @param jsonEndData $AppEnd 事件属性
     */
    private void trackAppEnd(long startTime, long pausedTime, String jsonEndData) {
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
                properties.put("event_duration", Double.valueOf(duration(startTime, endTime)));
                properties.put("event_time", endTrackTime == 0 ? pausedTime : endTrackTime);
                ChannelUtils.mergeUtmToEndData(endDataJsonObject, properties);
                mSensorsDataInstance.trackInternal("$AppEnd", properties);
                mDbAdapter.commitAppEndData(""); // 保存的信息只使用一次就置空，防止后面状态错乱再次发送。
                mSensorsDataInstance.flushSync();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 计算退出事件时长
     *
     * @param startTime 启动时间
     * @param endTime 退出时间
     * @return 时长
     */
    private String duration(long startTime, long endTime) {
        long duration = endTime - startTime;
        try {
            if (duration < 0 || duration > 24 * 60 * 60 * 1000) {
                return String.valueOf(0);
            }
            float durationFloat = duration / 1000.0f;
            return durationFloat < 0 ? String.valueOf(0) : String.format(Locale.CHINA, "%.3f", durationFloat);
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return String.valueOf(0);
        }
    }

    /**
     * 存储当前的 AppEnd 事件关键信息
     */
    private void generateAppEndData(long messageTime, long endElapsedTime) {
        try {
            long timer = messageTime == 0 ? System.currentTimeMillis() : messageTime;
            endDataProperty.put(EVENT_TIMER, endElapsedTime == 0 ? SystemClock.elapsedRealtime() : endElapsedTime);
            endDataProperty.put(TRACK_TIMER, timer);
            endDataProperty.put(APP_VERSION, app_version);
            endDataProperty.put(LIB_VERSION, lib_version);
            // 合并 $utm 信息
            ChannelUtils.mergeUtmToEndData(ChannelUtils.getLatestUtmProperties(), endDataProperty);
            mDbAdapter.commitAppEndData(endDataProperty.toString());
            mDbAdapter.commitAppEndTime(timer);
        } catch (Exception e) {
            SALog.printStackTrace(e);
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
        boolean sessionTimeOut = Math.abs(currentTime - endTrackTime) > mSensorsDataInstance.mSessionTime;
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
     * 检查 DateFormat 是否为空，如果为空则进行初始化
     */
    private void checkFirstDay() {
        if (mFirstDay.get() == null) {
            mFirstDay.commit(TimeUtils.formatTime(System.currentTimeMillis(), TimeUtils.YYYY_MM_DD));
        }
    }

    private boolean isAutoTrackAppEnd() {
        return !mSensorsDataInstance.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_END);
    }

    private void buildScreenProperties(Activity activity) {
        activityProperty = AopUtil.buildTitleNoAutoTrackerProperties(activity);
        SensorsDataUtils.mergeJSONObject(activityProperty, endDataProperty);
        if (isDeepLinkParseSuccess(activity)) {
            // 清除 AppEnd 中的 DeepLink 信息
            ChannelUtils.removeDeepLinkInfo(endDataProperty);
            // 合并 utm 属性到 properties 中，用于 $AppStart 事件
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
            Intent intent = activity.getIntent();
            //判断 deepLink 信息是否已处理过
            if (!intent.getBooleanExtra(IS_ANALYTICS_DEEPLINK, false)) {
                if (DeepLinkManager.parseDeepLink(activity, mSensorsDataInstance.isSaveDeepLinkInfo(), mSensorsDataInstance.getDeepLinkCallback())) {
                    intent.putExtra(IS_ANALYTICS_DEEPLINK, true);
                    return true;
                }
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return false;
    }
}