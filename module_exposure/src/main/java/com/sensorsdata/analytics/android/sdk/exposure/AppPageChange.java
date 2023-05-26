package com.sensorsdata.analytics.android.sdk.exposure;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewTreeObserver;

import com.sensorsdata.analytics.android.sdk.SALog;


@SuppressLint("NewApi")
public class AppPageChange implements ViewTreeObserver.OnGlobalLayoutListener, ViewTreeObserver.OnScrollChangedListener, ViewTreeObserver.OnGlobalFocusChangeListener, ViewTreeObserver.OnWindowFocusChangeListener {
    private static final String TAG = "SA.AppPageChange";
    private final ExposedTransform.LayoutCallBack mLayoutCallBack;

    public AppPageChange(ExposedTransform.LayoutCallBack layoutCallBack) {
        this.mLayoutCallBack = layoutCallBack;
    }

    @Override
    public void onGlobalFocusChanged(View oldFocus, View newFocus) {
        SALog.i(TAG, "onGlobalFocusChanged");
        mLayoutCallBack.viewLayoutChange();
    }

    @Override
    public void onGlobalLayout() {
        SALog.i(TAG, "onGlobalLayout");
        mLayoutCallBack.viewLayoutChange();
    }

    @Override
    public void onScrollChanged() {
        SALog.i(TAG, "onScrollChanged");
        mLayoutCallBack.viewLayoutChange();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        SALog.i(TAG, "onWindowFocusChanged");
        mLayoutCallBack.viewLayoutChange();
    }
}