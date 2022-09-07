/*
 * Created by luweibin on 2021/12/17.
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

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertiesFetcher;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertyFilter;
import com.sensorsdata.analytics.android.sdk.plugin.property.impl.InternalCustomPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.plugin.property.impl.RealTimePropertyPlugin;
import com.sensorsdata.analytics.android.sdk.plugin.property.impl.ReferrerTitlePlugin;
import com.sensorsdata.analytics.android.sdk.plugin.property.impl.SAPresetPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.plugin.property.impl.SASuperPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 属性插件的管理类，用于管理插件的添加和移除，以及事件属性的获取
 */
public final class PropertyPluginManager {
    private static final String TAG = "SA.SAPropertyPluginManager";
    private final Map<String, SAPropertyPlugin> mPluginMap;
    private List<SAPropertyPlugin> mPluginsList = new ArrayList<>();

    public PropertyPluginManager(SensorsDataAPI sensorsDataAPI, SAContextManager contextManager) {
        mPluginMap = Collections.synchronizedMap(new LinkedHashMap<String, SAPropertyPlugin>());
        registerDefaultProperty(sensorsDataAPI, contextManager);
    }

    /**
     * 注册属性插件
     *
     * @param plugin 注册属性插件对象
     */
    public synchronized void registerPropertyPlugin(SAPropertyPlugin plugin) {
        try {
            if (plugin == null) return;
            String propertyType = plugin.getName();
            if (!mPluginMap.containsKey(propertyType)) {
                mPluginMap.put(propertyType, plugin);
                sortedPlugin();
            } else {
                SALog.i(TAG, String.format("plugin [ %s ] has exist!", propertyType));
            }
        } catch (Exception e) {
            SALog.i(TAG, "register property plugin exception! ", e);
        }
    }

    /**
     * 注销插件
     *
     * @param plugin plugin
     */
    public synchronized void unregisterPropertyPlugin(SAPropertyPlugin plugin) {
        if (plugin == null) {
            return;
        }
        String propertyType = plugin.getName();
        mPluginMap.remove(propertyType);
        sortedPlugin();
    }

    /**
     * 根据属性插件，添加属性
     *
     * @param filter 过滤属性插件
     * @return SAPropertiesFetcher
     */
    public synchronized SAPropertiesFetcher propertiesHandler(SAPropertyFilter filter) {
        try {
            if (mPluginsList.size() == 0) {
                return null;
            }
            SAPropertiesFetcher saPropertiesFetcher = new SAPropertiesFetcher();
            saPropertiesFetcher.setProperties(filter.getProperties());
            saPropertiesFetcher.setEventJson(SAPropertyFilter.LIB, filter.getEventJson(SAPropertyFilter.LIB));
            for (SAPropertyPlugin plugin : mPluginsList) {
                JSONObject filterJson = new JSONObject();
                JSONUtils.mergeJSONObject(saPropertiesFetcher.getProperties(), filterJson);
                filter.setProperties(filterJson);
                //step1.匹配插件
                if (plugin.isMatchedWithFilter(filter)) {
                    //step2.处理插件属性
                    plugin.properties(saPropertiesFetcher);
                }
            }
            SALog.i(TAG, "SAPropertiesFetcher: %s", saPropertiesFetcher);
            return saPropertiesFetcher;
        } catch (Exception e) {
            SALog.i(TAG, String.format("Event [%s] error is happened when matching property-plugins", filter.getEvent()), e);
        }
        return null;
    }

    /**
     * get SAPropertyPlugin
     *
     * @param pluginName PluginName
     * @return SAPropertyPlugin
     */
    public SAPropertyPlugin getPropertyPlugin(String pluginName) {
        return mPluginMap.get(pluginName);
    }

    private void registerDefaultProperty(SensorsDataAPI sensorsDataAPI, SAContextManager contextManager) {
        //插件顺序：预置属性插件->公共属性插件->实时获取属性插件->其它模块属性插件->自定义属性插件->设备 ID 属性插件
        registerPropertyPlugin(new SAPresetPropertyPlugin(contextManager));
        registerPropertyPlugin(new SASuperPropertyPlugin(sensorsDataAPI));
        registerPropertyPlugin(new ReferrerTitlePlugin());
        registerPropertyPlugin(new RealTimePropertyPlugin(contextManager));
        //自定义插件
        List<SAPropertyPlugin> plugins = contextManager.getInternalConfigs().saConfigOptions.getPropertyPlugins();
        if (plugins != null) {
            for (SAPropertyPlugin plugin : plugins) {
                registerPropertyPlugin(plugin);
            }
        }
        //事件自定义属性插件
        registerPropertyPlugin(new InternalCustomPropertyPlugin());
    }

    private void sortedPlugin() {
        mPluginsList = new ArrayList<>(mPluginMap.values());
        Collections.sort(mPluginsList, new Comparator<SAPropertyPlugin>() {
            @Override
            public int compare(SAPropertyPlugin o1, SAPropertyPlugin o2) {
                if (o1.priority().getPriority() >= o2.priority().getPriority()) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });
    }
}
