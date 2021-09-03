/*
 * Created by dengshiwei on 2021/07/31.
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

package com.sensorsdata.analytics.android.sdk.util;

import com.sensorsdata.analytics.android.sdk.SALog;

import java.lang.reflect.Method;

public class SAFragmentUtils {

    /**
     * Fragment 是否可见
     *
     * @param fragment Fragment
     * @return true 可见，false 不可见
     */
    public static boolean isFragmentVisible(Object fragment) {
        Object parentFragment = null;
        try {
            Method getParentFragmentMethod = fragment.getClass().getMethod("getParentFragment");
            parentFragment = getParentFragmentMethod.invoke(fragment);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }

        try {
            if (parentFragment == null) {
                if (!fragmentIsHidden(fragment) && fragmentGetUserVisibleHint(fragment) && fragmentIsResumed(fragment)) {
                    return true;
                }
            } else {
                if (!fragmentIsHidden(fragment) && fragmentGetUserVisibleHint(fragment) && fragmentIsResumed(fragment) &&
                        !fragmentIsHidden(parentFragment) && fragmentGetUserVisibleHint(parentFragment) && fragmentIsResumed(parentFragment)) {
                    return true;
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return false;
    }

    /**
     * 判断 Fragment 的 visible 属性
     *
     * @param fragment Fragment
     * @return true 可见，false 不可见
     */
    public static boolean fragmentGetUserVisibleHint(Object fragment) {
        try {
            Method getUserVisibleHintMethod = fragment.getClass().getMethod("getUserVisibleHint");
            return (boolean) getUserVisibleHintMethod.invoke(fragment);
        } catch (Exception e) {
            //ignored
        }
        return false;
    }

    /**
     * 判断 Fragment 是否隐藏
     *
     * @param fragment Fragment
     * @return true 隐藏，false 不隐藏
     */
    public static boolean fragmentIsHidden(Object fragment) {
        try {
            Method isHiddenMethod = fragment.getClass().getMethod("isHidden");
            return (boolean) isHiddenMethod.invoke(fragment);
        } catch (Exception e) {
            //ignored
        }
        return false;
    }

    /**
     * 判断 Object 是否是 Fragment
     *
     * @param object Object
     * @return true 是，false 不是
     */
    public static boolean isFragment(Object object) {
        try {
            if (object == null) {
                return false;
            }
            Class<?> supportFragmentClass = null;
            Class<?> androidXFragmentClass = null;
            Class<?> fragment = null;
            try {
                fragment = Class.forName("android.app.Fragment");
            } catch (Exception e) {
                //ignored
            }
            try {
                supportFragmentClass = Class.forName("android.support.v4.app.Fragment");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXFragmentClass = Class.forName("androidx.fragment.app.Fragment");
            } catch (Exception e) {
                //ignored
            }

            if (supportFragmentClass == null && androidXFragmentClass == null && fragment == null) {
                return false;
            }

            if ((supportFragmentClass != null && supportFragmentClass.isInstance(object)) ||
                    (androidXFragmentClass != null && androidXFragmentClass.isInstance(object)) ||
                    (fragment != null && fragment.isInstance(object))) {
                return true;
            }
        } catch (Exception e) {
            //ignored
        }
        return false;
    }

    /**
     * 判断 Fragment 是否 resume
     *
     * @param fragment Fragment
     * @return true 是，false 不是
     */
    public static boolean fragmentIsResumed(Object fragment) {
        try {
            Method isResumedMethod = fragment.getClass().getMethod("isResumed");
            return (boolean) isResumedMethod.invoke(fragment);
        } catch (Exception e) {
            //ignored
        }
        return false;
    }
}
