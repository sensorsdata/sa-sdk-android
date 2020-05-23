package com.sensorsdata.analytics.android.sdk.visual.util;

import android.os.Handler;
import android.os.HandlerThread;


public class Dispatch {

    private static String TAG = Dispatch.class.getSimpleName();
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    public static Dispatch getInstance() {
        return DispatchHolder.INSTANCE;
    }

    private Dispatch() {
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    private static class DispatchHolder {
        private static final Dispatch INSTANCE = new Dispatch();
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
