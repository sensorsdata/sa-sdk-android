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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class TrackTaskManagerThread implements Runnable {
    /**
     * 创建一个可重用固定线程数的线程池
     */
    private static final int POOL_SIZE = 1;

    private TrackTaskManager mTrackTaskManager;
    /**
     * 创建一个可重用固定线程数的线程池
     */
    private ExecutorService mPool;
    /**
     * 是否停止
     */
    private boolean isStop = false;

    TrackTaskManagerThread() {
        try {
            this.mTrackTaskManager = TrackTaskManager.getInstance();
            mPool = Executors.newFixedThreadPool(POOL_SIZE);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void run() {
        try {
            while (!isStop) {
                Runnable downloadTask = mTrackTaskManager.takeTrackEventTask();
                mPool.execute(downloadTask);
            }
            while (true) {
                Runnable downloadTask = mTrackTaskManager.pollTrackEventTask();
                if (downloadTask == null) {
                    break;
                }
                mPool.execute(downloadTask);
            }
            mPool.shutdown();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    void stop() {
        isStop = true;
        //解决队列阻塞时,停止队列还会触发一次事件
        if (mTrackTaskManager.isEmpty()) {
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {

                }
            });
        }
    }

    boolean isStopped() {
        return isStop;
    }
}
