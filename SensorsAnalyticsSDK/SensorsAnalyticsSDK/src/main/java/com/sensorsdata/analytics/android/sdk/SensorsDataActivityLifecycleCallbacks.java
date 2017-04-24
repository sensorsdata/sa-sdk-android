package com.sensorsdata.analytics.android.sdk;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Application;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

/**
 * Created by 王灼洲 on 2017/4/12
 */

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class SensorsDataActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private static final String LOGTAG = "SA.LifecycleCallbacks";
    private boolean resumeFromBackground = false;
    private Integer startedActivityCount = 0;
    private final Object mActivityLifecycleCallbacksLock = new Object();
    private final SensorsDataAPI mSensorsDataInstance;
    private final PersistentFirstStart mFirstStart;

    public SensorsDataActivityLifecycleCallbacks(SensorsDataAPI instance, PersistentFirstStart firstStart) {
        this.mSensorsDataInstance = instance;
        this.mFirstStart = firstStart;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        synchronized (mActivityLifecycleCallbacksLock) {
            if (startedActivityCount == 0) {
                // XXX: 注意内部执行顺序
                boolean firstStart = mFirstStart.get();
                if (firstStart) {
                    mFirstStart.commit(false);
                }

                try {
                    mSensorsDataInstance.appBecomeActive();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (mSensorsDataInstance.isAutoTrackEnabled()) {
                    try {
                        if (!mSensorsDataInstance.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_START)) {
                            JSONObject properties = new JSONObject();
                            properties.put("$resume_from_background", resumeFromBackground);
                            properties.put("$is_first_time", firstStart);
                            SensorsDataUtils.getScreenNameAndTitleFromActivity(properties, activity);

                            mSensorsDataInstance.track("$AppStart", properties);
                        }

                        if (!mSensorsDataInstance.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_END)) {
                            mSensorsDataInstance.trackTimer("$AppEnd", TimeUnit.SECONDS);
                        }
                    } catch (InvalidDataException | JSONException e) {
                        Log.w(LOGTAG, e);
                    }
                }

                // 下次启动时，从后台恢复
                resumeFromBackground = true;
            }

            startedActivityCount = startedActivityCount + 1;
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        boolean mShowAutoTrack = true;
        if (mSensorsDataInstance.isActivityAutoTrackIgnored(activity.getClass())) {
            mShowAutoTrack = false;
        }
        if (mSensorsDataInstance.isAutoTrackEnabled() && mShowAutoTrack && !mSensorsDataInstance.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_VIEW_SCREEN)) {
            try {
                JSONObject properties = new JSONObject();
                properties.put("$screen_name", activity.getClass().getCanonicalName());
                SensorsDataUtils.getScreenNameAndTitleFromActivity(properties, activity);

                if (activity instanceof ScreenAutoTracker) {
                    ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) activity;

                    String screenUrl = screenAutoTracker.getScreenUrl();
                    JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                    if (otherProperties != null) {
                        SensorsDataUtils.mergeJSONObject(otherProperties, properties);
                    }

                    mSensorsDataInstance.trackViewScreen(screenUrl, properties);
                } else {
                    mSensorsDataInstance.track("$AppViewScreen", properties);
                }
            } catch (Exception e) {
                Log.w(LOGTAG, e);
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
        synchronized (mActivityLifecycleCallbacksLock) {
            startedActivityCount = startedActivityCount - 1;

            if (startedActivityCount == 0) {
                try {
                    mSensorsDataInstance.appEnterBackground();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (mSensorsDataInstance.isAutoTrackEnabled()) {
                    try {
                        if (!mSensorsDataInstance.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_END)) {
                            JSONObject properties = new JSONObject();
                            SensorsDataUtils.getScreenNameAndTitleFromActivity(properties, activity);
                            mSensorsDataInstance.track("$AppEnd");
                        }
                    } catch (Exception e) {
                        Log.w(LOGTAG, e);
                    }
                }
                try {
                    mSensorsDataInstance.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}
