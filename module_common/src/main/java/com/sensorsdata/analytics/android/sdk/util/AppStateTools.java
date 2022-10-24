/*
 * Created by dengshiwei on 2022/07/05.
 * Copyright 2015－2022 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.dialog.SensorsDataDialogUtils;
import com.sensorsdata.analytics.android.sdk.monitor.SensorsDataActivityLifecycleCallbacks;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class AppStateTools implements SensorsDataActivityLifecycleCallbacks.SAActivityLifecycleCallbacks {
    private static final String TAG = "AppStateTools";
    // current front activity
    private WeakReference<Activity> mForeGroundActivity = new WeakReference<>(null);
    // current front fragment
    private String mCurrentFragmentName = null;
    private int mCurrentRootWindowsHashCode = -1;
    private int mActivityCount = 0;
    private final List<AppState> mAppStateList = new ArrayList<>();

    public static AppStateTools getInstance() {
        return SingleHolder.mSingleInstance;
    }

    @Override
    public void onNewIntent(Intent intent) {

    }

    private static class SingleHolder {
        private static final AppStateTools mSingleInstance = new AppStateTools();
    }

    private AppStateTools() {
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        try {
            setForegroundActivity(activity);
            if (!activity.isChild()) {
                mCurrentRootWindowsHashCode = -1;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }

        try {
            if (!SensorsDataDialogUtils.isSchemeActivity(activity)) {
                SensorsDataUtils.handleSchemeUrl(activity, activity.getIntent());
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (mActivityCount++ == 0) {
            for (AppState appState : mAppStateList) {
                try {
                    appState.onForeground();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        setForegroundActivity(activity);
        View decorView = null;
        try {
            Window window = activity.getWindow();
            if (window != null) {
                decorView = window.getDecorView();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        if (!activity.isChild()) {
            if (decorView != null) {
                mCurrentRootWindowsHashCode = decorView.hashCode();
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (!activity.isChild()) {
            mCurrentRootWindowsHashCode = -1;
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        mActivityCount--;
        if (mActivityCount == 0) {
            for (AppState appState : mAppStateList) {
                try {
                    appState.onBackground();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    public void delayInit(Context context) {
        try {
            // delay init state count =1
            if (context instanceof Activity) {
                onActivityStarted((Activity) context);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public Activity getForegroundActivity() {
        return this.mForeGroundActivity.get();
    }

    private void setForegroundActivity(Activity activity) {
        this.mForeGroundActivity = new WeakReference(activity);
    }

    public void setFragmentScreenName(Object fragment, String fragmentScreenName) {
        try {
            Method getParentFragmentMethod = fragment.getClass().getMethod("getParentFragment");
            Object parentFragment = getParentFragmentMethod.invoke(fragment);
            // 如果存在 fragment 多层嵌套场景，只取父 fragment
            if (parentFragment == null) {
                mCurrentFragmentName = fragmentScreenName;
                SALog.i(TAG, "setFragmentScreenName | " + fragmentScreenName + " is not nested fragment and set");
            } else {
                SALog.i(TAG, "setFragmentScreenName | " + fragmentScreenName + " is nested fragment and ignored");
            }
        } catch (Exception e) {
            //ignored
        }
    }

    public void addAppStateListener(AppState appState) {
        mAppStateList.add(appState);
    }

    public boolean isAppOnForeground() {
        return mActivityCount != 0;
    }

    public String getFragmentScreenName() {
        return mCurrentFragmentName;
    }

    public int getCurrentRootWindowsHashCode() {
        if (this.mCurrentRootWindowsHashCode == -1 && this.mForeGroundActivity != null && this.mForeGroundActivity.get() != null) {
            Activity activity = this.mForeGroundActivity.get();
            if (activity != null) {
                Window window = activity.getWindow();
                if (window != null && window.isActive()) {
                    this.mCurrentRootWindowsHashCode = window.getDecorView().hashCode();
                }
            }
        }
        return this.mCurrentRootWindowsHashCode;
    }

    public interface AppState {
        void onForeground();

        void onBackground();
    }
}
