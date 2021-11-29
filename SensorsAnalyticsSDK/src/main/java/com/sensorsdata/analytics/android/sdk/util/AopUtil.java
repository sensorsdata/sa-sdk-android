/*
 * Created by wangzhuozhou on 2016/12/2.
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.ScreenAutoTracker;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataFragmentTitle;
import com.sensorsdata.analytics.android.sdk.visual.ViewTreeStatusObservable;
import com.sensorsdata.analytics.android.sdk.visual.model.ViewNode;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class AopUtil {

    private static final String TAG = "SA.AopUtil";
    @SuppressLint("NewApi")
    private static LruCache<String, WeakReference<Object>> sLruCache = new LruCache<>(10);

    // 采集 viewType 忽略以下包内 view 直接返回对应的基础控件 viewType
    private static ArrayList<String> sOSViewPackage = new ArrayList<String>() {{
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

                    String viewText = getViewText(child);
                    if (!TextUtils.isEmpty(viewText)) {
                        stringBuilder.append(viewText);
                        stringBuilder.append("-");
                    }
                }
            }
            return stringBuilder.toString();
        } catch (Throwable e) {
            SALog.d(TAG, e.getMessage());
            return stringBuilder != null ? stringBuilder.toString() : "";
        }
    }

    public static String getViewText(View child) {
        if (child instanceof EditText) {
            return "";
        }
        try {
            Class<?> switchCompatClass = null;
            try {
                switchCompatClass = Class.forName("android.support.v7.widget.SwitchCompat");
            } catch (Exception e) {
                //ignored
            }

            if (switchCompatClass == null) {
                try {
                    switchCompatClass = Class.forName("androidx.appcompat.widget.SwitchCompat");
                } catch (Exception e) {
                    //ignored
                }
            }

            CharSequence viewText = null;

            if (child instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) child;
                viewText = checkBox.getText();
            } else if (switchCompatClass != null && switchCompatClass.isInstance(child)) {
                CompoundButton switchCompat = (CompoundButton) child;
                Method method;
                if (switchCompat.isChecked()) {
                    method = child.getClass().getMethod("getTextOn");
                } else {
                    method = child.getClass().getMethod("getTextOff");
                }
                viewText = (String) method.invoke(child);
            } else if (child instanceof RadioButton) {
                RadioButton radioButton = (RadioButton) child;
                viewText = radioButton.getText();
            } else if (child instanceof ToggleButton) {
                ToggleButton toggleButton = (ToggleButton) child;
                boolean isChecked = toggleButton.isChecked();
                if (isChecked) {
                    viewText = toggleButton.getTextOn();
                } else {
                    viewText = toggleButton.getTextOff();
                }
            } else if (child instanceof Button) {
                Button button = (Button) child;
                viewText = button.getText();
            } else if (child instanceof CheckedTextView) {
                CheckedTextView textView = (CheckedTextView) child;
                viewText = textView.getText();
            } else if (child instanceof TextView) {
                TextView textView = (TextView) child;
                viewText = textView.getText();
            } else if (child instanceof ImageView) {
                ImageView imageView = (ImageView) child;
                if (!TextUtils.isEmpty(imageView.getContentDescription())) {
                    viewText = imageView.getContentDescription().toString();
                }
            } else {
                viewText = child.getContentDescription();
            }
            if (TextUtils.isEmpty(viewText) && child instanceof TextView) {
                viewText = ((TextView) child).getHint();
            }
            if (viewText != null && !TextUtils.isEmpty(viewText)) {
                return viewText.toString();
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return "";
    }

    public static Activity getActivityFromContext(Context context, View view) {
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
     * 尝试读取页面 title
     *
     * @param properties JSONObject
     * @param fragment Fragment
     * @param activity Activity
     */
    public static void getScreenNameAndTitleFromFragment(JSONObject properties, Object fragment, Activity activity) {
        try {
            String screenName = null;
            String title = null;
            if (fragment instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) fragment;
                JSONObject trackProperties = screenAutoTracker.getTrackProperties();
                if (trackProperties != null) {
                    if (trackProperties.has(AopConstants.SCREEN_NAME)) {
                        screenName = trackProperties.optString(AopConstants.SCREEN_NAME);
                    }

                    if (trackProperties.has(AopConstants.TITLE)) {
                        title = trackProperties.optString(AopConstants.TITLE);
                    }
                    SensorsDataUtils.mergeJSONObject(trackProperties, properties);
                }
            }

            if (TextUtils.isEmpty(title) && fragment.getClass().isAnnotationPresent(SensorsDataFragmentTitle.class)) {
                SensorsDataFragmentTitle sensorsDataFragmentTitle = fragment.getClass().getAnnotation(SensorsDataFragmentTitle.class);
                if (sensorsDataFragmentTitle != null) {
                    title = sensorsDataFragmentTitle.title();
                }
            }

            boolean isTitleNull = TextUtils.isEmpty(title);
            boolean isScreenNameNull = TextUtils.isEmpty(screenName);
            if (isTitleNull || isScreenNameNull) {
                if (activity == null) {
                    activity = getActivityFromFragment(fragment);
                }
                if (activity != null) {
                    if (isTitleNull) {
                        title = SensorsDataUtils.getActivityTitle(activity);
                    }

                    if (isScreenNameNull) {
                        screenName = fragment.getClass().getCanonicalName();
                        screenName = String.format(Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), screenName);
                    }
                }
            }

            if (!TextUtils.isEmpty(title)) {
                properties.put(AopConstants.TITLE, title);
            }

            if (TextUtils.isEmpty(screenName)) {
                screenName = fragment.getClass().getCanonicalName();
            }
            properties.put("$screen_name", screenName);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
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
                if (getActivityMethod != null) {
                    activity = (Activity) getActivityMethod.invoke(fragment);
                }
            } catch (Exception e) {
                //ignored
            }
        }
        return activity;
    }

    /**
     * 构建 Title 和 Screen 的名称
     *
     * @param activity 页面
     * @return JSONObject
     */
    public static JSONObject buildTitleAndScreenName(Activity activity) {
        JSONObject propertyJSON = new JSONObject();
        try {
            propertyJSON.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
            String activityTitle = AopUtil.getActivityTitle(activity);
            if (!TextUtils.isEmpty(activityTitle)) {
                propertyJSON.put(AopConstants.TITLE, activityTitle);
            }

            if (activity instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) activity;
                JSONObject trackProperties = screenAutoTracker.getTrackProperties();
                if (trackProperties != null) {
                    SensorsDataUtils.mergeJSONObject(trackProperties, propertyJSON);
                }
            }
        } catch (Exception ex) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(ex);
            return new JSONObject();
        }
        return propertyJSON;
    }

    /**
     * 构建 Title 和 Screen 的名称
     *
     * @param activity 页面
     * @return JSONObject
     */
    public static JSONObject buildTitleNoAutoTrackerProperties(Activity activity) {
        JSONObject propertyJSON = new JSONObject();
        try {
            propertyJSON.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
            String activityTitle = AopUtil.getActivityTitle(activity);
            if (!TextUtils.isEmpty(activityTitle)) {
                propertyJSON.put(AopConstants.TITLE, activityTitle);
            }
            if (activity instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) activity;
                JSONObject trackProperties = screenAutoTracker.getTrackProperties();
                if (trackProperties != null) {
                    if (trackProperties.has(AopConstants.SCREEN_NAME)) {
                        propertyJSON.put(AopConstants.SCREEN_NAME, trackProperties.optString(AopConstants.SCREEN_NAME));
                    }
                    if (trackProperties.has(AopConstants.TITLE)) {
                        propertyJSON.put(AopConstants.TITLE, trackProperties.optString(AopConstants.TITLE));
                    }
                }
            }
        } catch (Exception ex) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(ex);
            return new JSONObject();
        }
        return propertyJSON;
    }

    /**
     * 获取 CompoundButton text
     *
     * @param view view
     * @return CompoundButton 显示的内容
     */
    public static String getCompoundButtonText(View view) {
        try {
            CompoundButton switchButton = (CompoundButton) view;
            Method method;
            if (switchButton.isChecked()) {
                method = view.getClass().getMethod("getTextOn");
            } else {
                method = view.getClass().getMethod("getTextOff");
            }
            return (String) method.invoke(view);
        } catch (Exception ex) {
            return "UNKNOWN";
        }
    }

    public static String getViewId(View view) {
        String idString = null;
        try {
            idString = (String) view.getTag(R.id.sensors_analytics_tag_view_id);
            if (TextUtils.isEmpty(idString)) {
                if (view.getId() != View.NO_ID) {
                    idString = view.getContext().getResources().getResourceEntryName(view.getId());
                }
            }
        } catch (Exception e) {
            //ignore
        }
        return idString;
    }

    /**
     * 采集 View 的 $element_type 主要区分继承系统 View 和继承系统 View 的自定义 View
     *
     * @param viewName View.getCanonicalName（）返回的 name
     * @param defaultTypeName 默认的 typeName
     * @return typeName
     */
    public static String getViewType(String viewName, String defaultTypeName) {
        if (TextUtils.isEmpty(viewName)) {
            return defaultTypeName;
        }
        if (TextUtils.isEmpty(defaultTypeName)) {
            return viewName;
        }

        if (isOSViewByPackage(viewName)) {
            return defaultTypeName;
        }

        return viewName;
    }

    /**
     * 通过反射判断类的类型
     *
     * @param view 判断类型的 viewGroup
     * @return viewType
     */
    public static String getViewGroupTypeByReflect(View view) {
        Class<?> compatClass;
        String viewType = view.getClass().getCanonicalName();
        compatClass = getClassByName("android.support.v7.widget.CardView");
        if (compatClass != null && compatClass.isInstance(view)) {
            return getViewType(viewType, "CardView");
        }
        compatClass = getClassByName("androidx.cardview.widget.CardView");
        if (compatClass != null && compatClass.isInstance(view)) {
            return getViewType(viewType, "CardView");
        }
        compatClass = getClassByName("android.support.design.widget.NavigationView");
        if (compatClass != null && compatClass.isInstance(view)) {
            return getViewType(viewType, "NavigationView");
        }
        compatClass = getClassByName("com.google.android.material.navigation.NavigationView");
        if (compatClass != null && compatClass.isInstance(view)) {
            return getViewType(viewType, "NavigationView");
        }
        return viewType;
    }

    /**
     * 通过反射判断类的类型
     *
     * @param view 判断类型的 view
     * @return viewType
     */
    public static String getViewTypeByReflect(View view) {
        Class<?> compatClass;
        String viewType = view.getClass().getCanonicalName();
        compatClass = getClassByName("android.widget.Switch");
        if (compatClass != null && compatClass.isInstance(view)) {
            return getViewType(viewType, "Switch");
        }
        compatClass = getClassByName("android.support.v7.widget.SwitchCompat");
        if (compatClass != null && compatClass.isInstance(view)) {
            return getViewType(viewType, "SwitchCompat");
        }
        compatClass = getClassByName("androidx.appcompat.widget.SwitchCompat");
        if (compatClass != null && compatClass.isInstance(view)) {
            return getViewType(viewType, "SwitchCompat");
        }
        return viewType;
    }

    private static Class<?> getClassByName(String name) {
        Class<?> compatClass;
        try {
            compatClass = Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
        return compatClass;
    }

    /**
     * ViewType 被忽略
     *
     * @param viewType Class
     * @return 是否被忽略
     */
    public static boolean isViewIgnored(Class viewType) {
        try {
            if (viewType == null) {
                return true;
            }

            List<Class> mIgnoredViewTypeList = SensorsDataAPI.sharedInstance().getIgnoredViewTypeList();
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
            List<Class> mIgnoredViewTypeList = SensorsDataAPI.sharedInstance().getIgnoredViewTypeList();
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
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
            return true;
        }
    }

    /**
     * 获取 Activity 的 title
     *
     * @param activity Activity
     * @return Activity 的 title
     */
    private static String getActivityTitle(Activity activity) {
        try {
            if (activity != null) {
                try {
                    String activityTitle = null;
                    if (!TextUtils.isEmpty(activity.getTitle())) {
                        activityTitle = activity.getTitle().toString();
                    }

                    if (Build.VERSION.SDK_INT >= 11) {
                        String toolbarTitle = SensorsDataUtils.getToolbarTitle(activity);
                        if (!TextUtils.isEmpty(toolbarTitle)) {
                            activityTitle = toolbarTitle;
                        }
                    }

                    if (TextUtils.isEmpty(activityTitle)) {
                        PackageManager packageManager = activity.getPackageManager();
                        if (packageManager != null) {
                            ActivityInfo activityInfo = packageManager.getActivityInfo(activity.getComponentName(), 0);
                            if (!TextUtils.isEmpty(activityInfo.loadLabel(packageManager))) {
                                activityTitle = activityInfo.loadLabel(packageManager).toString();
                            }
                        }
                    }

                    return activityTitle;
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
            return null;
        }
    }

    /**
     * 合并 JSONObject
     *
     * @param source JSONObject
     * @param dest JSONObject
     */
    public static void mergeJSONObject(final JSONObject source, JSONObject dest) {
        try {
            Iterator<String> superPropertiesIterator = source.keys();
            while (superPropertiesIterator.hasNext()) {
                String key = superPropertiesIterator.next();
                Object value = source.get(key);
                if (value instanceof Date) {
                    dest.put(key, TimeUtils.formatDate((Date) value));
                } else {
                    dest.put(key, value);
                }
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
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

    /**
     * properties 注入点击事件信息
     * 属性的优先级为：预置属性低于 {@link ScreenAutoTracker#getTrackProperties()} 低于
     * {@link SensorsDataAPI#setViewProperties(View, JSONObject)} 低于
     * {@link SensorsDataAPI#trackViewAppClick(View, JSONObject)}
     *
     * @param view 点击的 view
     * @param properties 事件属性
     * @param isFromUser 是否由用户触发
     * @return isTrackEvent 是否发送事件
     */
    public static boolean injectClickInfo(View view, JSONObject properties, boolean isFromUser) {
        if (view == null || properties == null) {
            return false;
        }
        try {
            if (!ViewUtil.isTrackEvent(view, isFromUser)) {
                return false;
            }
            Context context = view.getContext();
            JSONObject eventJson = new JSONObject();
            Activity activity = AopUtil.getActivityFromContext(context, view);
            //1.获取预置属性
            //ViewId
            String idString = AopUtil.getViewId(view);
            if (!TextUtils.isEmpty(idString)) {
                eventJson.put(AopConstants.ELEMENT_ID, idString);
            }

            ViewNode viewNode = ViewUtil.getViewContentAndType(view);
            String viewText = viewNode.getViewContent();
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                eventJson.put(AopConstants.ELEMENT_CONTENT, viewText);
            }
            //$element_type
            eventJson.put(AopConstants.ELEMENT_TYPE, viewNode.getViewType());

            //2.获取 Activity 页面信息及 ScreenAutoTracker 定义的属性
            if (activity != null) {
                SensorsDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), eventJson);
            }

            //fragmentName
            Object fragment = AopUtil.getFragmentFromView(view, activity);
            if (fragment != null) {
                AopUtil.getScreenNameAndTitleFromFragment(eventJson, fragment, activity);
            }
            //3.获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.sensors_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, eventJson);
            }
            //4.事件传入的自定义属性
            JSONUtils.mergeDistinctProperty(eventJson, properties);
            return true;
        } catch (JSONException e) {
            SALog.printStackTrace(e);
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
                        activity = AopUtil.getActivityFromContext(context, view);
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
                if (!TextUtils.isEmpty(fragmentName)) {
                    WeakReference<Object> weakReference = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
                        weakReference = sLruCache.get(fragmentName);
                    }
                    Object object;
                    if (weakReference != null) {
                        object = weakReference.get();
                        if (object != null) {
                            return object;
                        }
                    }

                    object = Class.forName(fragmentName).newInstance();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                        sLruCache.put(fragmentName, new WeakReference<>(object));
                    }

                    return object;
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    public static ViewNode addViewPathProperties(Activity activity, View view, JSONObject properties) {
        try {
            if (view == null) {
                return null;
            }
            if (activity == null) {
                return null;
            }
            if (properties == null) {
                properties = new JSONObject();
            }
            if ((SensorsDataAPI.sharedInstance().isHeatMapEnabled() || SensorsDataAPI.sharedInstance().isVisualizedAutoTrackEnabled())
                    && (SensorsDataAPI.sharedInstance().isHeatMapActivity(activity.getClass()) || SensorsDataAPI.sharedInstance().isVisualizedAutoTrackActivity(activity.getClass()))) {
                String elementSelector = ViewUtil.getElementSelector(view);
                if (!TextUtils.isEmpty(elementSelector)) {
                    properties.put(AopConstants.ELEMENT_SELECTOR, elementSelector);
                }
            }
            ViewNode viewNode = ViewTreeStatusObservable.getInstance().getViewNode(view);
            if (viewNode != null) {
                if (!TextUtils.isEmpty(viewNode.getViewPath())) {
                    if ((SensorsDataAPI.sharedInstance().isVisualizedAutoTrackEnabled() && SensorsDataAPI.sharedInstance().isVisualizedAutoTrackActivity(activity.getClass()))
                            || (SensorsDataAPI.sharedInstance().isHeatMapEnabled() && SensorsDataAPI.sharedInstance().isHeatMapActivity(activity.getClass()))) {
                        properties.put(AopConstants.ELEMENT_PATH, viewNode.getViewPath());
                    }
                }
                if (!TextUtils.isEmpty(viewNode.getViewPosition())) {
                    properties.put(AopConstants.ELEMENT_POSITION, viewNode.getViewPosition());
                }
                return viewNode;
            }
        } catch (JSONException e) {
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