package com.sensorsdata.analytics.android.sdk.exposure;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.business.exposure.SAExposureListener;
import com.sensorsdata.analytics.android.sdk.util.Dispatcher;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.SAViewUtils;
import com.sensorsdata.analytics.android.sdk.util.visual.ViewNode;
import com.sensorsdata.analytics.android.sdk.util.visual.ViewTreeStatusObservable;

import org.json.JSONObject;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class StayDurationRunnable implements Runnable {

    private final ExposureView mExposureView;
    private static final String TAG = "SA.StayDurationRunnable";

    public StayDurationRunnable(ExposureView exposureView) {
        this.mExposureView = exposureView;
    }

    @Override
    public void run() {
        try {
            if (!shouldExposure(mExposureView)) {
                return;
            }
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
            if (activity != null) {
                SALog.i(TAG, "activity is not null.");
                JSONObject activityProperty = SAViewUtils.getScreenNameAndTitle(view);
                if (activityProperty == null || TextUtils.isEmpty(activityProperty.toString())) {
                    activityProperty = SAViewUtils.buildTitleAndScreenName(activity);
                }
                JSONUtils.mergeJSONObject(activityProperty, jsonObject);
            }
            SALog.i(TAG, "StayDurationRunnable:" + mExposureView);

            ViewNode viewNode = ViewTreeStatusObservable.getInstance().getViewNode(view);
            if (viewNode != null) {
                if (!TextUtils.isEmpty(viewNode.getViewPath())) {
                    jsonObject.put("$element_path", viewNode.getViewPath());
                }
                if (!TextUtils.isEmpty(viewNode.getViewPosition())) {
                    jsonObject.put("$element_position", viewNode.getViewPosition());
                }
            }
            String viewText = SAViewUtils.getViewContent(view);
            if (!TextUtils.isEmpty(viewText)) {
                jsonObject.put("$element_content", viewText);
            }
            jsonObject.put("$element_type", SAViewUtils.getViewType(view));
            String eventName = mExposureView.getExposureData().getEvent();
            //曝光事件发送
            SensorsDataAPI.sharedInstance().track(eventName, jsonObject);
            didExposure(mExposureView);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        mExposureView.setLastVisible(true);
        mExposureView.setExposed(true);
        mExposureView.setActivityChange(false);
    }

    /*
        是否曝光
     */
    private boolean shouldExposure(final ExposureView mExposureView) {
        try {
            final SAExposureListener exposureListener = mExposureView.getExposureData().getExposureListener();
            if (exposureListener != null) {
                FutureTask<Boolean> futureTask = new FutureTask<>(new Callable<Boolean>() {
                    @Override
                    public Boolean call() {
                        return exposureListener.shouldExposure(mExposureView.getView(), mExposureView.getExposureData());
                    }
                });
                Dispatcher.getInstance().getUiThreadHandler().post(futureTask);
                if (!futureTask.get(3000, TimeUnit.MILLISECONDS)) {
                    SALog.i(TAG, "Exposure fail, shouldExposure is false");
                    return false;
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return true;
    }

    /*
     曝光完成回调
     */
    private void didExposure(final ExposureView mExposureView) {
        try {
            final SAExposureListener exposureListener = mExposureView.getExposureData().getExposureListener();
            if (exposureListener != null && Dispatcher.getInstance().getUiThreadHandler() != null) {
                Dispatcher.getInstance().getUiThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        exposureListener.didExposure(mExposureView.getView(), mExposureView.getExposureData());
                    }
                });
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}