/*
 * Created by dengshiwei on 2022/07/04.
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

package com.sensorsdata.analytics.android.sdk.visual.utils;

import android.os.Handler;
import android.os.HandlerThread;


public class Dispatcher {

    private static String TAG = Dispatcher.class.getSimpleName();
    private Handler mHandler;

    public static Dispatcher getInstance() {
        return DispatchHolder.INSTANCE;
    }

    private Dispatcher() {
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
    }

    private static class DispatchHolder {
        private static final Dispatcher INSTANCE = new Dispatcher();
    }

    public void post(Runnable r) {
        postDelayed(r, 0);
    }

    public void postDelayed(Runnable r, long delayMillis) {
        mHandler.removeCallbacks(r);
        mHandler.postDelayed(r, delayMillis);
    }

    public void removeCallbacksAndMessages() {
        mHandler.removeCallbacksAndMessages(null);
    }
}
