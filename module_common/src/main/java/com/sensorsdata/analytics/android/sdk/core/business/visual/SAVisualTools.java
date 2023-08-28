/*
 * Created by dengshiwei on 2022/09/09.
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

package com.sensorsdata.analytics.android.sdk.core.business.visual;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.mediator.SAModuleManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.dialog.SensorsDataDialogUtils;
import com.sensorsdata.analytics.android.sdk.util.SADisplayUtil;

public class SAVisualTools {
    public static void showOpenHeatMapDialog(final Activity activity, final String featureCode, final String postUrl) {
        if (checkProjectIsValid(postUrl)) {
            if (!SAModuleManager.getInstance().hasModuleByName(Modules.Visual.MODULE_NAME)) {
                SensorsDataDialogUtils.showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_heatmap_sdk_fail));
                return;
            }
            if (!SensorsDataAPI.sharedInstance().isNetworkRequestEnable()) {
                SensorsDataDialogUtils.showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_heatmap_network_fail));
                return;
            }
            if (!SensorsDataAPI.sharedInstance().isHeatMapEnabled()) {
                SensorsDataDialogUtils.showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_heatmap_sdk_fail));
                return;
            }
            SAModuleManager.getInstance().invokeModuleFunction(Modules.Visual.MODULE_NAME, Modules.Visual.METHOD_SHOW_OPEN_HEATMAP_DIALOG, activity, featureCode, postUrl);
        } else {
            SensorsDataDialogUtils.showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_visual_dialog_error));
        }
    }

    public static void showOpenVisualizedAutoTrackDialog(final Activity activity, final String featureCode, final String postUrl) {
        if (checkProjectIsValid(postUrl)) {
            if (!SAModuleManager.getInstance().hasModuleByName(Modules.Visual.MODULE_NAME)) {
                SensorsDataDialogUtils.showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_visual_sdk_fail));
                return;
            }
            if (!SensorsDataAPI.sharedInstance().isNetworkRequestEnable()) {
                SensorsDataDialogUtils.showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_visual_network_fail));
                return;
            }
            if (!SensorsDataAPI.sharedInstance().isVisualizedAutoTrackEnabled()) {
                SensorsDataDialogUtils.showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_visual_sdk_fail));
                return;
            }
            SAModuleManager.getInstance().invokeModuleFunction(Modules.Visual.MODULE_NAME, Modules.Visual.METHOD_SHOW_OPEN_VISUALIZED_AUTOTRACK_DIALOG, activity, featureCode, postUrl);
        } else {
            SensorsDataDialogUtils.showDialog(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_visual_dialog_error));
        }
    }

    public static void showPairingCodeInputDialog(Context context) {
        if (SAModuleManager.getInstance().hasModuleByName(Modules.Visual.MODULE_NAME)) {
            SAModuleManager.getInstance().invokeModuleFunction(Modules.Visual.MODULE_NAME, Modules.Visual.METHOD_SHOW_PAIRING_CODE_INPUTDIALOG, context);
        } else {
            SensorsDataDialogUtils.showDialog(context, SADisplayUtil.getStringResource(context, R.string.sensors_analytics_visual_heatmap_sdk_fail));
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
