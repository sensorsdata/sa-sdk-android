/*
 * Created by zhangxiangwei on 2021/01/28.
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

import android.text.TextUtils;
import android.view.View;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentVisualConfig;
import com.sensorsdata.analytics.android.sdk.visual.ViewTreeStatusObservable;
import com.sensorsdata.analytics.android.sdk.visual.model.VisualConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 可视化全埋点自定义属性缓存处理，提供缓存、序列化能力
 */
public class VisualPropertiesCache {

    private static final String TAG = "SA.VP.VisualPropertiesCache";

    private PersistentVisualConfig mPersistentVisualConfig;

    public VisualPropertiesCache() {
        this.mPersistentVisualConfig = (PersistentVisualConfig) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.VISUAL_PROPERTIES);
    }

    public void save2Cache(String config) {
        SALog.i(TAG, "save2Cache config is:" + config);
        this.mPersistentVisualConfig.commit(config);
        doOnSaveCache(config);
    }

    public String getVisualCache() {
        return this.mPersistentVisualConfig.get();
    }

    public VisualConfig getVisualConfig() {
        String persistentVisualConfig = this.mPersistentVisualConfig.get();
        SALog.i(TAG, "local visual config is :" + persistentVisualConfig);
        if (TextUtils.isEmpty(persistentVisualConfig)) {
            return null;
        }
        try {
            VisualConfig config = new VisualConfig();
            JSONObject object = new JSONObject(persistentVisualConfig);
            config.appId = object.optString("app_id");
            config.os = object.optString("os");
            config.project = object.optString("project");
            config.version = object.optString("version");
            JSONArray jsonArray = object.optJSONArray("events");

            if (jsonArray != null && jsonArray.length() > 0) {
                List<VisualConfig.VisualPropertiesConfig> visualPropertiesConfigs = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject visualPropertiesObject = jsonArray.optJSONObject(i);
                    if (visualPropertiesObject == null) {
                        continue;
                    }
                    VisualConfig.VisualPropertiesConfig propertiesConfig = new VisualConfig.VisualPropertiesConfig();
                    propertiesConfig.eventName = visualPropertiesObject.optString("event_name");
                    propertiesConfig.eventType = visualPropertiesObject.optString("event_type");

                    JSONObject eventObject = visualPropertiesObject.optJSONObject("event");
                    if (eventObject != null) {
                        VisualConfig.VisualEvent event = new VisualConfig.VisualEvent();
                        event.elementPath = eventObject.optString("element_path");
                        event.elementPosition = eventObject.optString("element_position");
                        event.elementContent = eventObject.optString("element_content");
                        event.screenName = eventObject.optString("screen_name");
                        event.limitElementPosition = eventObject.optBoolean("limit_element_position");
                        event.limitElementContent = eventObject.optBoolean("limit_element_content");
                        event.isH5 = eventObject.optBoolean("h5");
                        propertiesConfig.event = event;
                    }

                    List<VisualConfig.VisualProperty> visualProperties = new ArrayList<>();
                    JSONArray properties = visualPropertiesObject.optJSONArray("properties");
                    if (properties != null && properties.length() > 0) {
                        for (int j = 0; j < properties.length(); j++) {
                            JSONObject propertyObject = properties.optJSONObject(j);
                            VisualConfig.VisualProperty visualProperty = new VisualConfig.VisualProperty();
                            visualProperty.elementPath = propertyObject.optString("element_path");
                            visualProperty.elementPosition = propertyObject.optString("element_position");
                            visualProperty.screenName = propertyObject.optString("screen_name");
                            visualProperty.name = propertyObject.optString("name");
                            visualProperty.regular = propertyObject.optString("regular");
                            visualProperty.isH5 = propertyObject.optBoolean("h5");
                            visualProperty.type = propertyObject.optString("type");
                            visualProperty.webViewElementPath = propertyObject.optString("webview_element_path");
                            visualProperties.add(visualProperty);
                        }
                        propertiesConfig.properties = visualProperties;
                    }
                    visualPropertiesConfigs.add(propertiesConfig);
                }
                config.events = visualPropertiesConfigs;
            }
            return config;
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    private void doOnSaveCache(String config) {
        try {
            List<View> viewList = ViewTreeStatusObservable.getInstance().getCurrentWebView();
            if (viewList == null || viewList.size() == 0) {
                return;
            }
            for (View view : viewList) {
                VisualPropertiesManager.getInstance().getVisualPropertiesH5Helper().sendToWeb(view, "updateH5VisualConfig", config);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public JSONArray getH5JsonArrayFromCache(String eventName, String webViewElementPath) {
        String persistentVisualConfig = this.mPersistentVisualConfig.get();
        if (TextUtils.isEmpty(persistentVisualConfig)) {
            return null;
        }
        try {
            JSONObject object = new JSONObject(persistentVisualConfig);
            JSONArray array = new JSONArray();
            JSONArray jsonArray = object.optJSONArray("events");
            if (jsonArray != null && jsonArray.length() > 0) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject visualPropertiesObject = jsonArray.optJSONObject(i);
                    if (visualPropertiesObject == null) {
                        continue;
                    }
                    VisualConfig.VisualPropertiesConfig propertiesConfig = new VisualConfig.VisualPropertiesConfig();
                    propertiesConfig.eventName = visualPropertiesObject.optString("event_name");
                    if (!TextUtils.equals(propertiesConfig.eventName, eventName)) {
                        continue;
                    }
                    JSONArray properties = visualPropertiesObject.optJSONArray("properties");
                    if (properties != null && properties.length() > 0) {
                        for (int j = 0; j < properties.length(); j++) {
                            JSONObject propertyObject = properties.optJSONObject(j);
                            VisualConfig.VisualProperty visualProperty = new VisualConfig.VisualProperty();
                            visualProperty.webViewElementPath = propertyObject.optString("webview_element_path");
                            if (TextUtils.equals(visualProperty.webViewElementPath, webViewElementPath)) {
                                array.put(propertyObject);
                            }
                        }
                    }
                }
                return array;
            }
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }
}
