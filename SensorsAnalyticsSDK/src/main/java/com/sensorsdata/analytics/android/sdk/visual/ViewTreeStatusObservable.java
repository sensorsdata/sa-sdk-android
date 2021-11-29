/*
 * Created by zhangxiangwei on 2021/05/25.
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

package com.sensorsdata.analytics.android.sdk.visual;


import android.app.Activity;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalFocusChangeListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.ViewTreeObserver.OnScrollChangedListener;

import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.AppStateManager;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.util.ViewUtil;
import com.sensorsdata.analytics.android.sdk.util.WindowHelper;
import com.sensorsdata.analytics.android.sdk.visual.model.ViewNode;
import com.sensorsdata.analytics.android.sdk.visual.util.Dispatcher;
import com.sensorsdata.analytics.android.sdk.visual.util.VisualUtil;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ViewTreeStatusObservable implements OnGlobalLayoutListener, OnScrollChangedListener, OnGlobalFocusChangeListener {
    private static final String TAG = "SA.ViewTreeStatusObservable";
    public static volatile ViewTreeStatusObservable viewTreeStatusObservable;
    private Runnable mTraverseRunnable = new TraverseRunnable();
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

    class TraverseRunnable implements Runnable {
        TraverseRunnable() {
        }

        public void run() {
            SALog.i(TAG, "start traverse...");
            traverseNode();
            SALog.i(TAG, "stop traverse...");
        }
    }

    public void onGlobalFocusChanged(View oldFocus, View newFocus) {
        SALog.i(TAG, "onGlobalFocusChanged");
        traverse();
    }

    public void onGlobalLayout() {
        SALog.i(TAG, "onGlobalLayout");
        traverse();
    }

    public void onScrollChanged() {
        SALog.i(TAG, "onScrollChanged");
        traverse();
    }

    public void traverse() {
        try {
            Dispatcher.getInstance().postDelayed(mTraverseRunnable, 100);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public ViewNode getViewNode(View view) {
        ViewNode viewNode = null;
        try {
            viewNode = mViewNodesWithHashCode.get(view.hashCode());
            if (viewNode == null) {
                viewNode = ViewUtil.getViewPathAndPosition(view);
                if (viewNode != null) {
                    mViewNodesWithHashCode.put(view.hashCode(), viewNode);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return viewNode;
    }

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
                    Activity activity = AppStateManager.getInstance().getForegroundActivity();
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
                Activity activity = AppStateManager.getInstance().getForegroundActivity();
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
            mViewNodesHashMap.clear();
            mViewNodesWithHashCode.clear();
            mWebViewHashMap.clear();
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
            ViewNode viewNode = ViewUtil.getViewPathAndPosition(view, true);
            if (viewNode != null) {
                // 缓存 ViewNode,用于获取 $element_path
                sparseArray.put(view.hashCode(), viewNode);
                if (!TextUtils.isEmpty(viewNode.getViewPath())) {
                    JSONObject jsonObject = VisualUtil.getScreenNameAndTitle(view, null);
                    if (jsonObject != null) {
                        String screenName = jsonObject.optString(AopConstants.SCREEN_NAME);
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
}