/*
 * Created by chenru on 2022/4/1 上午10:46.
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

package com.sensorsdata.analytics.android.sdk;

import com.sensorsdata.analytics.android.sdk.core.event.InputData;

public class SAEventManager {
    private static final String TAG = "SA.EventManager";
    private volatile static SAEventManager mSingleton = null;

    private SAEventManager() {
    }

    public static SAEventManager getInstance() {
        if (mSingleton == null) {
            synchronized (SAEventManager.class) {
                if (mSingleton == null) {
                    mSingleton = new SAEventManager();
                }
            }
        }
        return mSingleton;
    }

    public void trackEvent(InputData inputData) {
        SensorsDataAPI.sharedInstance().getSAContextManager().trackEvent(inputData);
    }

    public void trackQueueEvent(Runnable runnable) {
        TrackTaskManager.getInstance().addTrackEventTask(runnable);
    }
}
