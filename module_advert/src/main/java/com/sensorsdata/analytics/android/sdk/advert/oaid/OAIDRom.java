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

package com.sensorsdata.analytics.android.sdk.advert.oaid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;

import java.lang.reflect.Method;

@SuppressWarnings("All")
public final class OAIDRom {
    private static final String TAG = "SA.OAIDRom";

    private OAIDRom() {
        super();
    }

    public static String sysProperty(String key, String defValue) {
        String res = null;
        try {
            @SuppressLint("PrivateApi") Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getMethod("get", new Class<?>[]{String.class, String.class});
            res = (String) method.invoke(clazz, new Object[]{key, defValue});
        } catch (Throwable th) {
            SALog.i(TAG, th);
        }
        if (res == null) {
            res = "";
        }
        return res;
    }

    // 华为、荣耀
    public static boolean isHuawei() {
        return Build.MANUFACTURER.equalsIgnoreCase("HUAWEI") ||
                Build.BRAND.equalsIgnoreCase("HUAWEI") ||
                Build.BRAND.equalsIgnoreCase("HONOR");
    }

    // 华为
    public static boolean isEmui() {
        return !TextUtils.isEmpty(sysProperty("ro.build.version.emui", ""));
    }

    // 欧珀、真我
    public static boolean isOppo() {
        return Build.MANUFACTURER.equalsIgnoreCase("OPPO") ||
                Build.BRAND.equalsIgnoreCase("OPPO") ||
                Build.BRAND.equalsIgnoreCase("REALME") ||
                !TextUtils.isEmpty(sysProperty("ro.build.version.opporom", ""));
    }

    // 维沃、爱酷
    public static boolean isVivo() {
        return Build.MANUFACTURER.equalsIgnoreCase("VIVO") ||
                Build.BRAND.equalsIgnoreCase("VIVO") ||
                !TextUtils.isEmpty(sysProperty("ro.vivo.os.version", ""));
    }

    // 小米、红米
    public static boolean isXiaomi() {
        return Build.MANUFACTURER.equalsIgnoreCase("XIAOMI") ||
                Build.BRAND.equalsIgnoreCase("XIAOMI") ||
                Build.BRAND.equalsIgnoreCase("REDMI");
    }
    // 小米
    public static boolean isMiui() {
        return !TextUtils.isEmpty(sysProperty("ro.miui.ui.version.name", ""));
    }

    // 黑鲨手机
    public static boolean isBlackShark() {
        return Build.MANUFACTURER.equalsIgnoreCase("BLACKSHARK") ||
                Build.BRAND.equalsIgnoreCase("BLACKSHARK");
    }

    // 一加手机
    public static boolean isOnePlus() {
        return Build.MANUFACTURER.equalsIgnoreCase("ONEPLUS") ||
                Build.BRAND.equalsIgnoreCase("ONEPLUS");
    }

    // 三星手机
    public static boolean isSamsung() {
        return Build.MANUFACTURER.equalsIgnoreCase("SAMSUNG") ||
                Build.BRAND.equalsIgnoreCase("SAMSUNG");
    }

    // 魅族
    public static boolean isMeizu() {
        return Build.MANUFACTURER.equalsIgnoreCase("MEIZU") ||
                Build.BRAND.equalsIgnoreCase("MEIZU") ||
                Build.DISPLAY.toUpperCase().contains("FLYME");
    }

    // 联想、乐檬
    public static boolean isLenovo() {
        return Build.MANUFACTURER.equalsIgnoreCase("LENOVO") ||
                Build.BRAND.equalsIgnoreCase("LENOVO") ||
                Build.BRAND.equalsIgnoreCase("ZUK");
    }

    // 努比亚
    public static boolean isNubia() {
        return Build.MANUFACTURER.equalsIgnoreCase("NUBIA") ||
                Build.BRAND.equalsIgnoreCase("NUBIA");
    }

    // 华硕
    public static boolean isASUS() {
        return Build.MANUFACTURER.equalsIgnoreCase("ASUS") ||
                Build.BRAND.equalsIgnoreCase("ASUS");
    }

    // 中兴
    public static boolean isZTE() {
        return Build.MANUFACTURER.equalsIgnoreCase("ZTE") ||
                Build.BRAND.equalsIgnoreCase("ZTE");
    }

    // 摩托罗拉手机
    public static boolean isMotolora() {
        return Build.MANUFACTURER.equalsIgnoreCase("MOTOLORA") ||
                Build.BRAND.equalsIgnoreCase("MOTOLORA");
    }

    // 酷派手机
    public static boolean isCoolpad(Context context) {
        try {
            context.getPackageManager().getPackageInfo("com.coolpad.deviceidsupport", 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
