/*
 * Created by chenru on 2020/07/06.
 * Copyright 2015－2020 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.deeplink;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.ServerUrl;
import com.sensorsdata.analytics.android.sdk.util.ChannelUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Set;

public class DeepLinkManager {
    public static final String IS_ANALYTICS_DEEPLINK = "is_analytics_deeplink";
    public static final String IS_RESUMED_ANALYTICS_DEEPLINK = "is_resumed_analytics_deeplink";

    public enum DeepLinkType {
        CHANNEL,
        SENSORSDATA
    }

    /**
     * 是否是 DeepLink 唤起
     *
     * @param intent Intent
     * @return 是否是 DeepLink 唤起
     */
    private static boolean isDeepLink(Intent intent) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && intent != null && Intent.ACTION_VIEW.equals(intent.getAction());
    }

    /**
     * 是否是是 UtmDeepLink
     *
     * @param intent Intent
     * @return 是否是 UtmDeepLink
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static boolean isUtmDeepLink(Intent intent) {
        if (!isDeepLink(intent) || intent.getData() == null) {
            return false;
        }
        Uri uri = intent.getData();
        Set<String> parameterNames = uri.getQueryParameterNames();
        if (parameterNames != null && parameterNames.size() > 0) {
            return ChannelUtils.hasLinkUtmProperties(parameterNames);
        }
        return false;
    }

    /**
     * 是否是神策 DeepLink
     *
     * @param serverHost 数据接收地址 host
     * @param intent DeepLink 唤起的 Intent
     * @return 是否是神策 DeepLink
     */
    private static boolean isSensorsDataDeepLink(Intent intent, String serverHost) {
        if (!isDeepLink(intent) || TextUtils.isEmpty(serverHost) || intent.getData() == null) {
            return false;
        }
        Uri uri = intent.getData();
        List<String> paths = uri.getPathSegments();
        if (paths != null && !paths.isEmpty()) {
            if (paths.get(0).equals("sd")) {
                String host = uri.getHost();
                return !TextUtils.isEmpty(host) && (host.equals(serverHost) || host.equals("sensorsdata"));
            }
        }
        return false;
    }

    public static DeepLinkProcessor createDeepLink(Intent intent, String serverUrl) {
        if (intent == null) {
            return null;
        }
        //优先判断是否是神策 DeepLink 短链
        if (isSensorsDataDeepLink(intent, new ServerUrl(serverUrl).getHost())) {
            return new SensorsDataDeepLink(intent, serverUrl);
        }
        if (isUtmDeepLink(intent)) {
            return new ChannelDeepLink(intent);
        }
        return null;
    }

    private static void trackDeepLinkLaunchEvent(DeepLinkProcessor deepLink) {
        JSONObject properties = new JSONObject();
        try {
            properties.put("$deeplink_url", deepLink.getDeepLinkUrl());
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
        SensorsDataUtils.mergeJSONObject(ChannelUtils.getUtmProperties(), properties);
        SensorsDataAPI.sharedInstance().track("$AppDeeplinkLaunch", properties);
    }

    public interface OnDeepLinkParseFinishCallback {
        void onFinish(DeepLinkType deepLinkStatus, String pageParams, boolean success, long duration);
    }

    public static void parseDeepLink(DeepLinkProcessor deepLink, final Activity activity, final JSONObject properties, final JSONObject endDataProperty, final boolean isSaveDeepLinkInfo, final SensorsDataDeepLinkCallback callback) {
        Intent intent = activity.getIntent();
        // 注册 DeepLink 解析完成 callback.
        deepLink.setDeepLinkParseFinishCallback(new OnDeepLinkParseFinishCallback() {
            @Override
            public void onFinish(DeepLinkType deepLinkStatus, String params, boolean success, long duration) {
                ChannelUtils.mergeUtmToEndData(ChannelUtils.getLatestUtmProperties(), endDataProperty);
                if (isSaveDeepLinkInfo) {
                    ChannelUtils.saveDeepLinkInfo(activity.getApplicationContext());
                }
                if (callback != null && deepLinkStatus == DeepLinkType.SENSORSDATA) {
                    callback.onReceive(params, success, duration);
                }
            }
        });
        deepLink.parseDeepLink(intent);
        // 合并 utm 属性到 properties 中
        deepLink.mergeDeepLinkProperty(properties);
        //触发 $AppDeeplinkLaunch 事件
        DeepLinkManager.trackDeepLinkLaunchEvent(deepLink);
    }
}
