/*
 * Created by wangzhuozhou on 2015/08/01.
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

import com.sensorsdata.analytics.android.sdk.ThreadNameConstants;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class SensorsDataTimer {
    private static SensorsDataTimer instance;
    private ScheduledExecutorService mScheduledExecutorService;

    private SensorsDataTimer() {
    }

    public static SensorsDataTimer getInstance() {
        if (instance == null) {
            instance = new SensorsDataTimer();
        }
        return instance;
    }

    /**
     * 开启 timer 线程池
     *
     * @param runnable Runnable
     * @param initialDelay long
     * @param timePeriod long
     */
    public void timer(final Runnable runnable, long initialDelay, long timePeriod) {
        if (isShutdown()) {
            mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryWithName(ThreadNameConstants.THREAD_APP_END_DATA_SAVE_TIMER));
            mScheduledExecutorService.scheduleAtFixedRate(runnable, initialDelay, timePeriod, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 关闭 timer 线程池
     */
    public void shutdownTimerTask() {
        if (mScheduledExecutorService != null) {
            mScheduledExecutorService.shutdown();
        }
    }

    /**
     * 当前线程池是否可用
     *
     * @return Boolean 返回当前线程池状态 true : 不可用 false : 可用
     */
    private boolean isShutdown() {
        return mScheduledExecutorService == null || mScheduledExecutorService.isShutdown();
    }

    static class ThreadFactoryWithName implements ThreadFactory {

        private final String name;

        ThreadFactoryWithName(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, name);
        }
    }

}
