/*
 * Created by luweibin on 2021/10/21.
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

package com.sensorsdata.analytics.android.sdk.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.sensorsdata.analytics.android.sdk.SALog;

public class SASpUtils {
    private static final String TAG = "SA.SASpUtils";

    public interface ISharedPreferencesProvider {
        SharedPreferences createSharedPreferences(Context context, String name, int mode);
    }

    private static ISharedPreferencesProvider mSharedPreferencesProvider;

    /**
     * 自定义 SharedPreferences 的创建，由于在 ContentProvider 中会用到 SharedPreferences，应用需要尽早设置
     * 在 Application 的 attachBaseContext 方法中设置才能生效
     *
     * @param sharedPreferencesProvider 自定义的 SharedPreferences 提供者
     */
    public static void setSharedPreferencesProvider(ISharedPreferencesProvider sharedPreferencesProvider) {
        mSharedPreferencesProvider = sharedPreferencesProvider;
    }

    public static SharedPreferences getSharedPreferences(Context context, String name, int mode) {
        if (mSharedPreferencesProvider != null) {
            SharedPreferences userDefault = mSharedPreferencesProvider.createSharedPreferences(context, name, mode);
            if (userDefault != null) {
                SALog.d(TAG, "create SharedPreferences by user default, file name is: " + name);
                return userDefault;
            }
        }
        return context.getSharedPreferences(name, mode);
    }

}
