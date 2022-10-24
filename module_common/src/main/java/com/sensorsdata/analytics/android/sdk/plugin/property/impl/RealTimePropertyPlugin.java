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

import android.content.Context;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataGPSLocation;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.plugin.property.SAPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertiesFetcher;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertyFilter;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;

/**
 * 需要实时获取的属性插件
 */
public class RealTimePropertyPlugin extends SAPropertyPlugin {
    SAContextManager mContextManager;
    Context mContext;

    public RealTimePropertyPlugin(SAContextManager saContextManager) {
        mContext = saContextManager.getContext();
        mContextManager = saContextManager;
    }

    @Override
    public boolean isMatchedWithFilter(SAPropertyFilter filter) {
        return filter.getType().isTrack() && "Android".equals(filter.getEventJson(SAPropertyFilter.LIB).optString("$lib"));
    }

    @Override
    public void properties(SAPropertiesFetcher fetcher) {
        try {
            // 当前网络状况
            String networkType = NetworkUtils.networkType(mContext);
            fetcher.getProperties().put("$wifi", "WIFI".equals(networkType));
            fetcher.getProperties().put("$network_type", networkType);
            SensorsDataGPSLocation location = mContextManager.getInternalConfigs().gpsLocation;
            if (location != null) {
                location.toJSON(fetcher.getProperties());
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }

        // 屏幕方向
        try {
            String screenOrientation = SensorsDataAPI.sharedInstance().getScreenOrientation();
            if (!TextUtils.isEmpty(screenOrientation)) {
                fetcher.getProperties().put("$screen_orientation", screenOrientation);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
