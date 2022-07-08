/*
 * Created by chenru on 2022/4/25 下午5:05(format year/.
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

package com.sensorsdata.analytics.android.sdk.advert.oaid.impl;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.sensorsdata.analytics.android.sdk.util.ThreadUtils;

import java.util.concurrent.LinkedBlockingQueue;

class OAIDService implements ServiceConnection {
    public static final LinkedBlockingQueue<IBinder> BINDER_QUEUE = new LinkedBlockingQueue<>(1);

    class Task implements Runnable {
        final IBinder binder;

        Task(IBinder iBinder) {
            binder = iBinder;
        }

        @Override
        public void run() {
            try {
                if (BINDER_QUEUE.size() > 0) {
                    BINDER_QUEUE.clear();
                }
                BINDER_QUEUE.put(binder);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        ThreadUtils.getSinglePool().execute(new Task(service));
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }
}
