/*
 * Created by chenru on 2022/4/25 下午5:05(format year/.
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

package com.sensorsdata.analytics.android.sdk.advert.deeplink;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.advert.oaid.SAOaidHelper;
import com.sensorsdata.analytics.android.sdk.advert.utils.ChannelUtils;
import com.sensorsdata.analytics.android.sdk.advert.SAAdvertConstants;
import com.sensorsdata.analytics.android.sdk.SAEventManager;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.ServerUrl;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.core.eventbus.SAEventBus;
import com.sensorsdata.analytics.android.sdk.core.eventbus.SAEventBusConstants;
import com.sensorsdata.analytics.android.sdk.deeplink.SADeepLinkObject;
import com.sensorsdata.analytics.android.sdk.deeplink.SensorsDataDeepLinkCallback;
import com.sensorsdata.analytics.android.sdk.deeplink.SensorsDataDeferredDeepLinkCallback;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.util.Base64Coder;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class DeepLinkManager {
    public static final String IS_ANALYTICS_DEEPLINK = "is_analytics_deeplink";
    private static DeepLinkProcessor mDeepLinkProcessor;
    private static SensorsDataDeepLinkCallback mDeepLinkCallback;
    private static SensorsDataDeferredDeepLinkCallback mDeferredDeepLinkCallback;
    private static boolean mEnableDeepLinkInstallSource = false;
    private static JSONObject mCacheProperties;
    private static boolean mIsDeepLink = false;

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
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && intent != null && Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null;
    }

    /**
     * 是否是是 UtmDeepLink
     *
     * @param intent Intent
     * @return 是否是 UtmDeepLink
     */
    private static boolean isUtmDeepLink(Intent intent) {
        if (!isDeepLink(intent)) {
            return false;
        }
        Uri uri = intent.getData();
        if (uri.isOpaque()) {
            SALog.d("ChannelDeepLink", uri + " isOpaque");
            return false;
        }
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
     * @param customADChannelUrl 广告集群接收地址
     * @param intent DeepLink 唤起的 Intent
     * @return 是否是神策 DeepLink
     */
    private static boolean isSensorsDataDeepLink(Intent intent, String serverHost, String customADChannelUrl) {
        if (!isDeepLink(intent)) {
            return false;
        }
        Uri uri = intent.getData();
        List<String> paths = uri.getPathSegments();
        if (paths != null && !paths.isEmpty()) {
            String host = uri.getHost();
            if (paths.get(0).equals("slink")) {
                return !TextUtils.isEmpty(host) && !TextUtils.isEmpty(customADChannelUrl) && (NetworkUtils.compareMainDomain(customADChannelUrl, host) || host.equals("sensorsdata"));
            } else if (paths.get(0).equals("sd") && !TextUtils.isEmpty(serverHost)) {
                return !TextUtils.isEmpty(host) && (host.equals(serverHost) || host.equals("sensorsdata"));
            }
        }
        return false;
    }

    private static DeepLinkProcessor createDeepLink(Intent intent, String serverUrl, String customADChannelUrl) {
        if (intent == null) {
            return null;
        }
        //优先判断是否是神策 DeepLink 短链
        if (isSensorsDataDeepLink(intent, new ServerUrl(serverUrl).getHost(), NetworkUtils.getHost(customADChannelUrl))) {
            return new SensorsDataDeepLink(intent, serverUrl, customADChannelUrl);
        }
        if (isUtmDeepLink(intent)) {
            return new ChannelDeepLink(intent);
        }
        return null;
    }

    private static void trackDeepLinkLaunchEvent(final Context context, DeepLinkProcessor deepLink) {
        final JSONObject properties = new JSONObject();
        final boolean isDeepLinkInstallSource = deepLink instanceof SensorsDataDeepLink && mEnableDeepLinkInstallSource;
        try {
            properties.put("$deeplink_url", deepLink.getDeepLinkUrl());
            properties.put("$time", new Date(System.currentTimeMillis()));
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
        JSONUtils.mergeJSONObject(ChannelUtils.getLatestUtmProperties(), properties);
        JSONUtils.mergeJSONObject(ChannelUtils.getUtmProperties(), properties);
        SAEventManager.getInstance().trackQueueEvent(new Runnable() {
            @Override
            public void run() {
                if (isDeepLinkInstallSource) {
                    try {
                        properties.put("$ios_install_source", ChannelUtils.getDeviceInfo(context,
                                SensorsDataUtils.getIdentifier(context), SAOaidHelper.getOpenAdIdentifier(context)));
                    } catch (JSONException e) {
                        SALog.printStackTrace(e);
                    }
                }
                SAEventManager.getInstance().trackEvent(new InputData().setEventType(EventType.TRACK)
                        .setEventName("$AppDeeplinkLaunch").setProperties(properties));
            }
        });
    }

    public interface OnDeepLinkParseFinishCallback {
        void onFinish(DeepLinkType deepLinkStatus, String pageParams, boolean success, long duration);
    }


    /**
     * 是否已经解析 Intent 获取过 Deeplink 信息
     *
     * @param activity Activity 页面
     * @return DeepLink 是否已解析
     */
    private static boolean isParsedDeepLink(Activity activity) {
        try {
            if (!SensorsDataUtils.isUniApp() || !ChannelUtils.isDeepLinkBlackList(activity)) {
                Intent intent = activity.getIntent();
                if (intent != null && intent.getData() != null) {
                    //判断 deepLink 信息是否已处理过
                    return intent.getBooleanExtra(DeepLinkManager.IS_ANALYTICS_DEEPLINK, false);
                }
            }
        } catch (Throwable ex) {
            SALog.i(SAAdvertConstants.TAG, ex.getMessage());
        }
        return false;
    }

    /**
     * 解析 deeplink 信息
     *
     * @param activity Activity
     * @param isSaveDeepLinkInfo 本地是否保存 utm 信息
     */
    public static void parseDeepLink(final Activity activity, final boolean isSaveDeepLinkInfo) {
        try {
            if (!isDeepLink(activity.getIntent()) || isParsedDeepLink(activity)) {
                return;
            }
            Intent intent = activity.getIntent();
            mDeepLinkProcessor = createDeepLink(intent, SensorsDataAPI.sharedInstance().getServerUrl(), SensorsDataAPI.getConfigOptions().getCustomADChannelUrl());
            if (mDeepLinkProcessor == null) {
                return;
            }
            // 清除本地 utm 属性
            ChannelUtils.clearUtm();
            mIsDeepLink = true;
            //清除本地 utm 属性
            // 注册 DeepLink 解析完成 callback.
            mDeepLinkProcessor.setDeepLinkParseFinishCallback(new OnDeepLinkParseFinishCallback() {
                @Override
                public void onFinish(DeepLinkType deepLinkStatus, String params, boolean success, long duration) {
                    if (isSaveDeepLinkInfo) {
                        ChannelUtils.saveDeepLinkInfo();
                    }

                    SAEventBus.getInstance().post(SAEventBusConstants.Tag.DEEPLINK_LAUNCH, ChannelUtils.getLatestUtmProperties());
                    if (deepLinkStatus == DeepLinkType.SENSORSDATA) {
                        try {
                            if (null != mDeferredDeepLinkCallback) {
                                mDeferredDeepLinkCallback.onReceive(new SADeepLinkObject(params, "", success, duration));
                            } else if (null != mDeepLinkCallback) {
                                mDeepLinkCallback.onReceive(params, success, duration);
                            }
                        } catch (Exception e) {
                            SALog.printStackTrace(e);
                        }
                    }
                }
            });
            mDeepLinkProcessor.parseDeepLink(intent);
            // 缓存 Deeplink 信息到内存中
            cacheProperties();
            //触发 $AppDeeplinkLaunch 事件
            DeepLinkManager.trackDeepLinkLaunchEvent(activity.getApplicationContext(), mDeepLinkProcessor);
            activity.getIntent().putExtra(DeepLinkManager.IS_ANALYTICS_DEEPLINK, true);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 合并渠道信息到 properties 中
     *
     * @param properties 属性
     */
    public static void mergeDeepLinkProperty(JSONObject properties) {
        try {
            if (mDeepLinkProcessor != null) {
                mDeepLinkProcessor.mergeDeepLinkProperty(properties);
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 重置 DeepLink 解析器
     */
    public static void resetDeepLinkProcessor() {
        mDeepLinkProcessor = null;
    }

    public static void setDeepLinkCallback(SensorsDataDeepLinkCallback callback) {
        mDeepLinkCallback = callback;
    }

    public static void setDeferredDeepLinkCallback(SensorsDataDeferredDeepLinkCallback callback) {
        mDeferredDeepLinkCallback = callback;
    }

    /**
     * DeepLink 是否采集设备信息
     *
     * @param enable 是否开启采集设备信息
     */
    public static void enableDeepLinkInstallSource(boolean enable) {
        mEnableDeepLinkInstallSource = enable;
    }

    /**
     * 缓存 utm 信息，用于 $AppStart 事件
     */
    private static void cacheProperties() {
        if (mCacheProperties == null) {
            mCacheProperties = new JSONObject();
        }
        mergeDeepLinkProperty(mCacheProperties);
    }

    /**
     * 合并内存中 utm 属性到 $AppStart 事件中
     *
     * @param properties $AppStart 属性
     */
    public static void mergeCacheProperties(JSONObject properties) {
        if (properties == null || mCacheProperties == null || mCacheProperties.length() == 0) {
            return;
        }
        JSONUtils.mergeJSONObject(mCacheProperties, properties);
        mCacheProperties = null;
    }

    public static void requestDeferredDeepLink(Context context, JSONObject params, String androidId, String oaid, JSONObject presetProperties, String url, boolean isSaveDeepLinkInfo) {
        if (mIsDeepLink) return;
        try {
            JSONObject jsonObject = new JSONObject();
            String ids;
            if (params != null) {
                if (params.has("$oaid")) {
                    oaid = params.optString("$oaid");
                    params.remove("$oaid");
                }
                ids = ChannelUtils.getDeviceInfo(context, androidId, oaid);
                if (params.has("$gaid")) {
                    String gaid = params.optString("$gaid");
                    ids = String.format("%s##gaid=%s", ids, gaid);
                    params.remove("$gaid");
                }
                if (params.has("$user_agent")) {
                    jsonObject.put("ua", params.optString("$user_agent"));
                    params.remove("$user_agent");
                }
                jsonObject.put("app_parameter", params.toString());
            } else {
                ids = ChannelUtils.getDeviceInfo(context, androidId, oaid);
            }
            jsonObject.put("ids", Base64Coder.encodeString(ids));
            jsonObject.put("model", presetProperties.optString("$model"));
            jsonObject.put("os", presetProperties.optString("$os"));
            jsonObject.put("os_version", presetProperties.optString("$os_version"));
            jsonObject.put("network", presetProperties.optString("$network_type"));
            jsonObject.put("app_id", presetProperties.optString("$app_id"));
            jsonObject.put("app_version", presetProperties.optString("$app_version"));
            jsonObject.put("timestamp", String.valueOf(System.currentTimeMillis()));
            jsonObject.put("project", new ServerUrl(SensorsDataAPI.sharedInstance().getServerUrl()).getProject());
            DeferredDeepLinkHelper.request(jsonObject, mDeferredDeepLinkCallback, url, isSaveDeepLinkInfo);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
