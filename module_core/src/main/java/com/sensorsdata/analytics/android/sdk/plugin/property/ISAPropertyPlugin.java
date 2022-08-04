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

import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertiesFetcher;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertyFilter;

/**
 * 属性插件
 */
public interface ISAPropertyPlugin {
    /**
     * 是否匹配属性插件
     * @param filter 过滤条件
     * @return 是否符合过滤条件
     */
    boolean isMatchedWithFilter(SAPropertyFilter filter);

    /**
     * 处理属性，可对属性进行增加、删除、修改操作
     * @param fetcher 接收属性
     */
    void properties(SAPropertiesFetcher fetcher);

    /**
     * 给插件指定属性的优先级
     *
     * @return 属性的优先级枚举对象
     */
    SAPropertyPluginPriority priority();

    /**
     * 属性插件名称，根据返回值去重
     * @return 名称
     */
    String getName();
}
