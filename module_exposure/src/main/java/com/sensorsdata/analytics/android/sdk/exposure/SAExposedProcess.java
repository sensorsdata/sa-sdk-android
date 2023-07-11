package com.sensorsdata.analytics.android.sdk.exposure;

import android.app.Activity;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.view.View;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.core.business.exposure.SAExposureConfig;
import com.sensorsdata.analytics.android.sdk.core.business.exposure.SAExposureData;
import com.sensorsdata.analytics.android.sdk.monitor.SensorsDataLifecycleMonitorManager;
import com.sensorsdata.analytics.android.sdk.util.AppStateTools;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.SAViewUtils;

import org.json.JSONObject;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;


public class SAExposedProcess {
    private static final String TAG = "SA.SAExposedProcess";
    private static final long DELAY_TIME = 500;
    private final SAExposureConfig mExposureConfig;
    private WeakHashMap<Activity, ExposedPage> mExposedPageWeakHashMap;
    private CallBack mCallBack;
    private Handler mHandler;
    private ExposureRunnable mExposureRunnable;
    private String mLastActivityUrl = "";
    private boolean isActivityChange;
    private WeakHashMap<ExposureView, StayDurationRunnable> mStayDurationRunnableWeakHashMap;
    private ExposedTransform mExposedTransform;//页面监听

    interface CallBack {
        void viewLayoutChange(Activity activity);

        void onActivityResumed(Activity activity);

        void onActivityPaused(Activity activity);

        int getExposureViewSize(Activity activity);

        Collection<ExposureView> getExposureViews(Activity activity);
    }


    class ExposureRunnable implements Runnable {
        private final ExposedPage mExposedPage;
        private final View mView;

        public ExposureRunnable(ExposedPage exposedPage, View view) {
            this.mExposedPage = exposedPage;
            this.mView = view;
        }

