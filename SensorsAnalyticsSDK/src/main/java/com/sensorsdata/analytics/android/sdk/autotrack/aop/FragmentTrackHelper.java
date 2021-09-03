/*
 * Created by dengshiwei on 2021/07/30.
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

package com.sensorsdata.analytics.android.sdk.autotrack.aop;

import android.os.Bundle;
import android.view.View;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.autotrack.SAFragmentLifecycleCallbacks;
import com.sensorsdata.analytics.android.sdk.util.SAFragmentUtils;

import java.util.HashSet;
import java.util.Set;

public class FragmentTrackHelper {
    // Fragment 的回调监听
    private static final Set<SAFragmentLifecycleCallbacks> FRAGMENT_CALLBACKS = new HashSet<>();

    /**
     * 插件 Hook 处理 Fragment 的 onViewCreated 生命周期
     *
     * @param object Fragment
     * @param rootView View
     * @param bundle Bundle
     */
    public static void onFragmentViewCreated(Object object, View rootView, Bundle bundle) {
        if (!SAFragmentUtils.isFragment(object)) {
            return;
        }
        for (SAFragmentLifecycleCallbacks fragmentCallbacks : FRAGMENT_CALLBACKS) {
            try {
                fragmentCallbacks.onViewCreated(object, rootView, bundle);
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    /**
     * 插件 Hook 处理 Fragment 的 onResume 生命周期
     *
     * @param object Fragment
     */
    public static void trackFragmentResume(Object object) {
        if (!SAFragmentUtils.isFragment(object)) {
            return;
        }
        for (SAFragmentLifecycleCallbacks fragmentCallbacks : FRAGMENT_CALLBACKS) {
            try {
                fragmentCallbacks.onResume(object);
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    /**
     * 插件 Hook 处理 Fragment 的 onPause 生命周期
     *
     * @param object Fragment
     */
    public static void trackFragmentPause(Object object) {
        if (!SAFragmentUtils.isFragment(object)) {
            return;
        }
        for (SAFragmentLifecycleCallbacks fragmentCallbacks : FRAGMENT_CALLBACKS) {
            try {
                fragmentCallbacks.onPause(object);
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    /**
     * 插件 Hook 处理 Fragment 的 setUserVisibleHint 回调
     *
     * @param object Fragment
     * @param isVisibleToUser 是否可见
     */
    public static void trackFragmentSetUserVisibleHint(Object object, boolean isVisibleToUser) {
        if (!SAFragmentUtils.isFragment(object)) {
            return;
        }
        for (SAFragmentLifecycleCallbacks fragmentCallbacks : FRAGMENT_CALLBACKS) {
            try {
                fragmentCallbacks.setUserVisibleHint(object, isVisibleToUser);
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    /**
     * 插件 Hook 处理 Fragment 的 onHiddenChanged 回调
     *
     * @param object Fragment
     * @param hidden Fragment 是否隐藏
     */
    public static void trackOnHiddenChanged(Object object, boolean hidden) {
        if (!SAFragmentUtils.isFragment(object)) {
            return;
        }
        for (SAFragmentLifecycleCallbacks fragmentCallbacks : FRAGMENT_CALLBACKS) {
            try {
                fragmentCallbacks.onHiddenChanged(object, hidden);
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    /**
     * 添加 Fragment 的回调监听
     *
     * @param fragmentLifecycleCallbacks SAFragmentLifecycleCallbacks
     */
    public static void addFragmentCallbacks(SAFragmentLifecycleCallbacks fragmentLifecycleCallbacks) {
        FRAGMENT_CALLBACKS.add(fragmentLifecycleCallbacks);
    }

    /**
     * 移除指定的 Fragment 的回调监听
     *
     * @param fragmentLifecycleCallbacks SAFragmentLifecycleCallbacks
     */
    public static void removeFragmentCallbacks(SAFragmentLifecycleCallbacks fragmentLifecycleCallbacks) {
        FRAGMENT_CALLBACKS.remove(fragmentLifecycleCallbacks);
    }
}
