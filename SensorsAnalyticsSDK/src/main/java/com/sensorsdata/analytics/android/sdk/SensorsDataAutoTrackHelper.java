/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.sensorsdata.analytics.android.sdk.util.AopUtil;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@SuppressWarnings("unused")
public class SensorsDataAutoTrackHelper {
    private static HashMap<Integer, Long> eventTimestamp = new HashMap<>();

    private static boolean isDeBounceTrack(Object object) {
        boolean isDeBounceTrack = false;
        long currentOnClickTimestamp = System.currentTimeMillis();
        Object targetObject = eventTimestamp.get(object.hashCode());
        if (targetObject != null) {
            long lastOnClickTimestamp = (long) targetObject;
            if ((currentOnClickTimestamp - lastOnClickTimestamp) < 500) {
                isDeBounceTrack = true;
            }
        }

        eventTimestamp.put(object.hashCode(), currentOnClickTimestamp);
        return isDeBounceTrack;
    }

    private static void traverseView(String fragmentName, ViewGroup root) {
        try {
            if (TextUtils.isEmpty(fragmentName)) {
                return;
            }

            if (root == null) {
                return;
            }

            final int childCount = root.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                final View child = root.getChildAt(i);
                if (child instanceof ListView ||
                        child instanceof GridView ||
                        child instanceof Spinner ||
                        child instanceof RadioGroup) {
                    child.setTag(R.id.sensors_analytics_tag_view_fragment_name, fragmentName);
                } else if (child instanceof ViewGroup) {
                    traverseView(fragmentName, (ViewGroup) child);
                } else {
                    child.setTag(R.id.sensors_analytics_tag_view_fragment_name, fragmentName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onFragmentViewCreated(Object object, View rootView, Bundle bundle) {
        try {
            if (!(object instanceof android.support.v4.app.Fragment)) {
                return;
            }

            android.support.v4.app.Fragment fragment = (android.support.v4.app.Fragment) object;
            //Fragment名称
            String fragmentName = fragment.getClass().getName();

            if (rootView instanceof ViewGroup) {
                traverseView(fragmentName, (ViewGroup) rootView);
            } else {
                rootView.setTag(R.id.sensors_analytics_tag_view_fragment_name, fragmentName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackRN(Object target, int reactTag, int s, boolean b) {
        try {
            if (!SensorsDataAPI.sharedInstance().isReactNativeAutoTrackEnabled()) {
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

            JSONObject properties = new JSONObject();
            properties.put(AopConstants.ELEMENT_TYPE, "RNView");
            if (target != null) {
                Class<?> clazz = Class.forName("com.facebook.react.uimanager.NativeViewHierarchyManager");
                Method resolveViewMethod = clazz.getMethod("resolveView", int.class);
                if (resolveViewMethod != null) {
                    Object object = resolveViewMethod.invoke(target, reactTag);
                    if (object != null) {
                        View view = (View) object;
                        //获取所在的 Context
                        Context context = view.getContext();

                        //将 Context 转成 Activity
                        Activity activity = AopUtil.getActivityFromContext(context, view);
                        //$screen_name & $title
                        if (activity != null) {
                            properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                            String activityTitle = AopUtil.getActivityTitle(activity);
                            if (!TextUtils.isEmpty(activityTitle)) {
                                properties.put(AopConstants.TITLE, activityTitle);
                            }
                        }
                        if (view instanceof CompoundButton) {//ReactSwitch
                            return;
                        }
                        if (view instanceof TextView) {
                            TextView textView = (TextView) view;
                            if (!TextUtils.isEmpty(textView.getText())) {
                                properties.put(AopConstants.ELEMENT_CONTENT, textView.getText().toString());
                            }
                        } else if (view instanceof ViewGroup) {
                            StringBuilder stringBuilder = new StringBuilder();
                            String viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                            if (!TextUtils.isEmpty(viewText)) {
                                viewText = viewText.substring(0, viewText.length() - 1);
                            }
                            properties.put(AopConstants.ELEMENT_CONTENT, viewText);
                        }
                    }
                }
            }
            SensorsDataAPI.sharedInstance().track(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void trackFragmentAppViewScreen(android.support.v4.app.Fragment fragment) {
        try {
            if (!SensorsDataAPI.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
                return;
            }

            if ("com.bumptech.glide.manager.SupportRequestManagerFragment".equals(fragment.getClass().getCanonicalName())) {
                return;
            }

            Set<Integer> fragmentsSets = SensorsDataAPI.sharedInstance().getAutoTrackFragments();
            boolean isAutoTrackFragment = SensorsDataAPI.sharedInstance().isFragmentAutoTrackAppViewScreen(fragment.getClass());
            if (!isAutoTrackFragment) {
                return;
            }

            if (fragment.getClass().getAnnotation(SensorsDataIgnoreTrackAppViewScreen.class) != null
                    && fragmentsSets == null  && isAutoTrackFragment) {
                return;
            }

            JSONObject properties = new JSONObject();

            String fragmentName = fragment.getClass().getCanonicalName();
            Activity activity = fragment.getActivity();
            if (activity != null) {
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AopConstants.TITLE, activityTitle);
                }
                properties.put(AopConstants.SCREEN_NAME, String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), fragmentName));
            } else {
                properties.put(AopConstants.SCREEN_NAME, fragmentName);
            }

            SensorsDataFragmentTitle sensorsDataFragmentTitle = fragment.getClass().getAnnotation(SensorsDataFragmentTitle.class);
            if (sensorsDataFragmentTitle != null) {
                properties.put(AopConstants.TITLE, sensorsDataFragmentTitle.title());
            }

            if (fragment instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) fragment;

                String screenUrl = screenAutoTracker.getScreenUrl();
                JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                if (otherProperties != null) {
                    SensorsDataUtils.mergeJSONObject(otherProperties, properties);
                }

                SensorsDataAPI.sharedInstance().trackViewScreen(screenUrl, properties);
            } else {
                SensorsDataAutoTrackAppViewScreenUrl autoTrackAppViewScreenUrl = fragment.getClass().getAnnotation(SensorsDataAutoTrackAppViewScreenUrl.class);
                if (autoTrackAppViewScreenUrl != null) {
                    String screenUrl = autoTrackAppViewScreenUrl.url();
                    if (TextUtils.isEmpty(screenUrl)) {
                        screenUrl = fragmentName;
                    }
                    SensorsDataAPI.sharedInstance().trackViewScreen(screenUrl, properties);
                } else {
                    SensorsDataAPI.sharedInstance().track("$AppViewScreen", properties);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackFragmentResume(Object object) {
        if (!SensorsDataAPI.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
            return;
        }

        if (!(object instanceof android.support.v4.app.Fragment)) {
            return;
        }

        android.support.v4.app.Fragment fragment = (android.support.v4.app.Fragment) object;
        android.support.v4.app.Fragment parentFragment = fragment.getParentFragment();
        if (parentFragment == null) {
            if (!fragment.isHidden() && fragment.getUserVisibleHint()) {
                trackFragmentAppViewScreen(fragment);
            }
        } else {
            if (!fragment.isHidden() && fragment.getUserVisibleHint() && !parentFragment.isHidden() && parentFragment.getUserVisibleHint()) {
                trackFragmentAppViewScreen(fragment);
            }
        }
    }

    public static void trackFragmentSetUserVisibleHint(Object object, boolean isVisibleToUser) {
        if (!SensorsDataAPI.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
            return;
        }

        if (!(object instanceof android.support.v4.app.Fragment)) {
            return;
        }

        android.support.v4.app.Fragment fragment = (android.support.v4.app.Fragment) object;

        android.support.v4.app.Fragment parentFragment = fragment.getParentFragment();
        if (parentFragment == null) {
            if (isVisibleToUser) {
                if (fragment.isResumed()) {
                    if (!fragment.isHidden()) {
                        trackFragmentAppViewScreen(fragment);
                    }
                }
            }
        } else {
            if (isVisibleToUser && parentFragment.getUserVisibleHint()) {
                if (fragment.isResumed() && parentFragment.isResumed()) {
                    if (!fragment.isHidden() && !parentFragment.isHidden()) {
                        trackFragmentAppViewScreen(fragment);
                    }
                }
            }
        }
    }

    public static void trackOnHiddenChanged(Object object, boolean hidden) {
        if (!SensorsDataAPI.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
            return;
        }

        if (!(object instanceof android.support.v4.app.Fragment)) {
            return;
        }

        android.support.v4.app.Fragment fragment = (android.support.v4.app.Fragment) object;
        android.support.v4.app.Fragment parentFragment = fragment.getParentFragment();
        if (parentFragment == null) {
            if (!hidden) {
                if (fragment.isResumed()) {
                    if (fragment.getUserVisibleHint()) {
                        trackFragmentAppViewScreen(fragment);
                    }
                }
            }
        } else {
            if (!hidden && !parentFragment.isHidden()) {
                if (fragment.isResumed() && parentFragment.isResumed()) {
                    if (fragment.getUserVisibleHint() && parentFragment.getUserVisibleHint()) {
                        trackFragmentAppViewScreen(fragment);
                    }
                }
            }
        }
    }

    public static void trackExpandableListViewOnGroupClick(ExpandableListView expandableListView, View view,
                                                           int groupPosition) {
        try {
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

            // ExpandableListView Type 被忽略
            if (AopUtil.isViewIgnored(ExpandableListView.class)) {
                return;
            }

            // View 被忽略
            if (AopUtil.isViewIgnored(expandableListView)) {
                return;
            }

            JSONObject properties = new JSONObject();

            AopUtil.addViewPathProperties(activity, view, properties);

            // $screen_name & $title
            if (activity != null) {
                properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AopConstants.TITLE, activityTitle);
                }
            }

            // ViewId
            String idString = AopUtil.getViewId(expandableListView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AopConstants.ELEMENT_ID, idString);
            }

            properties.put(AopConstants.ELEMENT_POSITION, String.format(Locale.CHINA, "%d", groupPosition));
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
                    e.printStackTrace();
                }
            }
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AopConstants.ELEMENT_CONTENT, viewText);
            }

            //fragmentName
            AopUtil.getFragmentNameFromView(expandableListView, properties);

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
                        e.printStackTrace();
                    }
                }
            }

            SensorsDataAPI.sharedInstance().track(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackExpandableListViewOnChildClick(ExpandableListView expandableListView, View view,
                                                           int groupPosition, int childPosition) {
        try {
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

            properties.put(AopConstants.ELEMENT_POSITION, String.format(Locale.CHINA, "%d:%d", groupPosition, childPosition));

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

            AopUtil.addViewPathProperties(activity, view, properties);

            //$screen_name & $title
            if (activity != null) {
                properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AopConstants.TITLE, activityTitle);
                }
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
                    e.printStackTrace();
                }
            }
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AopConstants.ELEMENT_CONTENT, viewText);
            }

            //fragmentName
            AopUtil.getFragmentNameFromView(expandableListView, properties);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.sensors_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            SensorsDataAPI.sharedInstance().track(AopConstants.APP_CLICK_EVENT_NAME, properties);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackTabHost(String tabName) {
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

            properties.put(AopConstants.ELEMENT_CONTENT, tabName);
            properties.put(AopConstants.ELEMENT_TYPE, "TabHost");

            SensorsDataAPI.sharedInstance().track(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackTabLayoutSelected(Object object, Object tab) {
        try {
            //关闭 AutoTrack
            if (!SensorsDataAPI.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            //$AppClick 被过滤
            if (SensorsDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //TabLayout 被忽略
            if (AopUtil.isViewIgnored(Class.forName("android.support.design.widget.TabLayout"))) {
                return;
            }

            if (isDeBounceTrack(tab)) {
                return;
            }

            Context context = null;
            if (object instanceof Context) {
                context = (Context) object;
            }

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, null);

            //Activity 被忽略
            if (activity != null) {
                if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            JSONObject properties = new JSONObject();

            //$screen_name & $title
            if (activity != null) {
                properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AopConstants.TITLE, activityTitle);
                }
            }

            Class<?> tabClass = Class.forName("android.support.design.widget.TabLayout$Tab");
            if (tabClass != null) {
                Method method = null;
                try {
                    method = tabClass.getMethod("getText");
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
            }

            //Type
            properties.put(AopConstants.ELEMENT_TYPE, "TabLayout");

            SensorsDataAPI.sharedInstance().track(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackMenuItem(Object object, MenuItem menuItem) {
        try {
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
            if (object instanceof Context) {
                context = (Context) object;
            }

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, null);

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
                e.printStackTrace();
            }

            JSONObject properties = new JSONObject();

            //$screen_name & $title
            if (activity != null) {
                properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AopConstants.TITLE, activityTitle);
                }
            }

            //ViewID
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AopConstants.ELEMENT_ID, idString);
            }

            //Content
            if (!TextUtils.isEmpty(menuItem.getTitle())) {
                properties.put(AopConstants.ELEMENT_CONTENT, menuItem.getTitle());
            }

            //Type
            properties.put(AopConstants.ELEMENT_TYPE, "MenuItem");

            SensorsDataAPI.sharedInstance().track(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackRadioGroup(RadioGroup view, int checkedId) {
        try {
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
                properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AopConstants.TITLE, activityTitle);
                }
            }

            properties.put(AopConstants.ELEMENT_TYPE, "RadioButton");

            //获取变更后的选中项的ID
            int checkedRadioButtonId = view.getCheckedRadioButtonId();
            if (activity != null) {
                try {
                    RadioButton radioButton = (RadioButton) activity.findViewById(checkedRadioButtonId);
                    if (radioButton != null) {
                        if (!TextUtils.isEmpty(radioButton.getText())) {
                            String viewText = radioButton.getText().toString();
                            if (!TextUtils.isEmpty(viewText)) {
                                properties.put(AopConstants.ELEMENT_CONTENT, viewText);
                            }
                        }
                        AopUtil.addViewPathProperties(activity, radioButton, properties);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //fragmentName
            AopUtil.getFragmentNameFromView(view, properties);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.sensors_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            SensorsDataAPI.sharedInstance().track(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
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
                if (dialog.getWindow() != null) {
                    String idString = (String) dialog.getWindow().getDecorView().getTag(R.id.sensors_analytics_tag_view_id);
                    if (!TextUtils.isEmpty(idString)) {
                        properties.put(AopConstants.ELEMENT_ID, idString);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //$screen_name & $title
            if (activity != null) {
                properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AopConstants.TITLE, activityTitle);
                }
            }

            properties.put(AopConstants.ELEMENT_TYPE, "Dialog");

            if (dialog instanceof android.app.AlertDialog) {
                android.app.AlertDialog alertDialog = (android.app.AlertDialog) dialog;
                Button button = alertDialog.getButton(whichButton);
                if (button != null) {
                    if (!TextUtils.isEmpty(button.getText())) {
                        properties.put(AopConstants.ELEMENT_CONTENT, button.getText());
                    }
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
                    }
                }

            } else if (dialog instanceof android.support.v7.app.AlertDialog) {
                android.support.v7.app.AlertDialog alertDialog = (android.support.v7.app.AlertDialog) dialog;
                Button button = alertDialog.getButton(whichButton);
                if (button != null) {
                    if (!TextUtils.isEmpty(button.getText())) {
                        properties.put(AopConstants.ELEMENT_CONTENT, button.getText());
                    }
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
                    }
                }
            }

            SensorsDataAPI.sharedInstance().track(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackListView(AdapterView<?> adapterView, View view, int position) {
        try {
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

            //View Type 被忽略
            if (AopUtil.isViewIgnored(adapterView.getClass())) {
                return;
            }

            JSONObject properties = new JSONObject();

            List<Class> mIgnoredViewTypeList = SensorsDataAPI.sharedInstance().getIgnoredViewTypeList();
            if (mIgnoredViewTypeList != null) {
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
                }
            }

            //ViewId
            String idString = AopUtil.getViewId(adapterView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AopConstants.ELEMENT_ID, idString);
            }

            //扩展属性
            Adapter adapter = adapterView.getAdapter();
            if (adapter != null && adapter instanceof SensorsAdapterViewItemTrackProperties) {
                try {
                    SensorsAdapterViewItemTrackProperties objectProperties = (SensorsAdapterViewItemTrackProperties) adapter;
                    JSONObject jsonObject = objectProperties.getSensorsItemTrackProperties(position);
                    if (jsonObject != null) {
                        AopUtil.mergeJSONObject(jsonObject, properties);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            AopUtil.addViewPathProperties(activity, view, properties);

            //Activity 名称和页面标题
            if (activity != null) {
                properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AopConstants.TITLE, activityTitle);
                }
            }

            //点击的 position
            properties.put(AopConstants.ELEMENT_POSITION, String.valueOf(position));

            String viewText = null;
            if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (view instanceof TextView) {
                viewText = ((TextView) view).getText().toString();
            }
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AopConstants.ELEMENT_CONTENT, viewText);
            }

            //fragmentName
            AopUtil.getFragmentNameFromView(adapterView, properties);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.sensors_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            SensorsDataAPI.sharedInstance().track(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackDrawerOpened(View view) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("$element_content", "Open");

            SensorsDataAPI.sharedInstance().setViewProperties(view, jsonObject);

            trackViewOnClick(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackDrawerClosed(View view) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("$element_content", "Close");

            SensorsDataAPI.sharedInstance().setViewProperties(view, jsonObject);

            trackViewOnClick(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackViewOnClick(View view) {
        try {
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

            //View 被忽略
            if (AopUtil.isViewIgnored(view)) {
                return;
            }

            long currentOnClickTimestamp = System.currentTimeMillis();
            String tag = (String) view.getTag(R.id.sensors_analytics_tag_view_onclick_timestamp);
            if (!TextUtils.isEmpty(tag)) {
                try {
                    long lastOnClickTimestamp = Long.parseLong(tag);
                    if ((currentOnClickTimestamp - lastOnClickTimestamp) < 500) {
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            view.setTag(R.id.sensors_analytics_tag_view_onclick_timestamp, String.valueOf(currentOnClickTimestamp));

            JSONObject properties = new JSONObject();

            AopUtil.addViewPathProperties(activity, view, properties);

            //ViewId
            String idString = AopUtil.getViewId(view);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AopConstants.ELEMENT_ID, idString);
            }

            //$screen_name & $title
            if (activity != null) {
                properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AopConstants.TITLE, activityTitle);
                }
            }

            String viewType = view.getClass().getCanonicalName();

            Class<?> switchCompatClass = null;
            try {
                switchCompatClass = Class.forName("android.support.v7.widget.SwitchCompat");
            } catch (Exception e) {
                //ignored
            }

            CharSequence viewText = null;
            if (view instanceof CheckBox) { // CheckBox
                viewType = "CheckBox";
                CheckBox checkBox = (CheckBox) view;
                viewText = checkBox.getText();
            } else if (view instanceof ViewPager) {
                viewType = "ViewPager";
                try {
                    ViewPager viewPager = (ViewPager) view;
                    viewText = viewPager.getAdapter().getPageTitle(viewPager.getCurrentItem());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (switchCompatClass != null && switchCompatClass.isInstance(view)) {
                viewType = "SwitchCompat";
                CompoundButton switchCompat = (CompoundButton) view;
                Method method;
                if (switchCompat.isChecked()) {
                    method = view.getClass().getMethod("getTextOn");
                } else {
                    method = view.getClass().getMethod("getTextOff");
                }
                viewText = (String)method.invoke(view);
            } else if (view instanceof RadioButton) { // RadioButton
                viewType = "RadioButton";
                RadioButton radioButton = (RadioButton) view;
                viewText = radioButton.getText();
            } else if (view instanceof ToggleButton) { // ToggleButton
                viewType = "ToggleButton";
                ToggleButton toggleButton = (ToggleButton) view;
                boolean isChecked = toggleButton.isChecked();
                if (isChecked) {
                    viewText = toggleButton.getTextOn();
                } else {
                    viewText = toggleButton.getTextOff();
                }
            } else if (view instanceof Button) { // Button
                viewType = "Button";
                Button button = (Button) view;
                viewText = button.getText();
            } else if (view instanceof CheckedTextView) { // CheckedTextView
                viewType = "CheckedTextView";
                CheckedTextView textView = (CheckedTextView) view;
                viewText = textView.getText();
            } else if (view instanceof TextView) { // TextView
                viewType = "TextView";
                TextView textView = (TextView) view;
                viewText = textView.getText();
            } else if (view instanceof ImageView) { // ImageView
                viewType = "ImageView";
                ImageView imageView = (ImageView) view;
                if (!TextUtils.isEmpty(imageView.getContentDescription())) {
                    viewText = imageView.getContentDescription().toString();
                }
            } else if (view instanceof RatingBar) {
                viewType = "RatingBar";
                RatingBar ratingBar = (RatingBar) view;
                viewText = String.valueOf(ratingBar.getRating());
            } else if (view instanceof SeekBar) {
                viewType = "SeekBar";
                SeekBar seekBar = (SeekBar) view;
                viewText = String.valueOf(seekBar.getProgress());
            } else if (view instanceof Spinner) {
                viewType = "Spinner";
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.toString().substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.toString().substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AopConstants.ELEMENT_CONTENT, viewText.toString());
            }

            //$element_type
            properties.put(AopConstants.ELEMENT_TYPE, viewType);

            //fragmentName
            AopUtil.getFragmentNameFromView(view, properties);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.sensors_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            SensorsDataAPI.sharedInstance().track(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackViewOnClick(Object anything) {
        try {
            if (anything == null) {
                return;
            }

            if (!(anything instanceof View)) {
                return;
            }

            trackViewOnClick((View) anything);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
