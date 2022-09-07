/*
 * Created by chenru on 2022/7/5 下午6:05.
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

package com.sensorsdata.analytics.android.sdk.advert.scan;

import static com.sensorsdata.analytics.android.sdk.advert.SAAdvertConstants.TAG;
import static com.sensorsdata.analytics.android.sdk.dialog.SensorsDataDialogUtils.dialogShowDismissOld;
import static com.sensorsdata.analytics.android.sdk.dialog.SensorsDataDialogUtils.showDialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.text.TextUtils;

import com.sensorsdata.analytics.advert.R;
import com.sensorsdata.analytics.android.sdk.SAEventManager;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.ServerUrl;
import com.sensorsdata.analytics.android.sdk.advert.oaid.SAOaidHelper;
import com.sensorsdata.analytics.android.sdk.advert.utils.ChannelUtils;
import com.sensorsdata.analytics.android.sdk.advert.utils.SAAdvertUtils;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.dialog.SensorsDataDialogUtils;
import com.sensorsdata.analytics.android.sdk.dialog.SensorsDataLoadingDialog;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.network.HttpCallback;
import com.sensorsdata.analytics.android.sdk.network.HttpMethod;
import com.sensorsdata.analytics.android.sdk.network.RequestHelper;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;
import com.sensorsdata.analytics.android.sdk.util.SADisplayUtil;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONObject;

public class ChannelDebugScanHelper implements IAdvertScanListener {
    @Override
    public void handlerScanUri(Activity activity, Uri uri) {
        if (ChannelUtils.hasUtmByMetaData(activity)) {
            showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_listener));
            return;
        }
        String monitorId = uri.getQueryParameter("monitor_id");
        if (TextUtils.isEmpty(monitorId)) {
            SensorsDataDialogUtils.startLaunchActivity(activity);
            return;
        }
        String url = SensorsDataAPI.sharedInstance().getServerUrl();
        if (TextUtils.isEmpty(url)) {
            showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_error_url));
            return;
        }
        ServerUrl serverUrl = new ServerUrl(url);
        String projectName = uri.getQueryParameter("project_name");
        if (serverUrl.getProject().equals(projectName)) {
            String projectId = uri.getQueryParameter("project_id");
            String accountId = uri.getQueryParameter("account_id");
            String isReLink = uri.getQueryParameter("is_relink");
            if ("1".equals(isReLink)) {//续连标识 1 :续连
                String deviceCode = uri.getQueryParameter("device_code");
                if (ChannelUtils.checkDeviceInfo(activity, deviceCode)) {//比较设备信息是否匹配
                    showChannelDebugActiveDialog(activity);
                } else {
                    showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_error_retry));
                }
            } else {
                showChannelDebugDialog(activity, serverUrl.getBaseUrl(), monitorId, projectId, accountId);
            }
        } else {
            showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_error_project));
        }
    }

    public static void showChannelDebugActiveDialog(final Activity activity) {
        showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_dialog_title),
                SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_dialog_content), SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_dialog_activate), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        trackChannelDebugInstallation(activity);
                        showChannelDebugActiveDialog(activity);
                    }
                }, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_dialog_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SensorsDataDialogUtils.startLaunchActivity(activity);
                    }
                });
    }

    private static void trackChannelDebugInstallation(final Activity activity) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject _properties = new JSONObject();
                    _properties.put("$ios_install_source", ChannelUtils.getDeviceInfo(activity,
                            SAAdvertUtils.getIdentifier(activity), SAOaidHelper.getOpenAdIdentifier(activity)));
                    // first step: track
                    SAEventManager.getInstance().trackEvent(new InputData().setEventType(EventType.TRACK).setEventName("$ChannelDebugInstall").setProperties(_properties));
                    // second step: profile_set_once or profile_set
                    JSONObject profileProperties = new JSONObject();
                    JSONUtils.mergeJSONObject(_properties, profileProperties);
                    profileProperties.put("$first_visit_time", new java.util.Date());
                    SAEventManager.getInstance().trackEvent(new InputData().setEventType(EventType.PROFILE_SET_ONCE).setProperties(profileProperties));
                    SensorsDataAPI.sharedInstance().flush();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        }).start();

    }

    public static void showChannelDebugDialog(final Activity activity,
                                              final String baseUrl,
                                              final String monitorId,
                                              final String projectId,
                                              final String accountId) {
        showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_dialog_starting), "",
                SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_dialog_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        Context context = activity.getApplicationContext();
                        boolean isTrackInstallation = ChannelUtils.isTrackInstallation();
                        if (!isTrackInstallation || ChannelUtils.isCorrectTrackInstallation()) {
                            String androidId = SensorsDataUtils.getIdentifier(context);
                            String oaid = SAOaidHelper.getOpenAdIdentifier(context);
                            if (isTrackInstallation && !ChannelUtils.isGetDeviceInfo(context, androidId, oaid)) {
                                showChannelDebugErrorDialog(activity);
                                return;
                            }
                            if (!NetworkUtils.isNetworkAvailable(context)) {
                                showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_error_network));
                                return;
                            }
                            String deviceCode = ChannelUtils.getDeviceInfo(activity, androidId, oaid);
                            final SensorsDataLoadingDialog loadingDialog = new SensorsDataLoadingDialog(activity);
                            dialogShowDismissOld(loadingDialog);
                            requestActiveChannel(baseUrl,
                                    monitorId, projectId, accountId,
                                    deviceCode, isTrackInstallation,
                                    new HttpCallback.JsonCallback() {
                                        @Override
                                        public void onFailure(int code, String errorMessage) {
                                            loadingDialog.dismiss();
                                            SALog.i(TAG, "ChannelDebug request error:" + errorMessage);
                                            showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_error_request));
                                        }

                                        @Override
                                        public void onResponse(JSONObject response) {
                                            loadingDialog.dismiss();
                                            if (response == null) {
                                                SALog.i(TAG, "ChannelDebug response error msg: response is null");
                                                showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_error_whitelist));
                                                return;
                                            }
                                            int code = response.optInt("code", 0);
                                            if (code == 1) {// 请求成功
                                                showChannelDebugActiveDialog(activity);
                                            } else {//请求失败
                                                SALog.i(TAG, "ChannelDebug response error msg:" + response.optString("message"));
                                                showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_error_whitelist));
                                            }
                                        }
                                    });
                        } else {
                            showChannelDebugErrorDialog(activity);
                        }
                    }
                }, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_dialog_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SensorsDataDialogUtils.startLaunchActivity(activity);
                    }
                });
    }


    private static void showChannelDebugErrorDialog(final Activity activity) {
        SensorsDataDialogUtils.showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_error_debug_fail_title),
                SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_error_debug_fail_content),
                SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_dialog_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SensorsDataDialogUtils.startLaunchActivity(activity);
                    }
                }, null, null);
    }

    private static void requestActiveChannel(String baseUrl, String monitorId,
                                             String projectId, String accountId,
                                             String deviceCode, boolean isActive,
                                             HttpCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("monitor_id", monitorId);
            json.put("distinct_id", SensorsDataAPI.sharedInstance().getDistinctId());
            json.put("project_id", projectId);
            json.put("account_id", accountId);
            json.put("has_active", isActive ? "true" : "false");
            json.put("device_code", deviceCode);
            new RequestHelper.Builder(HttpMethod.POST, baseUrl + "/api/sdk/channel_tool/url")
                    .jsonData(json.toString()).callback(callback).execute();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
