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
    private static TrackTaskManager trackTaskManager;

    public static synchronized TrackTaskManager getInstance() {
        try {
            if (null == trackTaskManager) {
                trackTaskManager = new TrackTaskManager();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trackTaskManager;
    }

    private TrackTaskManager() {
        mTrackEventTasks = new LinkedList<>();
    }

    public void addTrackEventTask(Runnable trackEvenTask) {
        try {
            synchronized (mTrackEventTasks) {
                mTrackEventTasks.addLast(trackEvenTask);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return null;
    }

}
