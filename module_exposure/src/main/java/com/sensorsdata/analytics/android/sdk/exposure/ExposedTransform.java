package com.sensorsdata.analytics.android.sdk.exposure;


import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.monitor.SensorsDataActivityLifecycleCallbacks;
import com.sensorsdata.analytics.android.sdk.util.WindowHelper;
import com.sensorsdata.analytics.android.sdk.util.visual.ViewUtil;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;

public class ExposedTransform implements SensorsDataActivityLifecycleCallbacks.SAActivityLifecycleCallbacks {

    private final String TAG = "SA.ExposedTransform";
    private final AppPageChange mAppPageChange;
    private final SAExposedProcess.CallBack mCallBack;
    private WeakReference<Activity> mActivityWeakReference;
    private volatile boolean isMonitor = false;
    // 标识 Activity Resume 引起的布局变化，会引起一次曝光计算
    private boolean isResumedLayoutChanged;
    private volatile int windowCount = -1;

    @Override
    public void onNewIntent(Intent intent) {

    }

    public synchronized void observerWindow(Activity activity) {
        int originWindowCount = windowCount;
        View[] views  = processViews();
        SALog.i(TAG, "originWindowCount:" + originWindowCount + ",windowCount:" + windowCount);
        //窗口增加
        if (originWindowCount != windowCount) {
            //移除以前的页面监听
            viewsRemoveTreeObserver(activity, views);
            //重新进行页面监听
            onActivityResumed(activity);
            return;
        }
        //正常情况未监听则进行监听,避免页面未改变导致的未监听
        if (!isMonitor) {
            onActivityResumed(activity);
        }
    }

    private View[] processViews() {
        try {
            WindowHelper.init();
            View[] views = WindowHelper.getSortedWindowViews();
            if (views.length > 0) {
                windowCount = views.length;
            } else {
                windowCount = 0;
            }
            return views;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    interface LayoutCallBack {
        void viewLayoutChange();
    }

    public ExposedTransform(final SAExposedProcess.CallBack callBack) {
        this.mCallBack = callBack;
        LayoutCallBack layoutCallBack = new LayoutCallBack() {
            @Override
            public void viewLayoutChange() {
                if (mActivityWeakReference != null) {
                    Activity activity = mActivityWeakReference.get();
                    if (activity != null) {
                        if (isViewChanged(activity)) {
                            isResumedLayoutChanged = false;
                            callBack.viewLayoutChange(activity);
                        }
                    }
                }
            }
        };
        mAppPageChange = new AppPageChange(layoutCallBack);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        mActivityWeakReference = new WeakReference<>(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        //避免在 onCreate 中操作可见性这里 activityWeakReference 为空无法监控到
        mActivityWeakReference = new WeakReference<>(activity);
        isResumedLayoutChanged = true;
        SALog.i(TAG, "onActivityResumed:" + activity);
        synchronized (this) {
            viewsAddTreeObserver(activity);
            mCallBack.onActivityResumed(activity);
        }
    }


    private void viewTreeObserver(View view) {
        ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(mAppPageChange);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            viewTreeObserver.addOnWindowFocusChangeListener(mAppPageChange);
        }
        viewTreeObserver.addOnScrollChangedListener(mAppPageChange);
        viewTreeObserver.addOnGlobalFocusChangeListener(mAppPageChange);
    }

    private void viewRemoveTreeObserver(View view) {
        ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
        viewTreeObserver.removeGlobalOnLayoutListener(mAppPageChange);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            viewTreeObserver.removeOnWindowFocusChangeListener(mAppPageChange);
        }
        viewTreeObserver.removeOnScrollChangedListener(mAppPageChange);
        viewTreeObserver.removeOnGlobalFocusChangeListener(mAppPageChange);
    }

    private void viewsAddTreeObserver(Activity activity) {
        SALog.i(TAG, "viewsAddTreeObserver:" + isMonitor);
        if (!isMonitor) {
            if (mCallBack.getExposureViewSize(activity) <= 0) {
                return;
            }
            processViews();
            boolean flag = true;
            View[] views = WindowHelper.getSortedWindowViews();
            View decorView = activity.getWindow().getDecorView();
            if (views != null && views.length > 0) {
                for (View view : views) {
                    if (decorView == view) {
                        //由于 onResume 的时候获取到的窗口数量不是最新的，因此需要加这个逻辑
                        flag = false;
                    }
                    viewTreeObserver(view);
                }
                if (flag) {
                    viewTreeObserver(decorView);
                }
            } else {
                viewTreeObserver(activity.getWindow().getDecorView());
            }
            isMonitor = true;
        }
    }

    private void viewsRemoveTreeObserver(Activity activity, View[] views) {
        SALog.i(TAG, "viewsRemoveTreeObserver:" + isMonitor);
        if (isMonitor) {
            isMonitor = false;
            if (views == null) {
                views = WindowHelper.getSortedWindowViews();
            }
            if (views != null && views.length > 0) {
                for (View view : views) {
                    viewRemoveTreeObserver(view);
                }
            } else {
                viewRemoveTreeObserver(activity.getWindow().getDecorView());
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        SALog.i(TAG, "onActivityPaused");
        synchronized (this) {
            viewsRemoveTreeObserver(activity, null);
            mCallBack.onActivityPaused(activity);
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    private boolean isViewChanged(Activity activity) {
        try {
            Collection<ExposureView> exposureViews = mCallBack.getExposureViews(activity);
            if (exposureViews == null || exposureViews.isEmpty()) {
                return false;
            }
            Iterator<ExposureView> iterator = exposureViews.iterator();
            ExposureView exposureView;
            boolean isViewChanged = false;
            while (iterator.hasNext()) {
                try {
                    exposureView = iterator.next();
                    View view = exposureView.getView();
                    int[] size = new int[2];
                    view.getLocationOnScreen(size);
                    String tempState = (String) view.getTag(R.id.sensors_analytics_tag_view_exposure_key);
                    String newState = size[0] + "," + size[1] + "," + ViewUtil.viewVisibilityInParents(view);
                    if (!newState.equals(tempState) || isResumedLayoutChanged) {
                        SALog.i(TAG, tempState + ", newSize = " + newState + ",view = " + view);
                        isViewChanged = true;
                        exposureView.setViewLayoutChange(true);
                    }
                    view.setTag(R.id.sensors_analytics_tag_view_exposure_key, newState);
                } catch (Exception exception) {
                    SALog.printStackTrace(exception);
                }
            }
            return isViewChanged;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return true;
    }
}
