/*
 * Created by dengshiwei on 2022/06/21.
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

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.core.business.SAPluginVersion;
import com.sensorsdata.analytics.android.sdk.core.business.session.SessionRelatedManager;
import com.sensorsdata.analytics.android.sdk.core.event.EventProcessor;
import com.sensorsdata.analytics.android.sdk.core.event.TrackEvent;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.internal.beans.InternalConfigOptions;
import com.sensorsdata.analytics.android.sdk.listener.SAEventListener;
import com.sensorsdata.analytics.android.sdk.monitor.TrackMonitor;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Iterator;

public abstract class BaseEventAssemble implements EventProcessor.IAssembleData {

    private static final String TAG = "SA.BaseEventAssemble";
    protected InternalConfigOptions mInternalConfigs;

    public BaseEventAssemble(SAContextManager saContextManager) {
        this.mInternalConfigs = saContextManager.getInternalConfigs();
    }

    protected void appendSessionId(EventType eventType, TrackEvent trackEvent) {
        if (eventType.isTrack()) {
            try {
                SessionRelatedManager.getInstance().handleEventOfSession(trackEvent.getEventName(), trackEvent.getProperties(), trackEvent.getTime());
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    /**
     * @param eventType EventType
     * @param trackEvent TrackEvent
     * @return true enter db, false is ignored
     */
    protected boolean handleEventCallback(EventType eventType, TrackEvent trackEvent) {
        if (!eventType.isTrack()) {
            return true;
        }
        if (!isEnterDb(trackEvent.getEventName(), trackEvent.getProperties())) {
            SALog.i(TAG, trackEvent.getEventName() + " event can not enter database");
            return false;
        }
        return true;
    }

    protected void appendPluginVersion(EventType eventType, TrackEvent trackEvent) {
        if (!eventType.isTrack()) {
            return;
        }
        SAPluginVersion.appendPluginVersion(trackEvent.getProperties());
    }

    protected void handleEventListener(EventType eventType, TrackEvent trackEvent, SAContextManager contextManager) {
        try {
            if (contextManager.getEventListenerList() != null && eventType.isTrack()) {
                for (SAEventListener eventListener : contextManager.getEventListenerList()) {
                    eventListener.trackEvent(trackEvent.toJSONObject());
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }

        try {
            if (eventType.isTrack()) {
                TrackMonitor.getInstance().callTrack(trackEvent.toJSONObject());
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    protected void handlePropertyProtocols(TrackEvent trackEvent) throws JSONException {
        if (trackEvent.getProperties().has("$project")) {
            if (this instanceof H5TrackAssemble) {
                trackEvent.getExtras().put("project", trackEvent.getProperties().optString("$project"));
            } else {
                trackEvent.setProject(trackEvent.getProperties().optString("$project"));
            }
            trackEvent.getProperties().remove("$project");
        }

        if (trackEvent.getProperties().has("$token")) {
            if (this instanceof H5TrackAssemble) {
                trackEvent.getExtras().put("token", trackEvent.getProperties().optString("$token"));
            } else {
                trackEvent.setToken(trackEvent.getProperties().optString("$token"));
            }
            trackEvent.getProperties().remove("$token");
        }

        if (trackEvent.getProperties().has("$time")) {
            try {
                if (this instanceof H5TrackAssemble) { // from h5
                    long time = trackEvent.getProperties().getLong("$time");
                    if (TimeUtils.isDateValid(time)) {
                        trackEvent.getExtras().put("time", time);
                    }
                } else {
                    Object timeDate = trackEvent.getProperties().opt("$time");
                    if (timeDate instanceof Date && TimeUtils.isDateValid((Date) timeDate)) {
                        trackEvent.setTime(((Date) timeDate).getTime());
                    }
                }
            } catch (Exception ex) {
                SALog.printStackTrace(ex);
            }
            trackEvent.getProperties().remove("$time");
        }
    }

    private boolean isEnterDb(String eventName, JSONObject eventProperties) {
        boolean enterDb = true;
        if (mInternalConfigs.sensorsDataTrackEventCallBack != null) {
            SALog.i(TAG, "SDK have set trackEvent callBack");
            try {
                enterDb = mInternalConfigs.sensorsDataTrackEventCallBack.onTrackEvent(eventName, eventProperties);
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
            if (enterDb) {
                try {
                    Iterator<String> it = eventProperties.keys();
                    while (it.hasNext()) {
                        String key = it.next();
                        Object value = eventProperties.opt(key);
                        if (value instanceof Date) {
                            eventProperties.put(key, TimeUtils.formatDate((Date) value, TimeUtils.SDK_LOCALE));
                        } else {
                            eventProperties.put(key, value);
                        }
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        }
        return enterDb;
    }
}
