/*
 * Created by dengshiwei on 2022/07/04.
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

package com.sensorsdata.analytics.android.sdk.util.visual;


import android.app.Activity;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.util.AppStateTools;
import com.sensorsdata.analytics.android.sdk.util.WindowHelper;
import com.sensorsdata.analytics.android.sdk.util.SAViewUtils;
import com.sensorsdata.analytics.android.sdk.util.WindowHelper;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ViewTreeStatusObservable {
    private static final String TAG = "SA.ViewTreeStatusObservable";
    public static volatile ViewTreeStatusObservable viewTreeStatusObservable;
    private SparseArray<ViewNode> mViewNodesWithHashCode = new SparseArray<>();
    private HashMap<String, ViewNode> mViewNodesHashMap = new HashMap<>();
    private HashMap<String, ViewNode> mWebViewHashMap = new HashMap<>();

    public static ViewTreeStatusObservable getInstance() {
        if (viewTreeStatusObservable == null) {
            synchronized (ViewTreeStatusObservable.class) {
                if (viewTreeStatusObservable == null) {
                    viewTreeStatusObservable = new ViewTreeStatusObservable();
                }
            }
        }
        return viewTreeStatusObservable;
    }

    public ViewNode getViewNode(View view) {
        ViewNode viewNode = null;
        try {
            viewNode = getViewPathAndPosition(view);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return viewNode;
    }

    /**
     * 自定义属性之前清空缓存信息
     */
    public void clearViewNodeCache() {
        mViewNodesWithHashCode.clear();
        mViewNodesHashMap.clear();
    }

    /**
     * 自定义属性获取属性对应数据
     *
     * @param reference 当前 view
     * @param elementPath 自定义属性 element_path
     * @param elementPosition 自定义属性位置
     * @param screenName 自定义属性页面
     * @return 自定义属性 ViewNode
     */
    public ViewNode getViewNode(WeakReference<View> reference, String elementPath, String elementPosition, String screenName) {
        ViewNode viewNode = null;
        try {
            viewNode = mViewNodesHashMap.get(generateKey(elementPath, elementPosition, screenName));
            // ViewTree 中不存在，需要主动遍历
            if (viewNode == null) {
                View rootView = null;
                if (reference != null && reference.get() != null) {
                    rootView = reference.get().getRootView();
                }
                if (rootView == null) {
                    Activity activity = AppStateTools.getInstance().getForegroundActivity();
                    if (activity != null && activity.getWindow() != null && activity.getWindow().isActive()) {
                        rootView = activity.getWindow().getDecorView();
                    }
                }
                if (rootView != null) {
                    traverseNode(rootView);
                }
                viewNode = mViewNodesHashMap.get(generateKey(elementPath, elementPosition, screenName));
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return viewNode;
    }

    /**
     * 通过 elementPath 获取目标 View
     *
     * @param elementPath view 的元素路径
     * @return 目标 View
     */
    public ViewNode getViewNode(String elementPath) {
        ViewNode viewNode = null;
        try {
            viewNode = mWebViewHashMap.get(elementPath);
            // ViewTree 中不存在，需要主动遍历
            if (viewNode == null || viewNode.getView() == null || viewNode.getView().get() == null) {
                View rootView = null;
                Activity activity = AppStateTools.getInstance().getForegroundActivity();
                if (activity != null && activity.getWindow() != null && activity.getWindow().isActive()) {
                    rootView = activity.getWindow().getDecorView();
                }
                if (rootView != null) {
                    traverseNode(rootView);
                }
                viewNode = mWebViewHashMap.get(elementPath);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return viewNode;
    }

    /**
     * WebView 缓存需要在页面销毁时候进行释放，优化性能。
     */
    public void clearWebViewCache() {
        try {
            if (mWebViewHashMap != null) {
                mWebViewHashMap.clear();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private void traverseNode() {
        traverseNode(null);
    }

    private void traverseNode(View rootView) {
        try {
            mViewNodesHashMap.clear();
            mViewNodesWithHashCode.clear();
            mWebViewHashMap.clear();
            SparseArray<ViewNode> tempSparseArray = new SparseArray<>();
            HashMap<String, ViewNode> tempHashMap = new HashMap<>();
            HashMap<String, ViewNode> tempWebViewHashMap = new HashMap<>();
            // 主动遍历
            if (rootView != null) {
                traverseNode(rootView, tempSparseArray, tempHashMap, tempWebViewHashMap);
            } else {
                // 被动缓存
                final View[] views = WindowHelper.getSortedWindowViews();
                for (View view : views) {
                    traverseNode(view, tempSparseArray, tempHashMap, tempWebViewHashMap);
                }
            }
            mViewNodesHashMap = tempHashMap;
            mViewNodesWithHashCode = tempSparseArray;
            mWebViewHashMap = tempWebViewHashMap;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public List<View> getCurrentWebView() {
        try {
            if (mWebViewHashMap.size() == 0) {
                traverseNode();
            }
            if (mWebViewHashMap.size() > 0) {
                List<View> list = new ArrayList<>();
                for (ViewNode viewNode : mWebViewHashMap.values()) {
                    WeakReference<View> reference = viewNode.getView();
                    if (reference != null && reference.get() != null) {
                        list.add(reference.get());
                    }
                }
                return list;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    private String generateKey(String elementPath, String elementPosition, String screenName) {
        StringBuilder key = new StringBuilder();
        key.append(elementPath);
        if (!TextUtils.isEmpty(elementPosition)) {
            key.append(elementPosition);
        }
        if (!TextUtils.isEmpty(screenName)) {
            key.append(screenName);
        }
        return key.toString();
    }

    private void traverseNode(final View view, final SparseArray<ViewNode> sparseArray, final HashMap<String, ViewNode> hashMap, final HashMap<String, ViewNode> webViewHashMap) {
        try {
            if (view == null) {
                return;
            }
            ViewNode viewNode = getCacheViewPathAndPosition(view, true);
            if (viewNode != null) {
                // 缓存 ViewNode,用于获取 $element_path
                sparseArray.put(view.hashCode(), viewNode);
                if (!TextUtils.isEmpty(viewNode.getViewPath())) {
                    JSONObject jsonObject = SAViewUtils.getScreenNameAndTitle(view);
                    if (jsonObject != null) {
                        String screenName = jsonObject.optString("$screen_name");
                        if (!TextUtils.isEmpty(screenName)) {
                            if (!TextUtils.isEmpty(viewNode.getViewContent())) {
                                hashMap.put(generateKey(viewNode.getViewPath(), viewNode.getViewPosition(), screenName), viewNode);
                            }
                            if (ViewUtil.instanceOfWebView(view)) {
                                webViewHashMap.put(viewNode.getViewPath() + screenName, viewNode);
                            }
                        }
                    }
                }
            }
            if (view instanceof ViewGroup) {
                final ViewGroup group = (ViewGroup) view;
                final int childCount = group.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    final View child = group.getChildAt(i);
                    if (child != null) {
                        traverseNode(child, sparseArray, hashMap, webViewHashMap);
                    }
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }


    /**
     * 获取 view 控件的 ViewNode 信息
     *
     * @param clickView 需要获取 ViewNode 信息的 View 对象
     * @return 返回 View 对象的 ViewNode 信息
     */
    public ViewNode getViewPathAndPosition(View clickView) {
        return getSingleViewPathAndPosition(clickView, false);
    }

    /**
     * 对 view 进行遍历的过程中进行父控件的缓存读取
     *
     * @param clickView 需要获取 ViewNode 信息的 View 对象
     * @param fromVisual 是否主动获取
     * @return 当前 clickView 的 ViewNode 节点信息
     */
    private ViewNode getCacheViewPathAndPosition(View clickView, boolean fromVisual) {
        ViewNode currentNode = mViewNodesWithHashCode.get(clickView.hashCode());
        if (currentNode != null) {
            return currentNode;
        }
        ViewParent viewParent;
        View parent_view = null;
        viewParent = clickView.getParent();
        if (viewParent instanceof ViewGroup) {
            parent_view = (View) viewParent;
        }
        if (parent_view == null) {
            currentNode = ViewUtil.getViewPathAndPosition(clickView, fromVisual);
        } else {
            StringBuilder opx = new StringBuilder();
            StringBuilder px = new StringBuilder();
            ViewNode parentNode = mViewNodesWithHashCode.get(parent_view.hashCode());
            if (parentNode == null) {
                parentNode = ViewUtil.getViewPathAndPosition(parent_view, fromVisual);
                mViewNodesWithHashCode.put(parent_view.hashCode(), parentNode);
            }
            String listPosition = "";
            if (parentNode != null) {
                opx.append(parentNode.getViewOriginalPath());
                px.append(parentNode.getViewPath());
                listPosition = parentNode.getViewPosition();
            }

            final int viewPosition = ((ViewGroup) parent_view).indexOfChild(clickView);
            currentNode = ViewUtil.getViewNode(clickView, viewPosition, fromVisual);
            // 这个地方由于 viewPosition 当前控件是列表的子控件的时候，表示当前控件位于父控件的位置；当前控件是非列表的子控件的时候，表示上一个列表的位置。因此通过上一个 View 的 listPosition 进行替换[-]没有什么大的问题
            if (currentNode != null && !TextUtils.isEmpty(currentNode.getViewPath()) && currentNode.getViewPath().contains("-") && !TextUtils.isEmpty(listPosition)) {
                int replacePosition = px.lastIndexOf("-");
                if (replacePosition != -1) {
                    px.replace(replacePosition, replacePosition + 1, String.valueOf(listPosition));
                }
            }
            if (currentNode != null) {
                opx.append(currentNode.getViewOriginalPath());
                px.append(currentNode.getViewPath());
                currentNode = new ViewNode(clickView, currentNode.getViewPosition(), opx.toString(), px.toString(), currentNode.getViewContent());
            }
        }
        mViewNodesWithHashCode.put(clickView.hashCode(), currentNode);
        return currentNode;
    }

    /**
     * 不走缓存获取单个 view 的具体信息
     *
     * @param clickView 当前 view
     * @param fromVisual 是否来着可视化
     * @return view 的 ViewNode 结构
     */
    private ViewNode getSingleViewPathAndPosition(View clickView, boolean fromVisual) {
        ViewNode currentNode;
        ViewParent viewParent;
        View parent_view = null;
        viewParent = clickView.getParent();
        if (viewParent instanceof ViewGroup) {
            parent_view = (View) viewParent;
        }
        if (parent_view == null) {
            currentNode = ViewUtil.getViewPathAndPosition(clickView, fromVisual);
        } else {
            StringBuilder opx = new StringBuilder();
            StringBuilder px = new StringBuilder();
            ViewNode parentNode = ViewUtil.getViewPathAndPosition(parent_view, fromVisual);
            String listPosition = "";
            if (parentNode != null) {
                opx.append(parentNode.getViewOriginalPath());
                px.append(parentNode.getViewPath());
                listPosition = parentNode.getViewPosition();
            }
            final int viewPosition = ((ViewGroup) parent_view).indexOfChild(clickView);
            currentNode = ViewUtil.getViewNode(clickView, viewPosition, fromVisual);
            if (currentNode != null) {
                // 这个地方由于 viewPosition 当前控件是列表的子控件的时候，表示当前控件位于父控件的位置；当前控件是非列表的子控件的时候，表示上一个列表的位置。因此通过上一个 View 的 listPosition 进行替换[-]没有什么大的问题
                if (!TextUtils.isEmpty(currentNode.getViewPath()) && currentNode.getViewPath().contains("-") && !TextUtils.isEmpty(listPosition)) {
                    int replacePosition = px.lastIndexOf("-");
                    if (replacePosition != -1) {
                        px.replace(replacePosition, replacePosition + 1, String.valueOf(listPosition));
                    }
                }
                opx.append(currentNode.getViewOriginalPath());
                px.append(currentNode.getViewPath());
                currentNode = new ViewNode(clickView, currentNode.getViewPosition(), opx.toString(), px.toString(), currentNode.getViewContent());
            }
        }
        return currentNode;
    }
}