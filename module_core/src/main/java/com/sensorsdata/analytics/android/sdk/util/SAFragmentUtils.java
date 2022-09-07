/*
 * Created by dengshiwei on 2021/07/31.
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.LruCache;
import android.view.View;
import android.view.ViewParent;
import android.view.Window;

import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

public class SAFragmentUtils {
    @SuppressLint("NewApi")
    private static LruCache<String, WeakReference<Object>> sFragmentLruCache = new LruCache<>(Integer.MAX_VALUE);

    public static void setFragmentToCache(String fragmentName, Object object) {
        if (!TextUtils.isEmpty(fragmentName) && null != object && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            sFragmentLruCache.put(fragmentName, new WeakReference<>(object));
        }
    }

    public static Object getFragmentFromCache(String fragmentName) {
        try {
            if (!TextUtils.isEmpty(fragmentName)) {
                WeakReference<Object> weakReference = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
                    weakReference = sFragmentLruCache.get(fragmentName);
                }
                Object object;
                if (null != weakReference) {
                    object = weakReference.get();
                    if (null != object) {
                        return object;
                    }
                }
                object = Class.forName(fragmentName).newInstance();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                    sFragmentLruCache.put(fragmentName, new WeakReference<>(object));
                }
                return object;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

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
    /**
     * 获取点击 view 的 fragment 对象
     *
     * @param view 点击的 view
     * @return object 这里是 fragment 实例对象
     */
    public static Object getFragmentFromView(View view) {
        return getFragmentFromView(view, null);
    }

    /**
     * 根据 Fragment 获取对应的 Activity
     *
     * @param fragment，Fragment
     * @return Activity or null
     */
    public static Activity getActivityFromFragment(Object fragment) {
        Activity activity = null;
        if (Build.VERSION.SDK_INT >= 11) {
            try {
                Method getActivityMethod = fragment.getClass().getMethod("getActivity");
                activity = (Activity) getActivityMethod.invoke(fragment);
            } catch (Exception e) {
                //ignored
            }
        }
        return activity;
    }

    /**
     * 获取点击 view 的 fragment 对象
     *
     * @param view 点击的 view
     * @param activity Activity
     * @return object 这里是 fragment 实例对象
     */
    public static Object getFragmentFromView(View view, Activity activity) {
        try {
            if (view != null) {
                String fragmentName = (String) view.getTag(R.id.sensors_analytics_tag_view_fragment_name);
                String fragmentName2 = (String) view.getTag(R.id.sensors_analytics_tag_view_fragment_name2);
                if (!TextUtils.isEmpty(fragmentName2)) {
                    fragmentName = fragmentName2;
                }
                if (TextUtils.isEmpty(fragmentName)) {
                    if (activity == null) {
                        //获取所在的 Context
                        Context context = view.getContext();
                        //将 Context 转成 Activity
                        activity = SAViewUtils.getActivityOfView(context, view);
                    }
                    if (activity != null) {
                        Window window = activity.getWindow();
                        if (window != null && window.isActive()) {
                            Object tag = window.getDecorView().getRootView().getTag(R.id.sensors_analytics_tag_view_fragment_name);
                            if (tag != null) {
                                fragmentName = traverseParentViewTag(view);
                            }
                        }
                    }
                }
                return getFragmentFromCache(fragmentName);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    private static String traverseParentViewTag(View view) {
        try {
            ViewParent parentView = view.getParent();
            String fragmentName = null;
            while (TextUtils.isEmpty(fragmentName) && parentView instanceof View) {
                fragmentName = (String) ((View) parentView).getTag(R.id.sensors_analytics_tag_view_fragment_name);
                parentView = parentView.getParent();
            }
            return fragmentName;
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return "";
    }
}
