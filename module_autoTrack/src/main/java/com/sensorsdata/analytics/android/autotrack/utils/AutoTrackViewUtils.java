/*
 * Created by dengshiwei on 2022/07/06.
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

package com.sensorsdata.analytics.android.autotrack.utils;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TabHost;

import com.sensorsdata.analytics.android.autotrack.core.beans.ViewContext;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.util.ReflectUtil;
import com.sensorsdata.analytics.android.sdk.util.SAFragmentUtils;
import com.sensorsdata.analytics.android.sdk.util.SAViewUtils;
import com.sensorsdata.analytics.android.sdk.util.WindowHelper;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.List;

public class AutoTrackViewUtils {

    /**
     * get Activity or Fragment of TabLayout
     */
    public static ViewContext getTabLayoutContext(Object objectTab, View tabLayoutView) {
        try {
            Object fragment = null;
            Activity activity = null;
            if (objectTab instanceof Context) {
                activity = SAViewUtils.getActivityOfView((Context) objectTab, null);
            } else {
                Field[] fields = objectTab.getClass().getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    Object bridgeObject = field.get(objectTab);
                    if (bridgeObject instanceof Activity) {
                        activity = (Activity) bridgeObject;
                        break;
                    } else if (SAFragmentUtils.isFragment(bridgeObject)) {
                        fragment = bridgeObject;
                        break;
                    } else if (bridgeObject instanceof View) {
                        View view = (View) bridgeObject;
                        activity = SAViewUtils.getActivityOfView(view.getContext(), null);
                        break;
                    }
                }
            }

            if (tabLayoutView != null) {
                if (activity == null) {
                    activity = SAViewUtils.getActivityOfView(tabLayoutView.getContext(), null);
                }

                if (fragment == null) {
                    // get fragment
                    fragment = SAFragmentUtils.getFragmentFromView(tabLayoutView, activity);
                }
            }

            if (activity == null && fragment != null) {
                activity = SAFragmentUtils.getActivityFromFragment(fragment);
            }
            return new ViewContext(activity, fragment, tabLayoutView);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    public static View getTabView(String tabName) {
        // 2020/4/27 新增  1. 解决 TabHost 点击取不到 element_content 2. 可视化增加 $element_path
        View view = WindowHelper.getClickView(tabName);
        if (view != null) {
            //循环向上获取 tabHostView
            View currentView = view;
            View tabHostView = null;
            while (null == tabHostView && null != currentView && null != currentView.getParent()) {
                currentView = (View) currentView.getParent();
                if (currentView instanceof TabHost) {
                    tabHostView = currentView;
                }
            }
            //tabHostView 的忽略判断
            if (null != tabHostView && SAViewUtils.isViewIgnored(tabHostView)) {
                return null;
            }
        }
        return view;
    }

    public static String getTabLayoutText(View view, Object tab) {
        String viewText = null;
        try {
            if (view != null) {
                if (view instanceof ViewGroup) {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = SAViewUtils.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } else {
                    viewText = SAViewUtils.getViewContent(view);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }

        if (TextUtils.isEmpty(viewText)) {
            Object obj_text = ReflectUtil.callMethod(tab, "getText");
            if (obj_text != null) {
                viewText = obj_text.toString();
            }
        }
        return viewText;
    }

    public static View getTabLayout(Object tab) {
        View view = getAndroidXTabLayout(tab);
        if (view == null) {
            view = getSupportTabLayout(tab);
        }
        return view;
    }

    public static boolean isBindViewPager(View tabLayout) {
        if (tabLayout == null) {
            return false;
        }
        View viewPager = ReflectUtil.findField(tabLayout.getClass(), tabLayout, "viewPager");
        if (viewPager != null) {
            return true;
        } else {
            View tempView = findTabLayout(tabLayout);
            return tabLayout == tempView;
        }
    }

    private static View getSupportTabLayout(Object tab) {
        View view = null;
        try {
            Class<?> supportTabLayoutCLass = Class.forName("android.support.design.widget.TabLayout");
            // TabLayout is ignored
            if (SAViewUtils.isViewIgnored(supportTabLayoutCLass)) {
                return null;
            }

            //反射获取 TabLayout，进行 view 忽略判断。
            if (ReflectUtil.isInstance(tab, "android.support.design.widget.TabLayout$Tab")) {
                view = ReflectUtil.findField(new String[]{"android.support.design.widget.TabLayout$Tab"}, tab, "mParent", "parent");
                if (null != view && ReflectUtil.isInstance(view, "android.support.design.widget.TabLayout")
                        && SAViewUtils.isViewIgnored(view)) {
                    return null;
                }
            }
        } catch (Exception e) {
            //ignored
        }
        return view;
    }

    private static View getAndroidXTabLayout(Object tab) {
        View view = null;
        try {
            Class<?> androidXTabLayoutCLass = Class.forName("com.google.android.material.tabs.TabLayout");
            if (SAViewUtils.isViewIgnored(androidXTabLayoutCLass)) {
                return null;
            }
            //反射获取 TabLayout，进行 view 忽略判断。
            if (ReflectUtil.isInstance(tab, "com.google.android.material.tabs.TabLayout$Tab")) {
                view = ReflectUtil.findField(new String[]{"com.google.android.material.tabs.TabLayout$Tab"}, tab, "parent");
                if (null != view && ReflectUtil.isInstance(view, "com.google.android.material.tabs.TabLayout")
                        && SAViewUtils.isViewIgnored(view)) {
                    return null;
                }
            }
        } catch (Exception e) {
            //ignored
        }

        return view;
    }

    private static View findTabLayout(View view) {
        ViewParent parentView = view.getParent();
        if (parentView instanceof ViewGroup) {
            for (int count = 0; count < ((ViewGroup) parentView).getChildCount(); count++) {
                View v = ((ViewGroup) parentView).getChildAt(count);
                if ("ViewPager".equals(v.getClass().getSimpleName())) {
                    View tempTabLayout = findTabLayoutView(v);
                    if (tempTabLayout != null) {
                        return tempTabLayout;
                    }
                    break;
                }
            }
        }
        if (parentView.getParent() == null) {
            return null;
        }
        return findTabLayout((View) parentView);
    }

    private static View findTabLayoutView(View view) {
        List<?> pageChangeListeners = ReflectUtil.findField(view.getClass(), view, "mOnPageChangeListeners");
        if (pageChangeListeners != null && pageChangeListeners.size() > 0) {
            for (Object listener : pageChangeListeners) {
                WeakReference<View> weakReference = ReflectUtil.findField(listener.getClass(), listener, "tabLayoutRef");
                if (weakReference != null && weakReference.get() != null) {
                    return weakReference.get();
                }
            }
        }
        return null;
    }
}
