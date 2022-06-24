/*
 * Created by wangzhuozhou on 2015/08/01.
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

import java.util.concurrent.LinkedBlockingQueue;

public class TrackTaskManager {
    private static TrackTaskManager trackTaskManager;
    /**
     * 请求线程队列
     */
    private final LinkedBlockingQueue<Runnable> mTrackEventTasks;

    private TrackTaskManager() {
        mTrackEventTasks = new LinkedBlockingQueue<>();
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
            mTrackEventTasks.put(trackEvenTask);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    Runnable takeTrackEventTask() {
        try {
            return mTrackEventTasks.take();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    Runnable pollTrackEventTask() {
        try {
            return mTrackEventTasks.poll();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    boolean isEmpty(){
        return mTrackEventTasks.isEmpty();
    }
}
