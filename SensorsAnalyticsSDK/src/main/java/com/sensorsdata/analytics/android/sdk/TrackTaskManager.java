/*
 * Created by wangzhuozhou on 2015/08/01.
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

package com.sensorsdata.analytics.android.sdk;

import java.util.concurrent.LinkedBlockingQueue;

public class TrackTaskManager {
    private static TrackTaskManager trackTaskManager;
    private boolean mDataCollectEnable = true;
    /**
     * 请求线程队列
     */
    private final LinkedBlockingQueue<Runnable> mTrackEventTasks;
    private final LinkedBlockingQueue<Runnable> mTrackEventTasksCache;

    private TrackTaskManager() {
        mTrackEventTasks = new LinkedBlockingQueue<>();
        mTrackEventTasksCache = new LinkedBlockingQueue<>();
    }

    public static synchronized TrackTaskManager getInstance() {
        try {
            if (null == trackTaskManager) {
                trackTaskManager = new TrackTaskManager();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return trackTaskManager;
    }

    void addTrackEventTask(Runnable trackEvenTask) {
        try {
            if (mDataCollectEnable) {
                mTrackEventTasks.put(trackEvenTask);
            } else {
                mTrackEventTasksCache.put(trackEvenTask);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 将任务添加到真正执行的队列
     * @param runnable Runnable
     */
    void transformTaskQueue(Runnable runnable) {
        try {
            if (mTrackEventTasks.size() < 50) {// 最多只处理 50 条
                mTrackEventTasks.put(runnable);
            }
        } catch (InterruptedException e) {
            SALog.printStackTrace(e);
        }
    }

    Runnable takeTrackEventTask() {
        try {
            if (mDataCollectEnable) {
                return mTrackEventTasks.take();
            } else {
                return mTrackEventTasksCache.take();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    Runnable pollTrackEventTask() {
        try {
            if (mDataCollectEnable) {
                return mTrackEventTasks.poll();
            } else {
                return mTrackEventTasksCache.poll();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    boolean isEmpty(){
        return mTrackEventTasks.isEmpty();
    }

    void setDataCollectEnable(boolean isDataCollectEnable) {
        this.mDataCollectEnable = isDataCollectEnable;
        try {
            if (isDataCollectEnable) {
                mTrackEventTasksCache.put(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            } else {
                mTrackEventTasks.put(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        } catch (InterruptedException e) {
            SALog.printStackTrace(e);
        }
    }
}
