/*
 * Created by chenru on 2019/06/20.
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

package com.sensorsdata.analytics.android.demo.observer;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewTreeObserver;

import com.sensorsdata.analytics.android.demo.utils.FragmentPageManager;

import java.lang.ref.WeakReference;

public class AppActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

    private static final String TAG_ACTIVITY = "nice Activity ---> :";//过滤关键字 nice

    private static WeakReference<Activity> mResumeActivity = new WeakReference(null);

    /**
     * 返回当前 resume 的 Activity
     */
    public static Activity getResumedActivity() {
        return (Activity) mResumeActivity.get();
    }

    private void setResumeActivity(Activity activity) {
        mResumeActivity = new WeakReference(activity);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        Log.i(TAG_ACTIVITY, activity.getClass().getSimpleName() + ": onActivityCreated");
    }

    @Override
    public void onActivityStarted(Activity activity) {
        Log.i(TAG_ACTIVITY, activity.getClass().getSimpleName() + ": onActivityStarted");
    }

    @Override
    public void onActivityResumed(Activity activity) {
        Log.i(TAG_ACTIVITY, activity.getClass().getSimpleName() + ": onActivityResumed");
        setResumeActivity(activity);
        registerViewTreeObserver(activity);
    }


    @Override
    public void onActivityPaused(Activity activity) {
        Log.i(TAG_ACTIVITY, activity.getClass().getSimpleName() + ": onActivityPaused");
        setResumeActivity(null);
        unRegisterViewTreeObserver(activity);

        // Fragment 页面
        FragmentPageManager.cleanFragmentPageCalcultorOnPause();
    }

    @Override
    public void onActivityStopped(Activity activity) {
        Log.i(TAG_ACTIVITY, activity.getClass().getSimpleName() + ": onActivityStopped");
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        Log.i(TAG_ACTIVITY, activity.getClass().getSimpleName() + ": onActivitySaveInstanceState");
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Log.i(TAG_ACTIVITY, activity.getClass().getSimpleName() + ": onActivityDestroyed");
    }

    /**
     * 注册 ViewTreeObserver
     */
    private void registerViewTreeObserver(Activity activity) {
        ViewTreeObserver viewTreeObserver = activity.getWindow().getDecorView().getViewTreeObserver();
        viewTreeObserver.addOnGlobalFocusChangeListener(AppViewTreeObserver.getInstance());
        viewTreeObserver.addOnGlobalLayoutListener(AppViewTreeObserver.getInstance());
        viewTreeObserver.addOnScrollChangedListener(AppViewTreeObserver.getInstance());
    }

    /**
     * 取消注册 ViewTreeObserver
     */
    private void unRegisterViewTreeObserver(Activity activity) {
        ViewTreeObserver viewTreeObserver = activity.getWindow().getDecorView().getViewTreeObserver();
        viewTreeObserver.removeOnGlobalFocusChangeListener(AppViewTreeObserver.getInstance());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            viewTreeObserver.removeOnGlobalLayoutListener(AppViewTreeObserver.getInstance());
        }
        viewTreeObserver.removeOnScrollChangedListener(AppViewTreeObserver.getInstance());
    }
}
