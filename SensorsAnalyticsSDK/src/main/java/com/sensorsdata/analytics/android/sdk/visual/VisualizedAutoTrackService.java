/*
 * Created by renqingyou on 2019/04/13.
 * Copyright 2015－2020 Sensors Data Inc.
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

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.sensorsdata.analytics.android.sdk.SALog;

/**
 * Created by 任庆友 on 2019/04/13
 */

public class VisualizedAutoTrackService {
    private static VisualizedAutoTrackService instance;
    private static VisualizedAutoTrackViewCrawler mVTrack;

    private VisualizedAutoTrackService() {
    }

    public static VisualizedAutoTrackService getInstance() {
        if (instance == null) {
            instance = new VisualizedAutoTrackService();
        }
        return instance;
    }

    public void stop() {
        try {
            if (mVTrack != null) {
                mVTrack.stopUpdates(false);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public void resume() {
        try {
            if (mVTrack != null) {
                mVTrack.startUpdates();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public void start(Activity activity, String featureCode, String postUrl) {
        try {
            final String packageName = activity.getApplicationContext().getPackageName();
            final ApplicationInfo appInfo = activity.getApplicationContext().getPackageManager()
                    .getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            Bundle configBundle = appInfo.metaData;
            if (null == configBundle) {
                configBundle = new Bundle();
            }
            if (Build.VERSION.SDK_INT >= 16) {
                String resourcePackageName =
                        configBundle.getString("com.sensorsdata.analytics.android.ResourcePackageName");
                if (null == resourcePackageName) {
                    resourcePackageName = activity.getPackageName();
                }

                mVTrack = new VisualizedAutoTrackViewCrawler(activity, resourcePackageName, featureCode, postUrl);
                mVTrack.startUpdates();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public boolean isVisualizedAutoTrackRunning() {
        if (mVTrack != null) {
            return mVTrack.isVisualizedAutoTrackRunning();
        }
        return false;
    }
}
