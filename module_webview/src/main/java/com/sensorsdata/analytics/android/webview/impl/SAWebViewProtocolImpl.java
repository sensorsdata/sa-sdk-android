/*
 * Created by dengshiwei on 2022/09/13.
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

package com.sensorsdata.analytics.android.webview.impl;

import android.content.Context;
import android.os.Build;
import android.webkit.WebView;

import com.sensorsdata.analytics.android.sdk.jsbridge.AppWebViewInterface;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.core.mediator.SAModuleManager;

import org.json.JSONObject;

import java.lang.reflect.Method;

public class SAWebViewProtocolImpl {
    private static final String TAG = "SA.WebViewProtocolImpl";
    private final Context mContext;
    private final String JS_BRIDGE = "SensorsData_APP_JS_Bridge";
    public SAWebViewProtocolImpl(SAContextManager mContextManager) {
        this.mContext = mContextManager.getContext();
    }

    public <T> T invokeModuleFunction(String methodName, Object... argv) {
        switch (methodName) {
            case Modules.WebView.METHOD_SHOWUP_WEBVIEW:
                showUpWebView((WebView) argv[0],(JSONObject) argv[1], (Boolean) argv[2], (Boolean) argv[3]);
                break;
            case Modules.WebView.METHOD_SHOWUP_X5WEBVIEW:
                showUpX5WebView(argv[0],(JSONObject) argv[1], (Boolean) argv[2], (Boolean) argv[3]);
                break;
        }
        return null;
    }

    private void showUpWebView(WebView webView, JSONObject properties, boolean isSupportJellyBean, boolean enableVerify) {
        if (Build.VERSION.SDK_INT < 17 && !isSupportJellyBean) {
            SALog.i(TAG, "For applications targeted to API level JELLY_BEAN or below, this feature NOT SUPPORTED");
            return;
        }

        if (webView != null) {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.addJavascriptInterface(new AppWebViewInterface(mContext, properties, enableVerify, webView), JS_BRIDGE);
            SAModuleManager.getInstance().invokeModuleFunction(Modules.Visual.MODULE_NAME, Modules.Visual.METHOD_ADD_VISUAL_JAVASCRIPTINTERFACE, webView);
        }
    }

    private void showUpX5WebView(Object x5WebView, JSONObject properties, boolean isSupportJellyBean, boolean enableVerify) {
        try {
            if (Build.VERSION.SDK_INT < 17 && !isSupportJellyBean) {
                SALog.d(TAG, "For applications targeted to API level JELLY_BEAN or below, this feature NOT SUPPORTED");
                return;
            }

            if (x5WebView == null) {
                return;
            }

            Class<?> clazz = x5WebView.getClass();
            Method addJavascriptInterface = clazz.getMethod("addJavascriptInterface", Object.class, String.class);
            if (addJavascriptInterface == null) {
                return;
            }
            addJavascriptInterface.invoke(x5WebView, new AppWebViewInterface(mContext, properties, enableVerify), JS_BRIDGE);
            SAModuleManager.getInstance().invokeModuleFunction(Modules.Visual.MODULE_NAME, Modules.Visual.METHOD_ADD_VISUAL_JAVASCRIPTINTERFACE, x5WebView);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
