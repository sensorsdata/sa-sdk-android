/*
 * Created by wangzhuozhou on 2015/08/01.
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class TrackTaskManagerThread implements Runnable {
    /**
     * 创建一个可重用固定线程数的线程池
     */
    private static final int POOL_SIZE = 1;
    /**
     * 轮询时间，单位：毫秒
     */
    private static final int SLEEP_TIME = 300;
    private TrackTaskManager mTrackTaskManager;
    /**
     * 创建一个可重用固定线程数的线程池
     */
    private ExecutorService mPool;
    /**
     * 是否停止
     */
    private boolean isStop = false;

    public TrackTaskManagerThread() {
        try {
            this.mTrackTaskManager = TrackTaskManager.getInstance();
            mPool = Executors.newFixedThreadPool(POOL_SIZE);
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    @Override
    public void run() {
        try {
            while (!isStop) {
                Runnable downloadTask = mTrackTaskManager.getTrackEventTask();
                if (downloadTask != null) {
                    mPool.execute(downloadTask);
                } else {//如果当前任务队列中没有下载任务downloadTask
                    try {
                        // 查询任务完成失败的,重新加载任务队列
                        // 轮询
                        Thread.sleep(SLEEP_TIME);
                    } catch (InterruptedException e) {
                        com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
                    }
                }
            }

            if (isStop) {
                Runnable downloadTask = mTrackTaskManager.getTrackEventTask();
                while (downloadTask != null) {
                    mPool.execute(downloadTask);
                    downloadTask = mTrackTaskManager.getTrackEventTask();
                }
                mPool.shutdown();
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    public void setStop(boolean isStop) {
        this.isStop = isStop;
    }

}
