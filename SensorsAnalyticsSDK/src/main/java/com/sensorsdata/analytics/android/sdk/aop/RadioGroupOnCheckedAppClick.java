package com.sensorsdata.analytics.android.sdk.aop;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

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

public class RadioGroupOnCheckedAppClick {
    private final static String TAG = "RadioGroupOnCheckedAppClick";

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
            if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 2) {
                return;
            }

            //获取被点击的 View
            View view = (View) joinPoint.getArgs()[0];
            if (view == null) {
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

            if (view instanceof RadioGroup) { // RadioGroup
                properties.put(AopConstants.ELEMENT_TYPE, "RadioGroup");
                RadioGroup radioGroup = (RadioGroup) view;

                //获取变更后的选中项的ID
                int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
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
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                properties.put(AopConstants.ELEMENT_TYPE, view.getClass().getCanonicalName());
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
            SALog.i(TAG, "RadioGroup.OnCheckedChangeListener.onCheckedChanged AOP ERROR: " + e.getMessage());
        }
    }
}
