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
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.core.business.timer.EventTimer;
import com.sensorsdata.analytics.android.sdk.core.business.timer.EventTimerManager;
import com.sensorsdata.analytics.android.sdk.core.event.Event;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.core.event.TrackEvent;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.plugin.property.SAPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertiesFetcher;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertyFilter;
import com.sensorsdata.analytics.android.sdk.plugin.property.impl.InternalCustomPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.useridentity.Identities;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;

class TrackEventAssemble extends BaseEventAssemble {
    private static final String TAG = "SA.TrackEventAssemble";
    private final SAContextManager mContextManager;

    public TrackEventAssemble(SAContextManager saContextManager) {
        super(saContextManager);
        mContextManager = saContextManager;
    }

    @Override
    public Event assembleData(InputData input) {
        try {
            EventType eventType = input.getEventType();
            JSONObject properties = JSONUtils.cloneJsonObject(input.getProperties());
            if (properties == null) {
                properties = new JSONObject();
            }

            if (isEventIgnore(input.getEventName(), eventType, mContextManager)) {
                return null;
            }
            TrackEvent trackEvent = new TrackEvent();
            trackEvent.setProperties(properties);
            appendDefaultProperty(input, trackEvent);
            appendEventDuration(trackEvent);
            appendLibProperty(eventType, trackEvent);
            appendUserIDs(input, trackEvent);
            appendSessionId(eventType, trackEvent);
            appendPluginProperties(eventType, input.getProperties(), trackEvent);
            handlePropertyProtocols(trackEvent);
            if (!handleEventCallback(eventType, trackEvent)) {
                return null;
            }
            appendPluginVersion(eventType, trackEvent);
            overrideDeviceId(eventType, trackEvent, mContextManager);
            SADataHelper.assertPropertyTypes(trackEvent.getProperties());
            handleEventListener(eventType, trackEvent, mContextManager);
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
                    float duration = eventTimer.duration();
                    if (duration > 0) {
                        trackEvent.getProperties().put("event_duration", Float.valueOf(duration));
                    }
                }
                if (eventName.endsWith("_SATimer") && eventName.length() > 45) {// Timer 计时交叉计算拼接的字符串长度 45
                    eventName = eventName.substring(0, eventName.length() - 45);
                    trackEvent.setEventName(eventName);
                    SALog.i(TAG, "trigger event name = " + eventName);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private void appendLibProperty(EventType eventType, TrackEvent trackEvent) throws JSONException {
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
        libProperties.put("$lib_version", mContextManager.getSensorsDataAPI().getSDKVersion());
        libProperties.put("$app_version", AppInfoUtils.getAppVersionName(mContextManager.getContext()));
        //update lib $app_version from super properties
        JSONObject superProperties = PersistentLoader.getInstance().getSuperPropertiesPst().get();
        if (superProperties != null) {
            if (superProperties.has("$app_version")) {
                libProperties.put("$app_version", superProperties.get("$app_version"));
            }
        }

        if (mContextManager.getSensorsDataAPI().isAutoTrackEnabled()
                && propertyJson != null && isAutoTrackType(trackEvent.getEventName())) {
            SensorsDataAPI.AutoTrackEventType trackEventType = autoTrackEventTypeFromEventName(trackEvent.getEventName());
            if (trackEventType != null && !mContextManager.getSensorsDataAPI().isAutoTrackEventTypeIgnored(trackEventType)
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

    private void appendPluginProperties(EventType eventType, JSONObject properties, TrackEvent trackEvent) throws JSONException {
        SAPropertyFilter filter = new SAPropertyFilter();
        filter.setEvent(trackEvent.getEventName());
        filter.setTime(trackEvent.getTime());
        filter.setEventJson(SAPropertyFilter.LIB, trackEvent.getLib());
        if (trackEvent.getIdentities() != null) {
            filter.setEventJson(SAPropertyFilter.IDENTITIES, new JSONObject(trackEvent.getIdentities().toString()));
        }
        filter.setProperties(trackEvent.getProperties());
        filter.setType(eventType);
        // custom properties from user
        SAPropertyPlugin customPlugin = mContextManager.getPluginManager().getPropertyPlugin(InternalCustomPropertyPlugin.class.getName());
        if (customPlugin instanceof InternalCustomPropertyPlugin) {
            ((InternalCustomPropertyPlugin) customPlugin).saveCustom(properties);
        }

        SAPropertiesFetcher propertiesFetcher = mContextManager.getPluginManager().propertiesHandler(filter);
        if (propertiesFetcher != null) {
            trackEvent.setProperties(propertiesFetcher.getProperties());
            trackEvent.setLib(propertiesFetcher.getEventJson(SAPropertyFilter.LIB));
        }
    }

    private void appendUserIDs(InputData inputData, TrackEvent trackEvent) throws JSONException {
        String distinctId = mContextManager.getUserIdentityAPI().getDistinctId();
        String loginId = mContextManager.getUserIdentityAPI().getLoginId();
        String anonymousId = mContextManager.getUserIdentityAPI().getAnonymousId();
        try {
            //针对 SF 弹窗展示事件特殊处理
            if ("$PlanPopupDisplay".equals(trackEvent.getEventName())) {
                if (trackEvent.getProperties().has("$sf_internal_anonymous_id")) {
                    anonymousId = trackEvent.getProperties().optString("$sf_internal_anonymous_id");
                    trackEvent.getProperties().remove("$sf_internal_anonymous_id");
                    inputData.getProperties().remove("$sf_internal_anonymous_id");
                }

                if (trackEvent.getProperties().has("$sf_internal_login_id")) {
                    loginId = trackEvent.getProperties().optString("$sf_internal_login_id");
                    trackEvent.getProperties().remove("$sf_internal_login_id");
                    inputData.getProperties().remove("$sf_internal_login_id");
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

        EventType eventType = inputData.getEventType();
        JSONObject identitiesJson = mContextManager.getUserIdentityAPI().getIdentities(eventType);
        if (!EventType.TRACK_ID_UNBIND.getEventType().equals(trackEvent.getType())) {// unbind 不需要拼接 login_id、anonymous_id
            if (identitiesJson != null) {
                if (!TextUtils.isEmpty(anonymousId)) {
                    identitiesJson.put(Identities.ANONYMOUS_ID, anonymousId);
                } else {
                    anonymousId = identitiesJson.optString(Identities.ANONYMOUS_ID);
                }

                if (!TextUtils.isEmpty(loginId)) {
                    identitiesJson.put(mContextManager.getUserIdentityAPI().getIdentitiesInstance().getLoginIDKey(), loginId);
                } else {
                    loginId = identitiesJson.optString(mContextManager.getUserIdentityAPI().getIdentitiesInstance().getLoginIDKey());
                }
            }

            if (!TextUtils.isEmpty(loginId)) {
                trackEvent.setLoginId(loginId);
            }
            trackEvent.setAnonymousId(anonymousId);
        }

        trackEvent.setDistinctId(distinctId);
        trackEvent.setIdentities(identitiesJson);
        if (eventType == EventType.TRACK || eventType == EventType.TRACK_ID_BIND || eventType == EventType.TRACK_ID_UNBIND) {
            //是否首日访问
            trackEvent.getProperties().put("$is_first_day", mContextManager.isFirstDay(trackEvent.getTime()));
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
