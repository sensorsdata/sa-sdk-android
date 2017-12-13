package com.sensorsdata.analytics.android.sdk.aop;

import android.text.TextUtils;
import android.widget.TabHost;

import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.util.AopUtil;

import org.aspectj.lang.JoinPoint;
import org.json.JSONObject;

/**
 * Created by 王灼洲 on 2017/8/26
 */

public class TabHostOnTabChangedAppClick {
    private final static String TAG = "TabHostOnTabChangedAppClick";

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
            if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 1) {
                return;
            }

            //TabHost 被忽略
            if (AopUtil.isViewIgnored(TabHost.class)) {
                return;
            }

            //获取被点击的 tabName
            String tabName = (String) joinPoint.getArgs()[0];

            JSONObject properties = new JSONObject();

            //$title、$screen_name、$element_content
            try {
                if (!TextUtils.isEmpty(tabName)) {
                    String[] temp = tabName.split("##");

                    switch (temp.length) {
                        case 3:
                            properties.put(AopConstants.TITLE, temp[2]);
                        case 2:
                            properties.put(AopConstants.SCREEN_NAME, temp[1]);
                        case 1:
                            properties.put(AopConstants.ELEMENT_CONTENT, temp[0]);
                            break;
                    }
                }
            } catch (Exception e) {
                properties.put(AopConstants.ELEMENT_CONTENT, tabName);
                e.printStackTrace();
            }

            properties.put(AopConstants.ELEMENT_TYPE, "TabHost");

            SensorsDataAPI.sharedInstance().track(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            SALog.i(TAG, " onTabChanged AOP ERROR: " + e.getMessage());
        }
    }
}
