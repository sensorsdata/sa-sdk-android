/*
 * Created by zhangxiangwei on 2019/12/19.
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import com.sensorsdata.analytics.android.sdk.visual.ViewTreeStatusObservable;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

@SuppressLint("NewApi")
public class AppStateManager implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "AppStateManager";
    private volatile static AppStateManager mSingleton = null;

    private AppStateManager() {
    }

    private WeakReference<Activity> mForeGroundActivity = new WeakReference((Object) null);
    private int mCurrentRootWindowsHashCode = -1;
    private String mCurrentFragmentName = null;

    public static AppStateManager getInstance() {
        if (mSingleton == null) {
            synchronized (AppStateManager.class) {
                if (mSingleton == null) {
                    mSingleton = new AppStateManager();
                }
            }
        }
        return mSingleton;
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
            if (getParentFragmentMethod != null) {
                Object parentFragment = getParentFragmentMethod.invoke(fragment);
                // 如果存在 fragment 多层嵌套场景，只取父 fragment
                if (parentFragment == null) {
                    mCurrentFragmentName = fragmentScreenName;
                    SALog.i(TAG, "setFragmentScreenName | " + fragmentScreenName + " is not nested fragment and set");
                } else {
                    SALog.i(TAG, "setFragmentScreenName | " + fragmentScreenName + " is nested fragment and ignored");
                }
            }
        } catch (Exception e) {
            //ignored
        }
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

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        setForegroundActivity(activity);
        if (!activity.isChild()) {
            mCurrentRootWindowsHashCode = -1;
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        setForegroundActivity(activity);
        Window window = activity.getWindow();
        View decorView = null;
        if (window != null && window.isActive()) {
            decorView = window.getDecorView();
        }
        if (SensorsDataAPI.sharedInstance().isVisualizedAutoTrackEnabled() && Build.VERSION.SDK_INT >= 16) {
            if (decorView != null) {
                monitorViewTreeChange(decorView);
            }
        }
        if (!activity.isChild()) {
            if (decorView != null) {
                mCurrentRootWindowsHashCode = decorView.hashCode();
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (Build.VERSION.SDK_INT >= 16) {
            Window window = activity.getWindow();
            if (window != null && window.isActive()) {
                unRegisterViewTreeChange(window.getDecorView());
            }
        }
        if (!activity.isChild()) {
            mCurrentRootWindowsHashCode = -1;
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        ViewTreeStatusObservable.getInstance().clearWebViewCache();
    }

    private void unRegisterViewTreeChange(View root) {
        try {
            if (root.getTag(R.id.sensors_analytics_tag_view_tree_observer_listeners) != null) {
                ViewTreeStatusObservable observable = ViewTreeStatusObservable.getInstance();
                if (Build.VERSION.SDK_INT < 16) {
                    root.getViewTreeObserver().removeGlobalOnLayoutListener(observable);
                } else {
                    root.getViewTreeObserver().removeOnGlobalLayoutListener(observable);
                }
                root.getViewTreeObserver().removeOnGlobalFocusChangeListener(observable);
                root.getViewTreeObserver().removeOnScrollChangedListener(observable);
                root.setTag(R.id.sensors_analytics_tag_view_tree_observer_listeners, null);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private void monitorViewTreeChange(View root) {
        try {
            if (root.getTag(R.id.sensors_analytics_tag_view_tree_observer_listeners) == null) {
                root.getViewTreeObserver().addOnGlobalLayoutListener(ViewTreeStatusObservable.getInstance());
                root.getViewTreeObserver().addOnScrollChangedListener(ViewTreeStatusObservable.getInstance());
                root.getViewTreeObserver().addOnGlobalFocusChangeListener(ViewTreeStatusObservable.getInstance());
                root.setTag(R.id.sensors_analytics_tag_view_tree_observer_listeners, true);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
