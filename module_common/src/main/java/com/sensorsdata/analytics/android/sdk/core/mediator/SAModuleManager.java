/*
 * Created by dengshiwei on 2022/09/09.
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

package com.sensorsdata.analytics.android.sdk.core.mediator;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.protocol.SAModuleProtocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SAModuleManager {
    private static final String TAG = "SA.ModuleManager";
    private final Map<String, SAModuleProtocol> mServiceMap = new HashMap<>();

    private volatile static SAModuleManager mSingleton = null;

    private SAModuleManager() {
    }

    public static SAModuleManager getInstance() {
        if (mSingleton == null) {
            synchronized (SAModuleManager.class) {
                if (mSingleton == null) {
                    mSingleton = new SAModuleManager();
                }
            }
        }
        return mSingleton;
    }

    /**
     * 加载模块
     *
     * @param contextManager SAContextManager
     */
    public void installService(SAContextManager contextManager) {
        try {
            List<String> moduleNames = new ArrayList<String>() {
                {
                    add("com.sensorsdata.analytics.android.autotrack.core.SensorsAutoTrackAPI");  //全埋点模块
                    add("com.sensorsdata.analytics.android.webview.impl.SensorsWebViewAPI");    //webview 模块
                    add("com.sensorsdata.analytics.android.sdk.encrypt.impl.SAEncryptProtocolImpl");     //加密模块
                    add("com.sensorsdata.analytics.android.sdk.push.core.PushProtocolImp");  //推送模块
                    add("com.sensorsdata.analytics.android.sdk.visual.impl.SAVisualProtocolAPI");    //可视化模块
                    add("com.sensorsdata.analytics.android.sdk.exposure.SAExposureProtocolImpl");   //曝光模块
                    add("com.sensorsdata.analytics.android.sdk.advert.impl.SensorsAdvertProtocolAPI");  //广告模块
                }
            };
            List<SAModuleProtocol> protocolList = loadModule(moduleNames);
            Collections.sort(protocolList, new Comparator<SAModuleProtocol>() {
                @Override
                public int compare(SAModuleProtocol o1, SAModuleProtocol o2) {
                    return o2.getPriority() - o1.getPriority();
                }
            });
            for (SAModuleProtocol saModuleProtocol : protocolList) {
                try {
                    saModuleProtocol.install(contextManager);
                    mServiceMap.put(saModuleProtocol.getModuleName(), saModuleProtocol);
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private List<SAModuleProtocol> loadModule(List<String> moduleNames) {
        List<SAModuleProtocol> protocolList = new ArrayList<>();
        for (String moduleName : moduleNames) {
            try {
                Class<?> cls = Class.forName(moduleName);
                protocolList.add((SAModuleProtocol) cls.newInstance());
                SALog.i(TAG, "loadModule name: " + moduleName);
            } catch (Throwable e) {
                SALog.i(TAG, "loadModule name: " + moduleName + " error \n" + e);
            }
        }
        return protocolList;
    }

    /**
     * 根据模块名开启/关闭模块
     *
     * @param moduleName 模块名称
     * @param enable 设置模块状态
     */
    public void setModuleStateByName(String moduleName, boolean enable) {
        if (mServiceMap.containsKey(moduleName)) {
            SAModuleProtocol protocol = mServiceMap.get(moduleName);
            if (protocol != null && enable != protocol.isEnable()) {
                protocol.setModuleState(enable);
            }
        }
    }

    /**
     * 查询模块是否存在
     *
     * @param moduleName 模块名称
     * @return 模块是否存在
     */
    public boolean hasModuleByName(String moduleName) {
        if (TextUtils.isEmpty(moduleName)) {
            return false;
        }
        if (mServiceMap.containsKey(moduleName)) {
            SAModuleProtocol saModuleProtocol = mServiceMap.get(moduleName);
            if (saModuleProtocol != null) {
                return saModuleProtocol.isEnable();
            }
        }
        return false;
    }

    /**
     * 设置模块状态
     *
     * @param enable 是否可用
     */
    public void setModuleState(boolean enable) {
        for (SAModuleProtocol moduleProtocol : mServiceMap.values()) {
            moduleProtocol.setModuleState(enable);
        }
    }

    public <T> T invokeModuleFunction(String moduleName, String methodName, Object... argv) {
        try {
            SAModuleProtocol saModuleProtocol = getService(moduleName);
            if (saModuleProtocol != null) {
                return saModuleProtocol.invokeModuleFunction(methodName, argv);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    /**
     * 获取对应服务
     *
     * @param moduleName 模块名称
     * @return 模块实现接口
     */
    private SAModuleProtocol getService(String moduleName) {
        try {
            if (!SAModuleManager.getInstance().hasModuleByName(moduleName)) {
                return null;
            }
            return mServiceMap.get(moduleName);
        } catch (Exception exception) {
            SALog.printStackTrace(exception);
        }
        return null;
    }
}