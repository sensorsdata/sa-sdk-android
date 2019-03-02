package com.sensorsdata.analytics.android.sdk;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SensorsDataThreadPool {
    private static SensorsDataThreadPool singleton;
    private static ExecutorService executorService;

    public synchronized static SensorsDataThreadPool getInstance() {
        if (singleton == null || executorService == null ||
                executorService.isShutdown() || executorService.isTerminated()) {
            singleton = new SensorsDataThreadPool();
            int size = Runtime.getRuntime().availableProcessors();
            executorService = Executors.newFixedThreadPool(size * 2);
        }
        return singleton;
    }

    public void execute(Runnable runnable) {
        try {
            if (runnable != null) {
                executorService.execute(runnable);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
