/*
 * Created by dengshiwei on 2022/07/01.
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

package com.sensorsdata.analytics.android.sdk.visual.utils;

import android.os.Build;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ExpandableListView;

import com.sensorsdata.analytics.android.sdk.util.AppStateTools;
import com.sensorsdata.analytics.android.sdk.util.ReflectUtil;
import com.sensorsdata.analytics.android.sdk.util.SAFragmentUtils;
import com.sensorsdata.analytics.android.sdk.util.SAViewUtils;
import com.sensorsdata.analytics.android.sdk.util.SnapCache;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;
import com.sensorsdata.analytics.android.sdk.util.WindowHelper;
import com.sensorsdata.analytics.android.sdk.visual.model.ViewNode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;


public class ViewUtil {
    private static boolean sHaveCustomRecyclerView = false;
    private static final boolean sHaveRecyclerView = haveRecyclerView();
    private static Method sRecyclerViewGetChildAdapterPositionMethod;
    private static Class<?> sRecyclerViewClass;
    private static final SparseArray<String> sViewCache = new SparseArray<>();

    private static boolean instanceOfSupportSwipeRefreshLayout(Object view) {
        return ReflectUtil.isInstance(view, "android.support.v4.widget.SwipeRefreshLayout", "androidx.swiperefreshlayout.widget.SwipeRefreshLayout");
    }

    private static boolean instanceOfSupportViewPager(Object view) {
        return ReflectUtil.isInstance(view, "android.support.v4.view.ViewPager");
    }

    private static boolean instanceOfAndroidXViewPager(Object view) {
        return ReflectUtil.isInstance(view, "androidx.viewpager.widget.ViewPager");
    }

    public static boolean instanceOfWebView(Object view) {
        return view instanceof WebView || instanceOfX5WebView(view) || instanceOfUCWebView(view);
    }

    public static boolean instanceOfX5WebView(Object view) {
        return ReflectUtil.isInstance(view, "com.tencent.smtt.sdk.WebView");
    }

    private static boolean instanceOfUCWebView(Object view) {
        return ReflectUtil.isInstance(view, "com.alipay.mobile.nebulauc.impl.UCWebView$WebViewEx");
    }

    public static boolean instanceOfRecyclerView(Object view) {
        boolean result = ReflectUtil.isInstance(view, "android.support.v7.widget.RecyclerView", "androidx.recyclerview.widget.RecyclerView");
        if (!result) {
            result = sHaveCustomRecyclerView && view != null && sRecyclerViewClass != null && sRecyclerViewClass.isAssignableFrom(view.getClass());
        }
        return result;
    }

    /**
     * 获取 class name 和 刷新自定义 RecyclerView 状态
     *
     * @param clazz 需要获取名字的类
     * @return 获取的类名字
     */
    private static String getCanonicalAndCheckCustomView(Class<?> clazz) {
        String name = SnapCache.getInstance().getCanonicalName(clazz);
        if (name != null) {
            checkCustomRecyclerView(clazz, name);
        }
        return name;
    }

    /**
     * view 是否为 Fragment 中的顶层 View
     */
    private static Object instanceOfFragmentRootView(View parentView, View childView) {
        Object parentFragment = SAFragmentUtils.getFragmentFromView(parentView);
        Object childFragment = SAFragmentUtils.getFragmentFromView(childView);
        if (parentFragment == null && childFragment != null) {
            return childFragment;
        }
        return null;
    }

    /**
     * position RecyclerView item
     */
    private static int getChildAdapterPositionInRecyclerView(View childView, ViewGroup parentView) {
        if (instanceOfRecyclerView(parentView)) {
            try {
                sRecyclerViewGetChildAdapterPositionMethod = parentView.getClass().getMethod("getChildAdapterPosition", new Class[]{View.class});
            } catch (NoSuchMethodException e) {
                //ignored
            }
            if (sRecyclerViewGetChildAdapterPositionMethod == null) {
                try {
                    sRecyclerViewGetChildAdapterPositionMethod = parentView.getClass().getMethod("getChildPosition", new Class[]{View.class});
                } catch (NoSuchMethodException e2) {
                    //ignored
                }
            }
            try {
                if (sRecyclerViewGetChildAdapterPositionMethod != null) {
                    Object object = sRecyclerViewGetChildAdapterPositionMethod.invoke(parentView, childView);
                    if (object != null) {
                        return (Integer) object;
                    }
                }
            } catch (IllegalAccessException e) {
                //ignored
            } catch (InvocationTargetException e) {
                //ignored
            }
        } else if (sHaveCustomRecyclerView) {
            return invokeCRVGetChildAdapterPositionMethod(parentView, childView);
        }
        return -1;
    }

    private static int getCurrentItem(View view) {
        try {
            Method method = view.getClass().getMethod("getCurrentItem");
            Object object = method.invoke(view);
            if (object != null) {
                return (Integer) object;
            }
        } catch (IllegalAccessException e) {
            //ignored
        } catch (InvocationTargetException e2) {
            //ignored
        } catch (NoSuchMethodException e) {
            //ignored
        }
        return -1;
    }

    private static boolean haveRecyclerView() {
        try {
            Class.forName("android.support.v7.widget.RecyclerView");
            return true;
        } catch (ClassNotFoundException th) {
            try {
                Class.forName("androidx.recyclerview.widget.RecyclerView");
                return true;
            } catch (ClassNotFoundException e2) {
                return false;
            }
        }
    }

    private static void checkCustomRecyclerView(Class<?> viewClass, String viewName) {
        if (!sHaveRecyclerView && !sHaveCustomRecyclerView && viewName != null && viewName.contains("RecyclerView")) {
            try {
                if (findRecyclerInSuper(viewClass) != null && sRecyclerViewGetChildAdapterPositionMethod != null) {
                    sRecyclerViewClass = viewClass;
                    sHaveCustomRecyclerView = true;
                }
            } catch (Exception e) {
                //ignored
            }
        }
    }

    private static Class<?> findRecyclerInSuper(Class<?> viewClass) {
        while (viewClass != null && !viewClass.equals(ViewGroup.class)) {
            try {
                sRecyclerViewGetChildAdapterPositionMethod = viewClass.getMethod("getChildAdapterPosition", new Class[]{View.class});
            } catch (NoSuchMethodException e) {
                //ignored
            }
            if (sRecyclerViewGetChildAdapterPositionMethod == null) {
                try {
                    sRecyclerViewGetChildAdapterPositionMethod = viewClass.getMethod("getChildPosition", new Class[]{View.class});
                } catch (NoSuchMethodException e2) {
                    //ignored
                }
            }
            if (sRecyclerViewGetChildAdapterPositionMethod != null) {
                return viewClass;
            }
            viewClass = viewClass.getSuperclass();
        }
        return null;
    }

    private static int invokeCRVGetChildAdapterPositionMethod(View customRecyclerView, View childView) {
        try {
            if (customRecyclerView.getClass() == sRecyclerViewClass) {
                return (Integer) sRecyclerViewGetChildAdapterPositionMethod.invoke(customRecyclerView, new Object[]{childView});
            }
        } catch (IllegalAccessException e) {
            //ignored
        } catch (InvocationTargetException e2) {
            //ignored
        }
        return -1;
    }

    private static boolean isListView(View view) {
        return (view instanceof AdapterView) || ViewUtil.instanceOfRecyclerView(view) || ViewUtil.instanceOfAndroidXViewPager(view) || ViewUtil.instanceOfSupportViewPager(view);
    }

    private static boolean viewVisibilityInParents(View view) {
        if (view == null) {
            return false;
        }
        if (!SAViewUtils.isViewSelfVisible(view)) {
            return false;
        }
        ViewParent viewParent = view.getParent();
        while (viewParent instanceof View) {
            if (!SAViewUtils.isViewSelfVisible((View) viewParent)) {
                return false;
            }
            viewParent = viewParent.getParent();
            if (viewParent == null) {
                return false;
            }
        }
        return true;
    }

    public static void invalidateLayerTypeView(View[] views) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            for (View view : views) {
                if (ViewUtil.viewVisibilityInParents(view) && view.isHardwareAccelerated()) {
                    checkAndInvalidate(view);
                    if (view instanceof ViewGroup) {
                        invalidateViewGroup((ViewGroup) view);
                    }
                }
            }
        }
    }

    private static void checkAndInvalidate(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (view.getLayerType() != View.LAYER_TYPE_NONE) {
                view.invalidate();
            }
        }
    }

    private static void invalidateViewGroup(ViewGroup viewGroup) {
        for (int index = 0; index < viewGroup.getChildCount(); index++) {
            View child = viewGroup.getChildAt(index);
            if (SAViewUtils.isViewSelfVisible(child)) {
                checkAndInvalidate(child);
                if (child instanceof ViewGroup) {
                    invalidateViewGroup((ViewGroup) child);
                }
            }
        }
    }

    public static int getMainWindowCount(View[] windowRootViews) {
        int mainWindowCount = 0;
        WindowHelper.init();
        for (View windowRootView : windowRootViews) {
            if (windowRootView != null) {
                mainWindowCount += WindowHelper.getWindowPrefix(windowRootView).equals(WindowHelper.getMainWindowPrefix()) ? 1 : 0;
            }
        }
        return mainWindowCount;
    }

    public static boolean isWindowNeedTraverse(View root, String prefix, boolean skipOtherActivity) {
        if ((root.hashCode() == AppStateTools.getInstance().getCurrentRootWindowsHashCode())) {
            return true;
        }
        if (root instanceof ViewGroup) {
            if (!skipOtherActivity) {
                return true;
            }
            if (!(root.getWindowVisibility() == View.GONE || root.getVisibility() != View.VISIBLE || TextUtils.equals(prefix, WindowHelper.getMainWindowPrefix()) || root.getWidth() == 0 || root.getHeight() == 0)) {
                return true;
            }
        }
        if (root.getWindowVisibility() == View.VISIBLE || root.getVisibility() == View.VISIBLE) {
            if (WindowHelper.isCustomWindow(root)) {
                return true;
            }
        }
        return false;
    }


    public static ViewNode getViewPathAndPosition(View clickView, boolean fromVisual) {
        ArrayList<View> arrayList = new ArrayList<View>(8);
        arrayList.add(clickView);
        for (ViewParent parent = clickView.getParent(); parent instanceof ViewGroup; parent = parent.getParent()) {
            arrayList.add((ViewGroup) parent);
        }
        int endIndex = arrayList.size() - 1;
        View rootView = arrayList.get(endIndex);
        String listPosition = null;
        String elementContent = null;
        StringBuilder opx = new StringBuilder();
        StringBuilder px = new StringBuilder();
        if (rootView instanceof ViewGroup) {
            ViewGroup parentView = (ViewGroup) rootView;
            for (int i = endIndex - 1; i >= 0; i--) {
                final View childView = arrayList.get(i);
                final int viewPosition = parentView.indexOfChild(childView);
                final ViewNode viewNode = getViewNode(childView, viewPosition, fromVisual);
                if (viewNode != null) {
                    // 这个地方由于 viewPosition 当前控件是列表的子控件的时候，表示当前控件位于父控件的位置；当前控件是非列表的子控件的时候，表示上一个列表的位置。因此通过上一个 View 的 listPosition 进行替换[-]没有什么大的问题
                    if (!TextUtils.isEmpty(viewNode.getViewPath()) && viewNode.getViewPath().contains("-") && !TextUtils.isEmpty(listPosition)) {
                        int replacePosition = px.indexOf("-");
                        if (replacePosition != -1) {
                            px.replace(replacePosition, replacePosition + 1, String.valueOf(listPosition));
                        }
                    }
                    opx.append(viewNode.getViewOriginalPath());
                    px.append(viewNode.getViewPath());
                    listPosition = viewNode.getViewPosition();
                    elementContent = viewNode.getViewContent();
                }
                if (!(childView instanceof ViewGroup)) {
                    break;
                }
                parentView = (ViewGroup) childView;
            }
            return new ViewNode(clickView, listPosition, opx.toString(), px.toString(), elementContent);
        }
        return null;
    }

    private static int getViewPosition(View view, int viewIndex) {
        int idx = viewIndex;
        if (view.getParent() != null && (view.getParent() instanceof ViewGroup)) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (ViewUtil.instanceOfAndroidXViewPager(parent) || ViewUtil.instanceOfSupportViewPager(parent)) {
                idx = ViewUtil.getCurrentItem(parent);
            } else if (parent instanceof AdapterView) {
                idx += ((AdapterView) parent).getFirstVisiblePosition();
            } else if (ViewUtil.instanceOfRecyclerView(parent)) {
                int adapterPosition = ViewUtil.getChildAdapterPositionInRecyclerView(view, parent);
                if (adapterPosition >= 0) {
                    idx = adapterPosition;
                }
            }
        }
        return idx;
    }

    public static ViewNode getViewNode(View view, int viewIndex, boolean fromVisual) {
        int viewPosition = getViewPosition(view, viewIndex);
        ViewParent parentObject = view.getParent();
        if (parentObject == null) {
            return null;
        }
        if (!WindowHelper.isDecorView(view.getClass()) || (parentObject instanceof View)) {
            if (parentObject instanceof View) {
                View parentView = (View) parentObject;
                StringBuilder opx = new StringBuilder();
                StringBuilder px = new StringBuilder();
                String viewName = ViewUtil.getCanonicalAndCheckCustomView(view.getClass());
                Object fragment = null;
                String listPos = null;
                boolean isListView = false;
                // 处理嵌套场景，如果父 View 是列表类型控件，将父 View 的列表位置传递给非列表类型子控件; 列表类型子控件则直接用自身位置。
                ViewParent parent = parentView.getParent();
                if (parent instanceof View) {
                    View listParentView = (View) parent;
                    synchronized (sViewCache) {
                        String parentPos = (String) sViewCache.get(listParentView.hashCode());
                        if (!TextUtils.isEmpty(parentPos)) {
                            listPos = parentPos;
                        }
                    }
                }
                if (parentView instanceof ExpandableListView) {
                    ExpandableListView listParent = (ExpandableListView) parentView;
                    long elp = listParent.getExpandableListPosition(viewPosition);
                    if (ExpandableListView.getPackedPositionType(elp) != 2) {
                        isListView = true;
                        int groupIdx = ExpandableListView.getPackedPositionGroup(elp);
                        int childIdx = ExpandableListView.getPackedPositionChild(elp);
                        if (childIdx != -1) {
                            listPos = String.format(TimeUtils.SDK_LOCALE, "%d:%d", groupIdx, childIdx);
                            px.append(opx).append("/ELVG[").append(groupIdx).append("]/ELVC[-]/").append(viewName).append("[0]");
                            opx.append("/ELVG[").append(groupIdx).append("]/ELVC[").append(childIdx).append("]/").append(viewName).append("[0]");
                        } else {
                            listPos = String.format(TimeUtils.SDK_LOCALE, "%d", groupIdx);
                            px.append(opx).append("/ELVG[-]/").append(viewName).append("[0]");
                            opx.append("/ELVG[").append(groupIdx).append("]/").append(viewName).append("[0]");
                        }
                    } else if (viewPosition < listParent.getHeaderViewsCount()) {
                        opx.append("/ELH[").append(viewPosition).append("]/").append(viewName).append("[0]");
                        px.append("/ELH[").append(viewPosition).append("]/").append(viewName).append("[0]");
                    } else {
                        int footerIndex = viewPosition - (listParent.getCount() - listParent.getFooterViewsCount());
                        opx.append("/ELF[").append(footerIndex).append("]/").append(viewName).append("[0]");
                        px.append("/ELF[").append(footerIndex).append("]/").append(viewName).append("[0]");
                    }
                } else if (ViewUtil.isListView(parentView)) {
                    isListView = true;
                    listPos = String.format(TimeUtils.SDK_LOCALE, "%d", viewPosition);
                    px.append(opx).append("/").append(viewName).append("[-]");
                    opx.append("/").append(viewName).append("[").append(listPos).append("]");
                } else if (ViewUtil.instanceOfSupportSwipeRefreshLayout(parentView)) {
                    opx.append("/").append(viewName).append("[0]");
                    px.append("/").append(viewName).append("[0]");
                } else if ((fragment = ViewUtil.instanceOfFragmentRootView(parentView, view)) != null) {
                    viewName = ViewUtil.getCanonicalAndCheckCustomView(fragment.getClass());
                    opx.append("/").append(viewName).append("[0]");
                    px.append("/").append(viewName).append("[0]");
                } else {
                    viewPosition = SAViewUtils.getChildIndex(parentObject, view);
                    opx.append("/").append(viewName).append("[").append(viewPosition).append("]");
                    px.append("/").append(viewName).append("[").append(viewPosition).append("]");
                }
                if (WindowHelper.isDecorView(parentView.getClass())) {
                    if (opx.length() > 0) {
                        opx.deleteCharAt(0);
                    }
                    if (px.length() > 0) {
                        px.deleteCharAt(0);
                    }
                }
                if (!TextUtils.isEmpty(listPos)) {
                    synchronized (sViewCache) {
                        sViewCache.put(parentView.hashCode(), listPos);
                    }
                }
                return new ViewNode(view, listPos, opx.toString(), px.toString(), SAViewUtils.getViewContent(view, fromVisual), SAViewUtils.getViewType(view), isListView);
            }
        }
        return null;
    }

    public static void clear() {
        synchronized (sViewCache) {
            sViewCache.clear();
        }
    }
}