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
        mScheduledExecutorService = Executors.newScheduledThreadPool(1);
    }

    /**
     * start a timer task
     * @param runnable Runnable
     * @param initialDelay long
     * @param timePeriod long
     */
    public void timer(final Runnable runnable, long initialDelay, long timePeriod) {
        if (mScheduledExecutorService == null || mScheduledExecutorService.isShutdown()) {
            mScheduledExecutorService = Executors.newScheduledThreadPool(1);
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
