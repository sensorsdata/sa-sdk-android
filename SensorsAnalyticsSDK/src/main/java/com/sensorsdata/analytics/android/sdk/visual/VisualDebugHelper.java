/*
 * Created by zhangxiangwei on 2021/04/07.
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

package com.sensorsdata.analytics.android.sdk.visual;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.listener.SAEventListener;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.ThreadUtils;
import com.sensorsdata.analytics.android.sdk.visual.model.VisualConfig;
import com.sensorsdata.analytics.android.sdk.visual.property.VisualPropertiesManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class VisualDebugHelper {

    private static final String TAG = "SA.VP.VisualDebugHelper";
    private JSONArray mJsonArray;
    private TrackEventAdapter mEventListener = null;
    private final Object object = new Object();

    void startMonitor() {
        try {
            if (mEventListener == null) {
                final ExecutorService executorService = ThreadUtils.getSinglePool();
                mEventListener = new TrackEventAdapter() {
                    @Override
                    public void trackEvent(final JSONObject jsonObject) {
                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                handlerEvent(jsonObject);
                            }
                        });
                    }
                };
            }
            SensorsDataAPI.sharedInstance().addEventListener(mEventListener);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    void stopMonitor() {
        try {
            if (mEventListener != null) {
                SensorsDataAPI.sharedInstance().removeEventListener(mEventListener);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private synchronized void handlerEvent(JSONObject jsonObject) {
        try {
            if (jsonObject == null) {
                return;
            }

            if (!VisualizedAutoTrackService.getInstance().isServiceRunning()) {
                return;
            }

            String eventName = jsonObject.optString("event");
            if (!TextUtils.equals(AopConstants.APP_CLICK_EVENT_NAME, eventName) && !TextUtils.equals(AopConstants.WEB_CLICK_EVENT_NAME, eventName)) {
                SALog.i(TAG, "eventName is " + eventName + " filter");
                return;
            }

            JSONObject propertyObject = jsonObject.optJSONObject("properties");
            if (propertyObject == null) {
                return;
            }

            if (!VisualPropertiesManager.getInstance().checkAppIdAndProject()) {
                return;
            }

            VisualConfig visualConfig = VisualPropertiesManager.getInstance().getVisualConfig();
            if (visualConfig == null) {
                return;
            }
            // 校验配置是否为空校验配置是否为空
            List<VisualConfig.VisualPropertiesConfig> propertiesConfigs = visualConfig.events;
            if (propertiesConfigs == null || propertiesConfigs.size() == 0) {
                SALog.i(TAG, "propertiesConfigs is empty ");
                return;
            }

            if (TextUtils.equals(AopConstants.APP_CLICK_EVENT_NAME, eventName)) {
                String screenName = propertyObject.optString("$screen_name");
                if (TextUtils.isEmpty(screenName)) {
                    SALog.i(TAG, "screenName is empty ");
                    return;
                }
                List<VisualConfig.VisualPropertiesConfig> eventConfigList = VisualPropertiesManager.getInstance().getMatchEventConfigList(
                        propertiesConfigs,
                        VisualPropertiesManager.VisualEventType.getVisualEventType(eventName), screenName,
                        propertyObject.optString("$element_path"),
                        propertyObject.optString("$element_position"),
                        propertyObject.optString("$element_content"));
                if (eventConfigList.size() > 0) {
                    synchronized (object) {
                        for (VisualConfig.VisualPropertiesConfig config : eventConfigList) {
                            try {
                                JSONObject object = new JSONObject();
                                SensorsDataUtils.mergeJSONObject(jsonObject, object);
                                object.put("event_name", config.eventName);
                                if (mJsonArray == null) {
                                    mJsonArray = new JSONArray();
                                }
                                mJsonArray.put(object);
                            } catch (Exception e) {
                                SALog.printStackTrace(e);
                            }
                        }
                    }
                }
            } else if (TextUtils.equals(AopConstants.WEB_CLICK_EVENT_NAME, eventName)) {
                try {
                    JSONArray array = propertyObject.optJSONArray("sensorsdata_web_visual_eventName");
                    if (array == null) {
                        int hashCode = jsonObject.hashCode();
                        array = VisualPropertiesManager.getInstance().getVisualPropertiesH5Helper().getEventName(hashCode);
                        VisualPropertiesManager.getInstance().getVisualPropertiesH5Helper().clearCache(hashCode);
                    }
                    if (array != null && array.length() > 0) {
                        synchronized (object) {
                            for (int i = 0; i < array.length(); i++) {
                                try {
                                    JSONObject object = new JSONObject();
                                    SensorsDataUtils.mergeJSONObject(jsonObject, object);
                                    object.put("event_name", array.optString(i));
                                    if (mJsonArray == null) {
                                        mJsonArray = new JSONArray();
                                    }
                                    mJsonArray.put(object);
                                } catch (Exception e) {
                                    SALog.printStackTrace(e);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    String getDebugInfo() {
        synchronized (object) {
            if (mJsonArray != null) {
                String result = mJsonArray.toString();
                mJsonArray = null;
                return result;
            }
            return null;
        }
    }

    private abstract static class TrackEventAdapter implements SAEventListener {
        @Override
        public void login() {

        }

        @Override
        public void logout() {

        }

        @Override
        public void identify() {

        }

        @Override
        public void resetAnonymousId() {

        }
    }
}
