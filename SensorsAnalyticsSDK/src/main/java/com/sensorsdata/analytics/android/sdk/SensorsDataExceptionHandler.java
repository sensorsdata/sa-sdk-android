package com.sensorsdata.analytics.android.sdk;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class SensorsDataExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "SensorsDataAPI.Exception";

    private static final int SLEEP_TIMEOUT_MS = 3000;

    private static SensorsDataExceptionHandler sInstance;
    private final Thread.UncaughtExceptionHandler mDefaultExceptionHandler;

    SensorsDataExceptionHandler() {
        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public static void init() {
        if (sInstance == null) {
            synchronized (SensorsDataExceptionHandler.class) {
                if (sInstance == null) {
                    sInstance = new SensorsDataExceptionHandler();
                }
            }
        }
    }

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
        // Only one worker thread - giving priority to storing the event first and then flush
        SensorsDataAPI.allInstances(new SensorsDataAPI.InstanceProcessor() {
            @Override
            public void process(SensorsDataAPI sensorsData) {
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
                        ex.printStackTrace();
                    }
                    sensorsData.track("AppCrashed", messageProp);
                    sensorsData.clearLastScreenUrl();
                    if (!sensorsData.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_END)) {
                        sensorsData.track("$AppEnd");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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
                e1.printStackTrace();
            }
            mDefaultExceptionHandler.uncaughtException(t, e);
        } else {
            killProcessAndExit();
        }
    }

    private void killProcessAndExit() {
        try {
            Thread.sleep(SLEEP_TIMEOUT_MS);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }
}
