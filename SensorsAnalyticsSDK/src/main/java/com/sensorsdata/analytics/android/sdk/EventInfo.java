package com.sensorsdata.analytics.android.sdk;

public class EventInfo {

    public final String mEventType;
    public final String mEventName;
    public final String mPath;
    public final int mTriggerId;
    public final boolean mIsDeployed;

    public EventInfo(String eventName, String eventType, String path, int triggerId, boolean
            isDeployed) {
        mEventName = eventName;
        mEventType = eventType;
        mPath = path;
        mTriggerId = triggerId;
        mIsDeployed = isDeployed;
    }

    public String toString() {
        return "EventInfo "
                + "{ EventName: " + mEventName
                + ", EventType: " + mEventType
                + ", Path: " + mPath
                + ", TriggerId: " + mTriggerId
                + ", IsDeployed:" + mIsDeployed
                + "}";
    }

}
