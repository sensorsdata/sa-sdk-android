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
