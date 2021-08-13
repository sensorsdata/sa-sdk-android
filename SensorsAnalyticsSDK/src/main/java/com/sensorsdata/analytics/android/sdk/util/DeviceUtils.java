/*
 * Created by dengshiwei on 2020/05/12.
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
import android.graphics.Point;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.sensorsdata.analytics.android.sdk.SALog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

public class DeviceUtils {

    public static String getOS() {
        return Build.VERSION.RELEASE == null ? "UNKNOWN" : Build.VERSION.RELEASE;
    }

    public static String getManufacturer() {
        try {
            String manufacturer = Build.MANUFACTURER;
            if (manufacturer != null) {
                return manufacturer.trim().toUpperCase();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return "UNKNOWN";
    }

    public static String getBrand() {
        try {
            String brand = Build.BRAND;
            if (brand != null) {
                return brand.trim().toUpperCase();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return "UNKNOWN";
    }

    public static String getModel() {
        return TextUtils.isEmpty(Build.MODEL) ? "UNKNOWN" : Build.MODEL.trim();
    }

    /**
     * 获取屏幕的宽高信息
     *
     * @param context Context
     * @return 宽高信息
     */
    public static int[] getDeviceSize(Context context) {
        int[] size = new int[2];
        try {
            int screenWidth, screenHeight;
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            int rotation = display.getRotation();
            Point point = new Point();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                display.getRealSize(point);
                screenWidth = point.x;
                screenHeight = point.y;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                display.getSize(point);
                screenWidth = point.x;
                screenHeight = point.y;
            } else {
                screenWidth = display.getWidth();
                screenHeight = display.getHeight();
            }
            size[0] = getNaturalWidth(rotation, screenWidth, screenHeight);
            size[1] = getNaturalHeight(rotation, screenWidth, screenHeight);
        } catch (Exception e) {
            //context.getResources().getDisplayMetrics()这种方式获取屏幕高度不包括底部虚拟导航栏
            if (context.getResources() != null) {
                final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
                size[0] = displayMetrics.widthPixels;
                size[1] = displayMetrics.heightPixels;
            }
        }
        return size;
    }


    /**
     * 根据设备 rotation，判断屏幕方向，获取自然方向宽
     *
     * @param rotation 设备方向
     * @param width 逻辑宽
     * @param height 逻辑高
     * @return 自然尺寸
     */
    private static int getNaturalWidth(int rotation, int width, int height) {
        return rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180 ?
                width : height;
    }

    /**
     * 根据设备 rotation，判断屏幕方向，获取自然方向高
     *
     * @param rotation 设备方向
     * @param width 逻辑宽
     * @param height 逻辑高
     * @return 自然尺寸
     */
    private static int getNaturalHeight(int rotation, int width, int height) {
        return rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180 ?
                height : width;
    }

    /**
     * 获取鸿蒙系统 Version
     *
     * @return HarmonyOS Version
     */
    public static String getHarmonyOSVersion() {
        String version = null;
        if (isHarmonyOs()) {
            version = getProp("hw_sc.build.platform.version", "");
            if(TextUtils.isEmpty(version)){
                version = exec(SensorsDataUtils.COMMAND_HARMONYOS_VERSION);
            }
        }
        return version;
    }

    /**
     * 判断当前是否为鸿蒙系统
     *
     * @return 是否是鸿蒙系统，是：true，不是：false
     */
    private static boolean isHarmonyOs() {
        try {
            Class<?> buildExClass = Class.forName("com.huawei.system.BuildEx");
            Object osBrand = buildExClass.getMethod("getOsBrand").invoke(buildExClass);
            if (osBrand == null) {
                return false;
            }
            return "harmony".equalsIgnoreCase(osBrand.toString());
        } catch (Throwable e) {
            SALog.i("SA.HasHarmonyOS", e.getMessage());
            return false;
        }
    }

    private static String getProp(String property, String defaultValue) {
        try {
            Class spClz = Class.forName("android.os.SystemProperties");
            Method method = spClz.getDeclaredMethod("get", String.class);
            String value = (String) method.invoke(spClz, property);
            if (TextUtils.isEmpty(value)) {
                return defaultValue;
            }
            return value;
        } catch (Throwable throwable) {
            SALog.i("SA.SystemProperties", throwable.getMessage());
        }
        return defaultValue;
    }

    /**
     * 执行命令获取对应内容
     *
     * @param command 命令
     * @return 命令返回内容
     */
    public static String exec(String command) {
        InputStreamReader ir = null;
        BufferedReader input = null;
        try {
            Process process = Runtime.getRuntime().exec(command);
            ir = new InputStreamReader(process.getInputStream());
            input = new BufferedReader(ir);
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = input.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (Throwable e) {
            SALog.i("SA.Exec", e.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable e) {
                    SALog.i("SA.Exec", e.getMessage());
                }
            }
            if (ir != null) {
                try {
                    ir.close();
                } catch (IOException e) {
                    SALog.i("SA.Exec", e.getMessage());
                }
            }
        }
        return null;
    }
}
