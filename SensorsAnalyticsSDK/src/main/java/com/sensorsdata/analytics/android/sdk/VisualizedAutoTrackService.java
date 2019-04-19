/**
 Created by 任庆友 on 2019/04/13.
 Copyright © 2015－2019 Sensors Data Inc. All rights reserved.
 */

package com.sensorsdata.analytics.android.sdk;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

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
}
