/*
 * Created by zhangxiangwei on 2020/07/31.
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

package com.sensorsdata.analytics.android.sdk.util;


import com.sensorsdata.analytics.android.sdk.SALog;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public class ThreadUtils {
    private static final String TAG = "SA.ThreadUtils";
    private static final Map<Integer, Map<Integer, ExecutorService>> TYPE_PRIORITY_POOLS = new HashMap<>();
    private static final byte TYPE_SINGLE = -1;

    public static ExecutorService getSinglePool() {
        return getPoolByTypeAndPriority(TYPE_SINGLE);
    }

    public static ExecutorService getSinglePool(final int priority) {
        return getPoolByTypeAndPriority(TYPE_SINGLE, priority);
    }

    private static ExecutorService getPoolByTypeAndPriority(final int type) {
        return getPoolByTypeAndPriority(type, Thread.NORM_PRIORITY);
    }

    private static ExecutorService getPoolByTypeAndPriority(final int type, final int priority) {
        synchronized (TYPE_PRIORITY_POOLS) {
            ExecutorService pool;
            Map<Integer, ExecutorService> priorityPools = TYPE_PRIORITY_POOLS.get(type);
            if (priorityPools == null) {
                priorityPools = new ConcurrentHashMap<>();
                pool = ThreadPoolExecutorUtil.createPool(type, priority);
                priorityPools.put(priority, pool);
                TYPE_PRIORITY_POOLS.put(type, priorityPools);
            } else {
                pool = priorityPools.get(priority);
                if (pool == null) {
                    pool = ThreadPoolExecutorUtil.createPool(type, priority);
                    priorityPools.put(priority, pool);
                }
            }
            return pool;
        }
    }

    static final class ThreadPoolExecutorUtil extends ThreadPoolExecutor {

        private static ExecutorService createPool(final int type, final int priority) {
            switch (type) {
                case TYPE_SINGLE:
                    return new ThreadPoolExecutorUtil(1, 1,
                            0L, TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueueUtil(),
                            new UtilsThreadFactory("single", priority)
                    );
                default:
                    return new ThreadPoolExecutorUtil(type, type,
                            0L, TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueueUtil(),
                            new UtilsThreadFactory("fixed(" + type + ")", priority)
                    );
            }
        }

        private final AtomicInteger mSubmittedCount = new AtomicInteger();

        private LinkedBlockingQueueUtil mWorkQueue;

        ThreadPoolExecutorUtil(int corePoolSize, int maximumPoolSize,
                               long keepAliveTime, TimeUnit unit,
                               LinkedBlockingQueueUtil workQueue,
                               ThreadFactory threadFactory) {
            super(corePoolSize, maximumPoolSize,
                    keepAliveTime, unit,
                    workQueue,
                    threadFactory
            );
            workQueue.mPool = this;
            mWorkQueue = workQueue;
        }

        private int getSubmittedCount() {
            return mSubmittedCount.get();
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            mSubmittedCount.decrementAndGet();
            super.afterExecute(r, t);
        }

        @Override
        public void execute(Runnable command) {
            if (this.isShutdown()) return;
            mSubmittedCount.incrementAndGet();
            try {
                super.execute(command);
            } catch (RejectedExecutionException ignore) {
                SALog.i(TAG, "This will not happen!");
                mWorkQueue.offer(command);
            } catch (Throwable t) {
                mSubmittedCount.decrementAndGet();
            }
        }
    }

    private static final class LinkedBlockingQueueUtil extends LinkedBlockingQueue<Runnable> {

        private volatile ThreadPoolExecutorUtil mPool;

        private int mCapacity = Integer.MAX_VALUE;

        LinkedBlockingQueueUtil() {
            super();
        }

        LinkedBlockingQueueUtil(boolean isAddSubThreadFirstThenAddQueue) {
            super();
            if (isAddSubThreadFirstThenAddQueue) {
                mCapacity = 0;
            }
        }

        LinkedBlockingQueueUtil(int capacity) {
            super();
            mCapacity = capacity;
        }

        @Override
        public boolean offer(Runnable runnable) {
            if (mCapacity <= size() &&
                    mPool != null && mPool.getPoolSize() < mPool.getMaximumPoolSize()) {
                // create a non-core thread
                return false;
            }
            return super.offer(runnable);
        }
    }

    static final class UtilsThreadFactory extends AtomicLong
            implements ThreadFactory {
        private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
        private final String namePrefix;
        private final int priority;
        private final boolean isDaemon;

        UtilsThreadFactory(String prefix, int priority) {
            this(prefix, priority, false);
        }

        UtilsThreadFactory(String prefix, int priority, boolean isDaemon) {
            namePrefix = prefix + "-pool-" +
                    POOL_NUMBER.getAndIncrement() +
                    "-thread-";
            this.priority = priority;
            this.isDaemon = isDaemon;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + getAndIncrement()) {
                @Override
                public void run() {
                    try {
                        super.run();
                    } catch (Throwable t) {
                        SALog.i("ThreadUtils", "Request threw uncaught throwable");
                    }
                }
            };
            t.setDaemon(isDaemon);
            t.setPriority(priority);
            return t;
        }
    }
}

