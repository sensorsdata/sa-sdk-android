/*
 * Created by zhangxiangwei on 2019/12/19.
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

package com.sensorsdata.analytics.android.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import java.lang.ref.WeakReference;

@SuppressLint("NewApi")
public class AppStateManager implements Application.ActivityLifecycleCallbacks {

    private volatile static AppStateManager mSingleton = null;

    private int mActivityCount;

    private AppStateManager() {
    }

    private WeakReference<Activity> mForeGroundActivity = new WeakReference((Object) null);
    private int mCurrentRootWindowsHashCode = -1;

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

    public boolean isInBackground() {
        return mActivityCount <= 0;
    }

    public int getCurrentRootWindowsHashCode() {
        if (this.mCurrentRootWindowsHashCode == -1 && this.mForeGroundActivity != null && this.mForeGroundActivity.get() != null) {
            this.mCurrentRootWindowsHashCode = (this.mForeGroundActivity.get()).getWindow().getDecorView().hashCode();
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
        mActivityCount++;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        setForegroundActivity(activity);
        if (!activity.isChild()) {
            mCurrentRootWindowsHashCode = activity.getWindow().getDecorView().hashCode();
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
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
