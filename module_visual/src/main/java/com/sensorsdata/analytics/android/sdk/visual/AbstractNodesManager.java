package com.sensorsdata.analytics.android.sdk.visual;

import android.text.TextUtils;
import android.util.LruCache;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.util.Dispatcher;
import com.sensorsdata.analytics.android.sdk.visual.model.NodeInfo;
import com.sensorsdata.analytics.android.sdk.visual.model.CommonNode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractNodesManager {

    private static final String TAG = "SA.Visual.AbstractNodesManager";
    protected static final String CALL_TYPE_VISUALIZED_TRACK = "visualized_track";
    protected static final String CALL_TYPE_PAGE_INFO = "page_info";
    protected static LruCache<String, NodeInfo> sNodesCache;
    protected static LruCache<String, NodeInfo> sPageInfoCache;
    protected static final int LRU_CACHE_MAX_SIZE = 10;
    // 页面信息缓存，和页面截图合并后 Hash
    protected String mLastThirdNodeMsg = null;
    // 保存当前页面是否包含 WebView/Flutter 容器
    private boolean mHasWebView;
    // 是否含有错误提示信息，有错误提示则需要一直上传页面信息
    protected boolean mHasAlertInfo;

    /**
     * 元素信息处理
     *
     * @param nodes 元素节点信息
     */
    protected abstract void handlerVisualizedTrack(List<? extends CommonNode> nodes);

    /**
     * 页面信息处理
     *
     * @param msg 页面节点信息
     */
    protected abstract void handlerVisualizedPageInfo(String msg);

    /**
     * 可视化处理失败 Alert 信息处理
     *
     * @param unique Alert 信息的标识符
     * @param list Alert 具体信息
     */
    protected abstract void handlerVisualizedFailure(String unique, List<NodeInfo.AlertInfo> list);


    /**
     * 解析元素信息，针对不同平台的差异化处理
     *
     * @param object 带解析的平台差异信息
     * @return 元素信息
     */
    protected abstract CommonNode parseExtraNodesInfo(JSONObject object);

    public void handlerMessage(String message) {
        Dispatcher.getInstance().removeCallbacksAndMessages();
        if (!VisualizedAutoTrackService.getInstance().isServiceRunning() && !HeatMapService.getInstance().isServiceRunning()) {
            return;
        }
        if (TextUtils.isEmpty(message)) {
            return;
        }
        mLastThirdNodeMsg = String.valueOf(System.currentTimeMillis());
        mHasAlertInfo = false;
        try {
            JSONObject jsonObject = new JSONObject(message);
            String callType = jsonObject.optString("callType");
            switch (callType) {
                case CALL_TYPE_VISUALIZED_TRACK:
                    List<? extends CommonNode> list = parseResult(message);
                    if (list != null && list.size() > 0) {
                        if (sNodesCache == null) {
                            sNodesCache = new LruCache<>(LRU_CACHE_MAX_SIZE);
                        }
                        handlerVisualizedTrack(list);
                    }
                    break;
                case CALL_TYPE_PAGE_INFO:
                    handlerVisualizedPageInfo(message);
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

    public void handlerFailure(String webViewUrl, String message) {
        try {
            Dispatcher.getInstance().removeCallbacksAndMessages();
            if (!VisualizedAutoTrackService.getInstance().isServiceRunning() && !HeatMapService.getInstance().isServiceRunning()) {
                return;
            }
            if (TextUtils.isEmpty(message)) {
                return;
            }
            SALog.i(TAG, "handlerFailure url " + webViewUrl + ",msg: " + message);
            mHasAlertInfo = true;
            mLastThirdNodeMsg = String.valueOf(System.currentTimeMillis());
            List<NodeInfo.AlertInfo> list = parseAlertResult(message);
            if (list != null && list.size() > 0) {
                if (sNodesCache == null) {
                    sNodesCache = new LruCache<>(LRU_CACHE_MAX_SIZE);
                }
                handlerVisualizedFailure(webViewUrl, list);

            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public NodeInfo getNodes(String webViewUrl) {
        if (!VisualizedAutoTrackService.getInstance().isServiceRunning() && !HeatMapService.getInstance().isServiceRunning()) {
            return null;
        }
        if (sNodesCache == null) {
            sNodesCache = new LruCache<>(LRU_CACHE_MAX_SIZE);
        }
        return sNodesCache.get(webViewUrl);
    }

    public NodeInfo getPageInfo(String webViewUrl) {
        if (!VisualizedAutoTrackService.getInstance().isServiceRunning() && !HeatMapService.getInstance().isServiceRunning()) {
            return null;
        }
        if (sPageInfoCache == null) {
            sPageInfoCache = new LruCache<>(LRU_CACHE_MAX_SIZE);
        }
        return sPageInfoCache.get(webViewUrl);
    }

    void clear() {
        mLastThirdNodeMsg = null;
        mHasAlertInfo = false;
    }

    boolean hasAlertInfo() {
        return mHasAlertInfo;
    }

    private List<CommonNode> parseResult(String msg) {
        if (TextUtils.isEmpty(msg))
            return null;
        List<CommonNode> list = new ArrayList<>();
        Map<String, NodeRect> hashMap = new HashMap<>();
        try {
            JSONObject jsonObject = new JSONObject(msg);
            JSONArray data = jsonObject.optJSONArray("data");
            JSONArray extra = null;
            try {
                extra = jsonObject.optJSONArray("extra_elements");
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
            if (data != null) {
                findWebNodes(data, list, hashMap);
            }
            if (extra != null) {
                findWebNodes(extra, list, hashMap);
            }
            modifyWebNodes(list, hashMap);
            try {
                Collections.sort(list, new Comparator<CommonNode>() {
                    @Override
                    public int compare(CommonNode o1, CommonNode o2) {
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

    private List<NodeInfo.AlertInfo> parseAlertResult(String msg) {
        if (TextUtils.isEmpty(msg))
            return null;
        List<NodeInfo.AlertInfo> list = null;
        try {
            JSONObject jsonObject = new JSONObject(msg);
            JSONArray array = jsonObject.optJSONArray("data");
            if (array != null && array.length() > 0) {
                list = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject object = array.getJSONObject(i);
                    if (object != null) {
                        list.add(new NodeInfo.AlertInfo(
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


    private void findWebNodes(JSONArray array, List<CommonNode> list, Map<String, NodeRect> hashMap) {
        try {
            if (array != null && array.length() > 0) {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject object = array.optJSONObject(i);

                    CommonNode webNode = parseExtraNodesInfo(object);
                    webNode.setId(object.optString("id"));
                    webNode.set$element_content(object.optString("$element_content"));
                    webNode.setTop((float) object.optDouble("top"));
                    webNode.setLeft((float) object.optDouble("left"));
                    webNode.setScrollX((float) object.optDouble("scrollX"));
                    webNode.setScrollY((float) object.optDouble("scrollY"));
                    webNode.setWidth((float) object.optDouble("width"));
                    webNode.setHeight((float) object.optDouble("height"));
                    webNode.setLevel(object.optInt("level"));
                    webNode.set$element_path(object.optString("$element_path"));
                    webNode.set$element_position(object.optString("$element_position"));
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
                                    hashMap.put(subElementsId, new NodeRect(webNode.getTop(), webNode.getLeft()));
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

    // 为同时支持多 WebView 场景和 JS SDK 页面无法监听到页面变化，修改了 H5 Hash 处理逻辑，只要在 H5 页面需要始终进行上报。
    String getLastThirdMsg() {
        return mLastThirdNodeMsg;
    }

    // ImageHash 相同时不会遍历 ViewTree，此时需要用单例来维护页面是否包含 WebView，优化性能。
    void setHasThirdView(boolean hasWebView) {
        this.mHasWebView = hasWebView;
    }

    boolean hasThirdView() {
        return mHasWebView;
    }

    /**
     * 前端是根据相对坐标来定位的，但 H5 所有的坐标都是绝对坐标；当存在 subviews 属性时，需要做坐标系修正。
     * 需要考虑到 scrollY、scrollX
     *
     * @param webNodeList 原始的 web node 节点
     * @param hashMap subviews 集合
     */
    private void modifyWebNodes(List<? extends CommonNode> webNodeList, Map<String, NodeRect> hashMap) {
        if (webNodeList == null || webNodeList.size() == 0) {
            return;
        }
        synchronized (this) {
            for (CommonNode webNode : webNodeList) {
                webNode.setOriginLeft(webNode.getLeft());
                webNode.setOriginTop(webNode.getTop());
                if (!hashMap.containsKey(webNode.getId())) {
                    // 需要区分 WebView 顶层 H5 View 作为 rootView
                    webNode.setRootView(true);
                    float scrollY = 0f;
                    float scrollX = 0f;
                    if (!Float.isNaN(webNode.getScrollY())) {
                        scrollY = webNode.getScrollY();
                    }
                    if (!Float.isNaN(webNode.getScrollX())) {
                        scrollX = webNode.getScrollX();
                    }
                    webNode.setTop(webNode.getTop() + scrollY);
                    webNode.setLeft(webNode.getLeft() + scrollX);
                } else {
                    NodeRect rect = hashMap.get(webNode.getId());
                    if (rect != null) {
                        webNode.setTop(webNode.getTop() - rect.top);
                        webNode.setLeft(webNode.getLeft() - rect.left);
                    }
                }
            }
        }
    }

    static class NodeRect {
        public float top;
        public float left;

        public NodeRect(float top, float left) {
            this.top = top;
            this.left = left;
        }
    }
}
