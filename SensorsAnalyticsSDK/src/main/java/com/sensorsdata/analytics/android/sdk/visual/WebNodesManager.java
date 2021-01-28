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

import android.annotation.TargetApi;
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
import java.util.HashMap;
import java.util.Iterator;
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    void handlerMessage(String message) {
        Dispatcher.getInstance().removeCallbacksAndMessages();
        if (!VisualizedAutoTrackService.getInstance().isServiceRunning() && !HeatMapService.getInstance().isServiceRunning()) {
            return;
        }
        if (TextUtils.isEmpty(message)) {
            return;
        }
        SALog.i(TAG, "handlerMessage: " + message);
        mLastWebNodeMsg = message;
        mHasH5AlertInfo = false;
        try {
            JSONObject jsonObject = new JSONObject(message);
            String callType = jsonObject.optString("callType");
            switch (callType) {
                case CALL_TYPE_VISUALIZED_TRACK:
                    List<WebNode> list = parseResult(message);
                    if (list != null && list.size() > 0) {
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
                        if (sPageInfoCache == null) {
                            sPageInfoCache = new LruCache<>(LRU_CACHE_MAX_SIZE);
                        }
                        sPageInfoCache.put(pageInfo.getUrl(), pageInfo);
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    void handlerFailure(String webViewUrl, String message) {
        Dispatcher.getInstance().removeCallbacksAndMessages();
        if (!VisualizedAutoTrackService.getInstance().isServiceRunning() && !HeatMapService.getInstance().isServiceRunning()) {
            return;
        }
        if (TextUtils.isEmpty(message)) {
            return;
        }
        SALog.i(TAG, "handlerFailure url " + webViewUrl + ",msg: " + message);
        mHasH5AlertInfo = true;
        mLastWebNodeMsg = message;
        List<WebNodeInfo.AlertInfo> list = parseAlertResult(message);
        if (list != null && list.size() > 0) {
            if (sWebNodesCache == null) {
                sWebNodesCache = new LruCache<>(LRU_CACHE_MAX_SIZE);
            }
            sWebNodesCache.put(webViewUrl, WebNodeInfo.createWebAlertInfo(list));
        }
    }

    private List<WebNode> parseResult(String msg) {
        if (TextUtils.isEmpty(msg))
            return null;
        List<WebNode> list = new ArrayList<>();
        Map<String, WebNode> hashMap = new HashMap<>();
        try {
            JSONObject jsonObject = new JSONObject(msg);
            JSONArray array = jsonObject.getJSONArray("data");
            if (array != null && array.length() > 0) {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject object = array.getJSONObject(i);
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
                    JSONArray subElementsArray = object.getJSONArray("subelements");
                    List<String> subViewIds = new ArrayList<>();
                    if (subElementsArray != null && subElementsArray.length() > 0) {
                        for (int j = 0; j < subElementsArray.length(); j++) {
                            String subElementsId = subElementsArray.optString(j);
                            if (!TextUtils.isEmpty(subElementsId)) {
                                subViewIds.add(subElementsId);
                                if (!hashMap.containsKey(subElementsId)) {
                                    hashMap.put(subElementsId, webNode);
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
            if (!hashMap.isEmpty()) {
                modifyWebNodes(list, hashMap);
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

    /**
     * 前端是根据相对坐标来定位的，但 H5 所有的坐标都是绝对坐标；当存在 subviews 属性时，需要做坐标系修正。
     * 需要考虑到 scrollY、scrollX
     *
     * @param webNodeList 原始的 web node 节点
     * @param hashMap subviews 集合
     */
    private void modifyWebNodes(List<WebNode> webNodeList, Map<String, WebNode> hashMap) {
        if (webNodeList == null || webNodeList.size() == 0) {
            return;
        }
        synchronized (this) {
            for (WebNode webNode : webNodeList) {
                Iterator<Map.Entry<String, WebNode>> iterator = hashMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, WebNode> entry = iterator.next();
                    if (webNode != null && entry != null && TextUtils.equals(webNode.getId(), entry.getKey())) {
                        webNode.setTop(webNode.getTop() - webNode.getScrollY() - entry.getValue().getTop());
                        webNode.setLeft(webNode.getLeft() - webNode.getScrollX() - entry.getValue().getLeft());
                    }
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    WebNodeInfo getWebNodes(String webViewUrl) {
        if (!VisualizedAutoTrackService.getInstance().isServiceRunning() && !HeatMapService.getInstance().isServiceRunning()) {
            return null;
        }
        if (sWebNodesCache == null) {
            sWebNodesCache = new LruCache<>(LRU_CACHE_MAX_SIZE);
        }
        return sWebNodesCache.get(webViewUrl);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    WebNodeInfo getWebPageInfo(String webViewUrl) {
        if (!VisualizedAutoTrackService.getInstance().isServiceRunning() && !HeatMapService.getInstance().isServiceRunning()) {
            return null;
        }
        if (sPageInfoCache == null) {
            sPageInfoCache = new LruCache<>(LRU_CACHE_MAX_SIZE);
        }
        return sPageInfoCache.get(webViewUrl);
    }

    String getLastWebNodeMsg() {
        return mLastWebNodeMsg;
    }

    boolean hasH5AlertInfo() {
        return mHasH5AlertInfo;
    }

    public void clear() {
        mHasH5AlertInfo = false;
        mLastWebNodeMsg = null;
    }

}
