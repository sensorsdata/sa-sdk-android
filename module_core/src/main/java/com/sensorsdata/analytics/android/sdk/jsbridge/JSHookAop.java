/*
 * Created by dengshiwei on 2022/07/05.
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

package com.sensorsdata.analytics.android.sdk.jsbridge;

import android.os.Build;
import android.view.View;

import com.sensorsdata.analytics.android.sdk.AppWebViewInterface;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPIEmptyImplementation;
import com.sensorsdata.analytics.android.sdk.util.H5Util;
import com.sensorsdata.analytics.android.sdk.core.mediator.visual.SAVisual;

import java.util.Map;

public class JSHookAop {
    private static final String TAG = "SA.JSHookAop";

    public static void loadUrl(View webView, String url) {
        if (webView == null) {
            SALog.i(TAG, "WebView has not initialized.");
            return;
        }
        setupH5Bridge(webView);
    }

    public static void loadUrl(View webView, String url, Map<String, String> additionalHttpHeaders) {
        if (webView == null) {
            SALog.i(TAG, "WebView has not initialized.");
            return;
        }
        setupH5Bridge(webView);
    }

    public static void loadData(View webView, String data, String mimeType, String encoding) {
        if (webView == null) {
            SALog.i(TAG, "WebView has not initialized.");
            return;
        }
        setupH5Bridge(webView);
    }

    public static void loadDataWithBaseURL(View webView, String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        if (webView == null) {
            SALog.i(TAG, "WebView has not initialized.");
            return;
        }
        setupH5Bridge(webView);
    }

    public static void postUrl(View webView, String url, byte[] postData) {
        if (webView == null) {
            SALog.i(TAG, "WebView has not initialized.");
            return;
        }
        setupH5Bridge(webView);
    }

    private static void setupH5Bridge(View webView) {
        if (SensorsDataAPI.sharedInstance() instanceof SensorsDataAPIEmptyImplementation) {
            return;
        }
        if (isSupportJellyBean() && SensorsDataAPI.sharedInstance().getConfigOptions() != null && SensorsDataAPI.sharedInstance().getConfigOptions().isAutoTrackWebView()) {
            setupWebView(webView);
        }
        if (isSupportJellyBean()) {
            SAVisual.addVisualJavascriptInterface(webView);
        }
    }

    private static boolean isSupportJellyBean() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 && !SensorsDataAPI.getConfigOptions().isWebViewSupportJellyBean()) {
            SALog.d(TAG, "For applications targeted to API level JELLY_BEAN or below, this feature NOT SUPPORTED");
            return false;
        }
        return true;
    }

    private static void setupWebView(View webView) {
        if (webView != null && webView.getTag(com.sensorsdata.analytics.android.sdk.R.id.sensors_analytics_tag_view_webview) == null) {
            webView.setTag(com.sensorsdata.analytics.android.sdk.R.id.sensors_analytics_tag_view_webview, new Object());
            H5Util.addJavascriptInterface(webView, new AppWebViewInterface(SensorsDataAPI.sharedInstance().getSAContextManager().getContext(), null, false, webView), "SensorsData_APP_New_H5_Bridge");
        }
    }
}
