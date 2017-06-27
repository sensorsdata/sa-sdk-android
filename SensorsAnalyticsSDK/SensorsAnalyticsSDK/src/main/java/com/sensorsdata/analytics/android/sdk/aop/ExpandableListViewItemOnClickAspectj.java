package com.sensorsdata.analytics.android.sdk.aop;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsExpandableListViewItemTrackProperties;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Created by 王灼洲 on 2016/12/1
 * android.widget.ExpandableListView.OnChildClickListener.onChildClick
 * android.widget.ExpandableListView.OnGroupClickListener.onGroupClick
 */

@Aspect
public class ExpandableListViewItemOnClickAspectj {
    private final static String TAG = ExpandableListViewItemOnClickAspectj.class.getCanonicalName();

    /**
     * public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id)
     *
     * @param joinPoint JoinPoint
     * @throws Throwable Exception
     */
    @After("execution(* android.widget.ExpandableListView.OnChildClickListener.onChildClick(android.widget.ExpandableListView, android.view.View, int, int, long))")
    public void onChildClickAOP(final JoinPoint joinPoint) throws Throwable {
        AopThreadPool.getInstance().execute(new Runnable() {
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

                    //基本校验
                    if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 5) {
                        return;
                    }

                    //获取 ExpandableListView
                    ExpandableListView expandableListView = (ExpandableListView) joinPoint.getArgs()[0];
                    if (expandableListView == null) {
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
                        if (SensorsDataAPI.sharedInstance().isActivityAutoTrackIgnored(activity.getClass())) {
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

                    //获取 View
                    View view = (View) joinPoint.getArgs()[1];

                    //View 被忽略
                    if (AopUtil.isViewIgnored(view)) {
                        return;
                    }

                    //获取 groupPosition 位置
                    int groupPosition = (int) joinPoint.getArgs()[2];

                    //获取 childPosition 位置
                    int childPosition = (int) joinPoint.getArgs()[3];

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

//                    properties.put(AopConstants.ELEMENT_ACTION, "onChildClick");
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
                    SALog.i(TAG, " ExpandableListView.OnChildClickListener.onChildClick AOP ERROR: " + e.getMessage());
                }
            }
        });
    }

    /**
     * public boolean onGroupClick(ExpandableListView expandableListView, View view, int groupPosition, long l)
     *
     * @param joinPoint
     * @throws Throwable
     */
    @After("execution(* android.widget.ExpandableListView.OnGroupClickListener.onGroupClick(android.widget.ExpandableListView, android.view.View, int, long))")
    public void onGroupClickAOP(final JoinPoint joinPoint) throws Throwable {
        AopThreadPool.getInstance().execute(new Runnable() {
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

                    //基本校验
                    if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 4) {
                        return;
                    }

                    //获取 ExpandableListView
                    ExpandableListView expandableListView = (ExpandableListView) joinPoint.getArgs()[0];
                    if (expandableListView == null) {
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
                        if (SensorsDataAPI.sharedInstance().isActivityAutoTrackIgnored(activity.getClass())) {
                            return;
                        }
                    }

                    // ExpandableListView Type 被忽略
                    if (AopUtil.isViewIgnored(joinPoint.getArgs()[0].getClass())) {
                        return;
                    }

                    // View 被忽略
                    if (AopUtil.isViewIgnored(expandableListView)) {
                        return;
                    }

                    // 获取 View
                    View view = (View) joinPoint.getArgs()[1];

                    // 获取 groupPosition 位置
                    int groupPosition = (int) joinPoint.getArgs()[2];

                    JSONObject properties = new JSONObject();

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

//                    properties.put(AopConstants.ELEMENT_ACTION, "onGroupClick");
                    properties.put(AopConstants.ELEMENT_TYPE, "ExpandableListView");

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
                    SALog.i(TAG, " ExpandableListView.OnChildClickListener.onGroupClick AOP ERROR: " + e.getMessage());
                }
            }
        });
    }
}
