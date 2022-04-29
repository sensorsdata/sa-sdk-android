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

package com.sensorsdata.analytics.android.advert.plugin;

import com.sensorsdata.analytics.android.sdk.plugin.property.SensorsDataPropertyPluginManager;

public class SAAdvertPluginManager {
    private SAAdvertAppStartPlugin mStartPlugin;
    private SAAdvertAppViewScreenPlugin mViewScreenPlugin;

    public SAAdvertPluginManager() {
        mStartPlugin = new SAAdvertAppStartPlugin();
        mViewScreenPlugin = new SAAdvertAppViewScreenPlugin();
    }

    public void registerPlugin() {
        SensorsDataPropertyPluginManager.getInstance().registerPropertyPlugin(mStartPlugin);
        SensorsDataPropertyPluginManager.getInstance().registerPropertyPlugin(mViewScreenPlugin);
    }

    public void unregisterPlugin() {
        SensorsDataPropertyPluginManager.getInstance().unregisterPropertyPlugin(mStartPlugin);
        SensorsDataPropertyPluginManager.getInstance().unregisterPropertyPlugin(mViewScreenPlugin);
    }
}
