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
 * 属性插件需要实现的抽象类，用于自定义属性插件
 */
public abstract class SAPropertyPlugin implements ISAPropertyPlugin {

    @Override
    public  boolean isMatchedWithFilter(SAPropertyFilter filter){
        return filter.getType().isTrack();
    }

    @Override
    public abstract void properties(SAPropertiesFetcher fetcher);

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    public SAPropertyPluginPriority priority() {
        return SAPropertyPluginPriority.DEFAULT;
    }
}
