/*
 * Created by wangzhuozhou on 2015/08/01.
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

package com.sensorsdata.analytics.android.sdk;


import android.os.SystemClock;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;

import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

class SensorsDataExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final int SLEEP_TIMEOUT_MS = 500;

    private static SensorsDataExceptionHandler sInstance;
    private Thread.UncaughtExceptionHandler mDefaultExceptionHandler;
    private static boolean isTrackCrash = false;

    private SensorsDataExceptionHandler() {
        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    synchronized static void init() {
        if (sInstance == null) {
            sInstance = new SensorsDataExceptionHandler();
        }
    }

    static void enableAppCrash() {
        isTrackCrash = true;
    }

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
        try {
            if (isTrackCrash) {
                try {
                    final JSONObject messageProp = new JSONObject();
                    try {
                        Writer writer = new StringWriter();
                        PrintWriter printWriter = new PrintWriter(writer);
                        e.printStackTrace(printWriter);
                        Throwable cause = e.getCause();
                        while (cause != null) {
                            cause.printStackTrace(printWriter);
                            cause = cause.getCause();
                        }
                        printWriter.close();
                        String result = writer.toString();
                        messageProp.put("app_crashed_reason", result);
                    } catch (Exception ex) {
                        SALog.printStackTrace(ex);
                    }
                    SensorsDataAPI.sharedInstance().trackEvent(EventType.TRACK, "AppCrashed", messageProp, null);
                } catch (Exception ex) {
                    SALog.printStackTrace(ex);
                }
            }

            /*
             * 异常的情况会出现两种：
             * 1. 未完成 $AppEnd 事件，触发的异常，此时需要记录下 AppEndTime
             * 2. 完成了 $AppEnd 事件，下次启动时触发的异常。还未及时更新 $AppStart 的时间戳，导致计算时长偏大，所以需要重新更新启动时间戳
             */
            if (TextUtils.isEmpty(DbAdapter.getInstance().getAppEndData())) {
                DbAdapter.getInstance().commitAppStartTime(SystemClock.elapsedRealtime());
            } else {
                DbAdapter.getInstance().commitAppEndTime(System.currentTimeMillis());
            }

            if (SensorsDataAPI.sharedInstance().isMultiProcessFlushData()) {
                DbAdapter.getInstance().commitSubProcessFlushState(false);
            }

            // 注意这里要重置为 0，对于跨进程的情况，如果子进程崩溃，主进程但是没崩溃，造成统计个数异常，所以要重置为 0。
            DbAdapter.getInstance().commitActivityCount(0);
            SensorsDataAPI.sharedInstance().flush();

            try {
                Thread.sleep(SLEEP_TIMEOUT_MS);
            } catch (InterruptedException e1) {
                SALog.printStackTrace(e1);
            }
            if (mDefaultExceptionHandler != null) {
                mDefaultExceptionHandler.uncaughtException(t, e);
            } else {
                killProcessAndExit();
            }
        } catch (Exception exception) {
            //ignored
        }
    }

    private void killProcessAndExit() {
        try {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        } catch (Exception e) {
            //ignored
        }
    }
}
