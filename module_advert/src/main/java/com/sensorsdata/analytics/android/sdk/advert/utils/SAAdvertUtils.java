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

package com.sensorsdata.analytics.android.sdk.advert.utils;

import android.content.Context;

import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;
import com.sensorsdata.analytics.android.sdk.plugin.encrypt.SAStoreManager;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

public class SAAdvertUtils {
    /**
     * 获取是否激活标记位
     *
     * @param disableCallback 是否回传事件 callback
     * @return 是否已触发过激活事件
     */
    public static boolean isFirstTrackInstallation(boolean disableCallback) {
        if (disableCallback) {
            return !SAStoreManager.getInstance().isExists(DbParams.PersistentName.FIRST_INSTALL_CALLBACK);
        }
        return !SAStoreManager.getInstance().isExists(DbParams.PersistentName.FIRST_INSTALL);
    }

    /**
     * 设置激活标记位
     *
     * @param disableCallback 是否回传事件 callback
     */
    public static void setTrackInstallation(boolean disableCallback) {
        if (disableCallback) {
            SAStoreManager.getInstance().setString(DbParams.PersistentName.FIRST_INSTALL_CALLBACK, "false");
        }
        SAStoreManager.getInstance().setString(DbParams.PersistentName.FIRST_INSTALL, "false");
    }

    /**
     * 获取 AndroidID
     *
     * @return AndroidID
     */
    public static String getAndroidId(Context context) {
        return SensorsDataUtils.getAndroidID(context);
    }

    /**
     * 获取 IMEI
     *
     * @return IMEI 拼接数据
     */
    public static String getIMEI(Context context) {
        return String.format("imei=%s##imei_old=%s##imei_slot1=%s##imei_slot2=%s##imei_meid=%s",
                SensorsDataUtils.getIMEI(context),
                SensorsDataUtils.getIMEIOld(context),
                SensorsDataUtils.getSlot(context, 0),
                SensorsDataUtils.getSlot(context, 1),
                SensorsDataUtils.getMEID(context));
    }
}
