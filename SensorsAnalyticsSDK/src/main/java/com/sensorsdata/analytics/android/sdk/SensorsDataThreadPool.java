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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SensorsDataThreadPool {
    private static SensorsDataThreadPool singleton;
    private static ExecutorService executorService;
    private static final int POOL_SIZE = 3;
    public synchronized static SensorsDataThreadPool getInstance() {
        if (singleton == null || executorService == null ||
                executorService.isShutdown() || executorService.isTerminated()) {
            singleton = new SensorsDataThreadPool();
            executorService = Executors.newFixedThreadPool(POOL_SIZE);
        }
        return singleton;
    }

    public void execute(Runnable runnable) {
        try {
            initThreadPool();
            if (runnable != null) {
                executorService.execute(runnable);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    private void initThreadPool() {
        if (executorService.isShutdown() || executorService.isTerminated()) {
            executorService = Executors.newFixedThreadPool(POOL_SIZE);
        }
    }
}