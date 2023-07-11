package com.sensorsdata.analytics.android.sdk.exposure;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewParent;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.util.WindowHelper;

import java.util.HashMap;

public class ExposureVisible {

    private final HashMap<String, Boolean> mVisible;

    public ExposureVisible() {
        mVisible = new HashMap<>();
    }

    public void cleanVisible() {
        mVisible.clear();
    }

    public boolean isVisible(View view, Rect rect) {
        if (!isViewSelfVisible(view, rect)) {
            return false;
        }
        if (!isParentVisible(view)) {
            return false;
        }
        return view.isShown();
    }

    private boolean isViewSelfVisible(View view, Rect rect) {
        if (view == null || view.getWindowVisibility() == View.GONE) {
            SALog.i("SA.ExposureVisible", "view.getWindowVisibility() == View.GONE");
            return false;
        }

        Boolean localVisible = mVisible.get(view.hashCode() + "");
        boolean viewLocalVisible;
        if (localVisible == null) {
            viewLocalVisible = view.getLocalVisibleRect(rect);
            mVisible.put(view.hashCode() + "", viewLocalVisible);
        } else {
            viewLocalVisible = localVisible;
        }

        if (WindowHelper.isDecorView(view.getClass())) {
            return true;
        }
        if (view.getWidth() <= 0 || view.getHeight() <= 0 || view.getAlpha() <= 0.0f || !viewLocalVisible) {
            SALog.i("SA.ExposureVisible", "isViewSelfVisible，width = " + view.getWidth() + ",height = " + view.getHeight() + "，alpha = " + view.getAlpha());
            return false;
        }
        return (view.getAnimation() != null && view.getAnimation().getFillAfter()) || view.getVisibility() == View.VISIBLE;
    }

    private boolean isParentVisible(View view) {
        if (view == null) {
            return false;
        }
        ViewParent viewParent = view.getParent();
        do {
            if (!(viewParent instanceof View)) {
                return true;
            }
            if (!isViewSelfVisible((View) viewParent, new Rect())) {
                return false;
            }
            viewParent = viewParent.getParent();
        } while (viewParent != null);

        return false;
    }
}
