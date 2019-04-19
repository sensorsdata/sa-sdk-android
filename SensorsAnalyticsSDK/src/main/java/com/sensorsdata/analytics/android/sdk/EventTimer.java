/*
 * Created by wangzhuozhou on 2017/4/10.
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

import android.os.SystemClock;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

class EventTimer {
    EventTimer(TimeUnit timeUnit) {
        this.startTime = SystemClock.elapsedRealtime();
        this.timeUnit = timeUnit;
        this.eventAccumulatedDuration = 0;
        this.endTime = -1;
    }

    public EventTimer(TimeUnit timeUnit, long startTime, long endTime) {
        this.timeUnit = timeUnit;
        this.startTime = startTime;
        this.endTime = endTime;
        this.eventAccumulatedDuration = 0;
    }

    String duration() {
        endTime = endTime < 0 ? SystemClock.elapsedRealtime() : endTime;
        long duration = endTime - startTime + eventAccumulatedDuration;
        try {
            if (duration < 0 || duration > 24 * 60 * 60 * 1000) {
                return String.valueOf(0);
            }
            float durationFloat;
            if (timeUnit == TimeUnit.MILLISECONDS) {
                durationFloat = duration;
            } else if (timeUnit == TimeUnit.SECONDS) {
                durationFloat = duration / 1000.0f;
            } else if (timeUnit == TimeUnit.MINUTES) {
                durationFloat = duration / 1000.0f / 60.0f;
            } else if (timeUnit == TimeUnit.HOURS) {
                durationFloat = duration / 1000.0f / 60.0f / 60.0f;
            } else {
                durationFloat = duration;
            }
            return durationFloat < 0 ? String.valueOf(0) : String.format(Locale.CHINA, "%.3f", durationFloat);
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
            return String.valueOf(0);
        }
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEventAccumulatedDuration() {
        return eventAccumulatedDuration;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setEventAccumulatedDuration(long eventAccumulatedDuration) {
        this.eventAccumulatedDuration = eventAccumulatedDuration;
    }

    private final TimeUnit timeUnit;
    private long startTime;
    private long endTime;
    private long eventAccumulatedDuration;
}
