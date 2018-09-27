/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk;

import android.content.Context;
import android.view.OrientationEventListener;

public class SensorsDataScreenOrientationDetector extends OrientationEventListener {
    private int mCurrentOrientation;

    public SensorsDataScreenOrientationDetector(Context context, int rate) {
        super(context, rate);
    }

    public String getOrientation() {
        if (mCurrentOrientation == 0 || mCurrentOrientation == 180) {
            return "portrait";
        } else if (mCurrentOrientation == 90 || mCurrentOrientation == 270) {
            return "landscape";
        }
        return null;
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            return;
        }

        //只检测是否有四个角度的改变
        if (orientation < 45 || orientation > 315) { //0度
            mCurrentOrientation = 0;
        } else if (orientation > 45 && orientation < 135) { //90度
            mCurrentOrientation = 90;
        } else if (orientation > 135 && orientation < 225) { //180度
            mCurrentOrientation = 180;
        } else if (orientation > 225 && orientation < 315) { //270度
            mCurrentOrientation = 270;
        }
    }
}
