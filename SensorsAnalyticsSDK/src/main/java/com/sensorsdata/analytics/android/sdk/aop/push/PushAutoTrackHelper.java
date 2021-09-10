/*
 * Created by yuejz on 2021/02/19.
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

package com.sensorsdata.analytics.android.sdk.aop.push;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPIEmptyImplementation;
import com.sensorsdata.analytics.android.sdk.util.ReflectUtil;

import org.json.JSONObject;

import java.util.Date;

public class PushAutoTrackHelper {

    private static final String TAG = "SA.PushAutoTrackHelper";

    private static long lastPushClickTime = 0L;

    /**
     * 极光推送走厂商通道打开 Activity 时，调用方法
     *
     * @param intent Activity 的 intent
     */
    public static void trackJPushOpenActivity(Intent intent) {

        if (intent == null) {
            return;
        }

        if (!isTrackPushEnabled()) return;

        String data = null;
        //获取华为平台附带的 jpush 信息
        if (intent.getData() != null) {
            data = intent.getData().toString();
        }

        //获取除华为平台附带的 jpush 信息
        if (TextUtils.isEmpty(data) && intent.getExtras() != null) {
            data = intent.getExtras().getString("JMessageExtra");
        }
        SALog.i(TAG, "trackJPushOpenActivity is called, Intent data is " + data);
        if (TextUtils.isEmpty(data)) return;
        try {
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(data);
            } catch (Exception e) {
                SALog.i(TAG, "Failed to construct JSON");
            }
            if (jsonObject != null) {
                String title = jsonObject.optString("n_title");
                String content = jsonObject.optString("n_content");
                String extras = jsonObject.optString("n_extras");
                byte whichPushSDK = (byte) jsonObject.optInt("rom_type");
                String appPushChannel = PushUtils.getJPushSDKName(whichPushSDK);
                SALog.i(TAG, String.format("trackJPushOpenActivity is called, title is %s, content is %s," +
                        " extras is %s, appPushChannel is %s", title, content, extras, appPushChannel));
                if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content) || TextUtils.isEmpty(appPushChannel)) {
                    return;
                }
                String sfData = getSFData(extras);
                trackNotificationOpenedEvent(sfData,
                        title,
                        content,
                        "JPush",
                        appPushChannel);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 推送采集
     *
     * @param sfData 神策智能推荐字段
     * @param title 推送标题
     * @param content 推送内容
     * @param appPushServiceName App 三方推送服务商，如极光、个推
     * @param appPushChannel App 推送通道，如华为、小米
     */
    public static void trackNotificationOpenedEvent(String sfData,
                                                    String title,
                                                    String content,
                                                    String appPushServiceName,
                                                    String appPushChannel) {
        trackNotificationOpenedEvent(sfData, title, content, appPushServiceName, appPushChannel, 0L);
    }

    /**
     * 推送采集
     *
     * @param sfData 神策智能推荐字段
     * @param title 推送标题
     * @param content 推送内容
     * @param appPushServiceName App 三方推送服务商，如极光、个推
     * @param appPushChannel App 推送通道，如华为、小米
     * @param time 事件时间
     */
    private static void trackNotificationOpenedEvent(String sfData,
                                                     String title,
                                                     String content,
                                                     String appPushServiceName,
                                                     String appPushChannel,
                                                     long time) {
        try {
            if (isRepeatEvent()) {
                SALog.i(TAG, String.format("$AppPushClick Repeat trigger, title is %s, content is %s," +
                        " extras is %s, appPushChannel is %s, appPushServiceName is %s", title, content, sfData, appPushChannel, appPushServiceName));
                return;
            }

            JSONObject eventProperties = new JSONObject();
            eventProperties.put("$app_push_msg_title", title);
            eventProperties.put("$app_push_msg_content", content);
            eventProperties.put("$app_push_service_name", appPushServiceName);
            if (!TextUtils.isEmpty(appPushChannel)) {
                eventProperties.put("$app_push_channel", appPushChannel.toUpperCase());
            }
            try {
                JSONObject sfDataProperties = null;
                if (!TextUtils.isEmpty(sfData)) {
                    try {
                        SALog.i(TAG, "sfData is " + sfData);
                        sfDataProperties = new JSONObject(sfData);
                    } catch (Exception e) {
                        SALog.i(TAG, "Failed to construct JSON");
                    }
                }
                if (sfDataProperties != null && sfDataProperties.has("sf_plan_id")) {
                    eventProperties.put("$sf_msg_title", title);
                    eventProperties.put("$sf_msg_content", content);
                    eventProperties.put("$sf_msg_id", sfDataProperties.opt("sf_msg_id"));
                    eventProperties.put("$sf_plan_id", sfDataProperties.opt("sf_plan_id"));
                    eventProperties.put("$sf_audience_id", sfDataProperties.opt("sf_audience_id"));
                    eventProperties.put("$sf_link_url", sfDataProperties.opt("sf_link_url"));
                    eventProperties.put("$sf_plan_strategy_id", sfDataProperties.opt("sf_plan_strategy_id"));
                    eventProperties.put("$sf_plan_type", sfDataProperties.opt("sf_plan_type"));
                    eventProperties.put("$sf_strategy_unit_id", sfDataProperties.opt("sf_strategy_unit_id"));
                    eventProperties.put("$sf_enter_plan_time", sfDataProperties.opt("sf_enter_plan_time"));
                    eventProperties.put("$sf_channel_id", sfDataProperties.opt("sf_channel_id"));
                    eventProperties.put("$sf_channel_category", sfDataProperties.opt("sf_channel_category"));
                    eventProperties.put("$sf_channel_service_name", sfDataProperties.opt("sf_channel_service_name"));
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
            try {
                if (time > 0) {
                    eventProperties.put("$time", new Date(time));
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
            SensorsDataAPI.sharedInstance().track("$AppPushClick", eventProperties);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 触发个推推送点击事件
     *
     * @param title title
     * @param content content
     * @param sfData sfData
     * @param time time
     */
    public static void trackGeTuiNotificationClicked(String title, String content, String sfData, long time) {
        trackNotificationOpenedEvent(sfData, title, content, "GeTui", null, time);
    }

    /**
     * 推送采集是否重复触发
     *
     * @return false 表示不重复，true 表示重复
     */
    private static boolean isRepeatEvent() {
        long currentTime = SystemClock.elapsedRealtime();
        SALog.i(TAG, "currentTime: " + currentTime + ",lastPushClickTime: " + lastPushClickTime);
        if (currentTime - lastPushClickTime > 2000) {
            lastPushClickTime = currentTime;
            return false;
        }
        return true;
    }

    /**
     * 极光推送 hook 方法
     *
     * @param extras 极光推送额外字段
     * @param title 推送标题
     * @param content 推送内容
     * @param appPushChannel App 推送通道，如华为、小米
     */
    public static void trackJPushAppOpenNotification(String extras,
                                                     String title,
                                                     String content,
                                                     String appPushChannel) {

        if (!isTrackPushEnabled()) return;

        SALog.i(TAG, String.format("trackJPushAppOpenNotification is called, title is %s, content is %s," +
                " extras is %s, appPushChannel is %s, appPushServiceName is %s", title, content, extras, appPushChannel, "JPush"));
        String sfData = getSFData(extras);
        trackNotificationOpenedEvent(sfData, title, content, "JPush", appPushChannel);
    }

    /**
     * 魅族推送 hook 方法
     *
     * @param extras 极光推送额外字段
     * @param title 推送标题
     * @param content 推送内容
     * @param appPushServiceName App 三方推送服务商，如极光、个推
     */
    public static void trackMeizuAppOpenNotification(String extras,
                                                     String title,
                                                     String content,
                                                     String appPushServiceName) {
        if (!isTrackPushEnabled()) return;

        SALog.i(TAG, String.format("trackMeizuAppOpenNotification is called, title is %s, content is %s," +
                " extras is %s, appPushChannel is %s, appPushServiceName is %s", title, content, extras, "Meizu", appPushServiceName));
        try {
            String nExtras = extras;
            try {
                JSONObject extrasJson = null;
                try {
                    extrasJson = new JSONObject(extras);
                } catch (Exception e) {
                    SALog.i(TAG, "Failed to construct JSON");
                }
                //极光的魅族厂商通道
                if (extrasJson != null && extrasJson.has("JMessageExtra")) {
                    JSONObject jMessageJson = extrasJson.optJSONObject("JMessageExtra");
                    if (jMessageJson != null) {
                        JSONObject contentJson = jMessageJson.optJSONObject("m_content");
                        if (contentJson != null) {
                            nExtras = contentJson.optString("n_extras");
                        }
                    }
                    appPushServiceName = "JPush";
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
            String sfData = getSFData(nExtras);
            trackNotificationOpenedEvent(sfData, title, content, appPushServiceName, "Meizu");
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 个推通道点击 hook 方法
     *
     * @param gtNotificationMessage 个推点击接口中的 msg
     */
    public static void onGeTuiNotificationClicked(Object gtNotificationMessage) {
        if (gtNotificationMessage == null) {
            SALog.i(TAG, "gtNotificationMessage is null");
            return;
        }
        if (!isTrackPushEnabled()) return;
        try {
            String msgId = ReflectUtil.callMethod(gtNotificationMessage, "getMessageId");
            String title = ReflectUtil.callMethod(gtNotificationMessage, "getTitle");
            String content = ReflectUtil.callMethod(gtNotificationMessage, "getContent");
            if (!TextUtils.isEmpty(msgId) &&
                    !TextUtils.isEmpty(title) &&
                    !TextUtils.isEmpty(content)) {
                PushProcess.getInstance().trackGTClickDelayed(msgId, title, content);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 个推透传接口 hook 方法
     *
     * @param gtTransmitMessage 个推透传接口 msg
     */
    public static void onGeTuiReceiveMessageData(Object gtTransmitMessage) {
        if (gtTransmitMessage == null) {
            SALog.i(TAG, "gtNotificationMessage is null");
            return;
        }
        if (!isTrackPushEnabled()) return;
        try {
            byte[] bytes = ReflectUtil.callMethod(gtTransmitMessage, "getPayload");
            String msgId = ReflectUtil.callMethod(gtTransmitMessage, "getMessageId");

            if (bytes != null && !TextUtils.isEmpty(msgId)) {
                String sfData = new String(bytes);
                PushProcess.getInstance().trackReceiveMessageData(sfData, msgId);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 友盟通道点击 hook 的方法
     *
     * @param UMessage 友盟的 msg
     */
    public static void onUMengNotificationClick(Object UMessage) {
        if (UMessage == null) {
            SALog.i(TAG, "UMessage is null");
            return;
        }
        if (!isTrackPushEnabled()) return;
        try {
            JSONObject raw = ReflectUtil.callMethod(UMessage, "getRaw");
            if (raw == null) {
                SALog.i(TAG, "onUMengNotificationClick:raw is null");
                return;
            }
            JSONObject body = raw.optJSONObject("body");
            if (body != null) {
                String extra = raw.optString("extra");
                String title = body.optString("title");
                String content = body.optString("text");
                String sfData = getSFData(extra);
                trackNotificationOpenedEvent(sfData, title, content, "UMeng", null);
                SALog.i(TAG, String.format("onUMengNotificationClick is called, title is %s, content is %s," +
                        " extras is %s", title, content, extra));
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 友盟推送厂商 hook 方法
     *
     * @param intent Activity 的 intent
     */
    public static void onUMengActivityMessage(Intent intent) {
        if (intent == null) {
            SALog.i(TAG, "intent is null");
            return;
        }
        if (!isTrackPushEnabled()) return;

        try {
            String intentBody = intent.getStringExtra("body");
            if (!TextUtils.isEmpty(intentBody)) {
                JSONObject raw = new JSONObject(intentBody);
                JSONObject body = raw.optJSONObject("body");
                if (body != null) {
                    String extra = raw.optString("extra");
                    String title = body.optString("title");
                    String content = body.optString("text");
                    String messageSource = intent.getStringExtra("message_source");
                    String sfData = getSFData(extra);
                    trackNotificationOpenedEvent(sfData, title, content, "UMeng", messageSource);
                    SALog.i(TAG, String.format("onUMengActivityMessage is called, title is %s, content is %s," +
                            " extras is %s", title, content, extra));
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 自定义推送 onNotify hook 方法
     *
     * @param manager manager
     * @param tag tag
     * @param id id
     * @param notification notification
     */
    public static void onNotify(NotificationManager manager, String tag, int id, Notification notification) {
        if (!isTrackPushEnabled()) return;
        try {
            PushProcess.getInstance().onNotify(tag, id, notification);
            SALog.i(TAG, "onNotify");
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 自定义推送 onNotify hook 方法
     *
     * @param manager manager
     * @param id id
     * @param notification notification
     */
    public static void onNotify(NotificationManager manager, int id, Notification notification) {
        if (!isTrackPushEnabled()) return;
        try {
            onNotify(manager, null, id, notification);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * Activity onNewIntent 回调方法
     *
     * @param activity activity
     * @param intent intent
     */
    public static void onNewIntent(Object activity, Intent intent) {
        if (!isTrackPushEnabled()) return;
        try {
            if (activity instanceof Activity) {
                PushProcess.getInstance().onNotificationClick((Activity) activity, intent);
                SALog.i(TAG, "onNewIntent");
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 自定义推送 hook 方法
     *
     * @param pendingIntent pendingIntent
     * @param context context
     * @param requestCode requestCode
     * @param intent intent
     * @param flags flags
     * @param bundle bundle
     */
    public static void hookPendingIntentGetActivityBundle(PendingIntent pendingIntent, Context context, int requestCode, Intent intent, int flags, Bundle bundle) {
        hookPendingIntent(intent, pendingIntent);
    }

    /**
     * 自定义推送 hook 方法
     *
     * @param context context
     * @param requestCode requestCode
     * @param intent intent
     * @param flags flags
     */
    public static void hookIntentGetActivity(Context context, int requestCode, Intent intent, int flags) {
        hookIntent(intent);
    }

    /**
     * 自定义推送 hook 方法
     *
     * @param context context
     * @param requestCode requestCode
     * @param intent intent
     * @param flags flags
     * @param bundle bundle
     */
    public static void hookIntentGetActivityBundle(Context context, int requestCode, Intent intent, int flags, Bundle bundle) {
        hookIntent(intent);
    }

    /**
     * 自定义推送 hook 方法
     *
     * @param pendingIntent pendingIntent
     * @param context context
     * @param requestCode requestCode
     * @param intent intent
     * @param flags flags
     */
    public static void hookPendingIntentGetActivity(PendingIntent pendingIntent, Context context, int requestCode, Intent intent, int flags) {
        hookPendingIntent(intent, pendingIntent);
    }

    /**
     * Broadcast hook 方法
     *
     * @param receiver receiver
     * @param context context
     * @param intent intent
     */
    public static void onBroadcastReceiver(BroadcastReceiver receiver, Context context, Intent intent) {
        onBroadcastServiceIntent(intent);
    }

    /**
     * Service hook 方法
     *
     * @param service service
     * @param intent intent
     * @param startId startId
     */
    public static void onServiceStart(Service service, Intent intent, int startId) {
        onBroadcastServiceIntent(intent);
    }

    /**
     * Service hook 方法
     *
     * @param service service
     * @param intent intent
     * @param flags flags
     * @param startId startId
     */
    public static void onServiceStartCommand(Service service, Intent intent, int flags, int startId) {
        onBroadcastServiceIntent(intent);
    }

    /**
     * 自定义推送 hook 方法
     *
     * @param context context
     * @param requestCode requestCode
     * @param intent intent
     * @param flags flags
     */
    public static void hookIntentGetBroadcast(Context context, int requestCode, Intent intent, int flags) {
        hookIntent(intent);
    }

    /**
     * 自定义推送 hook 方法
     *
     * @param pendingIntent pendingIntent
     * @param context context
     * @param requestCode requestCode
     * @param intent intent
     * @param flags flags
     */
    public static void hookPendingIntentGetBroadcast(PendingIntent pendingIntent, Context context, int requestCode, Intent intent, int flags) {
        hookPendingIntent(intent, pendingIntent);
    }

    /**
     * 自定义推送 hook 方法
     *
     * @param context context
     * @param requestCode requestCode
     * @param intent intent
     * @param flags flags
     */
    public static void hookIntentGetService(Context context, int requestCode, Intent intent, int flags) {
        hookIntent(intent);
    }

    /**
     * 自定义推送 hook 方法
     *
     * @param pendingIntent pendingIntent
     * @param context context
     * @param requestCode requestCode
     * @param intent intent
     * @param flags flags
     */
    public static void hookPendingIntentGetService(PendingIntent pendingIntent, Context context, int requestCode, Intent intent, int flags) {
        hookPendingIntent(intent, pendingIntent);
    }

    /**
     * 自定义推送 hook 方法
     *
     * @param context context
     * @param requestCode requestCode
     * @param intent intent
     * @param flags flags
     */
    public static void hookIntentGetForegroundService(Context context, int requestCode, Intent intent, int flags) {
        hookIntent(intent);
    }

    /**
     * 自定义推送 hook 方法
     *
     * @param pendingIntent pendingIntent
     * @param context context
     * @param requestCode requestCode
     * @param intent intent
     * @param flags flags
     */
    public static void hookPendingIntentGetForegroundService(PendingIntent pendingIntent, Context context, int requestCode, Intent intent, int flags) {
        hookPendingIntent(intent, pendingIntent);
    }

    /**
     * PendingIntent 创建前调用的方法
     *
     * @param intent intent
     * @param pendingIntent pendingIntent
     */
    private static void hookPendingIntent(Intent intent, PendingIntent pendingIntent) {
        if (!isTrackPushEnabled()) return;
        try {
            PushProcess.getInstance().hookPendingIntent(intent, pendingIntent);
            SALog.i(TAG, "hookPendingIntent");
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * Broadcast 或者 Service 回调中调用
     *
     * @param intent intent
     */
    private static void onBroadcastServiceIntent(Intent intent) {
        if (!isTrackPushEnabled()) return;
        try {
            PushProcess.getInstance().onNotificationClick(null, intent);
            SALog.i(TAG, "onBroadcastServiceIntent");
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * PendingIntent 创建后调用
     *
     * @param intent intent
     */
    private static void hookIntent(Intent intent) {
        if (!isTrackPushEnabled()) return;
        try {
            PushProcess.getInstance().hookIntent(intent);
            SALog.i(TAG, "hookIntent");
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 是否开启推送采集
     *
     * @return false 不开启，true 开启推送采集
     */
    private static boolean isTrackPushEnabled() {
        try {
            if ((SensorsDataAPI.sharedInstance() instanceof SensorsDataAPIEmptyImplementation) ||
                    SensorsDataAPI.getConfigOptions() == null ||
                    !SensorsDataAPI.getConfigOptions().mEnableTrackPush) {
                SALog.i(TAG, "SDK or push disabled.");
                return false;
            }
            return true;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return false;
    }

    private static String getSFData(String extras) {
        String sfData = null;
        try {
            JSONObject sfDataJson = new JSONObject(extras);
            sfData = sfDataJson.optString("sf_data");
        } catch (Exception e) {
            SALog.i(TAG, "get sf_data failed");
        }
        return sfData;
    }
}
