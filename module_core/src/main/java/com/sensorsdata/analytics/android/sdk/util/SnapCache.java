/*
 * Created by dengshiwei on 2022/07/06.
 * Copyright 2015－2022 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sensorsdata.analytics.android.sdk.util;

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
        public String viewType;     //view 的类型
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
        return null;
    }

    @SuppressLint("NewApi")
    public void setViewId(View view, String viewId) {
    }

    @SuppressLint("NewApi")
    public Boolean getLocalVisibleRect(View view) {
        return null;
    }

    @SuppressLint("NewApi")
    public void setLocalVisibleRect(View view, Boolean localVisibleRect) {
    }

    @SuppressLint("NewApi")
    public String getViewText(View view) {
        return null;
    }

    @SuppressLint("NewApi")
    public void setViewText(View view, String viewText) {
    }
}
