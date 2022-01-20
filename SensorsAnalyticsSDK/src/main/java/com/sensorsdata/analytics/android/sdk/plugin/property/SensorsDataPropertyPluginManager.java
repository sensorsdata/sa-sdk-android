/*
 * Created by luweibin on 2021/12/17.
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

package com.sensorsdata.analytics.android.sdk.plugin.property;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 属性插件的管理类，用于管理插件的添加和移除，以及事件属性的获取
 */
public final class SensorsDataPropertyPluginManager {
    private static final String TAG = "SA.SAPropertyPluginManager";
    private final Map<String, SAPropertyPlugin> plugins = new LinkedHashMap<>();

    private static class SingleHolder {
        private static final SensorsDataPropertyPluginManager INSTANCE = new SensorsDataPropertyPluginManager();
    }

    public static SensorsDataPropertyPluginManager getInstance() {
        return SingleHolder.INSTANCE;
    }

    private SensorsDataPropertyPluginManager() {
    }

    /**
     * 注册属性插件
     *
     * @param plugin 注册属性插件对象
     */
    public final void registerPropertyPlugin(SAPropertyPlugin plugin) {
        try {
            if (plugin == null) return;
            String propertyType = getPluginType(plugin);
            if (!plugins.containsKey(propertyType)) {
                plugins.put(propertyType, plugin);
                //插件注册成功后，立即开启插件的初始化
                plugin.start();
            } else {
                SALog.i(TAG, String.format("plugin [ %s ] has exist!", propertyType));
            }
        } catch (Exception e) {
            SALog.i(TAG, "register property plugin exception! " + e.toString());
        }
    }

    /**
     * 根据事件的 eventName，eventType，properties 来匹配已注册的插件，获取当前事件能匹配上的属性
     *
     * @param eventName 事件名
     * @param eventType 事件类型
     * @param properties 事件已有的属性
     * @return 当前事件能匹配的属性
     */
    public final JSONObject properties(String eventName, EventType eventType, JSONObject properties) {
        // step1.获取当前事件能匹配上的所有插件
        // step2.收集匹配上的所有插件的属性
        // step3.返回匹配上的属性
        long startPropertiesTime = System.currentTimeMillis();
        JSONObject jsonObject;
        try {
            jsonObject = properties(filter(eventName, eventType, properties));
        } catch (Exception e) {
            SALog.i(TAG, String.format("Event [%s] error is happened when matching property-plugins, e=%s", eventName, e.toString()));
            jsonObject = new JSONObject();
        }
        SALog.i(TAG, String.format("Event [%s] spend [%sms] on matching property-plugins", eventName, (System.currentTimeMillis() - startPropertiesTime)));
        return jsonObject;
    }

    /**
     * 获取插件的类型，以插件的类名来区分插件的类型
     *
     * @param plugin 属性插件
     * @return 插件的类型
     */
    private String getPluginType(SAPropertyPlugin plugin) {
        if (plugin == null) return "";
        return plugin.getClass().getName();
    }

    /**
     * 过滤所有插件，筛选出符合当前事件条件的插件
     *
     * @param eventName 事件名
     * @param eventType 事件类型
     * @param properties 事件已有的属性
     * @return 符合条件的插件列表
     */
    private List<SAPropertyPlugin> filter(String eventName, EventType eventType, JSONObject properties) {
        List<SAPropertyPlugin> filterPlugins = new LinkedList<>();
        // step1.遍历插件
        for (SAPropertyPlugin plugin : plugins.values()) {
            // step2.过滤出匹配当前事件的插件
            if (isMatchEventType(plugin.getEventTypeFilter(), eventType)
                    && isMatchEventName(plugin.getEventNameFilter(), eventName)
                    && isMatchPropertyKey(plugin.getPropertyKeyFilter(), properties)
            ) {
                filterPlugins.add(plugin);
            }
        }
        // step3.将能匹配上当前事件的插件按优先级排序
        Collections.sort(filterPlugins, new Comparator<SAPropertyPlugin>() {
            @Override
            public int compare(SAPropertyPlugin o1, SAPropertyPlugin o2) {
                if (o1.priority().getPriority() >= o2.priority().getPriority()) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });
        // step4.返回已匹配上且已按优先级排序的插件
        return filterPlugins;
    }

    /**
     * 根据事件名匹配插件
     *
     * @param eventNameFilter 事件名匹配规则
     * @param eventName 事件名
     * @return 是否匹配
     */
    private boolean isMatchEventName(Set<String> eventNameFilter, String eventName) {
        if (eventNameFilter == null || eventNameFilter.size() == 0) {
            return true;
        } else {
            return eventNameFilter.contains(eventName);
        }
    }

    /**
     * 根据事件属性名匹配插件
     *
     * @param propertyKeyFilter 属性名匹配规则
     * @param properties 事件已有属性
     * @return 是否匹配
     */
    private boolean isMatchPropertyKey(Set<String> propertyKeyFilter, JSONObject properties) {
        if (propertyKeyFilter == null || propertyKeyFilter.size() == 0) {
            return true;
        } else {
            if (properties == null) {
                return false;
            }
            for (String propertyKey : propertyKeyFilter) {
                if (properties.has(propertyKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 根据事件类型匹配插件
     *
     * @param eventTypeFilter 事件类型匹配规则
     * @param eventType 当前事件类型
     * @return 是否匹配
     */
    private boolean isMatchEventType(Set<EventType> eventTypeFilter, EventType eventType) {
        // 如果用户没有实现 eventTypeFilter 添加支持事件类型，则默认只支持 track 类型事件
        if (eventTypeFilter == null || eventTypeFilter.isEmpty()) {
            if (eventType == EventType.TRACK) {
                return true;
            }
        }
        // 如果匹配到 EventType.ALL，则对所有类型事件都成功匹配
        if (eventTypeFilter.contains(EventType.ALL)) {
            return true;
        }
        return eventTypeFilter.contains(eventType);
    }

    /**
     * 根据属性插件列表，收集所有属性
     *
     * @param plugins 属性插件列表
     * @return 属性对象
     */
    private JSONObject properties(List<SAPropertyPlugin> plugins) {
        JSONObject jsonObject = new JSONObject();
        if (plugins == null) {
            return jsonObject;
        }
        for (SAPropertyPlugin plugin : plugins) {
            Map<String, Object> properties = plugin.properties();
            if (properties == null || properties.isEmpty()) {
                continue;
            }
            JSONObject pluginProperties = new JSONObject(properties);
            try {
                SADataHelper.assertPropertyTypes(pluginProperties);
                SensorsDataUtils.mergeJSONObject(pluginProperties, jsonObject);
            } catch (InvalidDataException e) {
                SALog.printStackTrace(e);
            }
        }
        return jsonObject;
    }

    /**
     * 通过插件类型，获取已经注册过的插件类型的属性
     *
     * @param clazz 指定获取已注册的插件类型
     * @return 已注册类型插件的事件属性 map
     */
    public final Map<String, Object> getPropertiesByPlugin(Class<?> clazz) {
        Map<String, Object> properties = new HashMap<>();
        if (clazz == null) return properties;
        String pluginType = clazz.getName();
        if (plugins.containsKey(pluginType)) {
            SAPropertyPlugin plugin = plugins.get(pluginType);
            if (plugin != null) {
                properties.putAll(plugin.properties());
            }
        }
        return properties;
    }
}
