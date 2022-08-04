/*
 * Created by chenru on 2022/4/25 下午5:05(format year/.
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

package com.sensorsdata.analytics.android.sdk.advert.plugin;

import com.sensorsdata.analytics.android.sdk.advert.deeplink.DeepLinkManager;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.plugin.property.SAPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertiesFetcher;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertyFilter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class SAAdvertAppStartPlugin extends SAPropertyPlugin {

    @Override
    public boolean isMatchedWithFilter(SAPropertyFilter filter) {
        return "$AppStart".equals(filter.getEvent());
    }

    @Override
    public void properties(SAPropertiesFetcher fetcher) {
        JSONObject object = new JSONObject();
        DeepLinkManager.mergeCacheProperties(object);
        if (object.length() > 0) {
            Iterator<String> it = object.keys();
            while (it.hasNext()) {
                String key = it.next();
                try {
                    fetcher.getProperties().put(key, object.opt(key));
                } catch (JSONException e) {
                    SALog.printStackTrace(e);
                }
            }
        }
    }
}
