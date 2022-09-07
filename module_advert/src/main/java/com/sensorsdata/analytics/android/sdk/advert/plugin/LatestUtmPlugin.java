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

package com.sensorsdata.analytics.android.sdk.advert.plugin;

import com.sensorsdata.analytics.android.sdk.core.SAModuleManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.ModuleConstants;
import com.sensorsdata.analytics.android.sdk.core.mediator.advert.SAAdvertModuleProtocol;
import com.sensorsdata.analytics.android.sdk.plugin.property.SAPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertiesFetcher;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertyFilter;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;

public class LatestUtmPlugin extends SAPropertyPlugin {

    @Override
    public boolean isMatchedWithFilter(SAPropertyFilter filter) {
        return filter.getType().isTrack()
                && SAModuleManager.getInstance().hasModuleByName(ModuleConstants.ModuleName.ADVERT_NAME)
                && !"$AppEnd".equals(filter.getEvent())
                && !"$AppDeeplinkLaunch".equals(filter.getEvent());
    }

    @Override
    public void properties(SAPropertiesFetcher fetcher) {
        SAAdvertModuleProtocol adProtocol = SAModuleManager.getInstance().getAdvertModuleService();
        if (adProtocol != null) {
            JSONUtils.mergeJSONObject(adProtocol.getLatestUtmProperties(), fetcher.getProperties());
        }
    }
}