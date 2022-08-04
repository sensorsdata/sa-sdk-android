/*
 * Created by dengshiwei on 2022/06/15.
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

package com.sensorsdata.analytics.android.sdk.core.event.imp;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.business.timer.EventTimer;
import com.sensorsdata.analytics.android.sdk.core.business.timer.EventTimerManager;
import com.sensorsdata.analytics.android.sdk.core.event.Event;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.core.event.TrackEvent;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.internal.beans.InternalConfigOptions;
import com.sensorsdata.analytics.android.sdk.plugin.property.SAPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertyFilter;
import com.sensorsdata.analytics.android.sdk.plugin.property.impl.InternalCustomPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.SAContextManager;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;

class TrackEventAssemble extends BaseEventAssemble {
    private static final String TAG = "SA.TrackEventAssemble";

    public TrackEventAssemble(InternalConfigOptions internalConfigs) {
        super(internalConfigs);
    }

    @Override
    public Event assembleData(InputData input) {
        try {
            EventType eventType = input.getEventType();
            JSONObject properties = JSONUtils.cloneJsonObject(input.getProperties());
            if (properties == null) {
                properties = new JSONObject();
            }

            SensorsDataAPI sensorsDataAPI = SensorsDataAPI.sharedInstance();
            if (isEventIgnore(input.getEventName(), eventType, sensorsDataAPI.getSAContextManager())) {
                return null;
            }

            TrackEvent trackEvent = new TrackEvent();
            trackEvent.setProperties(properties);
            appendDefaultProperty(input, trackEvent);
            appendEventDuration(trackEvent);
            appendLibProperty(eventType, trackEvent, sensorsDataAPI);
            handleSpecialEvent(trackEvent);
            appendUserIDs(eventType, trackEvent, sensorsDataAPI);
            appendSessionId(eventType, trackEvent);
            appendPluginProperties(eventType, properties, trackEvent, sensorsDataAPI.getSAContextManager());
            handlePropertyProtocols(trackEvent);
            if (!handleEventCallback(eventType, trackEvent)) {
                return null;
            }
            appendPluginVersion(eventType, trackEvent);
            SADataHelper.assertPropertyTypes(trackEvent.getProperties());
            handleEventListener(eventType, trackEvent, sensorsDataAPI.getSAContextManager());
            if (SALog.isLogEnabled()) {
                SALog.i(TAG, "track event:\n" + JSONUtils.formatJson(trackEvent.toJSONObject().toString()));
            }
            return trackEvent;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    private boolean isEventIgnore(String eventName, EventType eventType, SAContextManager contextManager) {
        if (eventType.isTrack()) {
            SADataHelper.assertEventName(eventName);
            //如果在线控制禁止了事件，则不触发
            return !TextUtils.isEmpty(eventName) && contextManager.getRemoteManager() != null &&
                    contextManager.getRemoteManager().ignoreEvent(eventName);
        }
        return false;
    }

    private void appendDefaultProperty(InputData inputData, TrackEvent trackEvent) {
        try {
            trackEvent.setTime(inputData.getTime());
            trackEvent.setEventName(inputData.getEventName());
            trackEvent.setType(inputData.getEventType().getEventType());
            SecureRandom secureRandom = new SecureRandom();
            trackEvent.setTrackId(secureRandom.nextInt());
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private void appendEventDuration(TrackEvent trackEvent) {
        try {
            String eventName = trackEvent.getEventName();
            if (!TextUtils.isEmpty(eventName)) {
                EventTimer eventTimer = EventTimerManager.getInstance().getEventTimer(eventName);
                if (eventTimer != null) {
                    if (eventName.endsWith("_SATimer") && eventName.length() > 45) {// Timer 计时交叉计算拼接的字符串长度 45
                        eventName = eventName.substring(0, eventName.length() - 45);
                        trackEvent.setEventName(eventName);
                        SALog.i(TAG, "trigger event name = " + eventName);
                    }
                    float duration = eventTimer.duration();
                    if (duration > 0) {
                        trackEvent.getProperties().put("event_duration", Float.valueOf(duration));
                    }
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private void appendLibProperty(EventType eventType, TrackEvent trackEvent, SensorsDataAPI sensorsDataAPI) throws JSONException {
        JSONObject libProperties = new JSONObject();
        JSONObject propertyJson = trackEvent.getProperties();
        String libDetail = null;
        if (propertyJson != null) {
            if (eventType.isTrack()) {
                String libMethod = trackEvent.getProperties().optString("$lib_method", "code");
                libProperties.put("$lib_method", libMethod);
                propertyJson.put("$lib_method", libMethod);
            } else {
                libProperties.put("$lib_method", "code");
            }
            // replace $lib_detail
            if (propertyJson.has("$lib_detail")) {
                libDetail = propertyJson.optString("$lib_detail");
                propertyJson.remove("$lib_detail");
            }
        } else {
            libProperties.put("$lib_method", "code");
            if (eventType.isTrack()) {
                propertyJson = new JSONObject();
                propertyJson.put("$lib_method", "code");
            }
        }
        libProperties.put("$lib", "Android");
        libProperties.put("$lib_version", SensorsDataAPI.sharedInstance().getSDKVersion());
        libProperties.put("$app_version", AppInfoUtils.getAppVersionName(mInternalConfigs.context));
        //update lib $app_version from super properties
        JSONObject superProperties = sensorsDataAPI.getSAContextManager().getSuperProperties().get();
        if (superProperties != null) {
            if (superProperties.has("$app_version")) {
                libProperties.put("$app_version", superProperties.get("$app_version"));
            }
        }

        if (sensorsDataAPI.isAutoTrackEnabled()
                && propertyJson != null && isAutoTrackType(trackEvent.getEventName())) {
            SensorsDataAPI.AutoTrackEventType trackEventType = autoTrackEventTypeFromEventName(trackEvent.getEventName());
            if (trackEventType != null && !sensorsDataAPI.isAutoTrackEventTypeIgnored(trackEventType)
                    && trackEvent.getProperties().has("$screen_name")) {
                String screenName = propertyJson.getString("$screen_name");
                if (!TextUtils.isEmpty(screenName)) {
                    String[] screenNameArray = screenName.split("\\|");
                    if (screenNameArray.length > 0) {
                        libDetail = String.format("%s##%s##%s##%s", screenNameArray[0], "", "", "");
                    }
                }
            }
        }

        if (TextUtils.isEmpty(libDetail)) {
            StackTraceElement[] trace = (new Exception()).getStackTrace();
            if (trace.length > 1) {
                StackTraceElement traceElement = trace[0];
                libDetail = String.format("%s##%s##%s##%s", traceElement
                                .getClassName(), traceElement.getMethodName(), traceElement.getFileName(),
                        traceElement.getLineNumber());
            }
        }

        libProperties.put("$lib_detail", libDetail);
        trackEvent.setLib(libProperties);
        trackEvent.setProperties(propertyJson);
    }

    private void handleSpecialEvent(TrackEvent trackEvent) {
        if (trackEvent.getProperties() == null) {
            return;
        }

        try {
            JSONObject properties = trackEvent.getProperties();
            //  handle $AppStart、$AppEnd
            if ("$AppEnd".equals(trackEvent.getEventName())) {
                long appEndTime = properties.optLong("event_time");
                // 退出时间戳不合法不使用，2000 为打点间隔时间戳
                if (appEndTime > 2000) {
                    trackEvent.setTime(appEndTime);
                }
                String appEnd_lib_version = properties.optString("$lib_version");
                if (!TextUtils.isEmpty(appEnd_lib_version)) {
                    trackEvent.getLib().put("$lib_version", appEnd_lib_version);
                } else {
                    properties.remove("$lib_version");
                }

                String appEnd_app_version = properties.optString("$app_version");
                if (TextUtils.isEmpty(appEnd_app_version)) {
                    properties.remove("$app_version");
                } else {
                    trackEvent.getLib().put("$app_version", appEnd_app_version);
                }

                properties.remove("event_time");
            } else if ("$AppStart".equals(trackEvent.getEventName())) {
                long appStartTime = properties.optLong("event_time");
                if (appStartTime > 0) {
                    trackEvent.setTime(appStartTime);
                }
                properties.remove("event_time");
            }
            trackEvent.setProperties(properties);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private void appendPluginProperties(EventType eventType, JSONObject properties, TrackEvent trackEvent, SAContextManager contextManager) throws JSONException {
        SAPropertyFilter filter = new SAPropertyFilter();
        filter.setEvent(trackEvent.getEventName());
        filter.setTime(trackEvent.getTime());
        filter.setEventJson(SAPropertyFilter.LIB, new JSONObject(trackEvent.getLib().toString()));
        filter.setEventJson(SAPropertyFilter.IDENTITIES, new JSONObject(trackEvent.getIdentities().toString()));
        filter.setProperties(trackEvent.getProperties());
        filter.setType(eventType);
        // custom properties from user
        SAPropertyPlugin customPlugin = contextManager.getPluginManager().getPropertyPlugin(InternalCustomPropertyPlugin.class.getName());
        if (customPlugin instanceof InternalCustomPropertyPlugin) {
            ((InternalCustomPropertyPlugin) customPlugin).saveCustom(properties);
        }

        contextManager.getPluginManager().propertiesHandler(filter);
    }

    private void appendUserIDs(EventType eventType, TrackEvent trackEvent, SensorsDataAPI sensorsDataAPI) throws JSONException {
        String distinctId = sensorsDataAPI.getDistinctId();
        String loginId = sensorsDataAPI.getLoginId();
        String anonymousId = sensorsDataAPI.getAnonymousId();
        try {
            //针对 SF 弹窗展示事件特殊处理
            if ("$PlanPopupDisplay".equals(trackEvent.getEventName())) {
                if (trackEvent.getProperties().has("$sf_internal_anonymous_id")) {
                    anonymousId = trackEvent.getProperties().optString("$sf_internal_anonymous_id");
                    trackEvent.getProperties().remove("$sf_internal_anonymous_id");
                }

                if (trackEvent.getProperties().has("$sf_internal_login_id")) {
                    loginId = trackEvent.getProperties().optString("$sf_internal_login_id");
                    trackEvent.getProperties().remove("$sf_internal_login_id");
                }
                if (!TextUtils.isEmpty(loginId)) {
                    distinctId = loginId;
                } else {
                    distinctId = anonymousId;
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }

        trackEvent.setDistinctId(distinctId);
        if (!TextUtils.isEmpty(loginId)) {
            trackEvent.setLoginId(loginId);
        }
        trackEvent.setAnonymousId(anonymousId);
        trackEvent.setIdentities(sensorsDataAPI.getSAContextManager().getUserIdentityAPI().getIdentities(eventType));
        if (eventType == EventType.TRACK || eventType == EventType.TRACK_ID_BIND || eventType == EventType.TRACK_ID_UNBIND) {
            //是否首日访问
            trackEvent.getProperties().put("$is_first_day", sensorsDataAPI.getSAContextManager().isFirstDay(trackEvent.getTime()));
        } else if (eventType == EventType.TRACK_SIGNUP) {
            trackEvent.setOriginalId(trackEvent.getAnonymousId());
        }
    }

    static boolean isAutoTrackType(String eventName) {
        if (!TextUtils.isEmpty(eventName)) {
            switch (eventName) {
                case "$AppStart":
                case "$AppEnd":
                case "$AppClick":
                case "$AppViewScreen":
                    return true;
                default:
                    break;
            }
        }
        return false;
    }

    static SensorsDataAPI.AutoTrackEventType autoTrackEventTypeFromEventName(String eventName) {
        if (TextUtils.isEmpty(eventName)) {
            return null;
        }

        switch (eventName) {
            case "$AppStart":
                return SensorsDataAPI.AutoTrackEventType.APP_START;
            case "$AppEnd":
                return SensorsDataAPI.AutoTrackEventType.APP_END;
            case "$AppClick":
                return SensorsDataAPI.AutoTrackEventType.APP_CLICK;
            case "$AppViewScreen":
                return SensorsDataAPI.AutoTrackEventType.APP_VIEW_SCREEN;
            default:
                break;
        }

        return null;
    }
}