package com.sensorsdata.analytics.android.sdk;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by 王灼洲 on 2016/12/28
 */

public class SensorsDataThreadPool {
    private static SensorsDataThreadPool singleton;
    private static Executor mExecutor;

    public static SensorsDataThreadPool getInstance() {
        if (singleton == null) {
            synchronized (SensorsDataThreadPool.class) {
                if (singleton == null) {
                    singleton = new SensorsDataThreadPool();
                    mExecutor = Executors.newFixedThreadPool(1);
                }
            }
        }
        return singleton;
    }

    public void execute(Runnable runnable) {
        try {
            if (runnable != null) {
                mExecutor.execute(runnable);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
