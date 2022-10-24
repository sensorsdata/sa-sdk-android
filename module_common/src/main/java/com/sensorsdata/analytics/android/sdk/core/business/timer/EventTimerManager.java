/*
 * Created by dengshiwei on 2022/06/16.
 * Copyright 2015－2021 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.core.business.timer;

import android.os.SystemClock;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;

import java.util.HashMap;
import java.util.Map;

public class EventTimerManager {
    private final Map<String, EventTimer> mTrackTimer;

    public static synchronized EventTimerManager getInstance() {
        return SingletonHolder.mInstance;
    }

    private static class SingletonHolder {
        private static final EventTimerManager mInstance = new EventTimerManager();
    }
    private EventTimerManager(){
        mTrackTimer = new HashMap<>();
    }

    public void addEventTimer(String eventName, EventTimer eventTimer) {
        synchronized (mTrackTimer) {
            // remind：update startTime before runnable queue
            mTrackTimer.put(eventName, eventTimer);
        }
    }

    public void removeTimer(String eventName) {
        synchronized (mTrackTimer) {
            mTrackTimer.remove(eventName);
        }
    }

    public void updateEndTime(String eventName, long endTime) {
        synchronized (mTrackTimer) {
            EventTimer eventTimer = mTrackTimer.get(eventName);
            if (eventTimer != null) {
                eventTimer.setEndTime(endTime);
            }
        }
    }

    public void updateTimerState(final String eventName, long startTime, final boolean isPause) {
        try {
            SADataHelper.assertEventName(eventName);
            synchronized (mTrackTimer) {
                EventTimer eventTimer = mTrackTimer.get(eventName);
                if (eventTimer != null && eventTimer.isPaused() != isPause) {
                    eventTimer.setTimerState(isPause, startTime);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public EventTimer getEventTimer(String eventName) {
        EventTimer eventTimer;
        synchronized (mTrackTimer) {
            eventTimer = mTrackTimer.get(eventName);
            mTrackTimer.remove(eventName);
        }
        return eventTimer;
    }

    public void clearTimers() {
        try {
            synchronized (mTrackTimer) {
                mTrackTimer.clear();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * App 从后台恢复，遍历 mTrackTimer
     * startTime = System.currentTimeMillis()
     */
    public void appBecomeActive() {
        synchronized (mTrackTimer) {
            try {
                for (Map.Entry<String, EventTimer> entry : mTrackTimer.entrySet()) {
                    if (entry != null) {
                        EventTimer eventTimer = entry.getValue();
                        if (eventTimer != null) {
                            eventTimer.setStartTime(SystemClock.elapsedRealtime());
                        }
                    }
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    /**
     * App 进入后台，遍历 mTrackTimer
     * eventAccumulatedDuration =
     * eventAccumulatedDuration + System.currentTimeMillis() - startTime - SessionIntervalTime
     */
    public void appEnterBackground() {
        synchronized (mTrackTimer) {
            try {
                for (Map.Entry<String, EventTimer> entry : mTrackTimer.entrySet()) {
                    if (entry != null) {
                        if ("$AppEnd".equals(entry.getKey())) {
                            continue;
                        }
                        EventTimer eventTimer = entry.getValue();
                        if (eventTimer != null && !eventTimer.isPaused()) {
                            long eventAccumulatedDuration = eventTimer.getEventAccumulatedDuration() + SystemClock.elapsedRealtime() - eventTimer.getStartTime();
                            eventTimer.setEventAccumulatedDuration(eventAccumulatedDuration);
                            eventTimer.setStartTime(SystemClock.elapsedRealtime());
                        }
                    }
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }
}
