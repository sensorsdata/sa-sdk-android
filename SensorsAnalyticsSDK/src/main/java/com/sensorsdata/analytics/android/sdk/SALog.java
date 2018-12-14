/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk;

import android.util.Log;


/**
 * Created by 王灼洲 on 2017/5/5
 */

public class SALog {
    private static SensorsDataAPI mSensorsDataAPI;

    private SALog() {

    }

    public static void init(SensorsDataAPI sensorsDataAPI) {
        mSensorsDataAPI = sensorsDataAPI;
    }

    public static void d(String tag, String msg) {
        if (mSensorsDataAPI.isDebugMode()) {
            info(tag, msg,null);
        }
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (mSensorsDataAPI.isDebugMode()) {
            info(tag, msg, tr);
        }

    }

    public static void i(String tag, String msg) {
        if (SensorsDataAPI.ENABLE_LOG) {
            info(tag, msg,null);
        }
    }

    public static void i(String tag, Throwable tr) {
        if (SensorsDataAPI.ENABLE_LOG) {
            info(tag,"",tr);
        }
    }

    public static void i(String tag, String msg, Throwable tr) {
        if (SensorsDataAPI.ENABLE_LOG) {
            info(tag,msg,tr);
        }
    }

    /**
     * 此方法谨慎修改
     * 插件配置 disableLog 会修改此方法
     * @param tag String
     * @param msg String
     * @param tr Throwable
     */
    public static void info(String tag, String msg, Throwable tr) {
        try {
            Log.i(tag, msg, tr);
        } catch (Exception e) {
            printStackTrace(e);
        }
    }

    /**
     * 此方法谨慎修改
     * 插件配置 disableLog 会修改此方法
     * @param e Exception
     */
    public static void printStackTrace(Exception e) {
        if (e != null) {
            e.printStackTrace();
        }
    }
}
