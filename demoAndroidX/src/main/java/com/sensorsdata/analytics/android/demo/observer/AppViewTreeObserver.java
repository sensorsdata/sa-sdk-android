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

import android.util.Log;
import android.view.View;

import com.sensorsdata.analytics.android.demo.utils.FragmentPageManager;

public class AppViewTreeObserver implements android.view.ViewTreeObserver.OnGlobalLayoutListener, android.view.ViewTreeObserver.OnScrollChangedListener, android.view.ViewTreeObserver.OnGlobalFocusChangeListener {

    private static final String TAG_VTO = "nice ViewTreeOb ---> :";//过滤关键字 nice
    public static volatile AppViewTreeObserver appViewTreeObserver;

    public static AppViewTreeObserver getInstance() {
        if (appViewTreeObserver == null) {
            synchronized (AppViewTreeObserver.class) {
                if (appViewTreeObserver == null) {
                    appViewTreeObserver = new AppViewTreeObserver();
                }
            }
        }
        return appViewTreeObserver;
    }

    @Override
    public void onGlobalFocusChanged(View oldFocus, View newFocus) {
        Log.i(TAG_VTO, "onGlobalFocusChanged");
    }

    @Override
    public void onGlobalLayout() {
        Log.i(TAG_VTO, "onGlobalLayout");

        // Fragment 页面
        FragmentPageManager.saveFragmentPageOnViewTreeObserver();
    }

    @Override
    public void onScrollChanged() {
        Log.i(TAG_VTO, "onScrollChanged");
    }
}
