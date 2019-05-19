/*
 * Created by wangzhuozhou on 2015/08/01.
 * Copyright 2015－2019 Sensors Data Inc.
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


import com.sensorsdata.analytics.android.sdk.data.DbAdapter;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataTimer;

import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class SensorsDataExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final int SLEEP_TIMEOUT_MS = 3000;

    private static SensorsDataExceptionHandler sInstance;
    private final Thread.UncaughtExceptionHandler mDefaultExceptionHandler;

    SensorsDataExceptionHandler() {
        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public synchronized static void init() {
        if (sInstance == null) {
            sInstance = new SensorsDataExceptionHandler();
        }
    }

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
        try {
            // Only one worker thread - giving priority to storing the event first and then flush
            SensorsDataAPI.allInstances(new SensorsDataAPI.InstanceProcessor() {
                @Override
                public void process(SensorsDataAPI sensorsData) {
                    try {
                        final JSONObject messageProp = new JSONObject();
                        SensorsDataTimer.getInstance().shutdownTimerTask();
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
                        DbAdapter.getInstance().commitAppPausedTime(System.currentTimeMillis());
                        sensorsData.track("AppCrashed", messageProp);
                    } catch (Exception e) {
                        com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
                    }
                }
            });

            SensorsDataAPI.allInstances(new SensorsDataAPI.InstanceProcessor() {
                @Override
                public void process(SensorsDataAPI sensorsData) {
                    sensorsData.flush();
                }
            });

            if (mDefaultExceptionHandler != null) {
                try {
                    Thread.sleep(SLEEP_TIMEOUT_MS);
                } catch (InterruptedException e1) {
                    SALog.printStackTrace(e1);
                }
                try {
                    mDefaultExceptionHandler.uncaughtException(t, e);
                } catch (Exception ex) {
                    //ignored
                }
            } else {
                killProcessAndExit();
            }
        } catch (Exception exception) {
            //ignored
        }
    }

    private void killProcessAndExit() {
        try {
            Thread.sleep(SLEEP_TIMEOUT_MS);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        } catch (Exception e) {
            //ignored
        }
    }
}
