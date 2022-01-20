package com.sensorsdata.analytics.android.sdk.visual.snap;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.LruCache;
import android.view.View;

/**
 * 截图上报缓存信息
 * 主要功能：
 * 1、对可视化连接状态时 ViewSnapshot 进行 view 遍历过程中间产物缓存
 * 2、对可视化 ViewTreeStatusObservable 进行 view 遍历中间产物缓存
 */
public class SnapCache {

    /**
     * view 缓存的临时信息
     */
    public static class ViewTempInfo {
        public String selectPath;   //element_select 信息
        public String viewId;       //view 的 getViewId
        public String viewText;     //view 的自身文本信息
        public String viewType;     //view 的类型
        public Boolean localVisibleRect;    //view 的可见性
    }

    private volatile static SnapCache mSnapCache;
    @SuppressLint("NewApi")
    private final LruCache<String, ViewTempInfo> mLruViewInfo = new LruCache<>(64);
    @SuppressLint("NewApi")
    private final LruCache<String, String> mLruCanonicalName = new LruCache<>(64);         //获取 Class 类的名称

    private SnapCache() {

    }

    public static SnapCache getInstance() {
        if (mSnapCache == null) {
            synchronized (SnapCache.class) {
                if (mSnapCache == null) {
                    mSnapCache = new SnapCache();
                }
            }
        }
        return mSnapCache;
    }

    @SuppressLint("NewApi")
    public String getCanonicalName(Class<?> cls) {
        if (cls == null) {
            return null;
        }
        String canonicalName = mLruCanonicalName.get(cls.hashCode() + "");
        if (canonicalName == null) {
            canonicalName = cls.getCanonicalName();
            if (TextUtils.isEmpty(canonicalName)) {
                canonicalName = "Anonymous";
            }
            mLruCanonicalName.put(cls.hashCode() + "", canonicalName);
        }

        return canonicalName;
    }

    @SuppressLint("NewApi")
    public String getSelectPath(View view) {
        if (view == null) {
            return null;
        }
        String hashCode = view.hashCode() + "";
        String selectPath = null;
        ViewTempInfo viewTempInfo = mLruViewInfo.get(hashCode);
        if (viewTempInfo != null) {
            selectPath = viewTempInfo.selectPath;
        }
        return selectPath;
    }

    @SuppressLint("NewApi")
    public void setSelectPath(View view, String selectPath) {
        if (view == null || selectPath == null || selectPath.equals("")) {
            return;
        }
        String hashCode = view.hashCode() + "";
        ViewTempInfo viewTempInfo = mLruViewInfo.get(hashCode);
        if (viewTempInfo == null) {
            viewTempInfo = new ViewTempInfo();
        }
        viewTempInfo.selectPath = selectPath;
        mLruViewInfo.put(hashCode, viewTempInfo);
    }


    @SuppressLint("NewApi")
    public String getViewType(View view) {
        if (view == null) {
            return null;
        }
        String viewType = null;
        ViewTempInfo viewTempInfo = mLruViewInfo.get(view.hashCode() + "");
        if (viewTempInfo != null) {
            viewType = viewTempInfo.viewType;
        }
        return viewType;
    }

    @SuppressLint("NewApi")
    public void setViewType(View view, String viewType) {
        if (view == null || viewType == null) {
            return;
        }
        String hashCode = view.hashCode() + "";
        ViewTempInfo viewTempInfo = mLruViewInfo.get(hashCode);
        if (viewTempInfo == null) {
            viewTempInfo = new ViewTempInfo();
        }
        viewTempInfo.viewType = viewType;
        mLruViewInfo.put(hashCode, viewTempInfo);
    }


    @SuppressLint("NewApi")
    public String getViewId(View view) {
        if (view == null) {
            return null;
        }
        String viewId = null;
        ViewTempInfo viewTempInfo = mLruViewInfo.get(view.hashCode() + "");
        if (viewTempInfo != null) {
            viewId = viewTempInfo.viewId;
        }
        return viewId;
    }

    @SuppressLint("NewApi")
    public void setViewId(View view, String viewId) {
        if (view == null || viewId == null) {
            return;
        }
        String hashCode = view.hashCode() + "";
        ViewTempInfo viewTempInfo = mLruViewInfo.get(hashCode);
        if (viewTempInfo == null) {
            viewTempInfo = new ViewTempInfo();
        }
        viewTempInfo.viewId = viewId;
        mLruViewInfo.put(hashCode, viewTempInfo);
    }

    @SuppressLint("NewApi")
    public Boolean getLocalVisibleRect(View view) {
        if (view == null) {
            return null;
        }
        Boolean localVisibleRect = null;
        ViewTempInfo viewTempInfo = mLruViewInfo.get(view.hashCode() + "");
        if (viewTempInfo != null) {
            localVisibleRect = viewTempInfo.localVisibleRect;
        }
        return localVisibleRect;
    }

    @SuppressLint("NewApi")
    public void setLocalVisibleRect(View view, Boolean localVisibleRect) {
        if (view == null || localVisibleRect == null) {
            return;
        }
        String hashCode = view.hashCode() + "";
        ViewTempInfo viewTempInfo = mLruViewInfo.get(hashCode);
        if (viewTempInfo == null) {
            viewTempInfo = new ViewTempInfo();
        }
        viewTempInfo.localVisibleRect = localVisibleRect;
        mLruViewInfo.put(hashCode, viewTempInfo);
    }

    @SuppressLint("NewApi")
    public String getViewText(View view) {
        if (view == null) {
            return null;
        }
        String viewText = null;
        ViewTempInfo viewTempInfo = mLruViewInfo.get(view.hashCode() + "");
        if (viewTempInfo != null) {
            viewText = viewTempInfo.viewText;
        }
        return viewText;
    }

    @SuppressLint("NewApi")
    public void setViewText(View view, String viewText) {
        if (view == null || viewText == null) {
            return;
        }
        String hashCode = view.hashCode() + "";
        ViewTempInfo viewTempInfo = mLruViewInfo.get(hashCode);
        if (viewTempInfo == null) {
            viewTempInfo = new ViewTempInfo();
        }
        viewTempInfo.viewText = viewText;
        mLruViewInfo.put(hashCode, viewTempInfo);
    }
}
