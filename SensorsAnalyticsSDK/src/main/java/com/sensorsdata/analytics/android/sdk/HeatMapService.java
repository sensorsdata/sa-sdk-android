package com.sensorsdata.analytics.android.sdk;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

/**
 * Created by 王灼洲 on 2017/9/7
 */

public class HeatMapService {
    private static HeatMapService instance;
    private static HeatMapViewCrawler mVTrack;

    private HeatMapService() {
    }

    public static HeatMapService getInstance() {
        if (instance == null) {
            instance = new HeatMapService();
        }
        return instance;
    }

    public void stop() {
        try {
            if (mVTrack != null) {
                mVTrack.stopUpdates(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        try {
            if (mVTrack != null) {
                mVTrack.startUpdates();
            }
        } catch (Exception e) {
            e.printStackTrace();
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

                mVTrack = new HeatMapViewCrawler(activity, resourcePackageName, featureCode, postUrl);
                mVTrack.startUpdates();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
