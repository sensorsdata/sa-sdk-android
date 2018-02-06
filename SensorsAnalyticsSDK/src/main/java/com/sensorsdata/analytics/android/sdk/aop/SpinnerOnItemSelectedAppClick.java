package com.sensorsdata.analytics.android.sdk.aop;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Spinner;

import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.util.AopUtil;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;

/**
 * Created by 王灼洲 on 2017/8/26
 */

public class SpinnerOnItemSelectedAppClick {
    private final static String TAG = "SpinnerOnItemSelectedAppClick";

    public static void onAppClick(JoinPoint joinPoint) {
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

            //获取被点击的 View
            android.widget.AdapterView adapterView = (android.widget.AdapterView) joinPoint.getArgs()[0];
            if (adapterView == null) {
                return;
            }

            //获取所在的 Context
            Context context = adapterView.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, adapterView);

            //Activity 被忽略
            if (activity != null) {
                if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(adapterView)) {
                return;
            }

            View view = (View) joinPoint.getArgs()[1];

            //position
            int position = (int) joinPoint.getArgs()[2];

            JSONObject properties = new JSONObject();

            AopUtil.addViewPathProperties(activity, view, properties);

            //ViewId
            String idString = AopUtil.getViewId(adapterView);
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

            if (adapterView instanceof Spinner) { // Spinner
                properties.put(AopConstants.ELEMENT_TYPE, "Spinner");
                Object item = adapterView.getItemAtPosition(position);
                properties.put(AopConstants.ELEMENT_POSITION, String.valueOf(position));
                if (item != null) {
                    if (item instanceof String) {
                        properties.put(AopConstants.ELEMENT_CONTENT, item);
                    }
                }
            } else {
                properties.put(AopConstants.ELEMENT_TYPE, adapterView.getClass().getCanonicalName());
            }

            //fragmentName
            AopUtil.getFragmentNameFromView(adapterView, properties);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) adapterView.getTag(R.id.sensors_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            SensorsDataAPI.sharedInstance().track(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            SALog.i(TAG, " AdapterView.OnItemSelectedListener.onItemSelected AOP ERROR: " + e.getMessage());
        }
    }
}
