/*
 * Created by dengshiwei on 2022/09/13.
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

package com.sensorsdata.analytics.android.sdk.core;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.TrackTaskManager;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;

public class SACoreHelper {
    private static final String TAG = "SA.EventManager";
    private volatile static SACoreHelper mSingleton = null;

    private SACoreHelper() {
    }

    public static SACoreHelper getInstance() {
        if (mSingleton == null) {
            synchronized (SACoreHelper.class) {
                if (mSingleton == null) {
                    mSingleton = new SACoreHelper();
                }
            }
        }
        return mSingleton;
    }

    public void trackEvent(InputData inputData) {
        try {
            SensorsDataAPI.sharedInstance().getSAContextManager().trackEvent(inputData);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public void trackQueueEvent(Runnable runnable) {
        try {
            TrackTaskManager.getInstance().addTrackEventTask(runnable);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
