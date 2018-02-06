package com.sensorsdata.analytics.android.sdk.aop;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;

import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsAdapterViewItemTrackProperties;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.util.AopUtil;

import org.aspectj.lang.JoinPoint;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by 王灼洲 on 2017/8/26
 */

public class AdapterViewOnItemClickListenerAppClick {
    private final static String TAG = "AdapterViewOnItemClickListenerAppClick";

    public static void onAppClick(JoinPoint joinPoint) {
        try {
            //闭 AutoTrack
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

            //AdapterView
            Object object = joinPoint.getArgs()[0];
            if (object == null) {
                return;
            }

            //View
            View view = (View) joinPoint.getArgs()[1];
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

            //View Type 被忽略
            if (AopUtil.isViewIgnored(object.getClass())) {
                return;
            }

            //position
            int position = (int) joinPoint.getArgs()[2];

            JSONObject properties = new JSONObject();

            //View 被忽略
            AdapterView adapterView = (AdapterView) object;

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
            SALog.i(TAG, " AdapterView.OnItemClickListener.onItemClick AOP ERROR: " + e.getMessage());
        }
    }
}
