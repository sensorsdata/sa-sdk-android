package com.sensorsdata.analytics.android.sdk.visual;

import android.app.Activity;
import android.text.TextUtils;
import android.util.LruCache;

import com.sensorsdata.analytics.android.sdk.util.AppStateTools;
import com.sensorsdata.analytics.android.sdk.util.SnapCache;
import com.sensorsdata.analytics.android.sdk.visual.model.FlutterNode;
import com.sensorsdata.analytics.android.sdk.visual.model.FlutterNodeInfo;
import com.sensorsdata.analytics.android.sdk.visual.model.NodeInfo;
import com.sensorsdata.analytics.android.sdk.visual.model.CommonNode;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class FlutterNodesManager extends AbstractNodesManager {

    @Override
    protected void handlerVisualizedTrack(List<? extends CommonNode> nodes) {
        Activity activity = AppStateTools.getInstance().getForegroundActivity();
        String activityName = "";
        if (activity != null) {
            activityName = activity.getClass().getCanonicalName();
        }
        if (!TextUtils.isEmpty(activityName)) {
            sNodesCache.put(activityName, FlutterNodeInfo.createNodesInfo(nodes));
        }
    }

    @Override
    protected void handlerVisualizedPageInfo(String msg) {
        FlutterNodeInfo pageInfo = parsePageInfo(msg);
        if (sPageInfoCache == null) {
            sPageInfoCache = new LruCache<>(LRU_CACHE_MAX_SIZE);
        }
        Activity activity = AppStateTools.getInstance().getForegroundActivity();
        if (activity == null) {
            return;
        }
        String mActivityName = SnapCache.getInstance().getCanonicalName(activity.getClass());
        sPageInfoCache.put(mActivityName, pageInfo);
    }

    @Override
    protected void handlerVisualizedFailure(String url, List<NodeInfo.AlertInfo> list) {
        sNodesCache.put(url, FlutterNodeInfo.createAlertInfo(list));
    }

    @Override
    protected CommonNode parseExtraNodesInfo(JSONObject object) {
        FlutterNode flutterNode = new FlutterNode();
        flutterNode.setTitle(object.optString("title"));
        flutterNode.setScreen_name(object.optString("screen_name"));
        flutterNode.setVisibility(true);
        return flutterNode;
    }

    protected FlutterNodeInfo parsePageInfo(String msg) {
        if (TextUtils.isEmpty(msg))
            return null;
        try {
            JSONObject jsonObject = new JSONObject(msg);
            JSONObject data = jsonObject.getJSONObject("data");
            return FlutterNodeInfo.createPageInfo(data.optString("title"), data.optString("screen_name"), data.optString("lib_version"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
