package com.sensorsdata.analytics.android.sdk.aop;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.ToggleButton;

import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.json.JSONObject;

/**
 * Created by 王灼洲 on 2016/11/29
 * CheckBox、ToggleButton 的onCheckedChanged事件
 */

@Aspect
public class CheckBoxOnCheckedChangedAspectj {
    private final static String TAG = CheckBoxOnCheckedChangedAspectj.class.getCanonicalName();

    @After("execution(* android.widget.CompoundButton.OnCheckedChangeListener.onCheckedChanged(android.widget.CompoundButton,boolean))")
    public void onCheckedChangedAOP(final JoinPoint joinPoint) throws Throwable {
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
                        if (SensorsDataAPI.sharedInstance().isActivityAutoTrackIgnored(activity.getClass())) {
                            return;
                        }
                    }

                    //View 被忽略
                    if (AopUtil.isViewIgnored(view)) {
                        return;
                    }

                    //是否选中
                    boolean isChecked = (boolean) joinPoint.getArgs()[1];

                    JSONObject properties = new JSONObject();

                    //ViewId
                    String idString = AopUtil.getViewId(view);
                    if (!TextUtils.isEmpty(idString)) {
                        properties.put(AopConstants.ELEMENT_ID, idString);
                    }

                    //Element Content
//                    if (isChecked) {
//                        properties.put(AopConstants.ELEMENT_CONTENT, "checked");
//                    } else {
//                        properties.put(AopConstants.ELEMENT_CONTENT, "unchecked");
//                    }

                    //$screen_name & $title
                    if (activity != null) {
                        properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                        String activityTitle = AopUtil.getActivityTitle(activity);
                        if (!TextUtils.isEmpty(activityTitle)) {
                            properties.put(AopConstants.TITLE, activityTitle);
                        }
                    }

                    String viewText = null;
                    if (view instanceof CheckBox) { // CheckBox
                        properties.put(AopConstants.ELEMENT_TYPE, "CheckBox");
                        CompoundButton compoundButton = (CompoundButton) view;
                        if (!TextUtils.isEmpty(compoundButton.getText())) {
                            viewText = compoundButton.getText().toString();
                        }
                    } else if (view instanceof SwitchCompat) {
                        properties.put(AopConstants.ELEMENT_TYPE, "SwitchCompat");
                        SwitchCompat switchCompat = (SwitchCompat) view;
                        if (!TextUtils.isEmpty(switchCompat.getTextOn())) {
                            viewText = switchCompat.getTextOn().toString();
                        }
//                        if (isChecked) {
//                            properties.put(AopConstants.ELEMENT_ACTION, "checked");
//                        } else {
//                            properties.put(AopConstants.ELEMENT_ACTION, "unchecked");
//                        }
                    } else if (view instanceof ToggleButton) { // ToggleButton
                        properties.put(AopConstants.ELEMENT_TYPE, "ToggleButton");
                        ToggleButton toggleButton = (ToggleButton) view;
                        if (isChecked) {
                            if (!TextUtils.isEmpty(toggleButton.getTextOn())) {
                                viewText = toggleButton.getTextOn().toString();
                            }
                        } else {
                            if (!TextUtils.isEmpty(toggleButton.getTextOff())) {
                                viewText = toggleButton.getTextOff().toString();
                            }
                        }
                    } else if (view instanceof RadioButton) { // RadioButton
                        properties.put(AopConstants.ELEMENT_TYPE, "RadioButton");
                        RadioButton radioButton = (RadioButton) view;
                        if (!TextUtils.isEmpty(radioButton.getText())) {
                            viewText = radioButton.getText().toString();
                        }
                    } else {
                        properties.put(AopConstants.ELEMENT_TYPE, view.getClass().getCanonicalName());
                    }

                    //Content
                    if (!TextUtils.isEmpty(viewText)) {
                        properties.put(AopConstants.ELEMENT_CONTENT, viewText);
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
                    SALog.i(TAG, " onCheckedChanged AOP ERROR: " + e.getMessage());
                }
            }
        });
    }
}
