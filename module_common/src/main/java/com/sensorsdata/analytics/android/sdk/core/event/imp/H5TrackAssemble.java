/*
 * Created by dengshiwei on 2022/06/16.
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
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.core.event.Event;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.core.event.TrackEvent;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertiesFetcher;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertyFilter;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.Locale;

class H5TrackAssemble extends BaseEventAssemble {
    private static final String TAG = "SA.H5TrackAssemble";

    public H5TrackAssemble(SAContextManager saContextManager) {
        super(saContextManager);
    }

    @Override
    public Event assembleData(InputData input) {
        try {
            if (TextUtils.isEmpty(input.getExtras())) {
                return null;
            }

            TrackEvent trackEvent = new TrackEvent();
            trackEvent.setExtras(new JSONObject(input.getExtras()));
            // update eventName
            String eventName = trackEvent.getExtras().optString("event");
            SADataHelper.assertEventName(eventName);
            trackEvent.setEventName(eventName);
            // update property
            JSONObject propertiesObject = trackEvent.getExtras().optJSONObject("properties");
            SADataHelper.assertPropertyTypes(propertiesObject);
            if (propertiesObject == null) {
                propertiesObject = new JSONObject();
            }
            trackEvent.setProperties(propertiesObject);

            String type = trackEvent.getExtras().getString("type");
            EventType eventType = EventType.valueOf(type.toUpperCase(Locale.getDefault()));
            SensorsDataAPI sensorsDataAPI = SensorsDataAPI.sharedInstance();
            SAContextManager contextManager = sensorsDataAPI.getSAContextManager();

            appendDefaultProperty(trackEvent);
            overrideH5Ids(eventType, trackEvent, sensorsDataAPI);
            appendNativeProperty(eventType, trackEvent, contextManager);
            appendPropertyPlugin(eventType, trackEvent, sensorsDataAPI);
            appendSessionId(eventType, trackEvent);
            handlePropertyProtocols(trackEvent);
            if (!handleEventCallback(eventType, trackEvent)) {
                return null;
            }
            appendPluginVersion(eventType, trackEvent);
            removeH5Property(trackEvent);
            if (updateIdentities(eventType, trackEvent, sensorsDataAPI, contextManager)) {
                // update lib、property
                SADataHelper.assertPropertyTypes(trackEvent.getProperties());
                trackEvent.getExtras().put("properties", trackEvent.getProperties());
                trackEvent.getExtras().put("lib", trackEvent.getLib());
                if (SALog.isLogEnabled()) {
                    SALog.i(TAG, "track event from H5:\n" + JSONUtils.formatJson(trackEvent.getExtras().toString()));
                }
                return trackEvent;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    private void overrideH5Ids(EventType eventType, TrackEvent trackEvent, SensorsDataAPI sensorsDataAPI) throws JSONException {
        String distinctIdKey = "distinct_id";
        if (eventType == EventType.TRACK_SIGNUP) {
            trackEvent.getExtras().put("original_id", sensorsDataAPI.getAnonymousId());
        } else if (!TextUtils.isEmpty(sensorsDataAPI.getLoginId())) {
            trackEvent.getExtras().put(distinctIdKey, sensorsDataAPI.getLoginId());
        } else {
            trackEvent.getExtras().put(distinctIdKey, sensorsDataAPI.getAnonymousId());
        }
        trackEvent.getExtras().put("anonymous_id", sensorsDataAPI.getAnonymousId());
    }

    private void appendDefaultProperty(TrackEvent trackEvent) {
        try {
            trackEvent.getExtras().put("_hybrid_h5", true);
            trackEvent.getExtras().put("time", System.currentTimeMillis());
            SecureRandom secureRandom = new SecureRandom();
            trackEvent.getExtras().put("_track_id", secureRandom.nextInt());
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private void appendNativeProperty(EventType eventType, TrackEvent trackEvent, SAContextManager contextManager) throws JSONException {
        JSONObject libObject = trackEvent.getExtras().optJSONObject("lib");
        if (libObject != null) {
            libObject.put("$app_version", AppInfoUtils.getAppVersionName(contextManager.getContext()));
            //update lib $app_version from super properties
            JSONObject superProperties = PersistentLoader.getInstance().getSuperPropertiesPst().get();
            if (superProperties != null) {
                if (superProperties.has("$app_version")) {
                    libObject.put("$app_version", superProperties.get("$app_version"));
                }
            }
            trackEvent.setLib(libObject);
        }

        //之前可能会因为没有权限无法获取运营商信息，检测再次获取
        SADataHelper.addCarrier(contextManager.getContext(), trackEvent.getProperties());
        if (eventType.isTrack()) {
            //是否首日访问
            trackEvent.getProperties().put("$is_first_day", contextManager.isFirstDay(trackEvent.getTime()));
        }
    }

    private void appendPropertyPlugin(EventType eventType, TrackEvent trackEvent, SensorsDataAPI sensorsDataAPI) {
        SAPropertyFilter filter = new SAPropertyFilter();
        filter.setEvent(trackEvent.getEventName());
        filter.setEventJson(SAPropertyFilter.LIB, trackEvent.getExtras().optJSONObject("lib"));
        filter.setProperties(trackEvent.getProperties());
        filter.setType(eventType);

        SAPropertiesFetcher propertiesFetcher = sensorsDataAPI.getSAContextManager().getPluginManager().propertiesHandler(filter);
        if (propertiesFetcher != null) {
            trackEvent.setProperties(propertiesFetcher.getProperties());
            trackEvent.setLib(propertiesFetcher.getEventJson(SAPropertyFilter.LIB));
        }
    }

    private void removeH5Property(TrackEvent trackEvent) {
        if (trackEvent.getExtras().has("_nocache")) {
            trackEvent.getExtras().remove("_nocache");
        }

        if (trackEvent.getExtras().has("server_url")) {
            trackEvent.getExtras().remove("server_url");
        }

        if (trackEvent.getExtras().has("_flush_time")) {
            trackEvent.getExtras().remove("_flush_time");
        }
    }

    private boolean updateIdentities(EventType eventType, TrackEvent trackEvent, SensorsDataAPI sensorsDataAPI, SAContextManager contextManager) {
        try {
            if (eventType != EventType.TRACK_SIGNUP && !TextUtils.isEmpty(sensorsDataAPI.getLoginId())) {
                trackEvent.getExtras().put("login_id", sensorsDataAPI.getLoginId());
            }
            if (contextManager.getUserIdentityAPI().mergeH5Identities(eventType, trackEvent.getExtras())) {
                return true;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return false;
    }
}
