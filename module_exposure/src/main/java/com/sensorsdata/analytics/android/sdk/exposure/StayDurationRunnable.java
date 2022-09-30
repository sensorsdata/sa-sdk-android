package com.sensorsdata.analytics.android.sdk.exposure;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;

import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.SAViewUtils;
import com.sensorsdata.analytics.android.sdk.util.visual.ViewNode;
import com.sensorsdata.analytics.android.sdk.util.visual.ViewTreeStatusObservable;

import org.json.JSONObject;

public class StayDurationRunnable implements Runnable {

    private final ExposureView mExposureView;
    private static final String TAG = "SA.StayDurationRunnable";

    public StayDurationRunnable(ExposureView exposureView) {
        this.mExposureView = exposureView;
    }

    @Override
    public void run() {
        try {
            JSONObject exposureProperties = mExposureView.getExposureData().getProperties();
            JSONObject jsonObject;
            if (exposureProperties == null) {
                jsonObject = new JSONObject();
            } else {
                jsonObject = new JSONObject(exposureProperties.toString());
            }
            View view = mExposureView.getView();
            if (view == null) {
                return;
            }
            Activity activity = SAViewUtils.getActivityOfView(view.getContext(), view);
            if (activity == null) {
                return;
            }
            SALog.i(TAG, "StayDurationRunnable:" + mExposureView);
            JSONObject activityProperty = SAViewUtils.getScreenNameAndTitle(view);
            if (activityProperty == null || TextUtils.isEmpty(activityProperty.toString())) {
                activityProperty = SAViewUtils.buildTitleAndScreenName(activity);
            }

            JSONUtils.mergeJSONObject(activityProperty, jsonObject);
            ViewNode viewNode = ViewTreeStatusObservable.getInstance().getViewNode(view);
            if (viewNode != null) {
                if (!TextUtils.isEmpty(viewNode.getViewPath())) {
                    jsonObject.put(AopConstants.ELEMENT_PATH, viewNode.getViewPath());
                }
                if (!TextUtils.isEmpty(viewNode.getViewPosition())) {
                    jsonObject.put(AopConstants.ELEMENT_POSITION, viewNode.getViewPosition());
                }
            }
            String viewText = SAViewUtils.getViewContent(view);
            if (!TextUtils.isEmpty(viewText)) {
                jsonObject.put(AopConstants.ELEMENT_CONTENT, viewText);
            }
            jsonObject.put(AopConstants.ELEMENT_TYPE, SAViewUtils.getViewType(view));
            String eventName = mExposureView.getExposureData().getEvent();
            //曝光事件发送
            SensorsDataAPI.sharedInstance().track(eventName, jsonObject);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        mExposureView.setLastVisible(true);
        mExposureView.setExposed(true);
    }
}