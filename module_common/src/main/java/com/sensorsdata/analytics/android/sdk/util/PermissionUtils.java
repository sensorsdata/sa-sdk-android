/*
 * Created by chenru on 2023/2/7 上午10:25.
 * Copyright 2015－2023 Sensors Data Inc.
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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import com.sensorsdata.analytics.android.sdk.SALog;

import java.util.HashSet;
import java.util.Set;

public class PermissionUtils {
    private static final String TAG = "SA.PermissionUtils";
    //校验一次已授权权限
    private static final Set<String> mPermissionGrantedSet = new HashSet<>();
    //每次运行只校验一次权限
    private static final Set<String> mPermissionGrantedOnceSet = new HashSet<String>() {
        {
            add(Manifest.permission.ACCESS_WIFI_STATE);
            add(Manifest.permission.ACCESS_NETWORK_STATE);
        }
    };

    /**
     * 判断某个权限是否授予了
     */
    public static boolean checkSelfPermission(Context context, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 只需检测一次的权限已授权则直接返回 true
            if (mPermissionGrantedSet.contains(permission)) {
                return true;
            }
            boolean isGranted = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
            // 只需检测一次的权限授权后添加到已授权 Set
            if (isGranted && mPermissionGrantedOnceSet.contains(permission)) {
                mPermissionGrantedSet.add(permission);
            }
            return isGranted;
        } else {
            try {
                boolean isGranted = context.getPackageManager().checkPermission(permission, context.getPackageName()) == PackageManager.PERMISSION_GRANTED;
                if (isGranted) {
                    //6.0 以下只需要判断一次授权
                    mPermissionGrantedSet.add(permission);
                }
                return isGranted;
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
        return false;
    }

    public static boolean hasReadPhoneStatePermission(Context context) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            if (!checkSelfPermission(context, Manifest.permission.READ_PRECISE_PHONE_STATE)) {
                SALog.i(TAG, "Don't have permission android.permission.READ_PRECISE_PHONE_STATE,getDeviceID failed");
                return false;
            }
        } else if (!checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)) {
            SALog.i(TAG, "Don't have permission android.permission.READ_PHONE_STATE,getDeviceID failed");
            return false;
        }
        return true;
    }
}
