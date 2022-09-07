/*
 * Created by yuejianzhong on 2020/11/04.
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

package com.sensorsdata.analytics.android.sdk.remote;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SAEventManager;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.ServerUrl;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.dialog.SensorsDataDialogUtils;
import com.sensorsdata.analytics.android.sdk.dialog.SensorsDataLoadingDialog;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.network.HttpCallback;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;
import com.sensorsdata.analytics.android.sdk.util.SADisplayUtil;

import org.json.JSONObject;

/**
 * SDK 调试采集控制时，调试管理类
 */
public class SensorsDataRemoteManagerDebug extends BaseSensorsDataSDKRemoteManager {

    private static final String TAG = "SA.SensorsDataRemoteManagerDebug";
    private String errorMsg = "";

    public SensorsDataRemoteManagerDebug(SensorsDataAPI sensorsDataAPI, Context context) {
        super(sensorsDataAPI, sensorsDataAPI.getSAContextManager());
        SALog.i(TAG, "remote config: Construct a SensorsDataRemoteManagerDebug");
    }

    @Override
    public void pullSDKConfigFromServer() {
        SALog.i(TAG, "remote config: Running pullSDKConfigFromServer");
    }

    @Override
    public void requestRemoteConfig(RandomTimeType randomTimeType, boolean enableConfigV) {
        SALog.i(TAG, "remote config: Running requestRemoteConfig");
    }

    @Override
    public void resetPullSDKConfigTimer() {
        SALog.i(TAG, "remote config: Running resetPullSDKConfigTimer");
    }

    @Override
    public void applySDKConfigFromCache() {
        SALog.i(TAG, "remote config: Running applySDKConfigFromCache");
    }

    @Override
    public void setSDKRemoteConfig(SensorsDataSDKRemoteConfig sdkRemoteConfig) {
        try {
            final JSONObject eventProperties = new JSONObject();
            JSONObject remoteConfigJson = sdkRemoteConfig.toJson().put("debug", true);
            String remoteConfigString = remoteConfigJson.toString();
            eventProperties.put("$app_remote_config", remoteConfigString);
            SAEventManager.getInstance().trackQueueEvent(new Runnable() {
                @Override
                public void run() {
                    SensorsDataAPI.sharedInstance().getSAContextManager().
                            trackEvent(new InputData().setEventName("$AppRemoteConfigChanged").setProperties(eventProperties).setEventType(EventType.TRACK));
                }
            });
            mSensorsDataAPI.flush();
            mSDKRemoteConfig = sdkRemoteConfig;
            SALog.i(TAG, "remote config: The remote configuration takes effect immediately");
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 校验采集配置
     *
     * @param uri 扫码 uri
     * @param activity activity
     */
    public void checkRemoteConfig(final Uri uri, final Activity activity) {
        if (verifyRemoteRequestParameter(uri, activity)) {
            SensorsDataDialogUtils.showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_common_title),
                    SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_remote_config), SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_common_continue), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final SensorsDataLoadingDialog loadingDialog = new SensorsDataLoadingDialog(activity);
                            SensorsDataDialogUtils.dialogShowDismissOld(loadingDialog);
                            // 发起请求
                            requestRemoteConfig(false, new HttpCallback.StringCallback() {
                                @Override
                                public void onFailure(int code, String errorMessage) {
                                    loadingDialog.dismiss();
                                    SensorsDataDialogUtils.showDialog(activity,  SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_remote_fail));
                                    SALog.i(TAG, "remote config: Remote request was failed,code is " + code +
                                            ",errorMessage is" + errorMessage);
                                }

                                @Override
                                public void onResponse(String response) {
                                    loadingDialog.dismiss();
                                    if (!TextUtils.isEmpty(response)) {
                                        SensorsDataSDKRemoteConfig sdkRemoteConfig = toSDKRemoteConfig(response);
                                        String nv = uri.getQueryParameter("nv");
                                        if (!sdkRemoteConfig.getNewVersion().equals(nv)) {

                                            SensorsDataDialogUtils.showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_remote_version_error),
                                                    String.format(SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_remote_version_tip), sdkRemoteConfig.getNewVersion(), nv),
                                                    SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_common_ok),
                                                    new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            SensorsDataDialogUtils.startLaunchActivity(activity);
                                                        }
                                                    }, null, null);
                                        } else {
                                            SensorsDataDialogUtils.showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_remote_succeed));
                                            setSDKRemoteConfig(sdkRemoteConfig);
                                        }
                                    } else {
                                        SensorsDataDialogUtils.showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_remote_other_error));
                                    }
                                    SALog.i(TAG, "remote config: Remote request was successful,response data is " + response);
                                }

                                @Override
                                public void onAfter() {

                                }
                            });
                        }
                    }, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_common_cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SensorsDataDialogUtils.startLaunchActivity(activity);
                        }
                    });
        } else {
            SensorsDataDialogUtils.showDialog(activity, errorMsg);
        }
    }

    /**
     * 校验本地网络配置，及 uri 中参数和本地参数
     *
     * @param uri 扫码唤醒时的 uri
     * @param activity activity
     * @return 是否校验通过
     */
    private boolean verifyRemoteRequestParameter(Uri uri, Activity activity) {
        boolean isVerify = false;
        String appId = uri.getQueryParameter("app_id");
        String os = uri.getQueryParameter("os");
        String project = uri.getQueryParameter("project");
        String nv = uri.getQueryParameter("nv");
        String localProject = "";
        String serverUrl = mSensorsDataAPI.getServerUrl();
        if (!TextUtils.isEmpty(serverUrl)) {
            localProject = new ServerUrl(serverUrl).getProject();
        }
        SALog.i(TAG, "remote config: ServerUrl is " + serverUrl);
        if (!NetworkUtils.isNetworkAvailable(mContextManager.getContext())) {
            errorMsg = SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_remote_tip_error_network);
        } else if (mSensorsDataAPI != null && !mSensorsDataAPI.isNetworkRequestEnable()) {
            errorMsg = SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_remote_tip_error_disable_network);
            SALog.i(TAG, "enableNetworkRequest is false");
        } else if (mDisableDefaultRemoteConfig) {
            errorMsg = SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_remote_tip_error_disable_remote);
            SALog.i(TAG, "disableDefaultRemoteConfig is true");
        } else if (!localProject.equals(project)) {
            errorMsg = SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_remote_tip_error_project);
        } else if (!"Android".equals(os)) {
            errorMsg = SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_remote_tip_error_os);
        } else if (!AppInfoUtils.getProcessName(activity).equals(appId)) {
            errorMsg = SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_remote_tip_error_appid);
        } else if (TextUtils.isEmpty(nv)) {
            errorMsg = SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_remote_tip_error_qrcode);
        } else {
            isVerify = true;
        }
        SALog.i(TAG, "remote config: Uri is " + uri.toString());
        SALog.i(TAG, "remote config: The verification result is " + isVerify);
        return isVerify;
    }
}
