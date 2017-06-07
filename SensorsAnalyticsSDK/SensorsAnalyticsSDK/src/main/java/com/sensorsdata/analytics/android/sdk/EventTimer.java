package com.sensorsdata.analytics.android.sdk;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by 王灼洲 on 2017/4/10
 */

class EventTimer {
    EventTimer(TimeUnit timeUnit) {
        this.startTime = System.currentTimeMillis();
        this.timeUnit = timeUnit;
        this.eventAccumulatedDuration = 0;
    }

    String duration() {
        long duration = System.currentTimeMillis() - startTime + eventAccumulatedDuration;
        try {
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
            e.printStackTrace();
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
    private long eventAccumulatedDuration;
}
