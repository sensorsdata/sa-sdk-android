package com.sensorsdata.analytics.android.sdk;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

/**
 * Created by 王灼洲 on 2017/4/12
 */

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class SensorsDataActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "SA.LifecycleCallbacks";
    private boolean resumeFromBackground = false;
    private Integer startedActivityCount = 0;
    private final Object mActivityLifecycleCallbacksLock = new Object();
    private final SensorsDataAPI mSensorsDataInstance;
    private final PersistentFirstStart mFirstStart;
    private final String mMainProcessName;

    public SensorsDataActivityLifecycleCallbacks(SensorsDataAPI instance, PersistentFirstStart firstStart, String mainProcessName) {
        this.mSensorsDataInstance = instance;
        this.mFirstStart = firstStart;
        this.mMainProcessName = mainProcessName;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        try {
            Uri uri = null;
            if (activity != null) {
                Intent intent = activity.getIntent();
                    if (intent != null) {
                        uri = intent.getData();
                    }
            }
            if (uri != null) {
                String host = uri.getHost();
                if ("heatmap".equals(host)) {
                    String featureCode = uri.getQueryParameter("feature_code");
                    String postUrl = uri.getQueryParameter("url");
                    showOpenHeatMapDialog(activity, featureCode, postUrl);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        try {
            synchronized (mActivityLifecycleCallbacksLock) {
                if (startedActivityCount == 0) {
                    // XXX: 注意内部执行顺序
                    boolean firstStart = mFirstStart.get();

                    try {
                        mSensorsDataInstance.appBecomeActive();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (SensorsDataUtils.isMainProcess(activity, mMainProcessName)) {
                        if (mSensorsDataInstance.isAutoTrackEnabled()) {
                            try {
                                if (!mSensorsDataInstance.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_START)) {
                                    if (firstStart) {
                                        mFirstStart.commit(false);
                                    }
                                    JSONObject properties = new JSONObject();
                                    properties.put("$resume_from_background", resumeFromBackground);
                                    properties.put("$is_first_time", firstStart);
                                    SensorsDataUtils.getScreenNameAndTitleFromActivity(properties, activity);

                                    mSensorsDataInstance.track("$AppStart", properties);
                                }

                                if (!mSensorsDataInstance.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_END)) {
                                    mSensorsDataInstance.trackTimer("$AppEnd", TimeUnit.SECONDS);
                                }
                            } catch (Exception e) {
                                SALog.i(TAG, e);
                            }
                        }

                        if (resumeFromBackground) {
                            try {
                                HeatMapService.getInstance().resume();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        // 下次启动时，从后台恢复
                        resumeFromBackground = true;
                    }
                }

                startedActivityCount = startedActivityCount + 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        try {
            boolean mShowAutoTrack = true;
            if (mSensorsDataInstance.isActivityAutoTrackAppViewScreenIgnored(activity.getClass())) {
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
                        SensorsDataAutoTrackAppViewScreenUrl autoTrackAppViewScreenUrl = activity.getClass().getAnnotation(SensorsDataAutoTrackAppViewScreenUrl.class);
                        if (autoTrackAppViewScreenUrl != null) {
                            String screenUrl = autoTrackAppViewScreenUrl.url();
                            if (TextUtils.isEmpty(screenUrl)) {
                                screenUrl = activity.getClass().getCanonicalName();
                            }
                            mSensorsDataInstance.trackViewScreen(screenUrl, properties);
                        } else {
                            mSensorsDataInstance.track("$AppViewScreen", properties);
                        }
                    }
                } catch (Exception e) {
                    SALog.i(TAG, e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
        try {
            synchronized (mActivityLifecycleCallbacksLock) {
                startedActivityCount = startedActivityCount - 1;

                if (startedActivityCount == 0) {
                    try {
                        HeatMapService.getInstance().stop();
                        mSensorsDataInstance.appEnterBackground();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (SensorsDataUtils.isMainProcess(activity, mMainProcessName)) {
                        if (mSensorsDataInstance.isAutoTrackEnabled()) {
                            try {
                                if (!mSensorsDataInstance.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_END)) {
                                    JSONObject properties = new JSONObject();
                                    SensorsDataUtils.getScreenNameAndTitleFromActivity(properties, activity);
                                    mSensorsDataInstance.clearLastScreenUrl();
                                    mSensorsDataInstance.track("$AppEnd", properties);
                                }
                            } catch (Exception e) {
                                SALog.i(TAG, e);
                            }
                        }
                    }
                    try {
                        mSensorsDataInstance.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    private void showOpenHeatMapDialog(final Activity context, final String featureCode, final String postUrl) {
        try {
            if (!SensorsDataAPI.sharedInstance().isAppHeatMapConfirmDialogEnabled()) {
                HeatMapService.getInstance().start(context, featureCode, postUrl);
                return;
            }

            boolean isWifi = false;
            try {
                String networkType = SensorsDataUtils.networkType(context);
                if (networkType.equals("WIFI")) {
                    isWifi = true;
                }
            } catch (Exception e) {

            }

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("提示");
            if (isWifi) {
                builder.setMessage("正在连接 APP 点击分析");
            } else {
                builder.setMessage("正在连接 APP 点击分析，建议在 WiFi 环境下使用");
            }
            builder.setCancelable(false);
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.setPositiveButton("继续", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    HeatMapService.getInstance().start(context, featureCode, postUrl);
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();

            try {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
