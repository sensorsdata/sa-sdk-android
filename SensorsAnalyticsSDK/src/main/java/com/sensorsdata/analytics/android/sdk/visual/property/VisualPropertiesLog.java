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

package com.sensorsdata.analytics.android.sdk.visual.property;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.visual.model.ViewNode;
import com.sensorsdata.analytics.android.sdk.visual.model.VisualConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VisualPropertiesLog implements VisualPropertiesManager.CollectLogListener {
    private JSONArray mJSONArray;
    private VisualPropertiesLog.Builder mBuilder;
    private final Object object = new Object();

    public synchronized String getVisualPropertiesLog() {
        synchronized (object) {
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
        mBuilder = new VisualPropertiesLog.Builder(eventType, screenName, elementPath, elementPosition, elementContent);
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
        mBuilder.buildPropertyElement(String.format("%s 属性未找到属性元素，属性元素路径为 %s，属性元素位置为 %s ", propertyName, propertyElementPath, propertyElementPosition));
        add2JsonArray(mBuilder.build());
    }

    @Override
    public void onParsePropertyContentFailure(String propertyName, String propertyType, String elementContent, String regular) {
        mBuilder.buildPropertyContentParse(String.format("%s 属性正则解析失败，元素内容 %s, 正则表达式为 %s,属性类型为 %s", propertyName, elementContent, regular, propertyType));
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

        Builder(String eventType, String screenName, String elementPath, String elementPosition, String elementContent) {
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
                this.switchControl = new JSONObject().put("title", "开关控制").put("message", "自定义属性运维配置开关关闭");
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
        }

        private void buildVisualConfig(String message) {
            try {
                this.visualConfig = new JSONObject().put("title", "获取配置").put("message", message);
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
        }

        private void buildEventConfig() {
            try {
                this.eventConfig = new JSONObject().put("title", "事件配置").put("message", "本地缓存不包含该事件配置");
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
        }

        private void buildPropertyElement(String message) {
            try {
                this.propertyElement = new JSONObject().put("title", "获取属性元素").put("message", message);
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
        }

        private void buildPropertyContentParse(String message) {
            try {
                this.propertyContentParse = new JSONObject().put("title", "解析属性").put("message", message);
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
        }

        private void buildOtherError(String message) {
            try {
                this.otherError = new JSONObject().put("title", "其他错误").put("message", message);
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
