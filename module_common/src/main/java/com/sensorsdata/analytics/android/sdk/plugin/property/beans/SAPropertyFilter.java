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

import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 过滤插件对象
 */
public class SAPropertyFilter {
    public static final String PROPERTIES = "properties";
    public static final String LIB = "lib";
    public static final String IDENTITIES = "identities";
    private String event;
    private EventType type;
    long time;
    private final Map<String,JSONObject> eventJson;

    public SAPropertyFilter() {
        eventJson = new HashMap<>();
    }

    /**
     * 获取事件名
     * @return 事件名
     */
    public String getEvent() {
        return event;
    }

    /**
     * 设置事件名
     * @param event 事件名
     */
    public void setEvent(String event) {
        this.event = event;
    }

    /**
     * 获取事件类型
     * @return 事件类型
     */
    public EventType getType() {
        return type;
    }

    /**
     * 设置事件类型
     * @param type 事件类型
     */
    public void setType(EventType type) {
        this.type = type;
    }

    /**
     * 获取 properties 中的属性
     * @return properties 属性
     */
    public JSONObject getProperties() {
        return eventJson.get("properties");
    }

    /**
     * 设置 properties 属性
     * @param properties properties
     */
    public void setProperties(JSONObject properties) {
        eventJson.put("properties", properties);
    }

    /**
     * 获取事件字段值
     * @param name 字段名
     * @return 字段值
     */
    public JSONObject getEventJson(String name) {
        return eventJson.get(name);
    }

    /**
     * 设置事件字段值
     * @param name 字段名
     * @param jsonObject 值
     */
    public void setEventJson(String name, JSONObject jsonObject) {
        this.eventJson.put(name, jsonObject);
    }

    /**
     * 获取事件时间戳
     * @return 时间戳
     */
    public long getTime() {
        return time;
    }

    /**
     * 设置事件时间戳
     * @param time 时间戳
     */
    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "SAPropertyFilter{" +
                "event='" + event + '\'' +
                ", type=" + type +
                ", time=" + time +
                ", eventJson=" + eventJson +
                '}';
    }
}
