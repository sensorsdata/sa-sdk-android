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

package com.sensorsdata.analytics.android.autotrack.core.autotrack;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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

import com.sensorsdata.analytics.android.autotrack.R;
import com.sensorsdata.analytics.android.autotrack.core.beans.AutoTrackConstants;
import com.sensorsdata.analytics.android.autotrack.core.beans.ViewContext;
import com.sensorsdata.analytics.android.autotrack.core.business.SAPageTools;
import com.sensorsdata.analytics.android.autotrack.utils.AopUtil;
import com.sensorsdata.analytics.android.autotrack.utils.AutoTrackViewUtils;
import com.sensorsdata.analytics.android.autotrack.utils.KeyboardViewUtil;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsAdapterViewItemTrackProperties;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsExpandableListViewItemTrackProperties;
import com.sensorsdata.analytics.android.sdk.core.SACoreHelper;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.core.mediator.SAModuleManager;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.ReflectUtil;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;
import com.sensorsdata.analytics.android.sdk.util.SAFragmentUtils;
import com.sensorsdata.analytics.android.sdk.util.SAPageInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.SAViewUtils;
import com.sensorsdata.analytics.android.sdk.util.WindowHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;

public class AppClickTrackImpl {
    private static final String TAG = "AppClickTrackImpl";
    private static final HashMap<Integer, Long> eventTimestamp = new HashMap<>();

