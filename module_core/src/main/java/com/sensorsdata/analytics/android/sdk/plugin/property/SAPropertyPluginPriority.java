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

/**
 * 属性插件的优先级枚举类
 */
public class SAPropertyPluginPriority {
    public static SAPropertyPluginPriority LOW = new SAPropertyPluginPriority(250);
    public static SAPropertyPluginPriority DEFAULT = new SAPropertyPluginPriority(500);
    public static SAPropertyPluginPriority HIGH = new SAPropertyPluginPriority(750);

    /**
     * 保留优先级，用于神策定义最高优先级的属性插件，该优先级不在用户的优先级范围之内，优先级 priority 为神策成立时间
     */
    protected static SAPropertyPluginPriority SUPER = new SAPropertyPluginPriority(1431656640);

    private final long priority;

    public SAPropertyPluginPriority(long priority) {
        this.priority = priority;
    }

    /**
     * 获取优先级
     *
     * @return 优先级
     */
    public long getPriority() {
        return priority;
    }
}
