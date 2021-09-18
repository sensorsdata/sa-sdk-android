/*
 * Created by zhangxiangwei on 2020/01/04.
 * Copyright 2015－2021 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.visual.model;

import android.view.View;

import java.io.Serializable;
import java.lang.ref.WeakReference;

public class ViewNode implements Serializable {
    private static final long serialVersionUID = -1242947408632673572L;
    private String viewPosition;
    private String viewOriginalPath;
    private String viewPath;
    private String viewContent;
    private String viewType;
    private boolean isListView;
    private WeakReference<View> view;

    public ViewNode(String viewContent, String viewType) {
        this(null, null, null, null, viewContent, viewType, false);
    }

    public ViewNode(View view, String viewPosition, String viewOriginalPath, String viewPath, String elementContent) {
        this(view, viewPosition, viewOriginalPath, viewPath, elementContent, null, false);
    }

    public ViewNode(View view, String viewPosition, String viewOriginalPath, String viewPath, String viewContent, String viewType, boolean isListView) {
        this.view = new WeakReference<>(view);
        this.viewPosition = viewPosition;
        this.viewOriginalPath = viewOriginalPath;
        this.viewPath = viewPath;
        this.viewContent = viewContent;
        this.viewType = viewType;
        this.isListView = isListView;
    }

    public String getViewPosition() {
        return viewPosition;
    }

    public void setViewPosition(String viewPosition) {
        this.viewPosition = viewPosition;
    }

    public String getViewOriginalPath() {
        return viewOriginalPath;
    }

    public void setViewOriginalPath(String viewOriginalPath) {
        this.viewOriginalPath = viewOriginalPath;
    }

    public String getViewPath() {
        return viewPath;
    }

    public void setViewPath(String viewPath) {
        this.viewPath = viewPath;
    }

    public String getViewContent() {
        return viewContent;
    }

    public void setViewContent(String viewContent) {
        this.viewContent = viewContent;
    }

    public String getViewType() {
        return viewType;
    }

    public void setViewType(String viewType) {
        this.viewType = viewType;
    }

    public boolean isListView() {
        return isListView;
    }

    public void setListView(boolean listView) {
        isListView = listView;
    }

    public WeakReference<View> getView() {
        return view;
    }

}
