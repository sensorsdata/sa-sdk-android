/*
 * Created by wangzhuozhou on 2015/08/01.
 * Copyright 2015－2020 Sensors Data Inc.
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

import java.util.LinkedList;

public class TrackTaskManager {
    private static TrackTaskManager trackTaskManager;
    /**
     * 请求线程队列
     */
    private final LinkedList<Runnable> mTrackEventTasks;

    private TrackTaskManager() {
        mTrackEventTasks = new LinkedList<>();
    }

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
}
