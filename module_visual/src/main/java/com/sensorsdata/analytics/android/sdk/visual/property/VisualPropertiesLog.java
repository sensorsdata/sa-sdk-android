/*
 * Created by zhangxiangwei on 2021/04/07.
 * Copyright 2015－2022 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.visual.property;

import android.content.Context;

import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.util.SADisplayUtil;
import com.sensorsdata.analytics.android.sdk.visual.model.ViewNode;
import com.sensorsdata.analytics.android.sdk.visual.model.VisualConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VisualPropertiesLog implements VisualPropertiesManager.CollectLogListener {
    private JSONArray mJSONArray;
    private Builder mBuilder;
    private final Object object = new Object();
    private Context mContext;
    public synchronized String getVisualPropertiesLog() {
        synchronized (object) {
            mContext = SensorsDataAPI.sharedInstance().getSAContextManager().getContext();
            if (mJSONArray != null) {
                return mJSONArray.toString();
            }
            return null;
        }
    }

    private synchronized void add2JsonArray(JSONObject jsonObject) {
        synchronized (object) {
            if (mJSONArray == null) {
                mJSONArray = new JSONArray();
            }
            mJSONArray.put(jsonObject);
        }
    }

    @Override
    public void onStart(String eventType, String screenName, ViewNode viewNode) {
        String elementPath = null;
        String elementPosition = null;
        String elementContent = null;
        if (viewNode != null) {
            elementPath = viewNode.getViewPath();
            elementPosition = viewNode.getViewPosition();
            elementContent = viewNode.getViewContent();
        }
        mBuilder = new VisualPropertiesLog.Builder(mContext, eventType, screenName, elementPath, elementPosition, elementContent);
    }

    @Override
    public void onSwitchClose() {
        mBuilder.buildSwitchControl();
        add2JsonArray(mBuilder.build());
    }

    @Override
    public void onCheckVisualConfigFailure(String message) {
        mBuilder.buildVisualConfig(message);
        add2JsonArray(mBuilder.build());
    }

    @Override
    public void onCheckEventConfigFailure() {
        mBuilder.buildEventConfig();
        add2JsonArray(mBuilder.build());
    }

    @Override
    public void onFindPropertyElementFailure(String propertyName, String propertyElementPath, String propertyElementPosition) {
        mBuilder.buildPropertyElement(String.format(SADisplayUtil.getStringResource(mContext, R.string.sensors_analytics_visual_property_error), propertyName, propertyElementPath, propertyElementPosition));
        add2JsonArray(mBuilder.build());
    }

    @Override
    public void onParsePropertyContentFailure(String propertyName, String propertyType, String elementContent, String regular) {
        mBuilder.buildPropertyContentParse(String.format(SADisplayUtil.getStringResource(mContext, R.string.sensors_analytics_visual_regex_error), propertyName, elementContent, regular, propertyType));
        add2JsonArray(mBuilder.build());
    }

    @Override
    public void onOtherError(String message) {
        mBuilder.buildOtherError(message);
        add2JsonArray(mBuilder.build());
    }


    public static class Builder {
        private JSONObject switchControl;
        private JSONObject visualConfig;
        private JSONObject eventConfig;
        private JSONObject propertyElement;
        private JSONObject propertyContentParse;
        private JSONObject otherError;
        private String eventType;
        private String screenName;
        private String elementPath;
        private String elementPosition;
        private String elementContent;
        private String localConfig;
        private Context mContext;

        Builder(Context context, String eventType, String screenName, String elementPath, String elementPosition, String elementContent) {
            this.mContext = context;
            this.eventType = eventType;
            this.screenName = screenName;
            this.elementPath = elementPath;
            this.elementPosition = elementPosition;
            this.elementContent = elementContent;
            this.localConfig = null;
            VisualConfig config = VisualPropertiesManager.getInstance().getVisualConfig();
            if (config != null) {
                localConfig = config.toString();
            }
        }

        private void buildSwitchControl() {
            try {
                this.switchControl = new JSONObject().put("title", SADisplayUtil.getStringResource(mContext, R.string.sensors_analytics_visual_switch_error))
                        .put("message", SADisplayUtil.getStringResource(mContext, R.string.sensors_analytics_visual_property_switch_error));
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
        }

        private void buildVisualConfig(String message) {
            try {
                this.visualConfig = new JSONObject().put("title", SADisplayUtil.getStringResource(mContext, R.string.sensors_analytics_visual_config_error)).put("message", message);
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
        }

        private void buildEventConfig() {
            try {
                this.eventConfig = new JSONObject().put("title", SADisplayUtil.getStringResource(mContext, R.string.sensors_analytics_visual_event_error))
                        .put("message", SADisplayUtil.getStringResource(mContext, R.string.sensors_analytics_visual_cache_error));
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
        }

        private void buildPropertyElement(String message) {
            try {
                this.propertyElement = new JSONObject().put("title", SADisplayUtil.getStringResource(mContext, R.string.sensors_analytics_visual_getProperty_error)).put("message", message);
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
        }

        private void buildPropertyContentParse(String message) {
            try {
                this.propertyContentParse = new JSONObject().put("title", SADisplayUtil.getStringResource(mContext, R.string.sensors_analytics_visual_parseProperty_error)).put("message", message);
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
        }

        private void buildOtherError(String message) {
            try {
                this.otherError = new JSONObject().put("title", SADisplayUtil.getStringResource(mContext, R.string.sensors_analytics_visual_other_error)).put("message", message);
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
        }

        private JSONObject build() {
            JSONObject object = new JSONObject();
            try {
                object.put("event_type", eventType);
                object.put("element_path", elementPath);
                object.put("element_position", elementPosition);
                object.put("element_content", elementContent);
                object.put("screen_name", screenName);
                object.put("local_config", localConfig);
                JSONArray message = new JSONArray();
                if (switchControl != null) {
                    message.put(switchControl);
                }
                if (visualConfig != null) {
                    message.put(visualConfig);
                }
                if (eventConfig != null) {
                    message.put(eventConfig);
                }
                if (propertyElement != null) {
                    message.put(propertyElement);
                }
                if (propertyContentParse != null) {
                    message.put(propertyContentParse);
                }
                if (otherError != null) {
                    message.put(otherError);
                }
                object.put("message", message);
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
            return object;
        }
    }
}
