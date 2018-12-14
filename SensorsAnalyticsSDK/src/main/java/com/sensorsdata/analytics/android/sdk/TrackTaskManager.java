/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk;

import java.util.LinkedList;

/**
 * Created by 王灼洲 on 2018/7/7
 */
public class TrackTaskManager {
    /**
     * 请求线程队列
     */
    private final LinkedList<Runnable> mTrackEventTasks;
    private final LinkedList<Runnable> mEventDBTasks;
    private static TrackTaskManager trackTaskManager;

    public static synchronized TrackTaskManager getInstance() {
        try {
            if (null == trackTaskManager) {
                trackTaskManager = new TrackTaskManager();
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
        return trackTaskManager;
    }

    private TrackTaskManager() {
        mTrackEventTasks = new LinkedList<>();
        mEventDBTasks = new LinkedList<>();
    }

    public void addTrackEventTask(Runnable trackEvenTask) {
        try {
            synchronized (mTrackEventTasks) {
                mTrackEventTasks.addLast(trackEvenTask);
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    public Runnable getTrackEventTask() {
        try {
            synchronized (mTrackEventTasks) {
                if (mTrackEventTasks.size() > 0) {
                    return mTrackEventTasks.removeFirst();
                }
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
        return null;
    }

    public void addEventDBTask(Runnable evenDBTask) {
        try {
            synchronized (mEventDBTasks) {
                mEventDBTasks.addLast(evenDBTask);
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    public Runnable getEventDBTask() {
        try {
            synchronized (mEventDBTasks) {
                if (mEventDBTasks.size() > 0) {
                    return mEventDBTasks.removeFirst();
                }
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
        return null;
    }

}
