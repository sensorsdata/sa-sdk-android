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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 属性插件需要实现的抽象类，用于自定义属性插件
 */
public abstract class SAPropertyPlugin implements ISAPropertyPlugin {

    private final Map<String, Object> mProperties = new HashMap<>();

    private final Map<String, Object> mDynamicProperties = new HashMap<>();

    private final Set<String> mEventNameFilter = new HashSet<>();

    private final Set<String> mPropertyKeyFilter = new HashSet<>();

    private final Set<EventType> mEventTypeFilter = new HashSet<>();

    final Set<String> getEventNameFilter() {
        mEventNameFilter.clear();
        eventNameFilter(mEventNameFilter);
        return mEventNameFilter;
    }

    final Set<String> getPropertyKeyFilter() {
        mPropertyKeyFilter.clear();
        propertyKeyFilter(mPropertyKeyFilter);
        return mPropertyKeyFilter;
    }

    final Set<EventType> getEventTypeFilter() {
        mEventTypeFilter.clear();
        eventTypeFilter(mEventTypeFilter);
        return mEventTypeFilter;
    }

    final Map<String, Object> properties() {
        // step1.更新动态属性
        mDynamicProperties.clear();
        appendDynamicProperties(mDynamicProperties);
        // step2.是否有动态属性
        if (mDynamicProperties.isEmpty()) {
            // step3.动态属性为空且静态属性为空的情况，再次尝试获取静态属性，目的是为了解决合规未通过前首次无法读取属性的问题
            if (mProperties.isEmpty()) {
                appendProperties(mProperties);
            }
            return mProperties;
        } else {
            // step3.动态属性不为空，合并动态属性和静态属性，优先级：动态属性 > 静态属性
            Map<String, Object> mergedProperties = new HashMap<>(mDynamicProperties.size() + mProperties.size());
            mergedProperties.putAll(this.mProperties);
            mergedProperties.putAll(mDynamicProperties);
            return mergedProperties;
        }
    }

    final void start() {
        appendProperties(mProperties);
    }

    @Override
    public abstract void appendProperties(Map<String, Object> properties);

    @Override
    public void appendDynamicProperties(Map<String, Object> dynamicProperties) {

    }

    @Override
    public void eventNameFilter(Set<String> eventNameFilter) {

    }

    @Override
    public void eventTypeFilter(Set<EventType> eventTypeFilter) {

    }

    @Override
    public void propertyKeyFilter(Set<String> propertyKeyFilter) {

    }

    @Override
    public SAPropertyPluginPriority priority() {
        return SAPropertyPluginPriority.DEFAULT;
    }
}
