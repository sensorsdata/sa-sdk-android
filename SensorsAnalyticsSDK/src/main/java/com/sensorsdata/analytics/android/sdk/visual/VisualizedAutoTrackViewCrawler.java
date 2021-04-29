/*
 * Created by renqingyou on 2019/04/13.
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

package com.sensorsdata.analytics.android.sdk.visual;

import android.annotation.TargetApi;
import android.app.Activity;

import com.sensorsdata.analytics.android.sdk.SALog;


@TargetApi(16)
class VisualizedAutoTrackViewCrawler extends AbstractViewCrawler {

    private VisualDebugHelper mVisualDebugHelper;

    VisualizedAutoTrackViewCrawler(Activity activity, String resourcePackageName, String featureCode, String postUrl, VisualDebugHelper visualDebugHelper) {
        super(activity, resourcePackageName, featureCode, postUrl, TYPE_VISUAL);
        mVisualDebugHelper = visualDebugHelper;
    }

    @Override
    public void startUpdates() {
        try {
            super.startUpdates();
            if (mVisualDebugHelper != null) {
                mVisualDebugHelper.stopMonitor();
                mVisualDebugHelper.startMonitor();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void stopUpdates(boolean clear) {
        try {
            super.stopUpdates(clear);
            if (mVisualDebugHelper != null) {
                mVisualDebugHelper.stopMonitor();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
