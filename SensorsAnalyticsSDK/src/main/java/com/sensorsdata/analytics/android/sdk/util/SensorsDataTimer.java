/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SensorsDataTimer {
    private static SensorsDataTimer instance;
    private ScheduledExecutorService mScheduledExecutorService;
    public static SensorsDataTimer getInstance() {
        if (instance == null) {
            instance = new SensorsDataTimer();
        }
        return instance;
    }

    private SensorsDataTimer() {
        mScheduledExecutorService = Executors.newScheduledThreadPool(2);
    }

    /**
     * start a timer task
     * @param runnable Runnable
     * @param initialDelay long
     * @param timePeriod long
     */
    public void timer(final Runnable runnable, long initialDelay, long timePeriod) {
        if (mScheduledExecutorService == null || mScheduledExecutorService.isShutdown()) {
            mScheduledExecutorService = Executors.newScheduledThreadPool(2);
        }

        mScheduledExecutorService.scheduleAtFixedRate(runnable, initialDelay, timePeriod, TimeUnit.MILLISECONDS);

    }

    /**
     * cancel timer task
     */
    public void cancelTimerTask() {
        if (mScheduledExecutorService != null) {
            mScheduledExecutorService.shutdown();
        }
    }
}
