/*
 * Created by dengshiwei on 2022/08/24.
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

package com.sensorsdata.analytics.android.autotrack.core.plugins;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.plugin.property.SAPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertiesFetcher;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertyFilter;

import org.json.JSONObject;

import java.util.Date;

public class AutoTrackEventPlugin extends SAPropertyPlugin {
    private String mEventName;

    @Override
    public boolean isMatchedWithFilter(SAPropertyFilter filter) {
        mEventName = filter.getEvent();
        return "$AppStart".equals(filter.getEvent()) || "$AppEnd".equals(filter.getEvent());
    }

    @Override
    public void properties(SAPropertiesFetcher fetcher) {
        try {
            JSONObject properties = fetcher.getProperties();
            JSONObject libProperties = fetcher.getEventJson(SAPropertyFilter.LIB);
            long eventTime = System.currentTimeMillis();
            //  handle $AppStart、$AppEnd
            if ("$AppEnd".equals(mEventName)) {
                long appEndTime = properties.optLong("event_time");
                // 退出时间戳不合法不使用，2000 为打点间隔时间戳
                if (appEndTime > 2000) {
                    eventTime = appEndTime;
                }
                String appEnd_lib_version = properties.optString("$lib_version");
                if (!TextUtils.isEmpty(appEnd_lib_version)) {
                    libProperties.put("$lib_version", appEnd_lib_version);
                } else {
                    properties.remove("$lib_version");
                }

                String appEnd_app_version = properties.optString("$app_version");
                if (TextUtils.isEmpty(appEnd_app_version)) {
                    properties.remove("$app_version");
                } else {
                    libProperties.put("$app_version", appEnd_app_version);
                }

                properties.remove("event_time");
            } else if ("$AppStart".equals(mEventName)) {
                long appStartTime = properties.optLong("event_time");
                if (appStartTime > 0) {
                    eventTime = appStartTime;
                }
                properties.remove("event_time");
            }
            properties.put("$time", new Date(eventTime));
            fetcher.setProperties(properties);
            fetcher.setEventJson(SAPropertyFilter.LIB, libProperties);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
