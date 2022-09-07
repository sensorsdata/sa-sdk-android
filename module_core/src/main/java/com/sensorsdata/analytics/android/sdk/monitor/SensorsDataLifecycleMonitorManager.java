/*
 * Created by chenru on 2022/3/10 上午11:49.
 * Copyright 2015－2022 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.monitor;

import com.sensorsdata.analytics.android.sdk.SensorsDataExceptionHandler;

public class SensorsDataLifecycleMonitorManager {
    private final SensorsDataActivityLifecycleCallbacks mCallback;
    private static final SensorsDataLifecycleMonitorManager instance = new SensorsDataLifecycleMonitorManager();

    private SensorsDataLifecycleMonitorManager() {
        mCallback = new SensorsDataActivityLifecycleCallbacks();
    }

    public static SensorsDataLifecycleMonitorManager getInstance() {
        return instance;
    }

    public void addActivityLifeCallback(SensorsDataActivityLifecycleCallbacks.SAActivityLifecycleCallbacks callbacks) {
        mCallback.addActivityLifecycleCallbacks(callbacks);
    }

    public void addCrashListener(SensorsDataExceptionHandler.SAExceptionListener listener) {
        SensorsDataExceptionHandler.addExceptionListener(listener);
    }

    public void removeCrashListener(SensorsDataExceptionHandler.SAExceptionListener listener) {
        SensorsDataExceptionHandler.removeExceptionListener(listener);
    }

    public void removeActivityLifeCallback(SensorsDataActivityLifecycleCallbacks.SAActivityLifecycleCallbacks callbacks) {
        mCallback.removeActivityLifecycleCallbacks(callbacks);
    }

    public SensorsDataActivityLifecycleCallbacks getCallback() {
        return mCallback;
    }
}