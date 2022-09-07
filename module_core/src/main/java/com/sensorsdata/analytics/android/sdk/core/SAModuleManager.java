/*
 * Created by chenru on 2022/3/11 下午6:43.
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

package com.sensorsdata.analytics.android.sdk.core;

import android.app.Activity;
import android.net.Uri;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.core.mediator.ModuleConstants;
import com.sensorsdata.analytics.android.sdk.core.mediator.advert.SAAdvertModuleProtocol;
import com.sensorsdata.analytics.android.sdk.core.mediator.autotrack.AutoTrackModuleProtocol;
import com.sensorsdata.analytics.android.sdk.core.mediator.encrypt.SAEncryptProtocol;
import com.sensorsdata.analytics.android.sdk.core.mediator.protocol.SAModuleProtocol;
import com.sensorsdata.analytics.android.sdk.core.mediator.protocol.SAScanListener;
import com.sensorsdata.analytics.android.sdk.core.mediator.visual.SAVisualProtocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public class SAModuleManager {

    private final Map<String, SAModuleProtocol> mServiceMap = new HashMap<>();

    private volatile static SAModuleManager mSingleton = null;
    private AutoTrackModuleProtocol mAutoTrackModuleProtocol;
    private SAEncryptProtocol mEncryptProtocol;

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
            ServiceLoader<SAModuleProtocol> serviceLoader = ServiceLoader.load(SAModuleProtocol.class);
            List<SAModuleProtocol> protocolList = new ArrayList<>();
            for (SAModuleProtocol saModuleProtocol : serviceLoader) {
                if (saModuleProtocol != null) {
                    protocolList.add(saModuleProtocol);
                }
            }
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

    /**
     * 根据模块名开启/关闭模块
     *
     * @param moduleName 模块名称 {@link ModuleConstants.ModuleName}
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
     * @param moduleName 模块名称 {@link ModuleConstants.ModuleName}
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

    /**
     * 获取对应服务
     *
     * @param moduleName 模块名称
     * @param clazz 模块 class
     * @return 模块实现接口
     */
    public <T> T getService(String moduleName, Class<T> clazz) {
        try {
            SAModuleProtocol protocol = mServiceMap.get(moduleName);
            if (protocol != null) {
                return clazz.cast(protocol);
            }

        } catch (Exception ignored) {

        }
        return null;
    }

    /**
     * 获取广告模块间交互服务
     *
     * @return 模块实现接口
     */
    public SAAdvertModuleProtocol getAdvertModuleService() {
        return getService(ModuleConstants.ModuleName.ADVERT_NAME, SAAdvertModuleProtocol.class);
    }

    /**
     * 获取可视化模块间交互服务
     *
     * @return 模块实现接口
     */
    public SAVisualProtocol getVisualModuleService() {
        return getService(ModuleConstants.ModuleName.VISUAL_NAME, SAVisualProtocol.class);
    }

    /**
     * invoke auto_track module method
     */
    public <T> T invokeAutoTrackFunction(String methodName, Object... argv) {
        try {
            if (mAutoTrackModuleProtocol == null) {
                if (SAModuleManager.getInstance().hasModuleByName(ModuleConstants.ModuleName.AUTO_TRACK_NAME)) {
                    mAutoTrackModuleProtocol = getService(ModuleConstants.ModuleName.AUTO_TRACK_NAME, AutoTrackModuleProtocol.class);
                }
            }

            if (mAutoTrackModuleProtocol != null) {
                return mAutoTrackModuleProtocol.invokeModuleFunction(methodName, argv);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    /**
     * invoke module method
     */
    public <T> T invokeEncryptModuleFunction(String methodName, Object... argv) {
        try {
            if (mEncryptProtocol == null) {
                mEncryptProtocol = getService(ModuleConstants.ModuleName.ENCRYPT_NAME, SAEncryptProtocol.class);
            }
            if (mEncryptProtocol != null) {
                return mEncryptProtocol.invokeModuleFunction(methodName, argv);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }


    /**
     * 扫码唤起 scheme 通知模块处理
     *
     * @param activity Activity
     * @param uri uri
     * @return 是否已处理
     */
    public boolean handlerScanUri(Activity activity, Uri uri) {
        if (uri == null || TextUtils.isEmpty(uri.getHost()) || mServiceMap.isEmpty()) {
            return false;
        }
        for (SAModuleProtocol protocol : mServiceMap.values()) {
            if (protocol.isEnable() && protocol instanceof SAScanListener) {
                if (((SAScanListener) protocol).handlerScanUri(activity, uri)) {
                    return true;
                }
            }
        }
        return false;
    }
}