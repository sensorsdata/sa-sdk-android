/*
 * Created by zhangxiangwei on 2019/12/31.
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

import android.os.Build;
import android.text.TextUtils;
import android.util.LruCache;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.visual.model.WebNode;
import com.sensorsdata.analytics.android.sdk.visual.model.WebNodeInfo;
import com.sensorsdata.analytics.android.sdk.visual.util.Dispatcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebNodesManager {

    private static final String TAG = "SA.Visual.WebNodesManager";
    private static final String CALL_TYPE_VISUALIZED_TRACK = "visualized_track";
    private static final String CALL_TYPE_PAGE_INFO = "page_info";
    private volatile static WebNodesManager mSingleton = null;
    private static LruCache<String, WebNodeInfo> sWebNodesCache;
    private static LruCache<String, WebNodeInfo> sPageInfoCache;

    private static final int LRU_CACHE_MAX_SIZE = 10;
    // 页面信息缓存，和页面截图合并后 Hash
    private String mLastWebNodeMsg = null;
    // 是否含有错误提示信息，有错误提示则需要一直上传页面信息
    private boolean mHasH5AlertInfo;
    // 保存最后一次的 WebView url
    private String mWebViewUrl;
    // 保存当前页面是否包含 WebView 容器
    private boolean mHasWebView;

    private WebNodesManager() {
    }

    public static WebNodesManager getInstance() {
        if (mSingleton == null) {
            synchronized (WebNodesManager.class) {
                if (mSingleton == null) {
                    mSingleton = new WebNodesManager();
                }
            }
        }
        return mSingleton;
    }

    void handlerMessage(String message) {
        Dispatcher.getInstance().removeCallbacksAndMessages();
        if (!VisualizedAutoTrackService.getInstance().isServiceRunning() && !HeatMapService.getInstance().isServiceRunning()) {
            return;
        }
        if (TextUtils.isEmpty(message)) {
            return;
        }
        mLastWebNodeMsg = String.valueOf(System.currentTimeMillis());
        mHasH5AlertInfo = false;
        try {
            JSONObject jsonObject = new JSONObject(message);
            String callType = jsonObject.optString("callType");
            switch (callType) {
                case CALL_TYPE_VISUALIZED_TRACK:
                    List<WebNode> list = parseResult(message);
                    if (list != null && list.size() > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                        if (sWebNodesCache == null) {
                            sWebNodesCache = new LruCache<>(LRU_CACHE_MAX_SIZE);
                        }
                        if (!TextUtils.isEmpty(mWebViewUrl)) {
                            sWebNodesCache.put(mWebViewUrl, WebNodeInfo.createWebNodesInfo(list));
                        }
                    }
                    break;
                case CALL_TYPE_PAGE_INFO:
                    WebNodeInfo pageInfo = parsePageInfo(message);
                    if (pageInfo != null) {
                        mWebViewUrl = pageInfo.getUrl();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                            if (sPageInfoCache == null) {
                                sPageInfoCache = new LruCache<>(LRU_CACHE_MAX_SIZE);
                            }
                            sPageInfoCache.put(pageInfo.getUrl(), pageInfo);
                        }
                    }
                    break;
                default:
                    break;
            }
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    void handlerFailure(String webViewUrl, String message) {
        try {
            Dispatcher.getInstance().removeCallbacksAndMessages();
            if (!VisualizedAutoTrackService.getInstance().isServiceRunning() && !HeatMapService.getInstance().isServiceRunning()) {
                return;
            }
            if (TextUtils.isEmpty(message)) {
                return;
            }
            SALog.i(TAG, "handlerFailure url " + webViewUrl + ",msg: " + message);
            mHasH5AlertInfo = true;
            mLastWebNodeMsg = String.valueOf(System.currentTimeMillis());
            List<WebNodeInfo.AlertInfo> list = parseAlertResult(message);
            if (list != null && list.size() > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                if (sWebNodesCache == null) {
                    sWebNodesCache = new LruCache<>(LRU_CACHE_MAX_SIZE);
                }
                sWebNodesCache.put(webViewUrl, WebNodeInfo.createWebAlertInfo(list));
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private List<WebNode> parseResult(String msg) {
        if (TextUtils.isEmpty(msg))
            return null;
        List<WebNode> list = new ArrayList<>();
        Map<String, WebNodeRect> hashMap = new HashMap<>();
        try {
            JSONObject jsonObject = new JSONObject(msg);
            JSONArray data = jsonObject.optJSONArray("data");
            JSONArray extra = jsonObject.optJSONArray("extra_elements");
            if (data != null) {
                findWebNodes(data, list, hashMap);
            }
            if (extra != null) {
                findWebNodes(extra, list, hashMap);
            }
            if (!hashMap.isEmpty()) {
                modifyWebNodes(list, hashMap);
            }
            try {
                Collections.sort(list, new Comparator<WebNode>() {
                    @Override
                    public int compare(WebNode o1, WebNode o2) {
                        return o1.getLevel() - o2.getLevel();
                    }
                });
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return list;
    }

    private WebNodeInfo parsePageInfo(String msg) {
        if (TextUtils.isEmpty(msg))
            return null;
        try {
            JSONObject jsonObject = new JSONObject(msg);
            JSONObject data = jsonObject.getJSONObject("data");
            return WebNodeInfo.createPageInfo(data.optString("$title"), data.optString("$url"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<WebNodeInfo.AlertInfo> parseAlertResult(String msg) {
        if (TextUtils.isEmpty(msg))
            return null;
        List<WebNodeInfo.AlertInfo> list = null;
        try {
            JSONObject jsonObject = new JSONObject(msg);
            JSONArray array = jsonObject.getJSONArray("data");
            if (array != null && array.length() > 0) {
                list = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject object = array.getJSONObject(i);
                    if (object != null) {
                        list.add(new WebNodeInfo.AlertInfo(
                                object.optString("title"),
                                object.optString("message"),
                                object.optString("link_text"),
                                object.optString("link_url")));
                    }
                }
            }
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return list;
    }

    static class WebNodeRect {
        public float top;
        public float left;

        public WebNodeRect(float top, float left) {
            this.top = top;
            this.left = left;
        }
    }

    /**
     * 前端是根据相对坐标来定位的，但 H5 所有的坐标都是绝对坐标；当存在 subviews 属性时，需要做坐标系修正。
     * 需要考虑到 scrollY、scrollX
     *
     * @param webNodeList 原始的 web node 节点
     * @param hashMap subviews 集合
     */
    private void modifyWebNodes(List<WebNode> webNodeList, Map<String, WebNodeRect> hashMap) {
        if (webNodeList == null || webNodeList.size() == 0) {
            return;
        }
        synchronized (this) {
            for (WebNode webNode : webNodeList) {
                webNode.setOriginLeft(webNode.getLeft());
                webNode.setOriginTop(webNode.getTop());
                if (!hashMap.containsKey(webNode.getId())) {
                    // 需要区分 WebView 顶层 H5 View 作为 rootView
                    webNode.setRootView(true);
                    webNode.setTop(webNode.getTop() + webNode.getScrollY());
                    webNode.setLeft(webNode.getLeft() + webNode.getScrollX());
                } else {
                    WebNodeRect rect = hashMap.get(webNode.getId());
                    if (rect != null) {
                        webNode.setTop(webNode.getTop() - rect.top);
                        webNode.setLeft(webNode.getLeft() - rect.left);
                    }
                }
            }
        }
    }

    private void findWebNodes(JSONArray array, List<WebNode> list, Map<String, WebNodeRect> hashMap) {
        try {
            if (array != null && array.length() > 0) {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject object = array.optJSONObject(i);
                    WebNode webNode = new WebNode();
                    webNode.setId(object.optString("id"));
                    webNode.set$element_content(object.optString("$element_content"));
                    webNode.set$element_selector(object.optString("$element_selector"));
                    webNode.setTagName(object.optString("tagName"));
                    webNode.setTop((float) object.optDouble("top"));
                    webNode.setLeft((float) object.optDouble("left"));
                    webNode.setScrollX((float) object.optDouble("scrollX"));
                    webNode.setScrollY((float) object.optDouble("scrollY"));
                    webNode.setWidth((float) object.optDouble("width"));
                    webNode.setHeight((float) object.optDouble("height"));
                    webNode.setScale((float) object.optDouble("scale"));
                    webNode.setVisibility(object.optBoolean("visibility"));
                    webNode.set$url(object.optString("$url"));
                    webNode.setzIndex(object.optInt("zIndex"));
                    webNode.set$title(object.optString("$title"));
                    webNode.setLevel(object.optInt("level"));
                    webNode.set$element_path(object.optString("$element_path"));
                    webNode.set$element_position(object.optString("$element_position"));
                    webNode.setList_selector(object.optString("list_selector"));
                    webNode.setLib_version(object.optString("lib_version"));
                    webNode.setEnable_click(object.optBoolean("enable_click", true));
                    webNode.setIs_list_view(object.optBoolean("is_list_view"));
                    JSONArray subElementsArray = object.optJSONArray("subelements");
                    List<String> subViewIds = new ArrayList<>();
                    if (subElementsArray != null && subElementsArray.length() > 0) {
                        for (int j = 0; j < subElementsArray.length(); j++) {
                            String subElementsId = subElementsArray.optString(j);
                            if (!TextUtils.isEmpty(subElementsId)) {
                                subViewIds.add(subElementsId);
                                if (!hashMap.containsKey(subElementsId)) {
                                    hashMap.put(subElementsId, new WebNodeRect(webNode.getTop(), webNode.getLeft()));
                                }
                            }
                        }
                    }
                    if (subViewIds.size() > 0) {
                        webNode.setSubelements(subViewIds);
                    }
                    list.add(webNode);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    WebNodeInfo getWebNodes(String webViewUrl) {
        if (!VisualizedAutoTrackService.getInstance().isServiceRunning() && !HeatMapService.getInstance().isServiceRunning()) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            if (sWebNodesCache == null) {
                sWebNodesCache = new LruCache<>(LRU_CACHE_MAX_SIZE);
            }
            return sWebNodesCache.get(webViewUrl);
        }
        return null;
    }

    WebNodeInfo getWebPageInfo(String webViewUrl) {
        if (!VisualizedAutoTrackService.getInstance().isServiceRunning() && !HeatMapService.getInstance().isServiceRunning()) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            if (sPageInfoCache == null) {
                sPageInfoCache = new LruCache<>(LRU_CACHE_MAX_SIZE);
            }
            return sPageInfoCache.get(webViewUrl);
        }
        return null;
    }

    // 为同时支持多 WebView 场景和 JS SDK 页面无法监听到页面变化，修改了 H5 Hash 处理逻辑，只要在 H5 页面需要始终进行上报。
    String getLastWebNodeMsg() {
        return mLastWebNodeMsg;
    }

    boolean hasH5AlertInfo() {
        return mHasH5AlertInfo;
    }

    public void clear() {
        mLastWebNodeMsg = null;
        mHasH5AlertInfo = false;
    }

    // ImageHash 相同时不会遍历 ViewTree，此时需要用单例来维护页面是否包含 WebView，优化性能。
    void setHasWebView(boolean hasWebView) {
        this.mHasWebView = hasWebView;
    }

    boolean hasWebView() {
        return mHasWebView;
    }
}
