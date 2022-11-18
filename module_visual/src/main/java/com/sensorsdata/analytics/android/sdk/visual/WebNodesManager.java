/*
 * Created by zhangxiangwei on 2019/12/31.
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

package com.sensorsdata.analytics.android.sdk.visual;

import android.text.TextUtils;
import android.util.LruCache;

import com.sensorsdata.analytics.android.sdk.visual.model.NodeInfo;
import com.sensorsdata.analytics.android.sdk.visual.model.CommonNode;
import com.sensorsdata.analytics.android.sdk.visual.model.WebNode;
import com.sensorsdata.analytics.android.sdk.visual.model.WebNodeInfo;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class WebNodesManager extends AbstractNodesManager {

    private static final String TAG = "SA.Visual.WebNodesManager";
    // 保存最后一次的 WebView url
    private String mWebViewUrl;


    @Override
    protected void handlerVisualizedTrack(List<? extends CommonNode> nodes) {
        if (!TextUtils.isEmpty(mWebViewUrl)) {
            sNodesCache.put(mWebViewUrl, WebNodeInfo.createNodesInfo(nodes));
        }
    }

    @Override
    protected void handlerVisualizedPageInfo(String msg) {
        WebNodeInfo pageInfo = parsePageInfo(msg);
        if (pageInfo == null) {
            return;
        }
        mWebViewUrl = pageInfo.getUrl();
        if (sPageInfoCache == null) {
            sPageInfoCache = new LruCache<>(LRU_CACHE_MAX_SIZE);
        }
        String url = pageInfo.getUrl();
        if (!TextUtils.isEmpty(url)) {
            sPageInfoCache.put(url, pageInfo);
        }
    }

    @Override
    protected void handlerVisualizedFailure(String url, List<NodeInfo.AlertInfo> list) {
        sNodesCache.put(url, WebNodeInfo.createAlertInfo(list));
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

    @Override
    protected CommonNode parseExtraNodesInfo(JSONObject object) {
        WebNode webNode = new WebNode();
        webNode.set$element_selector(object.optString("$element_selector"));
        webNode.setTagName(object.optString("tagName"));
        webNode.set$url(object.optString("$url"));
        webNode.setzIndex(object.optInt("zIndex"));
        webNode.set$title(object.optString("$title"));
        webNode.setList_selector(object.optString("list_selector"));
        webNode.setScale((float) object.optDouble("scale"));
        webNode.setVisibility(object.optBoolean("visibility"));
        webNode.setLib_version(object.optString("lib_version"));
        return webNode;
    }



}
