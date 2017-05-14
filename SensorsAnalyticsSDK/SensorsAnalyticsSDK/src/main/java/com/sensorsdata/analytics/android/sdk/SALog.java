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
        try {
            if (mSensorsDataAPI.isDebugMode()) {
                Log.i(tag, msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void d(String tag, String msg, Throwable tr) {
        try {
            if (mSensorsDataAPI.isDebugMode()) {
                Log.i(tag, msg, tr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void i(String tag, String msg) {
        try {
            if (SensorsDataAPI.ENABLE_LOG) {
                Log.i(tag, msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void i(String tag, Throwable tr) {
        try {
            if (SensorsDataAPI.ENABLE_LOG) {
                Log.i(tag, "", tr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void i(String tag, String msg, Throwable tr) {
        try {
            if (SensorsDataAPI.ENABLE_LOG) {
                Log.i(tag, msg, tr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
