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

package com.sensorsdata.analytics.android.sdk.visual.impl;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.jsbridge.H5Helper;
import com.sensorsdata.analytics.android.sdk.monitor.SensorsDataLifecycleMonitorManager;
import com.sensorsdata.analytics.android.sdk.util.SAViewUtils;
import com.sensorsdata.analytics.android.sdk.util.visual.ViewNode;
import com.sensorsdata.analytics.android.sdk.visual.HeatMapService;
import com.sensorsdata.analytics.android.sdk.visual.NodesProcess;
import com.sensorsdata.analytics.android.sdk.visual.R;
import com.sensorsdata.analytics.android.sdk.visual.VisualizedAutoTrackService;
import com.sensorsdata.analytics.android.sdk.visual.WebViewVisualInterface;
import com.sensorsdata.analytics.android.sdk.visual.property.VisualPropertiesManager;
import com.sensorsdata.analytics.android.sdk.visual.utils.AppStateManager;
import com.sensorsdata.analytics.android.sdk.visual.utils.VisualUtil;
import com.sensorsdata.analytics.android.sdk.visual.view.PairingCodeEditDialog;
import com.sensorsdata.analytics.android.sdk.visual.view.VisualDialog;

import org.json.JSONObject;

public class VisualProtocolImpl {
    private static final String TAG = "SA.SAVisualProtocolImpl";
    private final SAContextManager mSAContextManager;

    public VisualProtocolImpl(SAContextManager contextManager) {
        mSAContextManager = contextManager;
        SensorsDataLifecycleMonitorManager.getInstance().addActivityLifeCallback(new AppStateManager());
    }

    public <T> T invokeModuleFunction(String methodName, Object... argv) {
        try {
            switch (methodName) {
                case Modules.Visual.METHOD_ADD_VISUAL_JAVASCRIPTINTERFACE:
                    addVisualJavascriptInterface((View) argv[0]);
                    break;
                case Modules.Visual.METHOD_REQUEST_VISUAL_CONFIG:
                    VisualPropertiesManager.getInstance().requestVisualConfig(mSAContextManager);
                    break;
                case Modules.Visual.METHOD_MERGE_VISUAL_PROPERTIES:
                    mergeVisualProperties((JSONObject) argv[0], (View) argv[1]);
                    break;
                case Modules.Visual.METHOD_RESUME_VISUAL_SERVICE:
                    VisualizedAutoTrackService.getInstance().resume();
                    break;
                case Modules.Visual.METHOD_STOP_VISUAL_SERVICE:
                    VisualizedAutoTrackService.getInstance().stop();
                    break;
                case Modules.Visual.METHOD_RESUME_HEATMAP_SERVICE:
                    HeatMapService.getInstance().resume();
                    break;
                case Modules.Visual.METHOD_STOP_HEATMAP_SERVICE:
                    HeatMapService.getInstance().stop();
                    break;
                case Modules.Visual.METHOD_SHOW_PAIRING_CODE_INPUTDIALOG:
                    showPairingCodeInputDialog((Context) argv[0]);
                    break;
                case Modules.Visual.METHOD_SHOW_OPEN_HEATMAP_DIALOG:
                    VisualDialog.showOpenHeatMapDialog((Activity) argv[0], (String) argv[1], (String) argv[2]);
                    break;
                case Modules.Visual.METHOD_SHOW_OPEN_VISUALIZED_AUTOTRACK_DIALOG:
                    VisualDialog.showOpenVisualizedAutoTrackDialog((Activity) argv[0], (String) argv[1], (String) argv[2]);
                    break;
                case Modules.Visual.METHOD_H5_GET_APPVISUAL_CONFIG:
                    return (T) h5GetAppVisualConfig();
                case Modules.Visual.METHOD_FLUTTER_GET_APPVISUAL_CONFIG:
                    return (T) flutterGetAppVisualConfig();
                case Modules.Visual.METHOD_GET_VISUAL_STATE:
                    return (T) getVisualState();
                case Modules.Visual.METHOD_SEND_VISUALIZED_MESSAGE:
                    sendVisualizedMessage((String) argv[0]);
                    break;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    private void mergeVisualProperties(JSONObject srcObject, View view) {
        try {
            if (view == null) {
                return;
            }
            Activity activity = SAViewUtils.getActivityOfView(view.getContext(), view);
            // append $element_path
            ViewNode viewNode = VisualUtil.addViewPathProperties(activity, view, srcObject);
            // append visual custom properties
            if (mSAContextManager.getInternalConfigs().saConfigOptions.isVisualizedPropertiesEnabled()) {
                VisualPropertiesManager.getInstance().mergeVisualProperties(VisualPropertiesManager.VisualEventType.APP_CLICK, srcObject, viewNode);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public String h5GetAppVisualConfig() {
        String visualCache = appVisualConfig();
        if (!TextUtils.isEmpty(visualCache)) {
            return Base64.encodeToString(visualCache.getBytes(), Base64.DEFAULT);
        }
        return null;
    }

    /**
     * 获取可视化自定义配置信息
     *
     * @return 可视化自定义配置信息
     */
    private String appVisualConfig() {
        try {
            if (mSAContextManager.getInternalConfigs().saConfigOptions == null || !mSAContextManager.getInternalConfigs().saConfigOptions.isVisualizedPropertiesEnabled()) {
                return null;
            }
            VisualPropertiesManager.getInstance().getVisualPropertiesH5Helper().registerListeners();
            String visualCache = VisualPropertiesManager.getInstance().getVisualPropertiesCache().getVisualCache();
            return visualCache;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }


    private void addVisualJavascriptInterface(View webView) {
        if (webView != null && webView.getTag(R.id.sensors_analytics_tag_view_webview_visual) == null) {
            webView.setTag(R.id.sensors_analytics_tag_view_webview_visual, new Object());
            H5Helper.addJavascriptInterface(webView, new WebViewVisualInterface(webView), "SensorsData_App_Visual_Bridge");
        }
    }

    private void showPairingCodeInputDialog(final Context context) {
        if (mSAContextManager.getContext() == null) {
            SALog.i(TAG, "The argument context can't be null");
            return;
        }
        if (!(context instanceof Activity)) {
            SALog.i(TAG, "The static method showPairingCodeEditDialog(Context context) only accepts Activity as a parameter");
            return;
        }
        Activity activity = (Activity) context;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final PairingCodeEditDialog dialog = new PairingCodeEditDialog(context);
                dialog.show();
            }
        });
    }

    // Flutter 可视化需要
    public String flutterGetAppVisualConfig() {
        String visualCache = appVisualConfig();
        if (!TextUtils.isEmpty(visualCache)) {
            return Base64.encodeToString(visualCache.getBytes(), Base64.NO_WRAP);
        }
        return null;
    }

    public Boolean getVisualState() {
        return VisualizedAutoTrackService.getInstance().isServiceRunning() || HeatMapService.getInstance().isServiceRunning();
    }

    public void sendVisualizedMessage(String msg) {
        NodesProcess.getInstance().getFlutterNodesManager().handlerMessage(msg);
    }
}
