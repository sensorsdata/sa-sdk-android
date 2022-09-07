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

package com.sensorsdata.analytics.android.sdk.plugin.property.impl;

import com.sensorsdata.analytics.android.sdk.plugin.property.SAPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertiesFetcher;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertyFilter;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;

import org.json.JSONObject;

/**
 * 自定义属性的属性插件
 */
public class InternalCustomPropertyPlugin extends SAPropertyPlugin {

    private JSONObject mCustomProperties;

    @Override
    public boolean isMatchedWithFilter(SAPropertyFilter filter) {
        return "Android".equals(filter.getEventJson(SAPropertyFilter.LIB).optString("$lib"));
    }

    public InternalCustomPropertyPlugin() {
        mCustomProperties = new JSONObject();
    }

    @Override
    public void properties(SAPropertiesFetcher fetcher) {
        if (mCustomProperties != null) {
            JSONUtils.mergeJSONObject(mCustomProperties, fetcher.getProperties());
            mCustomProperties = null;
        }
    }

    /**
     * 设置自定义属性
     *
     * @param properties 自定义属性
     */
    public void saveCustom(JSONObject properties) {
        mCustomProperties = properties;
        if (properties != null) {
            mCustomProperties.remove("$device_id");
            mCustomProperties.remove("$anonymization_id");
        }
    }
}
