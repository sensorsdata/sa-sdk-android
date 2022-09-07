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

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.advert.utils.ChannelUtils;
import com.sensorsdata.analytics.advert.R;
import com.sensorsdata.analytics.android.sdk.SAEventManager;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.deeplink.SADeepLinkObject;
import com.sensorsdata.analytics.android.sdk.deeplink.SensorsDataDeferredDeepLinkCallback;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.network.HttpCallback;
import com.sensorsdata.analytics.android.sdk.network.HttpMethod;
import com.sensorsdata.analytics.android.sdk.network.RequestHelper;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;
import com.sensorsdata.analytics.android.sdk.util.SADisplayUtil;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;

import org.json.JSONObject;

import java.util.Map;

public class DeferredDeepLinkHelper {
    public static void request(final JSONObject jsonData, final SensorsDataDeferredDeepLinkCallback callBack, String url, final boolean isSaveDeepLinkInfo) {
        final long requestDeepLinkStartTime = System.currentTimeMillis();
        boolean isLegalHostUrl = !TextUtils.isEmpty(url) && (url.startsWith("http://") || url.startsWith("https://"));
        final SensorsDataAPI sensorsDataAPI = SensorsDataAPI.sharedInstance();
        if (!isLegalHostUrl && (TextUtils.isEmpty(sensorsDataAPI.getServerUrl()) || !sensorsDataAPI.getServerUrl().startsWith("http"))) {
            return;
        }
        new RequestHelper.Builder(HttpMethod.POST, NetworkUtils.getRequestUrl(isLegalHostUrl ? url : sensorsDataAPI.getServerUrl(), "slink/ddeeplink"))
                .jsonData(jsonData.toString())
                .callback(new HttpCallback.JsonCallback() {
                    private String parameter;
                    private String adChannel;
                    private boolean isSuccess = false;
                    private String errorMsg;
                    private String adSlinkId;
                    private String adSlinkTemplateId;
                    private String adSlinkType;

                    @Override
                    public void onFailure(int code, String errorMessage) {
                        errorMsg = errorMessage;
                    }

                    @Override
                    public void onResponse(JSONObject response) {
                        if (response != null) {
                            int code = response.optInt("code");
                            if (code == 0) {
                                isSuccess = true;
                                JSONObject channel = response.optJSONObject("channel_params");
                                Map<String, String> params = JSONUtils.json2Map(channel);
                                ChannelUtils.parseParams(params);
                                parameter = response.optString("parameter");
                                adChannel = response.optString("ad_channel");
                                adSlinkId = response.optString("ad_slink_id");
                                adSlinkTemplateId = response.optString("slink_template_id");
                                adSlinkType = response.optString("slink_type");
                            } else {
                                errorMsg = response.optString("msg");
                            }
                        } else {
                            errorMsg = "response is null";
                        }
                    }

                    @Override
                    public void onAfter() {
                        if (isSaveDeepLinkInfo) {
                            ChannelUtils.saveDeepLinkInfo();
                        }
                        long duration = System.currentTimeMillis() - requestDeepLinkStartTime;
                        final JSONObject properties = new JSONObject();
                        try {
                            if (!TextUtils.isEmpty(parameter)) {
                                properties.put("$deeplink_options", parameter);
                            }
                            if (!TextUtils.isEmpty(errorMsg)) {
                                properties.put("$deeplink_match_fail_reason", errorMsg);
                            }
                            if (!TextUtils.isEmpty(adChannel)) {
                                properties.put("$ad_deeplink_channel_info", adChannel);
                            }
                            if (!TextUtils.isEmpty(adSlinkId)) {
                                properties.put("$ad_slink_id", adSlinkId);
                            }
                            properties.put("$ad_app_match_type", "deferred deeplink");
                            properties.put("$event_duration", TimeUtils.duration(duration));
                            properties.put("$ad_device_info", jsonData.get("ids"));
                            if (!TextUtils.isEmpty(adSlinkTemplateId)) {
                                properties.put("$ad_slink_template_id", adSlinkTemplateId);
                            }
                            if (!TextUtils.isEmpty(adSlinkType)) {
                                properties.put("$ad_slink_type", adSlinkType);
                            }
                            if (callBack != null) {
                                try {
                                    if (callBack.onReceive(new SADeepLinkObject(parameter, adChannel, isSuccess, duration)) && isSuccess) {
                                        final JSONObject jsonObject = new JSONObject();
                                        jsonObject.put("$deeplink_options", parameter);
                                        if (!TextUtils.isEmpty(adSlinkId)) {
                                            jsonObject.put("$ad_slink_id", adSlinkId);
                                        }
                                        if (!TextUtils.isEmpty(adSlinkTemplateId)) {
                                            properties.put("$ad_slink_template_id", adSlinkTemplateId);
                                        }
                                        if (!TextUtils.isEmpty(adSlinkType)) {
                                            properties.put("$ad_slink_type", adSlinkType);
                                        }
                                        JSONUtils.mergeJSONObject(ChannelUtils.getUtmProperties(), jsonObject);
                                        SAEventManager.getInstance().trackQueueEvent(new Runnable() {
                                            @Override
                                            public void run() {
                                                SAEventManager.getInstance().trackEvent(new InputData().setEventType(EventType.TRACK)
                                                        .setEventName("$AdAppDeferredDeepLinkJump").setProperties(jsonObject));
                                            }
                                        });
                                    }
                                } catch (Exception e) {
                                    SALog.printStackTrace(e);
                                }
                            } else if (isSuccess) {
                                properties.put("$deeplink_match_fail_reason", SADisplayUtil.getStringResource(sensorsDataAPI.getSAContextManager().getContext(), R.string.sensors_analytics_ad_listener));
                            }
                            JSONUtils.mergeJSONObject(ChannelUtils.getUtmProperties(), properties);
                            SAEventManager.getInstance().trackQueueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    SAEventManager.getInstance().trackEvent(new InputData().setEventType(EventType.TRACK)
                                            .setEventName("$AppDeeplinkMatchedResult").setProperties(properties));
                                }
                            });
                        } catch (Exception e) {
                            SALog.printStackTrace(e);
                        }
                    }
                }).execute();
    }
}
