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
import com.sensorsdata.analytics.android.sdk.core.event.Event;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.core.event.TrackEvent;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;

import org.json.JSONException;
import org.json.JSONObject;

class ItemEventAssemble extends BaseEventAssemble {
    private static final String TAG = "SA.ItemEventAssemble";

    public ItemEventAssemble(SAContextManager saContextManager) {
        super(saContextManager);
    }

    @Override
    public Event assembleData(InputData input) {
        try {
            if (isEventIgnore(input)) {
                return null;
            }
            TrackEvent trackEvent = new TrackEvent();
            appendDefaultProperty(input, trackEvent);
            appendLibProperty(trackEvent);
            handlePropertyProtocols(trackEvent);
            if (SALog.isLogEnabled()) {
                SALog.i(TAG, "track item event:\n" + JSONUtils.formatJson(trackEvent.toJSONObject().toString()));
            }
            return trackEvent;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    private boolean isEventIgnore(InputData inputData) {
        try {
            SADataHelper.assertPropertyTypes(inputData.getProperties());
            SADataHelper.assertItemId(inputData.getItemId());
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return true;
        }
        return false;
    }

    private void appendDefaultProperty(InputData inputData, TrackEvent trackEvent) {
        if (SADataHelper.assertPropertyKey(inputData.getItemType())) {
            trackEvent.setItemType(inputData.getItemType());
        }
        trackEvent.setItemId(inputData.getItemId());
        trackEvent.setType(inputData.getEventType().getEventType());
        trackEvent.setTime(inputData.getTime());
        try {
            trackEvent.setProperties(TimeUtils.formatDate(JSONUtils.cloneJsonObject(inputData.getProperties())));
        } catch (InvalidDataException e) {
            SALog.printStackTrace(e);
        }
    }

    private void appendLibProperty(TrackEvent trackEvent) throws JSONException {
       SensorsDataAPI sensorsDataAPI = SensorsDataAPI.sharedInstance();
        JSONObject libProperties = new JSONObject();
        libProperties.put("$lib", "Android");
        libProperties.put("$lib_version", sensorsDataAPI.getSDKVersion());
        libProperties.put("$lib_method", "code");
        libProperties.put("$app_version", AppInfoUtils.getAppVersionName(sensorsDataAPI.getSAContextManager().getContext()));
        JSONObject superProperties = PersistentLoader.getInstance().getSuperPropertiesPst().get();
        if (superProperties != null) {
            if (superProperties.has("$app_version")) {
                libProperties.put("$app_version", superProperties.get("$app_version"));
            }
        }

        StackTraceElement[] trace = (new Exception()).getStackTrace();
        if (trace.length > 1) {
            StackTraceElement traceElement = trace[0];
            String libDetail = String.format("%s##%s##%s##%s", traceElement
                            .getClassName(), traceElement.getMethodName(), traceElement.getFileName(),
                    traceElement.getLineNumber());
            if (!TextUtils.isEmpty(libDetail)) {
                libProperties.put("$lib_detail", libDetail);
            }
        }
        trackEvent.setLib(libProperties);
    }
}
