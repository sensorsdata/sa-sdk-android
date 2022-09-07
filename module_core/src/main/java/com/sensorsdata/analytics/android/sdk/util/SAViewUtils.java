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

package com.sensorsdata.analytics.android.sdk.util;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class SAViewUtils {
    private static final String TAG = "SA.SAViewUtils";
    // 采集 viewType 忽略以下包内 view 直接返回对应的基础控件 viewType
    private static List<String> sOSViewPackage = new LinkedList<String>() {{
        add("android##widget");
        add("android##support##v7##widget");
        add("android##support##design##widget");
        add("android##support##text##emoji##widget");
        add("androidx##appcompat##widget");
        add("androidx##emoji##widget");
        add("androidx##cardview##widget");
        add("com##google##android##material");
    }};

    public static String traverseView(StringBuilder stringBuilder, ViewGroup root) {
        try {
            if (stringBuilder == null) {
                stringBuilder = new StringBuilder();
            }

            if (root == null) {
                return stringBuilder.toString();
            }

            final int childCount = root.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                final View child = root.getChildAt(i);
                if (child == null) {
                    continue;
                }
                if (child.getVisibility() != View.VISIBLE) {
                    continue;
                }

                if (child instanceof ViewGroup) {
                    traverseView(stringBuilder, (ViewGroup) child);
                } else {
                    if (isViewIgnored(child)) {
                        continue;
                    }

                    String viewText = SAViewUtils.getViewContent(child);
                    if (!TextUtils.isEmpty(viewText)) {
                        stringBuilder.append(viewText);
                        stringBuilder.append("-");
                    }
                }
            }
            return stringBuilder.toString();
        } catch (Throwable e) {
            SALog.i(TAG, e.getMessage());
            return stringBuilder != null ? stringBuilder.toString() : "";
        }
    }

    /**
     * 判断 View 是否被忽略
     *
     * @param view View
     * @return 是否被忽略
     */
    public static boolean isViewIgnored(View view) {
        try {
            //基本校验
            if (view == null) {
                return true;
            }

            //ViewType 被忽略
            List<Class<?>> mIgnoredViewTypeList = SensorsDataAPI.sharedInstance().getIgnoredViewTypeList();
            if (mIgnoredViewTypeList != null) {
                for (Class<?> clazz : mIgnoredViewTypeList) {
                    if (clazz.isAssignableFrom(view.getClass())) {
                        return true;
                    }
                }
            }

            //View 被忽略
            return "1".equals(view.getTag(R.id.sensors_analytics_tag_view_ignored));
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return true;
        }
    }

    /**
     * ViewType 被忽略
     *
     * @param viewType Class
     * @return 是否被忽略
     */
    public static boolean isViewIgnored(Class<?> viewType) {
        try {
            if (viewType == null) {
                return true;
            }

            List<Class<?>> mIgnoredViewTypeList = SensorsDataAPI.sharedInstance().getIgnoredViewTypeList();
            if (!mIgnoredViewTypeList.isEmpty()) {
                for (Class<?> clazz : mIgnoredViewTypeList) {
                    if (clazz.isAssignableFrom(viewType)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * get view type
     *
     * @param view view
     * @return viewType
     */
    public static String getViewTypeByReflect(View view) {
        Class<?> compatClass;
        String viewType = SnapCache.getInstance().getCanonicalName(view.getClass());
        compatClass = ReflectUtil.getClassByName("android.widget.Switch");
        if (compatClass != null && compatClass.isInstance(view)) {
            return getViewType(viewType, "Switch");
        }
        compatClass = ReflectUtil.getClassByName("android.support.v7.widget.SwitchCompat");
        if (compatClass != null && compatClass.isInstance(view)) {
            return getViewType(viewType, "SwitchCompat");
        }
        compatClass = ReflectUtil.getClassByName("androidx.appcompat.widget.SwitchCompat");
        if (compatClass != null && compatClass.isInstance(view)) {
            return getViewType(viewType, "SwitchCompat");
        }
        return viewType;
    }

    public static Activity getActivityOfView(Context context, View view) {
        Activity activity = null;
        try {
            if (context != null) {
                if (context instanceof Activity) {
                    activity = (Activity) context;
                } else if (context instanceof ContextWrapper) {
                    while (!(context instanceof Activity) && context instanceof ContextWrapper) {
                        context = ((ContextWrapper) context).getBaseContext();
                    }
                    if (context instanceof Activity) {
                        activity = (Activity) context;
                    }
                }

                if (activity == null && view != null) {
                    Object object = view.getTag(R.id.sensors_analytics_tag_view_activity);
                    if (object != null) {
                        if (object instanceof Activity) {
                            activity = (Activity) object;
                        }
                    }
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return activity;
    }

    /**
     * 通过反射判断类的类型
     *
     * @param view 判断类型的 viewGroup
     * @return viewType
     */
    public static String getViewGroupTypeByReflect(View view) {
        Class<?> compatClass;
        String viewType = SnapCache.getInstance().getCanonicalName(view.getClass());
        compatClass = ReflectUtil.getClassByName("android.support.v7.widget.CardView");
        if (compatClass != null && compatClass.isInstance(view)) {
            return SAViewUtils.getViewType(viewType, "CardView");
        }
        compatClass = ReflectUtil.getClassByName("androidx.cardview.widget.CardView");
        if (compatClass != null && compatClass.isInstance(view)) {
            return SAViewUtils.getViewType(viewType, "CardView");
        }
        compatClass = ReflectUtil.getClassByName("android.support.design.widget.NavigationView");
        if (compatClass != null && compatClass.isInstance(view)) {
            return SAViewUtils.getViewType(viewType, "NavigationView");
        }
        compatClass = ReflectUtil.getClassByName("com.google.android.material.navigation.NavigationView");
        if (compatClass != null && compatClass.isInstance(view)) {
            return SAViewUtils.getViewType(viewType, "NavigationView");
        }
        return viewType;
    }

    public static String getViewContent(View view) {
        return getViewContent(view, false);
    }

    public static String getViewContent(View view, boolean fromVisual) {
        try {
            String cacheViewType = SnapCache.getInstance().getViewType(view);
            String cacheViewText = SnapCache.getInstance().getViewText(view);
            CharSequence viewText = null;
            Object tab;
            if (cacheViewType == null || cacheViewText == null) {
                if (view instanceof CheckBox) { // CheckBox
                    CheckBox checkBox = (CheckBox) view;
                    viewText = checkBox.getText();
                } else if (view instanceof RadioButton) { // RadioButton
                    RadioButton radioButton = (RadioButton) view;
                    viewText = radioButton.getText();
                } else if (view instanceof ToggleButton) { // ToggleButton
                    ToggleButton toggleButton = (ToggleButton) view;
                    boolean isChecked = toggleButton.isChecked();
                    if (isChecked) {
                        viewText = toggleButton.getTextOn();
                    } else {
                        viewText = toggleButton.getTextOff();
                    }
                } else if (view instanceof CompoundButton) {
                    CompoundButton switchCompat = (CompoundButton) view;
                    Method method;
                    if (switchCompat.isChecked()) {
                        method = view.getClass().getMethod("getTextOn");
                    } else {
                        method = view.getClass().getMethod("getTextOff");
                    }
                    viewText = (String) method.invoke(view);
                } else if (view instanceof Button) { // Button
                    Button button = (Button) view;
                    viewText = button.getText();
                } else if (view instanceof CheckedTextView) { // CheckedTextView
                    CheckedTextView textView = (CheckedTextView) view;
                    viewText = textView.getText();
                } else if (view instanceof TextView) { // TextView
                    TextView textView = (TextView) view;
                    Object object = ReflectUtil.findField(new String[]{"androidx.appcompat.widget.AppCompatTextView"}, textView, "mPrecomputedTextFuture");
                    if (object == null) {
                        viewText = textView.getText();
                    }
                } else if (view instanceof ImageView) { // ImageView
                    ImageView imageView = (ImageView) view;
                    if (!TextUtils.isEmpty(imageView.getContentDescription())) {
                        viewText = imageView.getContentDescription().toString();
                    }
                } else if (view instanceof RatingBar) {
                    RatingBar ratingBar = (RatingBar) view;
                    viewText = String.valueOf(ratingBar.getRating());
                } else if (view instanceof SeekBar) {
                    SeekBar seekBar = (SeekBar) view;
                    viewText = String.valueOf(seekBar.getProgress());
                } else if (view instanceof Spinner) {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = SAViewUtils.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.toString().substring(0, viewText.length() - 1);
                    }
                } else if ((tab = instanceOfTabView(view)) != null) {
                    viewText = getTabLayoutContent(tab);
                } else if (instanceOfBottomNavigationItemView(view)) {
                    Object itemData = getMenuItemData(view);
                    if (itemData != null) {
                        try {
                            Class<?> menuItemImplClass = ReflectUtil.getCurrentClass(new String[]{"androidx.appcompat.view.menu.MenuItemImpl"});
                            if (menuItemImplClass != null) {
                                String title = ReflectUtil.findField(menuItemImplClass, itemData, new String[]{"mTitle"});
                                if (!TextUtils.isEmpty(title)) {
                                    viewText = title;
                                }
                            }
                        } catch (Exception e) {
                            //ignored
                        }
                    }
                } else if (instanceOfNavigationView(view)) {
                    viewText = isViewSelfVisible(view) ? "Open" : "Close";
                } else if (view instanceof ViewGroup) {
                    viewText = view.getContentDescription();
                    if (TextUtils.isEmpty(viewText)) {
                        try {
                            StringBuilder stringBuilder = new StringBuilder();
                            viewText = SAViewUtils.traverseView(stringBuilder, (ViewGroup) view);
                            if (!TextUtils.isEmpty(viewText)) {
                                viewText = viewText.toString().substring(0, viewText.length() - 1);
                            }
                        } catch (Exception e) {
                            //ignored
                        }
                    }
                }

                if (TextUtils.isEmpty(viewText) && view instanceof TextView) {
                    viewText = ((TextView) view).getHint();
                }

                if (TextUtils.isEmpty(viewText)) {
                    viewText = view.getContentDescription();
                }

                if (viewText == null) {
                    viewText = "";
                }
                SnapCache.getInstance().setViewText(view, viewText.toString());
            } else {
                viewText = cacheViewText;
            }

            if (view instanceof EditText) {
                // 自定义属性时需要放开 EditText
                if (fromVisual) {
                    viewText = ((EditText) view).getText();
                } else {
                    viewText = "";
                }
            }

            if (viewText == null) {
                viewText = "";
            }
            return viewText.toString();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return "";
    }

    public static String getViewType(View view) {
        String cacheViewType = SnapCache.getInstance().getViewType(view);
        String viewType;
        if (cacheViewType == null) {
            viewType = SnapCache.getInstance().getCanonicalName(view.getClass());
            if (view instanceof CheckBox) { // CheckBox
                viewType = SAViewUtils.getViewType(viewType, "CheckBox");
            } else if (view instanceof RadioButton) { // RadioButton
                viewType = SAViewUtils.getViewType(viewType, "RadioButton");
            } else if (view instanceof ToggleButton) { // ToggleButton
                viewType = SAViewUtils.getViewType(viewType, "ToggleButton");
            } else if (view instanceof CompoundButton) {
                viewType = SAViewUtils.getViewTypeByReflect(view);
            } else if (view instanceof Button) { // Button
                viewType = SAViewUtils.getViewType(viewType, "Button");
            } else if (view instanceof CheckedTextView) { // CheckedTextView
                viewType = SAViewUtils.getViewType(viewType, "CheckedTextView");
            } else if (view instanceof TextView) { // TextView
                viewType = SAViewUtils.getViewType(viewType, "TextView");
            } else if (view instanceof ImageView) { // ImageView
                viewType = SAViewUtils.getViewType(viewType, "ImageView");
            } else if (view instanceof RatingBar) {
                viewType = SAViewUtils.getViewType(viewType, "RatingBar");
            } else if (view instanceof SeekBar) {
                viewType = SAViewUtils.getViewType(viewType, "SeekBar");
            } else if (view instanceof Spinner) {
                viewType = SAViewUtils.getViewType(viewType, "Spinner");
            } else if (instanceOfTabView(view) != null) {
                viewType = SAViewUtils.getViewType(viewType, "TabLayout");
            }else if (instanceOfNavigationView(view)) {
                viewType = SAViewUtils.getViewType(viewType, "NavigationView");
            } else if (view instanceof ViewGroup) {
                viewType = SAViewUtils.getViewGroupTypeByReflect(view);
            }

            SnapCache.getInstance().setViewType(view, viewType);
        } else {
            viewType = cacheViewType;
        }

        return viewType;
    }

    public static boolean instanceOfBottomNavigationItemView(Object view) {
        return ReflectUtil.isInstance(view, "com.google.android.material.bottomnavigation.BottomNavigationItemView", "android.support.design.internal.NavigationMenuItemView");
    }

    public static boolean instanceOfNavigationView(Object view) {
        return ReflectUtil.isInstance(view, "android.support.design.widget.NavigationView", "com.google.android.material.navigation.NavigationView");
    }

    public static boolean instanceOfSupportListMenuItemView(Object view) {
        return ReflectUtil.isInstance(view, "android.support.v7.view.menu.ListMenuItemView");
    }

    public static boolean instanceOfAndroidXListMenuItemView(Object view) {
        return ReflectUtil.isInstance(view, "androidx.appcompat.view.menu.ListMenuItemView");
    }

    public static boolean instanceOfActionMenuItem(Object view) {
        return ReflectUtil.isInstance(view, "androidx.appcompat.view.menu.ActionMenuItem");
    }

    public static boolean instanceOfToolbar(Object view) {
        return ReflectUtil.isInstance(view, "androidx.appcompat.widget.Toolbar", "android.support.v7.widget.Toolbar", "android.widget.Toolbar");
    }

    public static Object getMenuItemData(View view) {
        try {
            Method method = view.getClass().getMethod("getItemData");
            return method.invoke(view);
        } catch (IllegalAccessException e) {
            //ignored
        } catch (InvocationTargetException e2) {
            //ignored
        } catch (NoSuchMethodException e) {
            //ignored
        }
        return null;
    }

    public static boolean isViewSelfVisible(View view) {
        if (view == null || view.getWindowVisibility() == View.GONE) {
            return false;
        }
        if (WindowHelper.isDecorView(view.getClass())) {
            return true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            Boolean localVisibleRect = SnapCache.getInstance().getLocalVisibleRect(view);
            boolean viewLocalVisiable;
            if (localVisibleRect == null) {
                viewLocalVisiable = view.getLocalVisibleRect(new Rect());
                SnapCache.getInstance().setLocalVisibleRect(view, viewLocalVisiable);
            } else {
                viewLocalVisiable = localVisibleRect;
            }
            if (view.getWidth() <= 0 || view.getHeight() <= 0 || view.getAlpha() <= 0.0f || !viewLocalVisiable) {
                return false;
            }
        }
        if ((view.getVisibility() == View.VISIBLE || view.getAnimation() == null || !view.getAnimation().getFillAfter()) && view.getVisibility() != View.VISIBLE) {
            return false;
        }
        return true;
    }

    public static String getElementSelector(View view) {
        String currentPath = SnapCache.getInstance().getSelectPath(view);
        if (currentPath != null) {
            return currentPath;
        }
        ViewParent viewParent;
        String selectPath;
        View parent_view = null;
        viewParent = view.getParent();
        if (viewParent instanceof ViewGroup) {
            parent_view = (View) viewParent;
        }
        String parentPath = null;
        if (parent_view != null) {
            parentPath = SnapCache.getInstance().getSelectPath(parent_view);
        }
        String path = SnapCache.getInstance().getCanonicalName(view.getClass());

        if (parent_view != null) {
            if (parentPath == null) {
                parentPath = getElementSelectorOrigin(parent_view);
                SnapCache.getInstance().setSelectPath(parent_view, parentPath);
            }
            StringBuilder sb = new StringBuilder();
            if (parentPath != null && !parentPath.equals("")) {
                sb.append(parentPath);
                sb.append("/");
            }
            int index = getChildIndex(viewParent, view);
            sb.append(path);
            sb.append("[");
            sb.append(index);
            sb.append("]");
            selectPath = sb.toString();
        } else {
            selectPath = getElementSelectorOrigin(view);
        }
        SnapCache.getInstance().setSelectPath(view, selectPath);
        return selectPath;
    }

    public static int getChildIndex(ViewParent parent, View child) {
        try {
            if (!(parent instanceof ViewGroup)) {
                return -1;
            }
            ViewGroup viewParent = (ViewGroup) parent;
            final String childIdName = SAViewUtils.getViewId(child);

            String childClassName = SnapCache.getInstance().getCanonicalName(child.getClass());
            int index = 0;
            for (int i = 0; i < viewParent.getChildCount(); i++) {
                View brother = viewParent.getChildAt(i);
                if (!Pathfinder.hasClassName(brother, childClassName)) {
                    continue;
                }
                String brotherIdName = SAViewUtils.getViewId(brother);
                if (null != childIdName && !childIdName.equals(brotherIdName)) {
                    index++;
                    continue;
                }
                if (brother == child) {
                    return index;
                }
                index++;
            }
            return -1;
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return -1;
        }
    }

    private static String getElementSelectorOrigin(View view) {
        ViewParent viewParent;
        List<String> viewPath = new LinkedList<>();
        do {
            viewParent = view.getParent();
            int index = getChildIndex(viewParent, view);
            viewPath.add(view.getClass().getCanonicalName() + "[" + index + "]");
            if (viewParent instanceof ViewGroup) {
                view = (ViewGroup) viewParent;
            }
        } while (viewParent instanceof ViewGroup);

        Collections.reverse(viewPath);

        StringBuilder stringBuffer = new StringBuilder();
        for (int i = 1; i < viewPath.size(); i++) {
            stringBuffer.append(viewPath.get(i));
            if (i != (viewPath.size() - 1)) {
                stringBuffer.append("/");
            }
        }
        return stringBuffer.toString();
    }

    public static String getViewId(View view) {
        String idString = null;
        try {
            idString = (String) view.getTag(R.id.sensors_analytics_tag_view_id);
            if (TextUtils.isEmpty(idString)) {
                if (isValid(view.getId())) {
                    idString = SnapCache.getInstance().getViewId(view);
                    if (idString == null) {
                        idString = view.getContext().getResources().getResourceEntryName(view.getId());
                        SnapCache.getInstance().setViewId(view, idString);
                    }
                }
            }
        } catch (Exception e) {
            if (SALog.isLogEnabled()) {
                exceptionCollect(view);
            }
        }
        return idString;
    }

    private static boolean isValid(int id) {
        return id != -1 && (id & 0xff000000) != 0 && (id & 0x00ff0000) != 0;
    }

    private static void exceptionCollect(View view) {
        try {
            if (view != null) {
                SALog.i(TAG, "viewClass:" + view.getClass());
                SALog.i(TAG, "viewId:" + view.getId());
                ViewParent viewParent = view.getParent();
                if (viewParent != null) {
                    if (viewParent instanceof View) {
                        View tmpParent = (View) viewParent;
                        SALog.i(TAG, "viewParentClass->ID:" + tmpParent.getId());
                    }
                } else {
                    if (view instanceof ViewGroup) {
                        int count = ((ViewGroup) view).getChildCount();
                        if (count > 0) {
                            View childView = ((ViewGroup) view).getChildAt(0);
                            SALog.i(TAG, "childView->ID:" + childView.getId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 判断是否是系统 view
     *
     * @param viewName view 的名称（包含包名）
     * @return 是否是系统 view  true:是  false: 否
     */
    private static boolean isOSViewByPackage(String viewName) {
        if (TextUtils.isEmpty(viewName)) {
            return false;
        }
        String viewNameTemp = viewName.replace(".", "##");
        for (String OSViewPackage : sOSViewPackage) {
            if (viewNameTemp.startsWith(OSViewPackage)) {
                return true;
            }
        }
        return false;
    }

    private static String getTabLayoutContent(Object tab) {
        String viewText = null;
        Class<?> currentTabClass;
        try {
            currentTabClass = ReflectUtil.getCurrentClass(new String[]{"android.support.design.widget.TabLayout$Tab", "com.google.android.material.tabs.TabLayout$Tab"});
            if (currentTabClass != null) {
                Object text = null;
                text = ReflectUtil.callMethod(tab, "getText");
                if (text != null) {
                    viewText = text.toString();
                }
                View customView = ReflectUtil.findField(currentTabClass, tab, new String[]{"mCustomView", "customView"});
                if (customView != null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    if (customView instanceof ViewGroup) {
                        viewText = SAViewUtils.traverseView(stringBuilder, (ViewGroup) customView);
                        if (!TextUtils.isEmpty(viewText)) {
                            viewText = viewText.substring(0, viewText.length() - 1);
                        }
                    } else {
                        viewText = SAViewUtils.getViewContent(customView);
                    }
                }
            }
        } catch (Exception e) {
            //ignored
        }
        return viewText;
    }

    private static Object instanceOfTabView(View tabView) {
        try {
            Class<?> currentTabViewClass = ReflectUtil.getCurrentClass(new String[]{"android.support.design.widget.TabLayout$TabView", "com.google.android.material.tabs.TabLayout$TabView"});
            if (currentTabViewClass != null && currentTabViewClass.isAssignableFrom(tabView.getClass())) {
                return ReflectUtil.findField(currentTabViewClass, tabView, "mTab", "tab");
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    /**
     * 采集 View 的 $element_type 主要区分继承系统 View 和继承系统 View 的自定义 View
     *
     * @param viewName View.getCanonicalName（）返回的 name
     * @param defaultTypeName 默认的 typeName
     * @return typeName
     */
    private static String getViewType(String viewName, String defaultTypeName) {
        if (TextUtils.isEmpty(viewName) || isOSViewByPackage(viewName)) {
            return defaultTypeName;
        }
        return viewName;
    }
}
