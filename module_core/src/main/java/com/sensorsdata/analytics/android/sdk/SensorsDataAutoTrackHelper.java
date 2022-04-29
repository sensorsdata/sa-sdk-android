/*
 * Created by wangzhuozhou on 2015/08/01.
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

package com.sensorsdata.analytics.android.sdk;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TabHost;

import com.sensorsdata.analytics.android.sdk.dialog.SensorsDataDialogUtils;
import com.sensorsdata.analytics.android.sdk.util.AopUtil;
import com.sensorsdata.analytics.android.sdk.util.KeyboardViewUtil;
import com.sensorsdata.analytics.android.sdk.util.ReflectUtil;
import com.sensorsdata.analytics.android.sdk.util.SAFragmentUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.ThreadUtils;
import com.sensorsdata.analytics.android.sdk.util.ViewUtil;
import com.sensorsdata.analytics.android.sdk.util.WindowHelper;
import com.sensorsdata.analytics.android.sdk.visual.WebViewVisualInterface;
import com.sensorsdata.analytics.android.sdk.visual.model.ViewNode;
import com.sensorsdata.analytics.android.sdk.visual.util.VisualUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class SensorsDataAutoTrackHelper {
    private static final String TAG = "SensorsDataAutoTrackHelper";
    private static HashMap<Integer, Long> eventTimestamp = new HashMap<>();

    private static boolean isDeBounceTrack(Object object) {
        long currentOnClickTimestamp = SystemClock.elapsedRealtime();
        Object targetObject = eventTimestamp.get(object.hashCode());
        if (targetObject != null) {
            long lastOnClickTimestamp = (long) targetObject;
            if ((currentOnClickTimestamp - lastOnClickTimestamp) < 500) {
                return true;
            }
        }
        eventTimestamp.put(object.hashCode(), currentOnClickTimestamp);
        return false;
    }

    public static void trackRN(Object target, int reactTag, int s, boolean b) {

    }

    public static void trackExpandableListViewOnGroupClick(ExpandableListView expandableListView, View view,
                                                           int groupPosition) {
        try {
            if (expandableListView == null || view == null) {
                return;
            }

            //关闭 AutoTrack
            if (!SensorsDataAPI.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //$AppClick 被过滤
            if (SensorsDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取所在的 Context
            Context context = expandableListView.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = null;
            if (context instanceof Activity) {
                activity = (Activity) context;
            }

            //Activity 被忽略
            if (activity != null) {
                if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            // 获取 view 所在的 fragment
            Object fragment = AopUtil.getFragmentFromView(expandableListView, activity);

            // fragment 忽略
            if (fragment != null) {
                if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(fragment.getClass())) {
                    return;
                }
            }

            // ExpandableListView Type 被忽略
            if (AopUtil.isViewIgnored(ExpandableListView.class)) {
                return;
            }

            // View 被忽略
            if (AopUtil.isViewIgnored(expandableListView)) {
                return;
            }

            JSONObject properties = new JSONObject();

            ViewNode viewNode = AopUtil.addViewPathProperties(activity, view, properties);

            // $screen_name & $title
            if (activity != null) {
                SensorsDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }

            // ViewId
            String idString = AopUtil.getViewId(expandableListView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AopConstants.ELEMENT_ID, idString);
            }
            properties.put(AopConstants.ELEMENT_TYPE, "ExpandableListView");

            String viewText = null;
            if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            } else {
                viewText = AopUtil.getViewText(view);
            }
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AopConstants.ELEMENT_CONTENT, viewText);
            }

            //fragmentName
            if (fragment != null) {
                AopUtil.getScreenNameAndTitleFromFragment(properties, fragment, activity);
            }

            // 获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.sensors_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            // 扩展属性
            ExpandableListAdapter listAdapter = expandableListView.getExpandableListAdapter();
            if (listAdapter != null) {
                if (listAdapter instanceof SensorsExpandableListViewItemTrackProperties) {
                    try {
                        SensorsExpandableListViewItemTrackProperties trackProperties = (SensorsExpandableListViewItemTrackProperties) listAdapter;
                        JSONObject jsonObject = trackProperties.getSensorsGroupItemTrackProperties(groupPosition);
                        if (jsonObject != null) {
                            AopUtil.mergeJSONObject(jsonObject, properties);
                        }
                    } catch (JSONException e) {
                        SALog.printStackTrace(e);
                    }
                }
            }

            SensorsDataAPI.sharedInstance().trackAutoEvent(AopConstants.APP_CLICK_EVENT_NAME, properties, viewNode);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void trackExpandableListViewOnChildClick(ExpandableListView expandableListView, View view,
                                                           int groupPosition, int childPosition) {
        try {
            if (expandableListView == null || view == null) {
                return;
            }

            //关闭 AutoTrack
            if (!SensorsDataAPI.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //$AppClick 被过滤
            if (SensorsDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取所在的 Context
            Context context = expandableListView.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, expandableListView);

            //Activity 被忽略
            if (activity != null) {
                if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            // 获取 view 所在的 fragment
            Object fragment = AopUtil.getFragmentFromView(expandableListView, activity);

            // fragment 忽略
            if (fragment != null) {
                if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(fragment.getClass())) {
                    return;
                }
            }

            //ExpandableListView 被忽略
            if (AopUtil.isViewIgnored(ExpandableListView.class)) {
                return;
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(expandableListView)) {
                return;
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(view)) {
                return;
            }

            //获取 View 自定义属性
            JSONObject properties = (JSONObject) view.getTag(R.id.sensors_analytics_tag_view_properties);

            if (properties == null) {
                properties = new JSONObject();
            }

            //扩展属性
            ExpandableListAdapter listAdapter = expandableListView.getExpandableListAdapter();
            if (listAdapter != null) {
                if (listAdapter instanceof SensorsExpandableListViewItemTrackProperties) {
                    SensorsExpandableListViewItemTrackProperties trackProperties = (SensorsExpandableListViewItemTrackProperties) listAdapter;
                    JSONObject jsonObject = trackProperties.getSensorsChildItemTrackProperties(groupPosition, childPosition);
                    if (jsonObject != null) {
                        AopUtil.mergeJSONObject(jsonObject, properties);
                    }
                }
            }

            ViewNode viewNode = AopUtil.addViewPathProperties(activity, view, properties);

            //$screen_name & $title
            if (activity != null) {
                SensorsDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }

            //ViewId
            String idString = AopUtil.getViewId(expandableListView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AopConstants.ELEMENT_ID, idString);
            }
            properties.put(AopConstants.ELEMENT_TYPE, "ExpandableListView");

            String viewText = null;
            if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            } else {
                viewText = AopUtil.getViewText(view);
            }
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AopConstants.ELEMENT_CONTENT, viewText);
            }

            //fragmentName
            if (fragment != null) {
                AopUtil.getScreenNameAndTitleFromFragment(properties, fragment, activity);
            }

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.sensors_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            SensorsDataAPI.sharedInstance().trackAutoEvent(AopConstants.APP_CLICK_EVENT_NAME, properties, viewNode);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void trackTabHost(final String tabName) {
        try {
            ThreadUtils.getSinglePool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        //关闭 AutoTrack
                        if (!SensorsDataAPI.sharedInstance().isAutoTrackEnabled()) {
                            return;
                        }

                        //$AppClick 被过滤
                        if (SensorsDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_CLICK)) {
                            return;
                        }

                        //TabHost 被忽略
                        if (AopUtil.isViewIgnored(TabHost.class)) {
                            return;
                        }

                        JSONObject properties = new JSONObject();
                        String elementContent = null;
                        // 2020/4/27 新增  1. 解决 TabHost 点击取不到 element_content 2. 可视化增加 $element_path
                        View view = WindowHelper.getClickView(tabName);
                        ViewNode viewNode = null;
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
                            if (null != tabHostView && AopUtil.isViewIgnored(tabHostView)) {
                                return;
                            }

                            Context context = view.getContext();
                            if (context == null) {
                                return;
                            }
                            Activity activity = null;
                            if (context instanceof Activity) {
                                activity = (Activity) context;
                            }
                            if (activity != null) {
                                if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                                    return;
                                }
                                SensorsDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);

                                Object fragment = AopUtil.getFragmentFromView(view, activity);
                                if (fragment != null) {
                                    if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(fragment.getClass())) {
                                        return;
                                    }
                                    AopUtil.getScreenNameAndTitleFromFragment(properties, fragment, activity);
                                }

                                viewNode = AopUtil.addViewPathProperties(activity, view, properties);
                            }
                            elementContent = ViewUtil.getViewContentAndType(view).getViewContent();
                        }
                        if (TextUtils.isEmpty(elementContent)) {
                            elementContent = tabName;
                        }
                        properties.put(AopConstants.ELEMENT_CONTENT, elementContent);
                        properties.put(AopConstants.ELEMENT_TYPE, "TabHost");

                        SensorsDataAPI.sharedInstance().trackAutoEvent(AopConstants.APP_CLICK_EVENT_NAME, properties, viewNode);
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void trackTabLayoutSelected(Object object, Object tab) {
        try {
            if (tab == null) {
                return;
            }
            //关闭 AutoTrack
            if (!SensorsDataAPI.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //$AppClick 被过滤
            if (SensorsDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            Class<?> supportTabLayoutCLass = null;
            Class<?> androidXTabLayoutCLass = null;
            try {
                supportTabLayoutCLass = Class.forName("android.support.design.widget.TabLayout");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXTabLayoutCLass = Class.forName("com.google.android.material.tabs.TabLayout");
            } catch (Exception e) {
                //ignored
            }

            if (supportTabLayoutCLass == null && androidXTabLayoutCLass == null) {
                return;
            }
            View tabLayout = null;
            //TabLayout 被忽略
            if (supportTabLayoutCLass != null) {
                //反射获取 TabLayout，进行 view 忽略判断。
                if (ReflectUtil.isInstance(tab, "android.support.design.widget.TabLayout$Tab")) {
                    View view = ReflectUtil.findField(new String[]{"android.support.design.widget.TabLayout$Tab"}, tab, "mParent");
                    if (null != view && ReflectUtil.isInstance(view, "android.support.design.widget.TabLayout")
                            && AopUtil.isViewIgnored(view)) {
                        return;
                    }
                    tabLayout = view;
                }

                if (AopUtil.isViewIgnored(supportTabLayoutCLass)) {
                    return;
                }
            }
            if (androidXTabLayoutCLass != null) {
                //反射获取 TabLayout，进行 view 忽略判断。
                if (ReflectUtil.isInstance(tab, "com.google.android.material.tabs.TabLayout$Tab")) {
                    View view = ReflectUtil.findField(new String[]{"com.google.android.material.tabs.TabLayout$Tab"}, tab, "parent");
                    if (null != view && ReflectUtil.isInstance(view, "com.google.android.material.tabs.TabLayout")
                            && AopUtil.isViewIgnored(view)) {
                        return;
                    }
                    tabLayout = view;
                }

                if (AopUtil.isViewIgnored(androidXTabLayoutCLass)) {
                    return;
                }
            }

            if (isDeBounceTrack(tab)) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = null;
            ViewNode viewNode = null;
            boolean isFragment = false;
            if (object instanceof Context) {
                activity = AopUtil.getActivityFromContext((Context) object, null);
            } else {
                try {
                    Field[] fields = object.getClass().getDeclaredFields();
                    for (Field field : fields) {
                        field.setAccessible(true);
                        Object bridgeObject = field.get(object);
                        if (bridgeObject instanceof Activity) {
                            activity = (Activity) bridgeObject;
                            break;
                        } else if (SAFragmentUtils.isFragment(bridgeObject)) {
                            object = bridgeObject;
                            isFragment = true;
                            break;
                        } else if (bridgeObject instanceof View) {
                            View view = (View) bridgeObject;
                            activity = AopUtil.getActivityFromContext(view.getContext(), null);
                        }
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
            // object 非 Activity/Fragment
            if (activity == null && !isFragment && tabLayout != null) {
                activity = AopUtil.getActivityFromContext(tabLayout.getContext(), null);
                // 获取 tabLayout 所在的 fragment
                Object fragment = AopUtil.getFragmentFromView(tabLayout, activity);
                if (fragment != null) {
                    object = fragment;
                    isFragment = true;
                }
            }
            //Activity 被忽略
            if (activity != null) {
                if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            if (isFragment) {
                if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(object.getClass())) {
                    return;
                }
            }

            JSONObject properties = new JSONObject();
            //Type
            properties.put(AopConstants.ELEMENT_TYPE, "TabLayout");

            //$screen_name & $title
            if (isFragment) {
                if (activity == null) {
                    activity = AopUtil.getActivityFromFragment(object);
                }
                AopUtil.getScreenNameAndTitleFromFragment(properties, object, activity);
            } else if (activity != null) {
                SensorsDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }

            Class<?> supportTabClass = null;
            Class<?> androidXTabClass = null;
            Class<?> currentTabClass;
            try {
                supportTabClass = Class.forName("android.support.design.widget.TabLayout$Tab");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXTabClass = Class.forName("com.google.android.material.tabs.TabLayout$Tab");
            } catch (Exception e) {
                //ignored
            }

            if (supportTabClass != null) {
                currentTabClass = supportTabClass;
            } else {
                currentTabClass = androidXTabClass;
            }

            if (currentTabClass != null) {
                Method method = null;
                try {
                    method = currentTabClass.getMethod("getText");
                } catch (NoSuchMethodException e) {
                    //ignored
                }

                if (method != null) {
                    Object text = method.invoke(tab);

                    //Content
                    if (text != null) {
                        properties.put(AopConstants.ELEMENT_CONTENT, text);
                    }
                }

                if (activity != null) {
                    try {
                        Field field;
                        try {
                            field = currentTabClass.getDeclaredField("mCustomView");
                        } catch (NoSuchFieldException ex) {
                            try {
                                field = currentTabClass.getDeclaredField("customView");
                            } catch (NoSuchFieldException e) {
                                field = null;
                            }
                        }

                        View view = null;
                        if (field != null) {
                            field.setAccessible(true);
                            view = (View) field.get(tab);
                            if (view != null) {
                                try {
                                    StringBuilder stringBuilder = new StringBuilder();
                                    String viewText;
                                    if (view instanceof ViewGroup) {
                                        viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                                        if (!TextUtils.isEmpty(viewText)) {
                                            viewText = viewText.substring(0, viewText.length() - 1);
                                        }
                                    } else {
                                        viewText = AopUtil.getViewText(view);
                                    }

                                    if (!TextUtils.isEmpty(viewText)) {
                                        properties.put(AopConstants.ELEMENT_CONTENT, viewText);
                                    }
                                } catch (Exception e) {
                                    SALog.printStackTrace(e);
                                }
                            }
                        }

                        View tabView = null;
                        try {
                            Field viewField = currentTabClass.getDeclaredField("view");
                            viewField.setAccessible(true);
                            try {
                                tabView = (View) viewField.get(tab);
                            } catch (IllegalAccessException e) {
                                SALog.printStackTrace(e);
                            }

                        } catch (NoSuchFieldException e) {
                            SALog.printStackTrace(e);
                        }
                        if (tabView == null) {
                            try {
                                Field mViewField = currentTabClass.getDeclaredField("mView");
                                mViewField.setAccessible(true);
                                try {
                                    tabView = (View) mViewField.get(tab);
                                } catch (IllegalAccessException e) {
                                    SALog.printStackTrace(e);
                                }
                            } catch (NoSuchFieldException e) {
                                SALog.printStackTrace(e);
                            }
                        }
                        if (tabView != null) {
                            viewNode = AopUtil.addViewPathProperties(activity, tabView, properties);
                        }

                        if (view == null || view.getId() == -1) {
                            try {
                                field = currentTabClass.getDeclaredField("mParent");
                            } catch (NoSuchFieldException ex) {
                                field = currentTabClass.getDeclaredField("parent");
                            }
                            field.setAccessible(true);
                            view = (View) field.get(tab);
                        }

                        if (view != null && view.getId() != View.NO_ID) {
                            String resourceId = activity.getResources().getResourceEntryName(view.getId());
                            if (!TextUtils.isEmpty(resourceId)) {
                                properties.put(AopConstants.ELEMENT_ID, resourceId);
                            }
                        }

                        if (view != null) {
                            //获取 View 自定义属性
                            JSONObject p = (JSONObject) view.getTag(R.id.sensors_analytics_tag_view_properties);
                            if (p != null) {
                                AopUtil.mergeJSONObject(p, properties);
                            }
                        }
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            }

            SensorsDataAPI.sharedInstance().trackAutoEvent(AopConstants.APP_CLICK_EVENT_NAME, properties, viewNode);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void trackMenuItem(MenuItem menuItem) {
        trackMenuItem(null, menuItem);
    }

    public static void trackMenuItem(final Object object, final MenuItem menuItem) {
        try {
            ThreadUtils.getSinglePool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (menuItem == null) {
                            return;
                        }
                        //关闭 AutoTrack
                        if (!SensorsDataAPI.sharedInstance().isAutoTrackEnabled()) {
                            return;
                        }

                        //$AppClick 被过滤
                        if (SensorsDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_CLICK)) {
                            return;
                        }

                        //MenuItem 被忽略
                        if (AopUtil.isViewIgnored(MenuItem.class)) {
                            return;
                        }

                        if (isDeBounceTrack(menuItem)) {
                            return;
                        }

                        Context context = null;
                        if (object != null) {
                            if (object instanceof Context) {
                                context = (Context) object;
                            }
                        }

                        View view = WindowHelper.getClickView(menuItem);
                        if (context == null && view != null) {
                            context = view.getContext();
                        }

                        //将 Context 转成 Activity
                        Activity activity = null;
                        if (context != null) {
                            activity = AopUtil.getActivityFromContext(context, null);
                        }

                        //Activity 被忽略
                        if (activity != null) {
                            if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                                return;
                            }
                        }

                        //获取View ID
                        String idString = null;
                        try {
                            if (context != null) {
                                idString = context.getResources().getResourceEntryName(menuItem.getItemId());
                            }
                        } catch (Exception e) {
                            SALog.printStackTrace(e);
                        }

                        JSONObject properties = new JSONObject();

                        //$screen_name & $title
                        if (activity != null) {
                            SensorsDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
                        }

                        //ViewID
                        if (!TextUtils.isEmpty(idString)) {
                            properties.put(AopConstants.ELEMENT_ID, idString);
                        }

                        // 2020/4/27 新增  1. 解决 Actionbar 返回按钮 获取不到 $element_content
                        String elementContent = null;
                        if (!TextUtils.isEmpty(menuItem.getTitle())) {
                            elementContent = menuItem.getTitle().toString();
                        }

                        ViewNode viewNode = null;
                        if (view != null) {
                            if (TextUtils.isEmpty(elementContent)) {
                                elementContent = ViewUtil.getViewContentAndType(view).getViewContent();
                            }
                            viewNode = AopUtil.addViewPathProperties(activity, view, properties);
                        }
                        properties.put(AopConstants.ELEMENT_CONTENT, elementContent);
                        //Type
                        properties.put(AopConstants.ELEMENT_TYPE, "MenuItem");

                        SensorsDataAPI.sharedInstance().trackAutoEvent(AopConstants.APP_CLICK_EVENT_NAME, properties, viewNode);
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void trackRadioGroup(RadioGroup view, int checkedId) {
        try {
            if (view == null) {
                return;
            }

            View childView = view.findViewById(checkedId);
            if (childView == null || !childView.isPressed()) {
                return;
            }

            //关闭 AutoTrack
            if (!SensorsDataAPI.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //$AppClick 被过滤
            if (SensorsDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取所在的 Context
            Context context = view.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, view);

            //Activity 被忽略
            if (activity != null) {
                if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            // 获取 view 所在的 fragment
            Object fragment = AopUtil.getFragmentFromView(view, activity);

            // fragment 忽略
            if (fragment != null) {
                if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(fragment.getClass())) {
                    return;
                }
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(view)) {
                return;
            }

            JSONObject properties = new JSONObject();

            //ViewId
            String idString = AopUtil.getViewId(view);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AopConstants.ELEMENT_ID, idString);
            }

            //$screen_name & $title
            if (activity != null) {
                SensorsDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }

            String viewType = "RadioButton";
            if (childView != null) {
                viewType = AopUtil.getViewType(childView.getClass().getCanonicalName(), "RadioButton");
            }
            properties.put(AopConstants.ELEMENT_TYPE, viewType);

            //获取变更后的选中项的ID
            int checkedRadioButtonId = view.getCheckedRadioButtonId();
            ViewNode viewNode = null;
            if (activity != null) {
                try {
                    RadioButton radioButton = activity.findViewById(checkedRadioButtonId);
                    if (radioButton != null) {
                        if (!TextUtils.isEmpty(radioButton.getText())) {
                            String viewText = radioButton.getText().toString();
                            if (!TextUtils.isEmpty(viewText)) {
                                properties.put(AopConstants.ELEMENT_CONTENT, viewText);
                            }
                        }
                        viewNode = AopUtil.addViewPathProperties(activity, radioButton, properties);
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }

            //fragmentName
            if (fragment != null) {
                AopUtil.getScreenNameAndTitleFromFragment(properties, fragment, activity);
            }

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.sensors_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            SensorsDataAPI.sharedInstance().trackAutoEvent(AopConstants.APP_CLICK_EVENT_NAME, properties, viewNode);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void trackDialog(DialogInterface dialogInterface, int whichButton) {
        try {
            //关闭 AutoTrack
            if (!SensorsDataAPI.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //$AppClick 被过滤
            if (SensorsDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取所在的Context
            Dialog dialog = null;
            if (dialogInterface instanceof Dialog) {
                dialog = (Dialog) dialogInterface;
            }

            if (dialog == null) {
                return;
            }

            if (isDeBounceTrack(dialog)) {
                return;
            }

            Context context = dialog.getContext();

            //将Context转成Activity
            Activity activity = AopUtil.getActivityFromContext(context, null);

            if (activity == null) {
                activity = dialog.getOwnerActivity();
            }

            //Activity 被忽略
            if (activity != null) {
                if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //Dialog 被忽略
            if (AopUtil.isViewIgnored(Dialog.class)) {
                return;
            }

            JSONObject properties = new JSONObject();

            try {
                Window window = dialog.getWindow();
                if (window != null && window.isActive()) {
                    String idString = (String) dialog.getWindow().getDecorView().getTag(R.id.sensors_analytics_tag_view_id);
                    if (!TextUtils.isEmpty(idString)) {
                        properties.put(AopConstants.ELEMENT_ID, idString);
                    }
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }

            //$screen_name & $title
            if (activity != null) {
                SensorsDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }

            //由于 RN 中 dialog 未屏蔽，直接走到原生，导致 dialog screen_name 取的是原生的。
            VisualUtil.mergeRnScreenNameAndTitle(properties);

            properties.put(AopConstants.ELEMENT_TYPE, "Dialog");

            Class<?> supportAlertDialogClass = null;
            Class<?> androidXAlertDialogClass = null;
            Class<?> currentAlertDialogClass;
            try {
                supportAlertDialogClass = Class.forName("android.support.v7.app.AlertDialog");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXAlertDialogClass = Class.forName("androidx.appcompat.app.AlertDialog");
            } catch (Exception e) {
                //ignored
            }

            if (supportAlertDialogClass == null && androidXAlertDialogClass == null) {
                return;
            }

            if (supportAlertDialogClass != null) {
                currentAlertDialogClass = supportAlertDialogClass;
            } else {
                currentAlertDialogClass = androidXAlertDialogClass;
            }

            ViewNode viewNode = null;
            if (dialog instanceof android.app.AlertDialog) {
                android.app.AlertDialog alertDialog = (android.app.AlertDialog) dialog;
                Button button = alertDialog.getButton(whichButton);
                if (button != null) {
                    if (!TextUtils.isEmpty(button.getText())) {
                        properties.put(AopConstants.ELEMENT_CONTENT, button.getText());
                    }
                    viewNode = AopUtil.addViewPathProperties(activity, button, properties);
                } else {
                    ListView listView = alertDialog.getListView();
                    if (listView != null) {
                        ListAdapter listAdapter = listView.getAdapter();
                        Object object = listAdapter.getItem(whichButton);
                        if (object != null) {
                            if (object instanceof String) {
                                properties.put(AopConstants.ELEMENT_CONTENT, object);
                            }
                        }
                        View clickView = listView.getChildAt(whichButton);
                        if (clickView != null) {
                            viewNode = AopUtil.addViewPathProperties(activity, clickView, properties);
                        }
                    }
                }

            } else if (currentAlertDialogClass.isInstance(dialog)) {
                Button button = null;
                try {
                    Method getButtonMethod = dialog.getClass().getMethod("getButton", int.class);
                    if (getButtonMethod != null) {
                        button = (Button) getButtonMethod.invoke(dialog, whichButton);
                    }
                } catch (Exception e) {
                    //ignored
                }

                if (button != null) {
                    if (!TextUtils.isEmpty(button.getText())) {
                        properties.put(AopConstants.ELEMENT_CONTENT, button.getText());
                    }
                    viewNode = AopUtil.addViewPathProperties(activity, button, properties);
                } else {
                    try {
                        Method getListViewMethod = dialog.getClass().getMethod("getListView");
                        if (getListViewMethod != null) {
                            ListView listView = (ListView) getListViewMethod.invoke(dialog);
                            if (listView != null) {
                                ListAdapter listAdapter = listView.getAdapter();
                                Object object = listAdapter.getItem(whichButton);
                                if (object != null) {
                                    if (object instanceof String) {
                                        properties.put(AopConstants.ELEMENT_CONTENT, object);
                                    }
                                }
                                View clickView = listView.getChildAt(whichButton);
                                if (clickView != null) {
                                    viewNode = AopUtil.addViewPathProperties(activity, clickView, properties);
                                }
                            }
                        }
                    } catch (Exception e) {
                        //ignored
                    }
                }
            }

            SensorsDataAPI.sharedInstance().trackAutoEvent(AopConstants.APP_CLICK_EVENT_NAME, properties, viewNode);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void trackListView(AdapterView<?> adapterView, View view, int position) {
        try {
            //防止 Spinner 恢复数据时造成的空指针问题.
            if (view == null) {
                return;
            }
            //闭 AutoTrack
            if (!SensorsDataAPI.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //$AppClick 被过滤
            if (SensorsDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取所在的 Context
            Context context = view.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, view);

            //Activity 被忽略
            if (activity != null) {
                if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            // 获取 view 所在的 fragment
            Object fragment = AopUtil.getFragmentFromView(adapterView, activity);

            // fragment 忽略
            if (fragment != null) {
                if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(fragment.getClass())) {
                    return;
                }
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(adapterView)) {
                return;
            }

            JSONObject properties = new JSONObject();

            if (adapterView instanceof ListView) {
                properties.put(AopConstants.ELEMENT_TYPE, "ListView");
                if (AopUtil.isViewIgnored(ListView.class)) {
                    return;
                }
            } else if (adapterView instanceof GridView) {
                properties.put(AopConstants.ELEMENT_TYPE, "GridView");
                if (AopUtil.isViewIgnored(GridView.class)) {
                    return;
                }
            } else if (adapterView instanceof Spinner) {
                properties.put(AopConstants.ELEMENT_TYPE, "Spinner");
                if (AopUtil.isViewIgnored(Spinner.class)) {
                    return;
                }
            }
            if(KeyboardViewUtil.isKeyboardView(view)){
                return;
            }

            //ViewId
            String idString = AopUtil.getViewId(adapterView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AopConstants.ELEMENT_ID, idString);
            }

            //扩展属性
            Adapter adapter = adapterView.getAdapter();
            if (adapter instanceof HeaderViewListAdapter) {
                adapter = ((HeaderViewListAdapter) adapter).getWrappedAdapter();
            }

            if (adapter instanceof SensorsAdapterViewItemTrackProperties) {
                try {
                    SensorsAdapterViewItemTrackProperties objectProperties = (SensorsAdapterViewItemTrackProperties) adapter;
                    JSONObject jsonObject = objectProperties.getSensorsItemTrackProperties(position);
                    if (jsonObject != null) {
                        AopUtil.mergeJSONObject(jsonObject, properties);
                    }
                } catch (JSONException e) {
                    SALog.printStackTrace(e);
                }
            }

            ViewNode viewNode = AopUtil.addViewPathProperties(activity, view, properties);

            //Activity 名称和页面标题
            if (activity != null) {
                SensorsDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }

            String viewText = null;
            if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            } else {
                viewText = AopUtil.getViewText(view);
            }
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AopConstants.ELEMENT_CONTENT, viewText);
            }

            //fragmentName
            if (fragment != null) {
                AopUtil.getScreenNameAndTitleFromFragment(properties, fragment, activity);
            }

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.sensors_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            SensorsDataAPI.sharedInstance().trackAutoEvent(AopConstants.APP_CLICK_EVENT_NAME, properties, viewNode);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void trackDrawerOpened(View view) {
        try {
            if (view == null) {
                return;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("$element_content", "Open");

            SensorsDataAPI.sharedInstance().setViewProperties(view, jsonObject);

            trackViewOnClick(view);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void trackDrawerClosed(View view) {
        try {
            if (view == null) {
                return;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("$element_content", "Close");

            SensorsDataAPI.sharedInstance().setViewProperties(view, jsonObject);

            trackViewOnClick(view);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void trackViewOnClick(View view) {
        if (view == null) {
            return;
        }
        trackViewOnClick(view, view.isPressed());
    }

    public static void trackViewOnClick(View view, boolean isFromUser) {
        try {
            if (view == null) {
                return;
            }
            //关闭 AutoTrack
            if (!SensorsDataAPI.sharedInstance().isAutoTrackEnabled()) {
                return;
            }
            //$AppClick 被过滤
            if (SensorsDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取所在的 Context
            Context context = view.getContext();

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, view);

            //Activity 被忽略
            if (activity != null) {
                if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            // 获取 view 所在的 fragment
            Object fragment = AopUtil.getFragmentFromView(view, activity);

            // fragment 忽略
            if (fragment != null) {
                if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(fragment.getClass())) {
                    return;
                }
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(view)) {
                return;
            }

            if (SensorsDataUtils.isDoubleClick(view)) {
                return;
            }

            if (KeyboardViewUtil.isKeyboardView(view)) {
                return;
            }

            JSONObject properties = new JSONObject();

            if (AopUtil.injectClickInfo(view, properties, isFromUser)) {
                SensorsDataAPI.sharedInstance().trackAutoEvent(AopConstants.APP_CLICK_EVENT_NAME, properties, AopUtil.addViewPathProperties(activity, view, properties));
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void track(String eventName, String properties) {
        try {
            if (TextUtils.isEmpty(eventName)) {
                return;
            }
            JSONObject pro = null;
            if (!TextUtils.isEmpty(properties)) {
                try {
                    pro = new JSONObject(properties);
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
            SensorsDataAPI.sharedInstance().trackInternal(eventName, pro);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void loadUrl(View webView, String url) {
        loadUrl2(webView, url);
        invokeWebViewLoad(webView, "loadUrl", new Object[]{url}, new Class[]{String.class});
    }

    public static void loadUrl2(View webView, String url) {
        if (webView == null) {
            SALog.i(TAG, "WebView has not initialized.");
            return;
        }
        setupH5Bridge(webView);
    }

    public static void loadUrl(View webView, String url, Map<String, String> additionalHttpHeaders) {
        loadUrl2(webView, url, additionalHttpHeaders);
        invokeWebViewLoad(webView, "loadUrl", new Object[]{url, additionalHttpHeaders}, new Class[]{String.class, Map.class});
    }

    public static void loadUrl2(View webView, String url, Map<String, String> additionalHttpHeaders) {
        if (webView == null) {
            SALog.i(TAG, "WebView has not initialized.");
            return;
        }
        setupH5Bridge(webView);
    }

    public static void loadData(View webView, String data, String mimeType, String encoding) {
        loadData2(webView, data, mimeType, encoding);
        invokeWebViewLoad(webView, "loadData", new Object[]{data, mimeType, encoding}, new Class[]{String.class, String.class, String.class});
    }

    public static void loadData2(View webView, String data, String mimeType, String encoding) {
        if (webView == null) {
            SALog.i(TAG, "WebView has not initialized.");
            return;
        }
        setupH5Bridge(webView);
    }

    public static void loadDataWithBaseURL(View webView, String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        loadDataWithBaseURL2(webView, baseUrl, data, mimeType, encoding, historyUrl);
        invokeWebViewLoad(webView, "loadDataWithBaseURL", new Object[]{baseUrl, data, mimeType, encoding, historyUrl},
                new Class[]{String.class, String.class, String.class, String.class, String.class});
    }

    public static void loadDataWithBaseURL2(View webView, String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        if (webView == null) {
            SALog.i(TAG, "WebView has not initialized.");
            return;
        }
        setupH5Bridge(webView);
    }

    public static void postUrl(View webView, String url, byte[] postData) {
        postUrl2(webView, url, postData);
        invokeWebViewLoad(webView, "postUrl", new Object[]{url, postData},
                new Class[]{String.class, byte[].class});
    }

    public static void postUrl2(View webView, String url, byte[] postData) {
        if (webView == null) {
            SALog.i(TAG, "WebView has not initialized.");
            return;
        }
        setupH5Bridge(webView);
    }

    private static void setupH5Bridge(View webView) {
        if (SensorsDataAPI.sharedInstance() instanceof SensorsDataAPIEmptyImplementation) {
            return;
        }
        if (isSupportJellyBean() && SensorsDataAPI.sharedInstance().getConfigOptions() != null && SensorsDataAPI.sharedInstance().getConfigOptions().isAutoTrackWebView) {
            setupWebView(webView);
        }
        if (isSupportJellyBean()) {
            addWebViewVisualInterface(webView);
        }
    }

    private static void invokeWebViewLoad(View webView, String methodName, Object[] params, Class[] paramTypes) {
        if (webView == null) {
            SALog.i(TAG, "WebView has not initialized.");
            return;
        }
        try {
            Class<?> clazz = webView.getClass();
            Method loadMethod = clazz.getMethod(methodName, paramTypes);
            loadMethod.invoke(webView, params);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    static void addWebViewVisualInterface(View webView) {
        if (webView != null && webView.getTag(R.id.sensors_analytics_tag_view_webview_visual) == null) {
            webView.setTag(R.id.sensors_analytics_tag_view_webview_visual, new Object());
            addJavascriptInterface(webView, new WebViewVisualInterface(webView), "SensorsData_App_Visual_Bridge");
        }
    }

    private static boolean isSupportJellyBean() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 && !SensorsDataAPI.getConfigOptions().isWebViewSupportJellyBean) {
            SALog.d(TAG, "For applications targeted to API level JELLY_BEAN or below, this feature NOT SUPPORTED");
            return false;
        }
        return true;
    }

    private static void setupWebView(View webView) {
        if (webView != null && webView.getTag(R.id.sensors_analytics_tag_view_webview) == null) {
            webView.setTag(R.id.sensors_analytics_tag_view_webview, new Object());
            addJavascriptInterface(webView, new AppWebViewInterface(SensorsDataAPI.sharedInstance().getContext(), null, false, webView), "SensorsData_APP_New_H5_Bridge");
        }
    }

    private static void addJavascriptInterface(View webView, Object obj, String interfaceName) {
        try {
            Class<?> clazz = webView.getClass();
            try {
                Method getSettingsMethod = clazz.getMethod("getSettings");
                Object settings = getSettingsMethod.invoke(webView);
                if (settings != null) {
                    Method setJavaScriptEnabledMethod = settings.getClass().getMethod("setJavaScriptEnabled", boolean.class);
                    setJavaScriptEnabledMethod.invoke(settings, true);
                }
            } catch (Exception e) {
                //ignore
            }
            Method addJSMethod = clazz.getMethod("addJavascriptInterface", Object.class, String.class);
            addJSMethod.invoke(webView, obj, interfaceName);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}