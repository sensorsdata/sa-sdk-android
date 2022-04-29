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
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.core.mediator.ModuleConstants;
import com.sensorsdata.analytics.android.sdk.core.mediator.advert.SAAdvertModuleProtocol;
import com.sensorsdata.analytics.android.sdk.core.mediator.protocol.SAModuleProtocol;
import com.sensorsdata.analytics.android.sdk.core.mediator.protocol.SAScanListener;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class SAModuleManager {

    private Map<String, SAModuleProtocol> mServiceMap = new HashMap<>();

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
     * @param context Context
     * @param options SAConfigOptions
     */
    public void installService(Context context, SAConfigOptions options) {
        ServiceLoader<SAModuleProtocol> serviceLoader = ServiceLoader.load(SAModuleProtocol.class);
        for (SAModuleProtocol saModuleProtocol : serviceLoader) {
            if (saModuleProtocol != null) {
                saModuleProtocol.install(context, options);
                mServiceMap.put(saModuleProtocol.getModuleName(), saModuleProtocol);
            }
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
        if(mServiceMap.containsKey(moduleName)){
            return mServiceMap.get(moduleName).isEnable();
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