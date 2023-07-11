package com.sensorsdata.analytics.android.sdk.exposure;

import android.graphics.Rect;
import android.text.TextUtils;
import android.view.View;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.core.business.exposure.SAExposureConfig;
import com.sensorsdata.analytics.android.sdk.core.business.exposure.SAExposureData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class ExposedPage {
    private final WeakHashMap<View, ExposureView> mViewWeakHashMap;
    private final Map<String, ExposureView> mExposureViewMap;
    private final ExposureVisible mExposureVisible;
    private static final String TAG = "SA.ExposedPage";

    public ExposedPage() {
        mViewWeakHashMap = new WeakHashMap<>();
        mExposureViewMap = new HashMap<>();
        mExposureVisible = new ExposureVisible();
    }

    public int getExposureViewSize() {
        return mViewWeakHashMap.size();
    }

    public synchronized void addExposureView(View view, ExposureView exposureView) {
        if (view == null || exposureView == null) {
            return;
        }
        mViewWeakHashMap.put(view, exposureView);
    }

    public synchronized void addExposureView(String identifier, ExposureView exposureView) {
        if (TextUtils.isEmpty(identifier) || exposureView == null) {
            return;
        }
        mExposureViewMap.put(identifier, exposureView);
    }

    public synchronized void removeExposureView(View view, String identifier) {
        if (view == null) {
            return;
        }
        ExposureView exposureView = getExposureView(view);
        if (exposureView != null && exposureView.getExposureData() != null) {
            if (exposureView.getExposureData().getIdentifier() != null && identifier != null) {
                if (exposureView.getExposureData().getIdentifier().equals(identifier)) {
                    mViewWeakHashMap.remove(view);
                    mExposureViewMap.remove(identifier);
                }
            } else {
                if (exposureView.getExposureData().getIdentifier() == null && identifier == null) {
                    mViewWeakHashMap.remove(view);
                }
            }
        }
    }

    public Collection<ExposureView> getExposureViews() {
        return mViewWeakHashMap.values();
    }

    public synchronized ExposureView getExposureView(View view) {
        if (view == null) {
            return null;
        }
        return mViewWeakHashMap.get(view);
    }

    public synchronized ExposureView getExposureView(String identifier) {
        if (TextUtils.isEmpty(identifier)) {
            return null;
        }
        return mExposureViewMap.get(identifier);
    }

    public synchronized void invisibleElement() {
        Iterator<View> iterator = mViewWeakHashMap.keySet().iterator();
        while (iterator.hasNext()) {
            View view = iterator.next();
            if (view != null) {
                ExposureView exposureView = mViewWeakHashMap.get(view);
                if (exposureView != null) {
                    exposureView.setLastVisible(false);
                }
            }
        }
    }

    public synchronized List<ExposureView> getExposureViewList(View mView) {
        mExposureVisible.cleanVisible();
        List<ExposureView> exposureViewList = new ArrayList<>();
        if (mView != null) {
            ExposureView exposureView = mViewWeakHashMap.get(mView);
            exposureViewList.add(exposureView);
        } else {
            Iterator<View> iterator = mViewWeakHashMap.keySet().iterator();
            while (iterator.hasNext()) {
                View view = iterator.next();
                if (view != null) {
                    ExposureView exposureView = mViewWeakHashMap.get(view);
                    SALog.i(TAG, "getExposureViewList->exposureview:" + exposureView);
                    if (viewIsExposed(exposureView)) {
                        exposureViewList.add(exposureView);
                    }
                }
            }
            mExposureVisible.cleanVisible();
            Collections.sort(exposureViewList, new Comparator<ExposureView>() {
                @Override
                public int compare(ExposureView o1, ExposureView o2) {
                    return (int) (o1.getAddTime() - o2.getAddTime());
                }
            });
        }
        return exposureViewList;
    }

    /**
     * ExposureView 是否满足曝光条件判断
     *
     * @param exposureView 待曝光的 view 信息
     * @return true 代表能曝光
     */
    private boolean viewIsExposed(ExposureView exposureView) {
        if (exposureView == null) {
            return false;
        }
        SAExposureData exposureData = exposureView.getExposureData();
        if (exposureData == null) {
            return false;
        }
        SAExposureConfig exposureConfig = exposureData.getExposureConfig();
        if (exposureConfig == null || !exposureView.isAddExposureView()) {
            return false;
        }
        boolean repeated = exposureConfig.isRepeated();
        boolean isExposed = isExposed(exposureView);
        SALog.i(TAG, "viewIsExposed:" + isExposed);
        if (repeated) {
            if (isExposed) {
                return true;
            }
        } else {
            if (isExposed) {
                if (!exposureView.isExposed() || exposureView.isActivityChange()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 偏向于 view 的可见性检查
     *
     * @param exposureView 待曝光的 view 信息
     * @return true 代表可见
     */
    private boolean isExposed(ExposureView exposureView) {
        //获取上次可见性
        boolean isLastVisible = exposureView.isLastVisible();
        //每次都需要检查可见性,需要对不可见的状态赋值
        View view = exposureView.getView();
        if (view == null) {
            return false;
        }
        Rect rect = new Rect();
        if (!mExposureVisible.isVisible(view, rect)) {
            exposureView.setLastVisible(false);
            return false;
        }
        //这里需要对上次使用上次可见性进行判断，需要上次不可见，本次可见才走到下面逻辑，中间判断可见性的时候对上次可见性状态进行过操作
        if (isLastVisible) {
            return false;
        }
        float areaRate = exposureView.getExposureData().getExposureConfig().getAreaRate();
        if (!visibleRect(view, rect, areaRate)) {
            return false;
        }
        return true;
    }

    private boolean visibleRect(View view, Rect rect, float areaRate) {
        if (view != null) {
            SALog.i(TAG, "width = " + rect.width() + ", height = " + rect.height() + ", MeasuredHeight = " + view.getMeasuredHeight() + ", MeasuredWidth = " + view.getMeasuredWidth());
            return (rect.width() * rect.height()) >= ((view.getMeasuredHeight() * view.getMeasuredWidth()) * areaRate);
        }
        return false;
    }

}
