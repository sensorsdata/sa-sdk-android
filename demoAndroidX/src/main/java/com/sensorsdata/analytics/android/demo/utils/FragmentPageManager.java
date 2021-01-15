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

package com.sensorsdata.analytics.android.demo.utils;

import android.app.Activity;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;

import com.sensorsdata.analytics.android.demo.observer.AppActivityLifecycleCallbacks;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

public class FragmentPageManager {

    private static final String TAG = "FragmentPageManager";
    private static final HandlerThread mFragmentPageHandlerThread = new HandlerThread("FragmentPageThread");
    private static final Object mHandlerLock = new Object();
    private static String mTagPage;
    private static volatile FragmentPageManager instance;
    // 缓存每个页面的 FragmentPageCalcultor
    private static Map<WeakReference<View>, FragmentPageCalcultor> mFragmentPageCalcultor = new LinkedHashMap();
    private static Handler mFragmentPageHandler = null;
    private static Runnable mFrgRunable = new Runnable() {
        public void run() {
            FragmentPageManager.getInstance().taskOn();
        }
    };

    private static FragmentPageManager getInstance() {
        if (instance == null) {
            synchronized (FragmentPageManager.class) {
                if (instance == null) {
                    instance = new FragmentPageManager();
                }
            }
        }
        return instance;
    }

    /**
     * 清除 FragmentPageCalcultor 对象
     */
    private static void clearFragmentPageCalcultor() {
        mFragmentPageCalcultor.clear();
    }

    private static Handler getFragmentPagHandler() {
        Handler handler;
        synchronized (mHandlerLock) {
            if (mFragmentPageHandler == null) {
                mFragmentPageHandlerThread.start();
                mFragmentPageHandler = new Handler(mFragmentPageHandlerThread.getLooper());
            }
            handler = mFragmentPageHandler;
        }
        return handler;
    }

    /**
     * ViewTreeObserver 时，延迟 250ms 保存 Fragment
     */
    public static void saveFragmentPageOnViewTreeObserver() {
        getFragmentPagHandler().removeCallbacks(mFrgRunable);
        getFragmentPagHandler().postDelayed(mFrgRunable, 250);
    }

    /**
     * onPause 时 clear Calcultor
     */
    public static void cleanFragmentPageCalcultorOnPause() {
        Log.e(TAG, "cleanFragmentPageCalcultorOnPause:" + mTagPage);
        clearFragmentPageCalcultor();
        getFragmentPagHandler().removeCallbacks(mFrgRunable);
    }

    /**
     * 开始 task
     */
    private void taskOn() {
        Activity activity = AppActivityLifecycleCallbacks.getResumedActivity();
        if (activity != null) {
            mTagPage = activity.getClass().getSimpleName();
            View[] decorView = new View[]{activity.getWindow().getDecorView()};
            for (View root : decorView) {
                FragmentPageCalcultor fragmentPageCalcultor = getCurrentCalcultor(root, activity);
                // 遍历
                fragmentPageCalcultor.traverseViewTree();
            }

        }
    }

    /**
     * 获取当前 Activity 的 calcultor
     */
    private FragmentPageCalcultor getCurrentCalcultor(View root, Activity activity) {
        for (WeakReference viewReference : mFragmentPageCalcultor.keySet()) {
            if (viewReference.get() == root) {
                return (FragmentPageCalcultor) mFragmentPageCalcultor.get(viewReference);
            }
        }
        FragmentPageCalcultor fragmentPageCalcultor = new FragmentPageCalcultor(root, activity.getClass().getSimpleName(), System.currentTimeMillis());
        mFragmentPageCalcultor.put(new WeakReference(root), fragmentPageCalcultor);
        return fragmentPageCalcultor;
    }
}