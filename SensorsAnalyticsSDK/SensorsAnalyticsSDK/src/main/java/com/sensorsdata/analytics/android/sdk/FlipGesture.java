package com.sensorsdata.analytics.android.sdk;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class FlipGesture implements SensorEventListener {

    public interface OnFlipGestureListener {
        public void onFlipGesture();
    }

    public FlipGesture(OnFlipGestureListener listener) {
        mListener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final float x = event.values[0];
        final float y = event.values[1];
        final float z = event.values[2];

        final float totalGravitySquared = x * x + y * y + z * z;

        if (totalGravitySquared > MINIMUM_TRIGGER_SPEED) {
            switch (mTriggerState) {
                case TRIGGER_STATE_NONE:
                    mLastFlipTime = event.timestamp;
                    mTriggerState = TRIGGER_STATE_BEGIN;
                    break;
                case TRIGGER_STATE_BEGIN:
                    if (event.timestamp - mLastFlipTime > MINIMUM_TRIGGER_DURATION) {
                        mListener.onFlipGesture();
                        mTriggerState = TRIGGER_STATE_OK;
                    }
                    break;
                case TRIGGER_STATE_OK:
                    break;
            }

        } else {
            mTriggerState = TRIGGER_STATE_NONE;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        ; // Do nothing
    }

    private long mLastFlipTime = -1;
    private final OnFlipGestureListener mListener;
    private int mTriggerState = TRIGGER_STATE_NONE;

    private static final int TRIGGER_STATE_NONE = -1;
    private static final int TRIGGER_STATE_BEGIN = 0;
    private static final int TRIGGER_STATE_OK = 1;

    private static final int MINIMUM_TRIGGER_SPEED = 300;
    private static final long MINIMUM_TRIGGER_DURATION = 250000000;
    private static final long MINIMUM_CANCEL_DURATION = 1000000000;

    // Higher is noisier but more responsive, 1.0 to 0.0
    private static final float ACCELEROMETER_SMOOTHING = 0.7f;

    private static final String LOGTAG = "SA.FlipGesture";
}
