package com.sensorsdata.analytics.android.sdk;

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

    long duration() {
        long duration = timeUnit.convert(System.currentTimeMillis() - startTime + eventAccumulatedDuration, TimeUnit.MILLISECONDS);
        return duration < 0 ? 0 : duration;
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
