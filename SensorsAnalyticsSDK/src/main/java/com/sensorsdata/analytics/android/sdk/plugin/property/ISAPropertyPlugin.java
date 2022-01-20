/*
 * Created by luweibin on 2021/12/16.
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

import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;

import java.util.Map;
import java.util.Set;

interface ISAPropertyPlugin {
    /**
     * 向插件中添加静态属性
     *
     * @param properties 添加静态属性的 map 对象
     */
    void appendProperties(Map<String, Object> properties);

    /**
     * 向插件中添加动态属性
     *
     * @param dynamicProperties 添加动态属性的 map 对象
     */
    void appendDynamicProperties(Map<String, Object> dynamicProperties);

    /**
     * 指定插件允许添加属性的事件名
     *
     * @param eventNameFilter 添加事件名的 Set
     */
    void eventNameFilter(Set<String> eventNameFilter);

    /**
     * 指定插件允许添加属性的事件类型
     *
     * @param eventTypeFilter 添加属性类型的对象
     */
    void eventTypeFilter(Set<EventType> eventTypeFilter);

    /**
     * 指定插件允许添加属性，当事件已有的属性包含 Set 中任意一条属性名的时候，添加该插件的属性
     * 例如：调用 SensorsDataAPI.track(String eventName, JSONObject properties) 接口
     * 如果 properties 属性含有 propertyKeyFilter 中的任意一条属性 Key，就视为插件成功匹配
     *
     * @param propertyKeyFilter 已有属性名的 Set
     */
    void propertyKeyFilter(Set<String> propertyKeyFilter);

    /**
     * 给插件指定属性的优先级
     *
     * @return 属性的优先级枚举对象
     */
    SAPropertyPluginPriority priority();
}