        @Override
        public void run() {
            try {
                List<ExposureView> exposureViewList = mExposedPage.getExposureViewList(mView);
                for (ExposureView exposureView : exposureViewList) {
                    if (exposureView.getView() != null && exposureView.isViewLayoutChange()) {
                        synchronized (SAExposedProcess.class) {
                            StayDurationRunnable mStayDurationRunnable = mStayDurationRunnableWeakHashMap.get(exposureView);
                            if (mStayDurationRunnable != null) {
                                mHandler.removeCallbacks(mStayDurationRunnable);
                                mStayDurationRunnableWeakHashMap.remove(exposureView);
                            }
                            SALog.i(TAG, "ExposureRunnable->exposureView:" + exposureView);
                            SAExposureData exposureData = exposureView.getExposureData();
                            if (exposureData != null) {
                                long delay = (long) (exposureData.getExposureConfig().getStayDuration() * 1000);
                                mStayDurationRunnable = new StayDurationRunnable(exposureView);
                                mStayDurationRunnableWeakHashMap.put(exposureView, mStayDurationRunnable);
                                mHandler.postDelayed(mStayDurationRunnable, delay);
                                exposureView.setViewLayoutChange(false);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    private synchronized void init() {
        try {
            if (mExposedPageWeakHashMap == null) {
                mExposedPageWeakHashMap = new WeakHashMap<>();
                mStayDurationRunnableWeakHashMap = new WeakHashMap<>();
                HandlerThread handlerThread = new HandlerThread("SA.Exposured");
                handlerThread.start();
                mHandler = new Handler(handlerThread.getLooper());
                mCallBack = new CallBack() {
                    @Override
                    public void viewLayoutChange(Activity activity) {
                        if (mExposureRunnable != null) {
                            mHandler.removeCallbacks(mExposureRunnable);
                        }
                        removeStayDurationRunnable();
                        ExposedPage exposedPage = mExposedPageWeakHashMap.get(activity);
                        if (exposedPage != null) {
                            mExposureRunnable = new ExposureRunnable(exposedPage, null);
                            if (mExposureConfig != null) {
                                SALog.i(TAG, "delayTime:" + mExposureConfig.getDelayTime());
                                mHandler.postDelayed(mExposureRunnable, mExposureConfig.getDelayTime());
                            } else {
                                SALog.i(TAG, "delayTime->500ms");
                                mHandler.postDelayed(mExposureRunnable, DELAY_TIME);
                            }
                        }
                    }

                    @Override
                    public void onActivityResumed(Activity activity) {
                        try {
                            String currentUrl = activity.getClass().getCanonicalName();
                            isActivityChange = !mLastActivityUrl.equals(currentUrl);
                            mLastActivityUrl = currentUrl;
                            if (isActivityChange) {
                                ExposedPage exposedPage = mExposedPageWeakHashMap.get(activity);
                                if (exposedPage != null) {
                                    Collection<ExposureView> exposureViews = exposedPage.getExposureViews();
                                    for (ExposureView exposureView : exposureViews) {
                                        if (exposureView != null) {
                                            exposureView.setActivityChange(true);
                                        }
                                    }
                                }
                            }

                            viewLayoutChange(activity);
                        } catch (Exception exception) {
                            SALog.printStackTrace(exception);
                        }
                    }

                    @Override
                    public void onActivityPaused(Activity activity) {
                        try {
                            if (mExposedPageWeakHashMap == null) {
                                return;
                            }
                            ExposedPage exposedPage = mExposedPageWeakHashMap.get(activity);
                            if (exposedPage != null) {
                                exposedPage.invisibleElement();
                            }
                            removeStayDurationRunnable();
                        } catch (Exception e) {
                            SALog.printStackTrace(e);
                        }
                    }

                    @Override
                    public int getExposureViewSize(Activity activity) {
                        try {
                            if (mExposedPageWeakHashMap != null) {
                                ExposedPage exposedPage = mExposedPageWeakHashMap.get(activity);
                                if (exposedPage != null) {
                                    return exposedPage.getExposureViewSize();
                                }
                            }
                        } catch (Exception e) {
                            SALog.printStackTrace(e);
                        }
                        return 0;
                    }

                    @Override
                    public Collection<ExposureView> getExposureViews(Activity activity) {
                        try {
                            if (mExposedPageWeakHashMap != null) {
                                ExposedPage exposedPage = mExposedPageWeakHashMap.get(activity);
                                if (exposedPage != null) {
                                    return exposedPage.getExposureViews();
                                }
                            }
                        } catch (Exception e) {
                            SALog.printStackTrace(e);
                        }
                        return null;
                    }
                };
                mExposedTransform = new ExposedTransform(mCallBack);
                SensorsDataLifecycleMonitorManager.getInstance().addActivityLifeCallback(mExposedTransform);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public SAExposedProcess(SAExposureConfig exposureConfig) {
        this.mExposureConfig = exposureConfig;
    }

    private void removeStayDurationRunnable() {
        synchronized (SAExposedProcess.class) {
            if (mStayDurationRunnableWeakHashMap == null) {
                return;
            }
            Iterator<ExposureView> iterator = mStayDurationRunnableWeakHashMap.keySet().iterator();
            while (iterator.hasNext()) {
                ExposureView exposureView = iterator.next();
                if (exposureView != null && exposureView.isViewLayoutChange()) {
                    SALog.i(TAG, "remove ExposureView = " + exposureView);
                    StayDurationRunnable stayDurationRunnable = mStayDurationRunnableWeakHashMap.get(exposureView);
                    mHandler.removeCallbacks(stayDurationRunnable);
                    iterator.remove();
                }
            }
        }
    }

    private Activity exposureProcess(View view, SAExposureData exposureData, boolean isAddExposureView) throws Exception {
        if (view == null || exposureData == null) {
            return null;
        }
        Activity activity = SAViewUtils.getActivityOfView(view.getContext(), view);
        if (activity == null) {
            activity = AppStateTools.getInstance().getForegroundActivity();
        }
        if (activity == null) {
            return null;
        }
        init();
        ExposedPage exposedPage = mExposedPageWeakHashMap.get(activity);
        if (exposedPage == null) {
            exposedPage = new ExposedPage();
            mExposedPageWeakHashMap.put(activity, exposedPage);
        }
        if (isAddExposureView) {
            SAExposureConfig saExposureConfig = exposureData.getExposureConfig();
            if (saExposureConfig == null) {
                exposureData.setExposureConfig(mExposureConfig);
            }
        }
        ExposureView exposureView = null;
        String identifier = exposureData.getIdentifier();
        if (!TextUtils.isEmpty(identifier)) {
            ExposureView tmpExposureView = exposedPage.getExposureView(identifier);
            ExposureView oldExposureView = exposedPage.getExposureView(view);
            if (oldExposureView != null && oldExposureView.getExposureData() != null && oldExposureView.getExposureData().getIdentifier() != null && !oldExposureView.getExposureData().getIdentifier().equals(exposureData.getIdentifier())) {
                oldExposureView.setLastVisible(false);
            }
            if (tmpExposureView != null) {
                exposureView = tmpExposureView.clone();
                exposureView.setView(view);//列表复用 view 改变
                if (isAddExposureView) {
                    exposureView.setExposureData(exposureData);
                }
            }
        } else if (exposedPage.getExposureView(view) != null) {
            exposureView = exposedPage.getExposureView(view);
            exposureView.setExposureData(exposureData);
        }
        if (exposureView == null) {
            exposureView = new ExposureView(exposureData, false, false, view);
        }
        if (isAddExposureView) {
            exposureView.setAddExposureView(true);
        }
        SALog.i(TAG, "addExposureView:" + exposureView);
        exposedPage.addExposureView(view, exposureView);
        exposedPage.addExposureView(exposureData.getIdentifier(), exposureView);

        return activity;
    }

    public void setExposureIdentifier(View view, String exposureIdentifier) {
        SAExposureData exposureData = new SAExposureData(null, null, exposureIdentifier, null);
        try {
            exposureProcess(view, exposureData, false);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public void addExposureView(View view, SAExposureData exposureData) {
        try {
            if (!verifyExposureData(exposureData)) {
                return;
            }
            Activity activity = exposureProcess(view, exposureData, true);
            if (activity == null) {
                return;
            }
            mCallBack.viewLayoutChange(activity);
            //通知页面开始监听
            if (mExposedTransform != null) {
                mExposedTransform.observerWindow(activity);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public synchronized void updateExposureView(View view, JSONObject properties) {
        if (view == null) {
            return;
        }
        ExposedPage exposedPage = mExposedPageWeakHashMap.get(SAViewUtils.getActivityOfView(view.getContext(), view));
        if (exposedPage != null) {
            ExposureView exposureView = exposedPage.getExposureView(view);
            if (exposureView != null && exposureView.getExposureData() != null) {
                JSONUtils.mergeJSONObject(properties, exposureView.getExposureData().getProperties());
            }
        }
    }

    private boolean verifyExposureData(SAExposureData exposureData) {
        if (exposureData == null) {
            SALog.i(TAG, "SAExposureData is null");
            return false;
        }
        if (TextUtils.isEmpty(exposureData.getEvent())) {
            SALog.i(TAG, "EventName is empty or null");
            return false;
        }
        SAExposureConfig exposureConfig = exposureData.getExposureConfig();
        if (exposureConfig != null) {
            if (exposureConfig.getAreaRate() > 1.0f || exposureConfig.getAreaRate() < 0.0f) {
                SALog.i(TAG, "SAExposureConfig areaRate must be 0~1");
                exposureConfig.setAreaRate(0.0f);
                return true;
            }
        } else {
            if (mExposureConfig != null) {
                if (mExposureConfig.getAreaRate() > 1.0f || mExposureConfig.getAreaRate() < 0.0f) {
                    SALog.i(TAG, "Global SAExposureConfig areaRate must be 0~1");
                    exposureConfig = new SAExposureConfig(0.0f, mExposureConfig.getStayDuration(), mExposureConfig.isRepeated());
                    exposureData.setExposureConfig(exposureConfig);
                    return true;
                }
            }
        }
        return true;
    }

    public void removeExposureView(View view, String identifier) {
        if (view == null) {
            return;
        }
        Activity activity = SAViewUtils.getActivityOfView(view.getContext(), view);
        if (activity == null) {
            activity = AppStateTools.getInstance().getForegroundActivity();
        }
        if (activity == null) {
            return;
        }
        ExposedPage exposedPage = null;
        if (mExposedPageWeakHashMap != null) {
            exposedPage = mExposedPageWeakHashMap.get(activity);
        }
        if (exposedPage != null) {
            exposedPage.removeExposureView(view, identifier);
        }
    }
}