    /**
     * ExpandableListView Group track
     *
     * @param sensorsDataAPI SensorsDataAPI
     * @param expandableListView ExpandableListView
     * @param view View
     * @param groupPosition groupPosition
     */
    public static void trackExpandableListViewOnGroupClick(SensorsDataAPI sensorsDataAPI, ExpandableListView expandableListView, View view,
                                                           int groupPosition) {
        try {
            if (expandableListView == null || view == null || isAppClickIgnore(sensorsDataAPI)) {
                return;
            }

            ViewContext viewContext = getAppClickViewContext(expandableListView, ExpandableListView.class);
            if (viewContext == null) {
                return;
            }

            JSONObject properties = buildPageProperty(viewContext.activity, viewContext.fragment);
            // ViewId
            String idString = SAViewUtils.getViewId(expandableListView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AutoTrackConstants.ELEMENT_ID, idString);
            }
            properties.put(AutoTrackConstants.ELEMENT_TYPE, "ExpandableListView");

            String viewText = null;
            if (view instanceof ViewGroup) {// child view is ViewGroup
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = SAViewUtils.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            } else {
                viewText = SAViewUtils.getViewContent(view);
            }
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AutoTrackConstants.ELEMENT_CONTENT, viewText);
            }

            // assemble interface extend property
            JSONUtils.mergeJSONObject(getExpandListViewExtendProperty(expandableListView, groupPosition, -1), properties);
            // 获取 View 自定义属性
            JSONUtils.mergeJSONObject((JSONObject) view.getTag(R.id.sensors_analytics_tag_view_properties), properties);
            trackAutoEvent(sensorsDataAPI, properties, view);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * ExpandableListView Child track
     *
     * @param sensorsDataAPI SensorsDataAPI
     * @param expandableListView ExpandableListView
     * @param view View
     * @param groupPosition groupPosition
     * @param childPosition childPosition
     */
    public static void trackExpandableListViewOnChildClick(SensorsDataAPI sensorsDataAPI, ExpandableListView expandableListView, View view,
                                                           int groupPosition, int childPosition) {
        try {
            if (expandableListView == null || view == null || SAViewUtils.isViewIgnored(view) || isAppClickIgnore(sensorsDataAPI)) {
                return;
            }
            ViewContext viewContext = getAppClickViewContext(expandableListView, ExpandableListView.class);
            if (viewContext == null) {
                return;
            }

            JSONObject properties = buildPageProperty(viewContext.activity, viewContext.fragment);
            //ViewId
            String idString = SAViewUtils.getViewId(expandableListView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AutoTrackConstants.ELEMENT_ID, idString);
            }
            properties.put(AutoTrackConstants.ELEMENT_TYPE, "ExpandableListView");

            String viewText = null;
            if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = SAViewUtils.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            } else {
                viewText = SAViewUtils.getViewContent(view);
            }
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AutoTrackConstants.ELEMENT_CONTENT, viewText);
            }
            // assemble interface extend property
            JSONUtils.mergeJSONObject(getExpandListViewExtendProperty(expandableListView, groupPosition, childPosition), properties);
            // 获取 View 自定义属性
            JSONUtils.mergeJSONObject((JSONObject) view.getTag(R.id.sensors_analytics_tag_view_properties), properties);
            trackAutoEvent(sensorsDataAPI, properties, view);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * TabHost track
     *
     * @param sensorsDataAPI SensorsDataAPI
     * @param tabName TabHost name
     */
    public static void trackTabHost(SensorsDataAPI sensorsDataAPI, final String tabName) {
        try {
            if (isAppClickIgnore(sensorsDataAPI)) {
                return;
            }

            ViewContext viewContext = getAppClickViewContext(AutoTrackViewUtils.getTabView(tabName), TabHost.class);
            if (viewContext == null) {
                return;
            }

            JSONObject properties = null;
            try {
                properties = buildPageProperty(viewContext.activity, viewContext.fragment);
                String elementContent = SAViewUtils.getViewContent(viewContext.view);
                if (TextUtils.isEmpty(elementContent)) {
                    elementContent = tabName;
                }
                properties.put(AutoTrackConstants.ELEMENT_CONTENT, elementContent);
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }

            if (properties == null) {
                properties = new JSONObject();
            }
            properties.put(AutoTrackConstants.ELEMENT_TYPE, "TabHost");
            trackAutoEvent(sensorsDataAPI, properties, viewContext.view);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * TabLayout track
     *
     * @param sensorsDataAPI SensorsDataAPI
     * @param objectTab TabLayout
     * @param tab Tab
     */
    public static void trackTabLayoutSelected(SensorsDataAPI sensorsDataAPI, Object objectTab, Object tab) {
        try {
            if (tab == null || isDeBounceTrack(tab) || isAppClickIgnore(sensorsDataAPI)) {
                return;
            }

            View tabLayout = AutoTrackViewUtils.getTabLayout(tab);
            if (tabLayout == null) {//ignore
                return;
            }
            ViewContext viewContext = AutoTrackViewUtils.getTabLayoutContext(objectTab, tabLayout);
            if (viewContext == null) {
                return;
            }
            if (viewContext.activity != null && sensorsDataAPI.isActivityAutoTrackAppClickIgnored(viewContext.activity.getClass())) {
                return;
            }
            if (viewContext.fragment != null && sensorsDataAPI.isActivityAutoTrackAppClickIgnored(viewContext.fragment.getClass())) {
                return;
            }

            JSONObject properties = buildPageProperty(viewContext.activity, viewContext.fragment);
            //Type
            properties.put(AutoTrackConstants.ELEMENT_TYPE, "TabLayout");

            Class<?> currentTabClass = null;
            try {
                currentTabClass = Class.forName("com.google.android.material.tabs.TabLayout$Tab");
            } catch (Exception e) {
                try {
                    currentTabClass = Class.forName("android.support.design.widget.TabLayout$Tab");
                } catch (Exception ex) {
                    //ignored
                }
            }

            View tabView = null;
            if (currentTabClass != null) {
                View tabCustomView = ReflectUtil.findField(currentTabClass, tab, new String[]{"mCustomView", "customView"});

                String text = AutoTrackViewUtils.getTabLayoutText(tabCustomView, tab);
                if (text != null) {
                    properties.put(AutoTrackConstants.ELEMENT_CONTENT, text);
                }

                if (tabCustomView == null || tabCustomView.getId() == View.NO_ID) {
                    tabCustomView = tabLayout;  // use TabLayout as tab view
                }

                if (tabCustomView != null) {
                    if (tabCustomView.getId() != View.NO_ID && viewContext.activity != null) {
                        String resourceId = viewContext.activity.getResources().getResourceEntryName(tabCustomView.getId());
                        if (!TextUtils.isEmpty(resourceId)) {
                            properties.put(AutoTrackConstants.ELEMENT_ID, resourceId);
                        }
                    }
                    // get Tab CustomView custom property
                    JSONObject p = (JSONObject) tabCustomView.getTag(R.id.sensors_analytics_tag_view_properties);
                    JSONUtils.mergeJSONObject(p, properties);
                }
                // get TabView
                tabView = ReflectUtil.findField(currentTabClass, tab, "view");
                if (tabView == null) {
                    tabView = ReflectUtil.findField(currentTabClass, tab, "mView");
                }

                if (tabLayout != null && tabView != null) {
                    boolean isBindViewPager = AutoTrackViewUtils.isBindViewPager(tabLayout);
                    if (isBindViewPager && !tabView.isPressed() || !isBindViewPager && tabView.isPressed()) {//include view pager scroll or only tabLayout press
                        properties.put("$referrer_title", SAPageTools.getCurrentTitle());
                    }
                }
            }
            trackAutoEvent(sensorsDataAPI, properties, tabView);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * MenuItem track
     *
     * @param sensorsDataAPI SensorsDataAPI
     * @param object Context
     * @param menuItem MenuItem
     */
    public static void trackMenuItem(SensorsDataAPI sensorsDataAPI, final Object object, final MenuItem menuItem) {
        try {
            if (menuItem == null || isDeBounceTrack(menuItem) || isAppClickIgnore(sensorsDataAPI)) {
                return;
            }

            ViewContext viewContext = getAppClickViewContext(object, WindowHelper.getClickView(menuItem), MenuItem.class);
            if (viewContext == null) {
                return;
            }

            JSONObject properties = buildPageProperty(viewContext.activity, viewContext.fragment);

            try {
                // View ID
                if (viewContext.activity != null) {
                    String idString = viewContext.activity.getResources().getResourceEntryName(menuItem.getItemId());
                    if (!TextUtils.isEmpty(idString)) {
                        properties.put(AutoTrackConstants.ELEMENT_ID, idString);
                    }
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }

            // 2020/4/27 新增  1. 解决 Actionbar 返回按钮 获取不到 $element_content
            if (menuItem.getTitle() != null) {
                String elementContent = menuItem.getTitle().toString();
                if (viewContext.view != null && TextUtils.isEmpty(elementContent)) {
                    elementContent = SAViewUtils.getViewContent(viewContext.view);
                }
                properties.put(AutoTrackConstants.ELEMENT_CONTENT, elementContent);
            }
            properties.put(AutoTrackConstants.ELEMENT_TYPE, "MenuItem");

            trackAutoEvent(sensorsDataAPI, properties, viewContext.view);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * RadioGroup track
     *
     * @param sensorsDataAPI SensorsDataAPI
     * @param view RadioGroup
     * @param checkedId checkedId
     */
    public static void trackRadioGroup(SensorsDataAPI sensorsDataAPI, RadioGroup view, int checkedId) {
        try {
            if (view == null || isAppClickIgnore(sensorsDataAPI)) {
                return;
            }

            View childView = view.findViewById(checkedId);
            if (childView == null || !childView.isPressed()) {
                return;
            }

            ViewContext viewContext = getAppClickViewContext(view, RadioGroup.class);
            if (viewContext == null) {
                return;
            }

            JSONObject properties = buildPageProperty(viewContext.activity, viewContext.fragment);

            //ViewId
            String idString = SAViewUtils.getViewId(view);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AutoTrackConstants.ELEMENT_ID, idString);
            }

            properties.put(AutoTrackConstants.ELEMENT_TYPE, SAViewUtils.getViewType(childView));

            //获取变更后的选中项的ID
            RadioButton radioButton = null;
            try {
                if (viewContext.activity != null) {
                    radioButton = viewContext.activity.findViewById(view.getCheckedRadioButtonId());
                    if (radioButton != null && !TextUtils.isEmpty(radioButton.getText())) {
                        properties.put(AutoTrackConstants.ELEMENT_CONTENT, radioButton.getText().toString());
                    }
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.sensors_analytics_tag_view_properties);
            JSONUtils.mergeJSONObject(p, properties);

            trackAutoEvent(sensorsDataAPI, properties, radioButton);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * Dialog track
     *
     * @param sensorsDataAPI SensorsDataAPI
     * @param dialogInterface DialogInterface
     * @param whichButton button
     */
    public static void trackDialog(SensorsDataAPI sensorsDataAPI, DialogInterface dialogInterface, int whichButton) {
        try {
            if (isAppClickIgnore(sensorsDataAPI) || SAViewUtils.isViewIgnored(Dialog.class)) {
                return;
            }
            JSONObject properties = new JSONObject();

            //获取所在的Context
            Dialog dialog = null;
            if (dialogInterface instanceof Dialog) {
                dialog = (Dialog) dialogInterface;
            }

            if (dialog == null || isDeBounceTrack(dialog)) {
                return;
            }

            Context context = dialog.getContext();

            //将Context转成Activity
            Activity activity = SAViewUtils.getActivityOfView(context, null);

            if (activity == null) {
                activity = dialog.getOwnerActivity();
            }

            //Activity 被忽略
            if (activity == null || sensorsDataAPI.isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                return;
            }

            //$screen_name & $title
            JSONUtils.mergeJSONObject(SAPageInfoUtils.getActivityPageInfo(activity), properties);
            try {
                Window window = dialog.getWindow();
                if (window != null && window.isActive()) {
                    String idString = (String) dialog.getWindow().getDecorView().getTag(com.sensorsdata.analytics.android.sdk.R.id.sensors_analytics_tag_view_id);
                    if (!TextUtils.isEmpty(idString)) {
                        properties.put(AutoTrackConstants.ELEMENT_ID, idString);
                    }
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }

            //由于 RN 中 dialog 未屏蔽，直接走到原生，导致 dialog screen_name 取的是原生的。
            JSONUtils.mergeDuplicateProperty(SAPageInfoUtils.getRNPageInfo(), properties);

            properties.put(AutoTrackConstants.ELEMENT_TYPE, "Dialog");

            Class<?> currentAlertDialogClass = null;
            try {
                currentAlertDialogClass = Class.forName("android.support.v7.app.AlertDialog");
            } catch (Exception e) {
                try {
                    currentAlertDialogClass = Class.forName("androidx.appcompat.app.AlertDialog");
                } catch (Exception e1) {
                    //ignored
                }
            }

            if (currentAlertDialogClass == null) {
                return;
            }

            View view = null;
            if (dialog instanceof android.app.AlertDialog) {
                android.app.AlertDialog alertDialog = (android.app.AlertDialog) dialog;
                Button button = alertDialog.getButton(whichButton);
                if (button != null) {
                    if (!TextUtils.isEmpty(button.getText())) {
                        properties.put(AutoTrackConstants.ELEMENT_CONTENT, button.getText());
                    }
                    view = button;
                } else {
                    ListView listView = alertDialog.getListView();
                    if (listView != null) {
                        ListAdapter listAdapter = listView.getAdapter();
                        Object object = listAdapter.getItem(whichButton);
                        if (object != null) {
                            if (object instanceof String) {
                                properties.put(AutoTrackConstants.ELEMENT_CONTENT, object);
                            }
                        }
                        view = listView.getChildAt(whichButton);
                    }
                }
            } else if (currentAlertDialogClass.isInstance(dialog)) {
                Button button = null;
                try {
                    Method getButtonMethod = dialog.getClass().getMethod("getButton", int.class);
                    button = (Button) getButtonMethod.invoke(dialog, whichButton);
                } catch (Exception e) {
                    //ignored
                }

                if (button != null) {
                    if (!TextUtils.isEmpty(button.getText())) {
                        properties.put(AutoTrackConstants.ELEMENT_CONTENT, button.getText());
                    }
                    view = button;
                } else {
                    try {
                        Method getListViewMethod = dialog.getClass().getMethod("getListView");
                        ListView listView = (ListView) getListViewMethod.invoke(dialog);
                        if (listView != null) {
                            ListAdapter listAdapter = listView.getAdapter();
                            Object object = listAdapter.getItem(whichButton);
                            if (object != null) {
                                if (object instanceof String) {
                                    properties.put(AutoTrackConstants.ELEMENT_CONTENT, object);
                                }
                            }
                            view = listView.getChildAt(whichButton);
                        }
                    } catch (Exception e) {
                        //ignored
                    }
                }
            }

            trackAutoEvent(sensorsDataAPI, properties, view);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * ListView click track
     *
     * @param sensorsDataAPI SensorsDataAPI
     * @param adapterView AdapterView
     * @param view View
     * @param position click item position
     */
    public static void trackListView(SensorsDataAPI sensorsDataAPI, AdapterView<?> adapterView, View view, int position) {
        try {
            //防止 Spinner 恢复数据时造成的空指针问题.
            if (view == null || isAppClickIgnore(sensorsDataAPI) || SAViewUtils.isViewIgnored(adapterView) || KeyboardViewUtil.isKeyboardView(view)) {
                return;
            }

            Class<?> viewType = null;
            String element_type = "";
            if (adapterView instanceof ListView) {
                viewType = ListView.class;
                element_type = "ListView";
            } else if (adapterView instanceof GridView) {
                viewType = GridView.class;
                element_type = "GridView";
            } else if (adapterView instanceof Spinner) {
                viewType = Spinner.class;
                element_type = "Spinner";
            }
            ViewContext viewContext = getAppClickViewContext(adapterView, view, viewType);
            if (viewContext == null) {
                return;
            }

            JSONObject properties = buildPageProperty(viewContext.activity, viewContext.fragment);
            if (!TextUtils.isEmpty(element_type)) {
                properties.put(AutoTrackConstants.ELEMENT_TYPE, element_type);
            }

            //ViewId
            String idString = SAViewUtils.getViewId(adapterView);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AutoTrackConstants.ELEMENT_ID, idString);
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
                    JSONUtils.mergeJSONObject(jsonObject, properties);
                } catch (JSONException e) {
                    SALog.printStackTrace(e);
                }
            }

            //$element_content
            String viewText = SAViewUtils.getViewContent(view);
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AutoTrackConstants.ELEMENT_CONTENT, viewText);
            }

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.sensors_analytics_tag_view_properties);
            JSONUtils.mergeJSONObject(p, properties);

            trackAutoEvent(sensorsDataAPI, properties, view);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * DrawerOpen track
     *
     * @param sensorsDataAPI SensorsDataAPI
     * @param view View
     */
    public static void trackDrawerOpened(SensorsDataAPI sensorsDataAPI, View view) {
        try {
            if (view == null) {
                return;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("$element_content", "Open");
            sensorsDataAPI.setViewProperties(view, jsonObject);
            trackViewOnClick(sensorsDataAPI, view, view.isPressed());
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * DrawerClose track
     *
     * @param sensorsDataAPI SensorsDataAPI
     * @param view View
     */
    public static void trackDrawerClosed(SensorsDataAPI sensorsDataAPI, View view) {
        try {
            if (view == null) {
                return;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("$element_content", "Close");
            sensorsDataAPI.setViewProperties(view, jsonObject);
            trackViewOnClick(sensorsDataAPI, view, view.isPressed());
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * view click event
     *
     * @param sensorsDataAPI SensorsDataAPI
     * @param view View
     * @param isFromUser is click from user or code
     */
    public static void trackViewOnClick(SensorsDataAPI sensorsDataAPI, View view, boolean isFromUser) {
        try {
            if (view == null || isAppClickIgnore(sensorsDataAPI) || isDeBounceTrack(view)
                    || KeyboardViewUtil.isKeyboardView(view)) {
                return;
            }

            ViewContext viewContext = getAppClickViewContext(view, view.getClass());
            if (viewContext == null) {
                return;
            }

            JSONObject properties = new JSONObject();
            properties = AopUtil.injectClickInfo(viewContext, properties, isFromUser);
            if (properties != null) {
                trackAutoEvent(sensorsDataAPI, properties, view);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * track event by SensorsDataTrackEvent Annotation
     *
     * @param sensorsDataAPI SensorsDataAPI
     * @param eventName EventName
     * @param properties Property
     */
    public static void track(final SensorsDataAPI sensorsDataAPI, final String eventName, String properties) {
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
            final JSONObject finalPro = pro;
            SACoreHelper.getInstance().trackQueueEvent(new Runnable() {
                @Override
                public void run() {
                    sensorsDataAPI.getSAContextManager().
                            trackEvent(new InputData().setEventName(eventName).setProperties(finalPro).setEventType(EventType.TRACK));
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private static void trackAutoEvent(final SensorsDataAPI sensorsDataAPI, final JSONObject properties, final View view) {
        // add $lib_method = autoTrack
        final JSONObject eventProperties = SADataHelper.appendLibMethodAutoTrack(properties);
        SACoreHelper.getInstance().trackQueueEvent(new Runnable() {
            @Override
            public void run() {
                if (view != null) {
                    SAModuleManager.getInstance().invokeModuleFunction(Modules.Visual.MODULE_NAME, Modules.Visual.METHOD_MERGE_VISUAL_PROPERTIES, properties, view);
                }
                sensorsDataAPI.getSAContextManager().
                        trackEvent(new InputData().setEventName(AutoTrackConstants.APP_CLICK_EVENT_NAME).setEventType(EventType.TRACK).setProperties(eventProperties));
            }
        });
    }

    private static boolean isAppClickIgnore(SensorsDataAPI sensorsDataAPI) {
        //AutoTrack is disable or $AppClick is ignored
        return !sensorsDataAPI.isAutoTrackEnabled() || sensorsDataAPI.isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_CLICK);
    }

    /**
     * The method is use to view click except TabLayout and Dialog
     */
    private static ViewContext getAppClickViewContext(View view, Class<?> classType) {
        return getAppClickViewContext(null, view, classType);
    }

    private static ViewContext getAppClickViewContext(Object parent, View view, Class<?> classType) {
        // View Type is ignored
        if (SAViewUtils.isViewIgnored(classType)) {
            return null;
        }

        //get Context
        Context context = parent instanceof Context ? (Context) parent : null;
        if (view != null) {
            // View is ignored
            if (SAViewUtils.isViewIgnored(view)) {
                return null;
            }
            if (context == null) {
                context = view.getContext();
                if (context == null) {
                    return null;
                }
            }
        }

        //Activity is ignored
        Activity activity = SAViewUtils.getActivityOfView(context, view);
        if (activity != null && SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
            return null;
        }

        // get view's fragment
        Object fragment = SAFragmentUtils.getFragmentFromView(view, activity);
        // fragment is ignored
        if (fragment != null && SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(fragment.getClass())) {
            return null;
        }
        return new ViewContext(activity, fragment, view);
    }

    private static JSONObject getExpandListViewExtendProperty(ExpandableListView expandableListView, int groupPosition, int childPosition) {
        try {
            // 扩展属性
            ExpandableListAdapter listAdapter = expandableListView.getExpandableListAdapter();
            if (listAdapter instanceof SensorsExpandableListViewItemTrackProperties) {
                SensorsExpandableListViewItemTrackProperties trackProperties = (SensorsExpandableListViewItemTrackProperties) listAdapter;
                return childPosition == -1 ? trackProperties.getSensorsGroupItemTrackProperties(groupPosition)
                        : trackProperties.getSensorsChildItemTrackProperties(groupPosition, childPosition);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    public static JSONObject buildPageProperty(Activity activity, Object fragment) {
        JSONObject properties = new JSONObject();
        // fragmentName
        if (fragment != null) {
            JSONUtils.mergeJSONObject(SAPageInfoUtils.getFragmentPageInfo(activity, fragment), properties);
        } else if (activity != null) {
            JSONUtils.mergeJSONObject(SAPageInfoUtils.getActivityPageInfo(activity), properties);
        }

        return properties;
    }

    /*
     * prevent multiple clicks
     */
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
}
