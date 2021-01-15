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

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.ViewParent;

import androidx.annotation.RequiresApi;

import java.lang.reflect.Method;

import static android.view.View.VISIBLE;

public class FrgUtil {

    private static Class<?> supportFragmentClass = null;
    private static Class<?> fragment = null;

    /**
     * Fragment 是否可见
     *
     * @return true 表示可见。
     * （ 当 fragment.isVisible 且 fragment.getView view 自身可见且所有的 ViewParent 可见，且所有 getParentFragment 也要符合此条件，返回 true ）
     */
    @TargetApi(11)
    public static boolean isVisible(Object fragment) {
        //校验 Fragment
        if (!isFragment(fragment)) {
            return false;
        }

        if (Build.VERSION.SDK_INT < 17) {
            try {
                Method isVisible = fragment.getClass().getMethod("isVisible");
                return (boolean) isVisible.invoke(fragment);
            } catch (Exception e) {
                //
            }
        }
        for (Object current = fragment; current != null; current = getParentFragment(current)) {
            try {
                Method getView = current.getClass().getMethod("getView");
                View view = (View) getView.invoke(current);
                Method isVisible = current.getClass().getMethod("isVisible");
                if (!(boolean) isVisible.invoke(current) || !viewVisibilityInParents(view)) {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private static Object getParentFragment(Object current) {
        Object parentFragment = null;
        try {
            Method getParentFragmentMethod = current.getClass().getMethod("getParentFragment");
            parentFragment = getParentFragmentMethod.invoke(current);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return parentFragment;
    }

    /**
     * 当前 View 在父容器中是否可见
     *
     * @return true 表示可见
     * （当 View 自身可见，且所有的 ViewParent 可见时，返回true）
     */
    @RequiresApi(api = 11)
    private static boolean viewVisibilityInParents(View view) {
        if (view == null) {
            return false;
        }
        if (!isViewSelfVisible(view)) {
            return false;
        }
        ViewParent viewParent = view.getParent();
        while (viewParent instanceof View) {
            if (!isViewSelfVisible((View) viewParent)) {
                return false;
            }
            viewParent = viewParent.getParent();
            if (viewParent == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * View 自身是否可见
     *
     * @return DecorView 时返回 true
     * View 宽、高、透明度 有一个 < 0 时，或 getLocalVisibleRect 为 false 时；返回 false 。
     * View getVisibility 不可见，且有 Animation getFillAfter 为  false 时；返回 false 。
     * View 无 Animation 时 getVisibility 不可见时返回 false 。
     */
    @RequiresApi(api = 11)
    private static boolean isViewSelfVisible(View mView) {
        if (mView == null) {
            return false;
        }
        if (mView.getWidth() <= 0 || mView.getHeight() <= 0 || mView.getAlpha() <= 0.0f || !mView.getLocalVisibleRect(new Rect())) {
            return false;
        }
        return (mView.getVisibility() != VISIBLE && mView.getAnimation() != null && mView.getAnimation().getFillAfter()) || mView.getVisibility() == VISIBLE;
    }

    private static boolean isFragment(Object object) {
        try {
            if (fragment == null) {
                try {
                    fragment = Class.forName("android.app.Fragment");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (supportFragmentClass == null) {
                try {
                    supportFragmentClass = Class.forName("android.support.v4.app.Fragment");
                } catch (Exception e) {
                    try {
                        supportFragmentClass = Class.forName("androidx.fragment.app.Fragment");
                    } catch (Exception e2) {
                        e.printStackTrace();
                    }
                }
            }

            if (supportFragmentClass == null && fragment == null) {
                return false;
            }

            if ((supportFragmentClass != null && supportFragmentClass.isInstance(object)) ||
                    (fragment != null && fragment.isInstance(object))) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}