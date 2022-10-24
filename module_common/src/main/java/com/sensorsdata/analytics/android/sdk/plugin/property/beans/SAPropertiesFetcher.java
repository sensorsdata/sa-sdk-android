/*
 * Created by yuejianzhong on 2022/05/10.
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

package com.sensorsdata.analytics.android.sdk.plugin.property.beans;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 接收属性对象
 */
public class SAPropertiesFetcher {
    private final Map<String, JSONObject> eventJson;

    public SAPropertiesFetcher() {
        eventJson = new HashMap<>();
    }

    /**
     * 获取 properties 中属性
     *
     * @return properties 中的属性
     */
    public JSONObject getProperties() {
        return eventJson.get(SAPropertyFilter.PROPERTIES);
    }

    /**
     * 设置 properties 中属性
     */
    public void setProperties(JSONObject properties) {
        eventJson.put(SAPropertyFilter.PROPERTIES, properties);
    }

    /**
     * 根据名称获取 Event、Profile 中的字段
     *
     * @param name 字段名
     * @return 对应 JSON
     */
    public JSONObject getEventJson(String name) {
        return eventJson.get(name);
    }

    /**
     * 保存  Event、Profile 中的对应的字段
     *
     * @param name 字段名
     * @param jsonObject 值
     */
    public void setEventJson(String name, JSONObject jsonObject) {
        this.eventJson.put(name, jsonObject);
    }

    @Override
    public String toString() {
        return "SAPropertiesFetcher{" +
                "eventJson=" + eventJson +
                '}';
    }
}
