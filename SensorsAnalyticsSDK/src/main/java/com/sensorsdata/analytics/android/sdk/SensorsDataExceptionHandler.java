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


import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

public class SensorsDataExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final int SLEEP_TIMEOUT_MS = 500;
    private static final ArrayList<SAExceptionListener> sExceptionListeners = new ArrayList<>();
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

    static void addExceptionListener(SAExceptionListener listener) {
        sExceptionListeners.add(listener);
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

            for (SAExceptionListener exceptionListener : sExceptionListeners) {
                try {
                    exceptionListener.uncaughtException(t, e);
                } catch (Exception e1) {
                    SALog.printStackTrace(e1);
                }
            }
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

    /**
     * 异常监听回调
     */
    public interface SAExceptionListener {
        void uncaughtException(final Thread t, final Throwable e);
    }
}
