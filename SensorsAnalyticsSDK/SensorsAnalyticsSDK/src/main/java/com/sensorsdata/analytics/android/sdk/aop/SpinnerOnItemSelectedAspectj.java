package com.sensorsdata.analytics.android.sdk.aop;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.widget.Spinner;

import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsObjectTrackProperties;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by 王灼洲 on 2016/11/29
 * spinner.setOnItemSelectedListener 事件
 */

@Aspect
public class SpinnerOnItemSelectedAspectj {
    private final static String TAG = SpinnerOnItemSelectedAspectj.class.getCanonicalName();

    @After("execution(* android.widget.AdapterView.OnItemSelectedListener.onItemSelected(android.widget.AdapterView,android.view.View,int,long))")
    public void onItemSelectedAOP(final JoinPoint joinPoint) throws Throwable {
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

                    //View 被忽略
                    if (AopUtil.isViewIgnored(adapterView)) {
                        return;
                    }

                    //position
                    int position = (int) joinPoint.getArgs()[2];

                    //获取 View 自定义属性
                    JSONObject properties = (JSONObject) adapterView.getTag(R.id.sensors_analytics_tag_view_properties);

                    if (properties == null) {
                        properties = new JSONObject();
                    }

                    //ViewId
                    String idString = AopUtil.getViewId(adapterView);
                    if (!TextUtils.isEmpty(idString)) {
                        properties.put(AopConstants.ELEMENT_ID, idString);
                    }

                    //Action
//                    properties.put(AopConstants.ELEMENT_ACTION, "onItemSelected");

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
                            } else if (item instanceof SensorsObjectTrackProperties) {
                                try {
                                    SensorsObjectTrackProperties objectTrackProperties = (SensorsObjectTrackProperties) item;
                                    JSONObject trackProperties = objectTrackProperties.getSensorsTrackProperties();
                                    if (trackProperties != null) {
                                        AopUtil.mergeJSONObject(trackProperties, properties);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else {
                        properties.put(AopConstants.ELEMENT_TYPE, adapterView.getClass().getCanonicalName());
                    }

                    SensorsDataAPI.sharedInstance().track(AopConstants.APP_CLICK_EVENT_NAME, properties);
                } catch (Exception e) {
                    e.printStackTrace();
                    SALog.i(TAG, " AdapterView.OnItemSelectedListener.onItemSelected AOP ERROR: " + e.getMessage());
                }
            }
        });
    }
}
