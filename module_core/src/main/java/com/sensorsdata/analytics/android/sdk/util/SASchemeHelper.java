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

package com.sensorsdata.analytics.android.sdk.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPIEmptyImplementation;
import com.sensorsdata.analytics.android.sdk.core.SAModuleManager;
import com.sensorsdata.analytics.android.sdk.dialog.SensorsDataDialogUtils;
import com.sensorsdata.analytics.android.sdk.remote.BaseSensorsDataSDKRemoteManager;
import com.sensorsdata.analytics.android.sdk.remote.SensorsDataRemoteManagerDebug;
import com.sensorsdata.analytics.android.sdk.core.mediator.visual.SAVisual;

public class SASchemeHelper {

    private final static String TAG = "SA.SASchemeUtil";

    public static void handleSchemeUrl(Activity activity, Intent intent) {
        if (SensorsDataAPI.isSDKDisabled()) {
            SALog.i(TAG, "SDK is disabled,scan code function has been turned off");
            return;
        }
        if (SensorsDataAPI.sharedInstance() instanceof SensorsDataAPIEmptyImplementation) {
            SALog.i(TAG, "SDK is not init");
            return;
        }
        try {
            Uri uri = null;
            if (activity != null && intent != null) {
                uri = intent.getData();
            }
            if (uri != null) {
                SensorsDataAPI sensorsDataAPI = SensorsDataAPI.sharedInstance();
                String host = uri.getHost();
                if (SAModuleManager.getInstance().handlerScanUri(activity, uri)) {
                    intent.setData(null);
                } else if ("heatmap".equals(host)) {
                    String featureCode = uri.getQueryParameter("feature_code");
                    String postUrl = uri.getQueryParameter("url");
                    if (checkProjectIsValid(postUrl)) {
                        SAVisual.showOpenHeatMapDialog(activity, featureCode, postUrl);
                    } else {
                        SensorsDataDialogUtils.showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_visual_dialog_error));
                    }
                    intent.setData(null);
                } else if ("debugmode".equals(host)) {
                    String infoId = uri.getQueryParameter("info_id");
                    String locationHref = uri.getQueryParameter("sf_push_distinct_id");
                    String project = uri.getQueryParameter("project");
                    SensorsDataDialogUtils.showDebugModeSelectDialog(activity, infoId, locationHref, project);
                    intent.setData(null);
                } else if ("visualized".equals(host)) {
                    String featureCode = uri.getQueryParameter("feature_code");
                    String postUrl = uri.getQueryParameter("url");
                    if (checkProjectIsValid(postUrl)) {
                        SAVisual.showOpenVisualizedAutoTrackDialog(activity, featureCode, postUrl);
                    } else {
                        SensorsDataDialogUtils.showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_visual_dialog_error));
                    }
                    intent.setData(null);
                } else if ("popupwindow".equals(host)) {
                    SensorsDataDialogUtils.showPopupWindowDialog(activity, uri);
                    intent.setData(null);
                } else if ("encrypt".equals(host)) {
                    String tip = SAModuleManager.getInstance().invokeEncryptModuleFunction("verifySecretKey", uri);
                    if (TextUtils.isEmpty(tip)) {
                        tip = "未检测到加密模块库，请集成加密模块库后重试";
                    }
                    ToastUtil.showLong(activity, tip);
                    SensorsDataDialogUtils.startLaunchActivity(activity);
                    intent.setData(null);
                } else if ("abtest".equals(host)) {
                    try {
                        ReflectUtil.callStaticMethod(Class.forName("com.sensorsdata.abtest.core.SensorsABTestSchemeHandler"), "handleSchemeUrl", uri.toString());
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                    SensorsDataDialogUtils.startLaunchActivity(activity);
                    intent.setData(null);
                } else if ("sensorsdataremoteconfig".equals(host)) {
                    // enable_log
                    SensorsDataAPI.sharedInstance().enableLog(true);
                    BaseSensorsDataSDKRemoteManager sensorsDataSDKRemoteManager = sensorsDataAPI.getSAContextManager().getRemoteManager();
                    // cancel retry
                    if (sensorsDataSDKRemoteManager != null) {
                        sensorsDataSDKRemoteManager.resetPullSDKConfigTimer();
                    }
                    final SensorsDataRemoteManagerDebug sensorsDataRemoteManagerDebug =
                            new SensorsDataRemoteManagerDebug(sensorsDataAPI, activity.getBaseContext());
                    // replace SensorsDataRemoteManagerDebug object
                    sensorsDataAPI.getSAContextManager().setRemoteManager(sensorsDataRemoteManagerDebug);
                    SALog.i(TAG, "Start debugging remote config");
                    sensorsDataRemoteManagerDebug.checkRemoteConfig(uri, activity);
                    intent.setData(null);
                } else if ("assistant".equals(host)) {
                    SAConfigOptions configOptions = SensorsDataAPI.getConfigOptions();
                    if (configOptions != null && configOptions.mDisableDebugAssistant) {
                        return;
                    }
                    String service = uri.getQueryParameter("service");
                    if ("pairingCode".equals(service)) {
                        SAVisual.showPairingCodeInputDialog(activity);
                    }
                } else {
                    SensorsDataDialogUtils.startLaunchActivity(activity);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private static boolean checkProjectIsValid(String url) {
        String serverUrl = SensorsDataAPI.sharedInstance().getServerUrl();
        String sdkProject = null, serverProject = null;
        if (!TextUtils.isEmpty(url)) {
            Uri schemeUri = Uri.parse(url);
            if (schemeUri != null) {
                sdkProject = schemeUri.getQueryParameter("project");
            }
        }
        if (!TextUtils.isEmpty(serverUrl)) {
            Uri serverUri = Uri.parse(serverUrl);
            if (serverUri != null) {
                serverProject = serverUri.getQueryParameter("project");
            }
        }
        return !TextUtils.isEmpty(sdkProject) && !TextUtils.isEmpty(serverProject) && TextUtils.equals(sdkProject, serverProject);
    }
}
