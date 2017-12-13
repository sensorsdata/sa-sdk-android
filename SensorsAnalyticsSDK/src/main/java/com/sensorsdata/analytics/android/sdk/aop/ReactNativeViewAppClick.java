package com.sensorsdata.analytics.android.sdk.aop;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.util.AopUtil;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;

import java.lang.reflect.Method;

/**
 * Created by 王灼洲 on 2017/8/26
 */

public class ReactNativeViewAppClick {
    private final static String TAG = "ReactNativeViewAppClick";

    public static void onAppClick(JoinPoint joinPoint) {
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

            int reactTag = (int) joinPoint.getArgs()[0];

            Object target = joinPoint.getTarget();
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
                        if (context == null) {
//                        return;
                        }

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
            SALog.i(TAG, "onNativeViewHierarchyManagerSetJSResponderAOP error: " + e.getMessage());
        }
    }
}
