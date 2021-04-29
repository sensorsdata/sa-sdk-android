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

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.visual.property.VisualPropertiesLog;
import com.sensorsdata.analytics.android.sdk.visual.property.VisualPropertiesManager;

/**
 * Created by 任庆友 on 2019/04/13
 */

public class VisualizedAutoTrackService {
    private static final String TAG = "VisualizedAutoTrackService";
    private static VisualizedAutoTrackService instance;
    private static VisualizedAutoTrackViewCrawler mVTrack;
    private boolean mDebugModeEnabled = false;
    private VisualPropertiesLog mVisualPropertiesLog;
    private VisualDebugHelper mVisualDebugHelper;
    private String mLastDebugInfo;

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
                if (mVisualDebugHelper == null) {
                    mVisualDebugHelper = new VisualDebugHelper();
                }
                mVTrack = new VisualizedAutoTrackViewCrawler(activity, resourcePackageName, featureCode, postUrl, mVisualDebugHelper);
                mVTrack.startUpdates();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public boolean isServiceRunning() {
        if (mVTrack != null) {
            return mVTrack.isServiceRunning();
        }
        return false;
    }

    String getDebugInfo() {
        try {
            if (mVisualDebugHelper != null) {
                mLastDebugInfo = mVisualDebugHelper.getDebugInfo();
                if (!TextUtils.isEmpty(mLastDebugInfo)) {
                    SALog.i(TAG, "visual debug info: " + mLastDebugInfo);
                    return mLastDebugInfo;
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    String getLastDebugInfo() {
        try {
            if (!TextUtils.isEmpty(mLastDebugInfo)) {
                SALog.i(TAG, "last debug info: " + mLastDebugInfo);
                return mLastDebugInfo;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    String getVisualLogInfo() {
        try {
            if (mVisualPropertiesLog != null) {
                String visualPropertiesLog = mVisualPropertiesLog.getVisualPropertiesLog();
                if (!TextUtils.isEmpty(visualPropertiesLog)) {
                    SALog.i(TAG, "visual log info: " + visualPropertiesLog);
                    return visualPropertiesLog;
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    void setDebugModeEnabled(boolean debugModeEnabled) {
        try {
            if (mDebugModeEnabled != debugModeEnabled) {
                if (debugModeEnabled) {
                    mVisualPropertiesLog = new VisualPropertiesLog();
                    VisualPropertiesManager.getInstance().registerCollectLogListener(mVisualPropertiesLog);
                } else {
                    mVisualPropertiesLog = null;
                    VisualPropertiesManager.getInstance().unRegisterCollectLogListener();
                }
            }
            mDebugModeEnabled = debugModeEnabled;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
