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

import android.content.Intent;
import android.os.SystemClock;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;

import org.json.JSONObject;

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

        if (!SensorsDataAPI.getConfigOptions().mEnableTrackPush) {
            SALog.i(TAG, "enableTrackPush is false");
            return;
        }

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
                trackNotificationOpenedEvent(extras,
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
    private static void trackNotificationOpenedEvent(String sfData,
                                                     String title,
                                                     String content,
                                                     String appPushServiceName,
                                                     String appPushChannel) {
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
            eventProperties.put("$app_push_channel", appPushChannel);
            try {
                JSONObject sfDataProperties = null;
                if (!TextUtils.isEmpty(sfData)) {
                    try {
                        String data = new JSONObject(sfData).optString("sf_data");
                        if (!TextUtils.isEmpty(data)) {
                            SALog.i(TAG, "sfData is " + data);
                            sfDataProperties = new JSONObject(data);
                        }
                    } catch (Exception e) {
                        SALog.i(TAG, "Failed to construct JSON");
                    }
                }
                if (sfDataProperties != null) {
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
            SensorsDataAPI.sharedInstance().track("$AppPushClick", eventProperties);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
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

        if (!SensorsDataAPI.getConfigOptions().mEnableTrackPush) {
            SALog.i(TAG, "enableTrackPush is false");
            return;
        }

        SALog.i(TAG, String.format("trackJPushAppOpenNotification is called, title is %s, content is %s," +
                " extras is %s, appPushChannel is %s, appPushServiceName is %s", title, content, extras, appPushChannel, "JPush"));
        trackNotificationOpenedEvent(extras, title, content, "JPush", appPushChannel);
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
        if (!SensorsDataAPI.getConfigOptions().mEnableTrackPush) {
            SALog.i(TAG, "enableTrackPush is false");
            return;
        }

        SALog.i(TAG, String.format("trackMeizuAppOpenNotification is called, title is %s, content is %s," +
                " extras is %s, appPushChannel is %s, appPushServiceName is %s", title, content, extras, "Meizu", appPushServiceName));
        try {
            String sfData = extras;
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
                            sfData = contentJson.optString("n_extras");
                        }
                    }
                    appPushServiceName = "JPush";
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
            trackNotificationOpenedEvent(sfData, title, content, appPushServiceName, "Meizu");
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
