/*
 * Created by dengshiwei on 2020/05/12.
 * Copyright 2015－2020 Sensors Data Inc.
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

import java.util.ArrayList;
import java.util.List;

public class DeviceUtils {
    private static final List<String> sManufacturer = new ArrayList<String>() {
        {
            add("HUAWEI");
            add("OPPO");
            add("vivo");
        }
    };

    public static String getOS() {
        return Build.VERSION.RELEASE == null ? "UNKNOWN" : Build.VERSION.RELEASE;
    }

    public static String getManufacturer() {
        String manufacturer = Build.MANUFACTURER == null ? "UNKNOWN" : Build.MANUFACTURER.trim();
        try {
            if (!TextUtils.isEmpty(manufacturer)) {
                for (String item : sManufacturer) {
                    if (item.equalsIgnoreCase(manufacturer)) {
                        return item;
                    }
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return manufacturer;
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
}
