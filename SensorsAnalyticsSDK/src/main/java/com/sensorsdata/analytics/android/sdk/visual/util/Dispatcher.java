package com.sensorsdata.analytics.android.sdk.visual.util;

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
        removeCallbacksAndMessages();
        mHandler.postDelayed(r, delayMillis);
    }

    public void removeCallbacksAndMessages() {
        mHandler.removeCallbacksAndMessages(null);
    }

}
