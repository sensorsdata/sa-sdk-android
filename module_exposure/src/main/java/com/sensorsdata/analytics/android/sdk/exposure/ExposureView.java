package com.sensorsdata.analytics.android.sdk.exposure;

import android.view.View;

import com.sensorsdata.analytics.android.sdk.core.business.exposure.SAExposureData;

import java.lang.ref.WeakReference;

public class ExposureView implements Cloneable {
    private SAExposureData exposureData;//曝光数据
    private boolean lastVisible;//上次可见性
    private boolean exposed;//是否已曝光
    private final long addTime;//view 加入时间
    private boolean isAddExposureView = false;//是否调用 addExposureView 接口设置 view
    private boolean isActivityChange;
    private boolean isViewLayoutChange;
    private WeakReference<View> viewWeakReference;

    public ExposureView(SAExposureData exposureData, boolean lastVisible, boolean exposed, View view) {
        this.exposureData = exposureData;
        this.lastVisible = lastVisible;
        this.exposed = exposed;
        viewWeakReference = new WeakReference<>(view);
        addTime = System.nanoTime();
    }

    public SAExposureData getExposureData() {
        return exposureData;
    }

    public void setExposureData(SAExposureData exposureData) {
        this.exposureData = exposureData;
    }

    public boolean isLastVisible() {
        return lastVisible;
    }

    public void setLastVisible(boolean lastVisible) {
        this.lastVisible = lastVisible;
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    public void setView(View view) {
        viewWeakReference = new WeakReference<>(view);
    }

    public View getView() {
        if (viewWeakReference != null) {
            return viewWeakReference.get();
        }
        return null;
    }

    public long getAddTime() {
        return addTime;
    }

    public boolean isAddExposureView() {
        return isAddExposureView;
    }

    public void setAddExposureView(boolean addExposureView) {
        isAddExposureView = addExposureView;
    }

    public boolean isActivityChange() {
        return isActivityChange;
    }

    public void setActivityChange(boolean activityChange) {
        isActivityChange = activityChange;
    }

    public boolean isViewLayoutChange() {
        return isViewLayoutChange;
    }

    public void setViewLayoutChange(boolean viewLayoutChange) {
        isViewLayoutChange = viewLayoutChange;
    }

    @Override
    protected ExposureView clone() throws CloneNotSupportedException {
        return (ExposureView) super.clone();
    }

    @Override
    public String toString() {
        return "ExposureView{" +
                "exposureData=" + exposureData +
                ", lastVisible=" + lastVisible +
                ", exposed=" + exposed +
                ", viewWeakReference=" + viewWeakReference.get() +
                ",isAddExposureView=" + isAddExposureView +
                ",isViewLayoutChange=" + isViewLayoutChange +
                '}';
    }
}
