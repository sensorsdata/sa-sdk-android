/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk.util;

import java.util.Timer;
import java.util.TimerTask;

public class SensorsDataTimer {

    private Timer mTimer;
    private TimerTask mTimerTask;
    private static SensorsDataTimer instance;
    private final int TIME_INTERVAL = 1000;

    public static SensorsDataTimer getInstance() {
        if (instance == null) {
            instance = new SensorsDataTimer();
        }
        return instance;
    }

    private SensorsDataTimer() {
    }

    /**
     * start a timer task
     * @param runnable
     */
    public void timer(final Runnable runnable) {
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        };
        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.schedule(mTimerTask, 500, TIME_INTERVAL);
        } else {
            mTimer.schedule(mTimerTask, 500, TIME_INTERVAL);
        }
    }

    /**
     * cancel timer task
     */
    public void cancleTimerTask() {
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }

        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
    }
}
