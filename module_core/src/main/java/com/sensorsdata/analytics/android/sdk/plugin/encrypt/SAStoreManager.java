/*
 * Created by yuejianzhong on 2021/12/14.
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

package com.sensorsdata.analytics.android.sdk.plugin.encrypt;

import android.content.Context;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class SAStoreManager extends AbstractStoreManager {

    public static final String SECRET_KEY = "sa_sp_encrypt_secret_key";
    private static final String TAG = "SA.SAStoreManager";
    private static final String SP_SENSORS_DATA_API = "com.sensorsdata.analytics.android.sdk.SensorsDataAPI";
    private static final String SP_SENSORS_DATA = "sensorsdata";
    private static final String SP_SENSORS_DATA_EXIT = "sensorsdata.exit";
    private final ArrayList<String> mAPIStoreKeys = new ArrayList<>();

    private SAStoreManager() {
        super();
    }

    public static SAStoreManager getInstance() {
        return SAStoreManager.SingletonHolder.INSTANCE;
    }

    /**
     * 注册插件
     * @param configPlugins 自定义插件
     * @param context Context
     */
    public void registerPlugins(List<StorePlugin> configPlugins, Context context) {
        if (configPlugins == null || configPlugins.isEmpty()) {// 注册默认的 Plugin
            mDefaultState = true;
            registerExitPlugin(context);
            registerAPIPlugin(context);
            /* 此 plugin storeKeys 返回值是 null，默认是使用最后注册的 plugin 保存，所以一定要在最后注册 */
            registerSensorsDataPlugin(context);
        } else {
            mDefaultState = false;
            if (isRegisterPlugin(context, SP_SENSORS_DATA_EXIT)) {
                registerExitPlugin(context);
            }
            if (isRegisterPlugin(context, SP_SENSORS_DATA_API)) {
                registerAPIPlugin(context);
            }
            /* 此 plugin storeKeys 返回值是 null，默认是使用最后注册的 plugin 保存，所以一定要在最后注册 */
            if (isRegisterPlugin(context, SP_SENSORS_DATA)) {
                registerSensorsDataPlugin(context);
            }
            // 添加自定义的 plugin
            for (StorePlugin plugin : configPlugins) {
                registerPlugin(plugin);
            }
        }
    }

    private static class SingletonHolder {
        private static final SAStoreManager INSTANCE = new SAStoreManager();
    }

    /**
     * 注册名为 "sensorsdata.exit" SP 的插件
     * @param context Context
     */
    private void registerExitPlugin(Context context) {
        final List<String> exitKeys = new ArrayList<>();
        exitKeys.add(DbParams.APP_EXIT_DATA);
        registerPlugin(new DefaultStorePlugin(context, SP_SENSORS_DATA_EXIT) {
            @Override
            public List<String> storeKeys() {
                return exitKeys;
            }
        });
    }

    /**
     * 注册名为 "com.sensorsdata.analytics.android.sdk.SensorsDataAPI" SP 的插件
     * @param context Context
     */
    private void registerAPIPlugin(Context context) {
        registerPlugin(new DefaultStorePlugin(context, SP_SENSORS_DATA_API) {
            @Override
            public List<String> storeKeys() {
                if (mAPIStoreKeys.isEmpty()) {
                    initAPIKeys();
                }
                return mAPIStoreKeys;
            }
        });
    }

    /**
     * 注册名为 "sensorsdata" SP 的 插件
     * @param context Context
     */
    private void registerSensorsDataPlugin(Context context) {
        registerPlugin(new DefaultStorePlugin(context, SP_SENSORS_DATA) {
            @Override
            public List<String> storeKeys() {
                return null;
            }
        });
    }

    /**
     * 动态构建，防止后续维护遗忘
     */
    private void initAPIKeys() {
        try {
            Field[] fields = DbParams.PersistentName.class.getDeclaredFields();
            for (Field field : fields) {
                mAPIStoreKeys.add((String) field.get(null));
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}