/*
 * Created by chenru on 2022/7/20 下午4:06.
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

package com.sensorsdata.analytics.android.sdk.advert;

import android.content.Context;
import android.text.TextUtils;

import com.sensorsdata.analytics.advert.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.ServerUrl;
import com.sensorsdata.analytics.android.sdk.advert.model.SASlinkResponse;
import com.sensorsdata.analytics.android.sdk.advert.model.SATLandingPageType;
import com.sensorsdata.analytics.android.sdk.advert.monitor.SensorsDataCreateSLinkCallback;
import com.sensorsdata.analytics.android.sdk.network.HttpCallback;
import com.sensorsdata.analytics.android.sdk.network.HttpMethod;
import com.sensorsdata.analytics.android.sdk.network.RequestHelper;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;
import com.sensorsdata.analytics.android.sdk.util.SADisplayUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SASlinkCreator {
    private String mTemplateID = "";
    private String mName = "";
    private String mToken = "";
    private String mChannelType = "";
    private String mChannelName = "";
    private String mUriSchemeSuffix = "";
    private String mRedirectURLOnOtherDevice = "";
    private String mCommonRedirectURI = "";
    private String mUtmSource = "";
    private String mUtmCampaign = "";
    private String mUtmMedium = "";
    private String mUtmTerm = "";
    private String mUtmContent = "";
    private SATLandingPageType mLandingPageType;
    private JSONObject mCustomProperties = new JSONObject();
    private String mRouteParam;
    private Map<String, String> mLandingPage = new HashMap<>();

    /**
     * Build share link class
     *
     * @param templateID templateID
     * @param channelName channelName
     * @param commonRedirectURI commonRedirectURI
     * @param accessToken token
     */
    public SASlinkCreator(String templateID, String channelName, String commonRedirectURI, String accessToken) {
        this.mTemplateID = templateID;
        this.mChannelName = channelName;
        this.mCommonRedirectURI = commonRedirectURI;
        this.mToken = accessToken;
    }

    public SASlinkCreator setCustomParams(JSONObject properties) {
        mCustomProperties = properties;
        return this;
    }

    /**
     * Set deep routing param
     *
     * @param routeParam routing param
     * @return SALinkCreator
     */
    public SASlinkCreator setRouteParam(String routeParam) {
        mRouteParam = routeParam;
        return this;
    }

    public String getRouteParam() {
        return mRouteParam;
    }

    public String getName() {
        return mName;
    }

    /**
     * set name
     *
     * @param name name
     * @return SALinkCreator
     */
    public SASlinkCreator setName(String name) {
        this.mName = name;
        return this;
    }

    private String getChannelType() {
        return mChannelType;
    }

    /**
     * set Channel Type
     *
     * @param channelType channel type
     * @return SALinkCreator
     */
    private SASlinkCreator setChannelType(String channelType) {
        this.mChannelType = channelType;
        return this;
    }

    public String getUriSchemeSuffix() {
        return mUriSchemeSuffix;
    }

    /**
     * set scheme suffix
     *
     * @param uriSchemeSuffix uriSchemeSuffix
     * @return SALinkCreator
     */
    public SASlinkCreator setUriSchemeSuffix(String uriSchemeSuffix) {
        this.mUriSchemeSuffix = uriSchemeSuffix;
        return this;
    }

    public String getRedirectURLOnOtherDevice() {
        return mRedirectURLOnOtherDevice;
    }

    /**
     * Set the landing page to jump when the non-mobile device is opened
     *
     * @param redirectURLOnOtherDevice landing page
     * @return SALinkCreator
     */
    public SASlinkCreator setRedirectURLOnOtherDevice(String redirectURLOnOtherDevice) {
        this.mRedirectURLOnOtherDevice = redirectURLOnOtherDevice;
        return this;
    }

    public SATLandingPageType getLandingPageType() {
        return mLandingPageType;
    }

    /**
     * Set landing page type
     *
     * @param landingPageType INTELLIGENCE:Smart landing page，OTHER：custom landing page
     * @return SALinkCreator
     */
    public SASlinkCreator setLandingPageType(SATLandingPageType landingPageType) {
        this.mLandingPageType = landingPageType;
        return this;
    }

    public JSONObject getCustomProperties() {
        return mCustomProperties;
    }

    /**
     * set custom params
     *
     * @param customProperties custom params
     * @return SALinkCreator
     */
    public SASlinkCreator setCustomProperties(JSONObject customProperties) {
        this.mCustomProperties = customProperties;
        return this;
    }

    public String getUtmSource() {
        return mUtmSource;
    }

    /**
     * set utmSource
     *
     * @param utmSource utmSource
     * @return SALinkCreator
     */
    public SASlinkCreator setUtmSource(String utmSource) {
        this.mUtmSource = utmSource;
        return this;
    }

    public String getUtmCampaign() {
        return mUtmCampaign;
    }

    /**
     * set utmCampaign
     *
     * @param utmCampaign utmCampaign
     * @return SALinkCreator
     */
    public SASlinkCreator setUtmCampaign(String utmCampaign) {
        this.mUtmCampaign = utmCampaign;
        return this;
    }

    public String getUtmMedium() {
        return mUtmMedium;
    }

    /**
     * set utmMedium
     *
     * @param utmMedium utmMedium
     * @return SALinkCreator
     */
    public SASlinkCreator setUtmMedium(String utmMedium) {
        this.mUtmMedium = utmMedium;
        return this;
    }

    public String getUtmTerm() {
        return mUtmTerm;
    }

    /**
     * set utmTerm
     *
     * @param utmTerm utmTerm
     * @return SALinkCreator
     */
    public SASlinkCreator setUtmTerm(String utmTerm) {
        this.mUtmTerm = utmTerm;
        return this;
    }

    public String getUtmContent() {
        return mUtmContent;
    }

    /**
     * set utmTerm
     *
     * @param utmContent utmContent
     * @return SALinkCreator
     */
    public SASlinkCreator setUtmContent(String utmContent) {
        this.mUtmContent = utmContent;
        return this;
    }

    public Map<String, String> getLandingPage() {
        return mLandingPage;
    }

    /**
     * set landing page
     *
     * @param landingPage custom landing page map，key：Manufacturer，value：link
     * @return SASlinkCreator
     */
    public SASlinkCreator setLandingPage(Map<String, String> landingPage) {
        this.mLandingPage = landingPage;
        return this;
    }

    /**
     * create share link
     *
     * @param context context
     * @param callback create dynamic link callback
     */
    public void createSLink(final Context context, final SensorsDataCreateSLinkCallback callback) {

        String requestUrl = NetworkUtils.getRequestUrl(SensorsDataAPI.getConfigOptions().getCustomADChannelUrl(), "slink/dynamic/links");
        try {
            if (checkInfo(context, callback, requestUrl)) {
                String serverUrl = SensorsDataAPI.sharedInstance().getServerUrl();
                String projectName = new ServerUrl(serverUrl).getProject();
                Map<String, String> headerMap = new HashMap<>();
                headerMap.put("token", mToken);
                JSONObject paramObj = new JSONObject();

                paramObj.put("project_name", projectName);
                paramObj.put("slink_template_id", mTemplateID);
                paramObj.put("name", getName());
                paramObj.put("channel_type", "app_share");
                paramObj.put("channel_name", mChannelName);
                paramObj.put("custom_param", getCustomProperties());
                paramObj.put("route_param", getRouteParam());
                paramObj.put("fixed_param", new JSONObject()
                        .put("channel_utm_campaign", getUtmCampaign())
                        .put("channel_utm_content", getUtmContent())
                        .put("channel_utm_medium", getUtmMedium())
                        .put("channel_utm_source", getUtmSource())
                        .put("channel_utm_term", getUtmTerm()));
                paramObj.put("uri_scheme_suffix", getUriSchemeSuffix());
                if (getLandingPageType() != null) {
                    paramObj.put("landing_page_type", getLandingPageType().getTypeName());
                }
                paramObj.put("other_landing_page_map", new JSONObject(getLandingPage()));
                paramObj.put("jump_address", mRedirectURLOnOtherDevice);
                new RequestHelper.Builder(HttpMethod.POST, requestUrl).header(headerMap).jsonData(paramObj.toString()).callback(new HttpCallback.JsonCallback() {
                    @Override
                    public void onFailure(int code, String errorMessage) {
                        if (code == 0) {
                            code = 10006;
                        }
                        trackSlinkCreate(code, errorMessage, callback);
                    }

                    @Override
                    public void onResponse(JSONObject response) {
                        int responseCode = 10004;
                        String responseMsg = SADisplayUtil.getStringResource(context, R.string.sensors_analytics_ad_create_link_response_data_error);
                        String slinkId = "";
                        String shareLink = "";
                        if (response != null) {
                            responseCode = response.optInt("code", 10004);
                            if (responseCode == 0) {
                                responseMsg = response.optString("msg", SADisplayUtil.getStringResource(context, R.string.sensors_analytics_ad_create_link_success));
                                JSONObject dataObj = response.optJSONObject("data");
                                if (dataObj != null) {
                                    slinkId = dataObj.optString("slink_id");
                                    shareLink = dataObj.optString("short_url");
                                }
                            } else if (responseCode != 10004) {
                                responseMsg = response.optString("msg");
                            }
                        }
                        trackSlinkCreate(responseCode, responseMsg, slinkId, shareLink, callback);
                    }
                }).execute();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 检测短链创建信息
     *
     * @param context Context
     * @param callback create dynamic link callback
     * @param requestUrl request url
     * @return isPass
     */
    private boolean checkInfo(Context context, SensorsDataCreateSLinkCallback callback, String requestUrl) {
        if (callback == null) {
            trackSlinkCreate(10005, SADisplayUtil.getStringResource(context, R.string.sensors_analytics_ad_create_link_callback_missing), null);
            return false;
        }
        if (!NetworkUtils.isNetworkAvailable(context)) {
            trackSlinkCreate(10002, SADisplayUtil.getStringResource(context, R.string.sensors_analytics_ad_create_link_no_network), callback);
            return false;
        }
        if (TextUtils.isEmpty(requestUrl) || !(requestUrl.startsWith("http://") || requestUrl.startsWith("https://"))) {
            trackSlinkCreate(10003, SADisplayUtil.getStringResource(context, R.string.sensors_analytics_ad_create_link_custom_url_error), callback);
            return false;
        }
        if (TextUtils.isEmpty(mToken)) {
            trackSlinkCreate(10001, SADisplayUtil.getStringResource(context, R.string.sensors_analytics_ad_create_link_token_missing), callback);
            return false;
        }
        if (TextUtils.isEmpty(mTemplateID)) {
            trackSlinkCreate(10001, SADisplayUtil.getStringResource(context, R.string.sensors_analytics_ad_create_link_template_id_missing), callback);
            return false;
        }
        if (TextUtils.isEmpty(mChannelName)) {
            trackSlinkCreate(10001, SADisplayUtil.getStringResource(context, R.string.sensors_analytics_ad_create_link_channel_name_missing), callback);
            return false;
        }
        if (TextUtils.isEmpty(mCommonRedirectURI)) {
            trackSlinkCreate(10001, SADisplayUtil.getStringResource(context, R.string.sensors_analytics_ad_create_link_common_redirect_url_missing), callback);
            return false;
        }
        return true;
    }

    private void trackSlinkCreate(int responseCode, String responseMsg, SensorsDataCreateSLinkCallback callback) {
        trackSlinkCreate(responseCode, responseMsg, "", "", callback);
    }

    private void trackSlinkCreate(int responseCode, String responseMsg, String slinkId, String shareLink, SensorsDataCreateSLinkCallback callback) {
        SASlinkResponse response = new SASlinkResponse();
        response.slink = shareLink;
        response.statusCode = responseCode;
        response.message = responseMsg;
        response.slinkID = slinkId;
        response.commonRedirectURI = mCommonRedirectURI;
        JSONObject params = new JSONObject();
        try {
            params.put("$ad_dynamic_slink_channel_type", "app_share")
                    .put("$ad_dynamic_slink_source", "Android")
                    .put("$ad_dynamic_slink_channel_name", mChannelName)
                    .put("$ad_dynamic_slink_data", "")
                    .put("$ad_dynamic_slink_short_url", shareLink)
                    .put("$ad_dynamic_slink_status", responseCode)
                    .put("$ad_dynamic_slink_msg", responseMsg.length() <= 200 ? responseMsg : responseMsg.substring(0, 200))
                    .put("$ad_slink_id", slinkId)
                    .put("$ad_slink_template_id", mTemplateID)
                    .put("$ad_slink_type", "dynamic");
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
        SensorsDataAPI.sharedInstance().track("$AdDynamicSlinkCreate", params);
        if (callback != null) {
            callback.onReceive(response);
        }
    }
}
