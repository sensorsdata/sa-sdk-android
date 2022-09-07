/*
 * Created by dengshiwei on 2022/07/15.
 * Copyright 2015－2021 Sensors Data Inc.
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

import android.view.View;

import com.sensorsdata.analytics.android.sdk.SALog;

import java.lang.reflect.Method;

public class WebUtils {
    private static final String TAG = "SA.WebUtils";

    public static void loadUrl(View webView, String url) {
        invokeWebViewLoad(webView, "loadUrl", new Object[]{url}, new Class[]{String.class});
    }

    private static void invokeWebViewLoad(View webView, String methodName, Object[] params, Class<?>[] paramTypes) {
        if (webView == null) {
            SALog.i(TAG, "WebView has not initialized.");
            return;
        }
        try {
            Class<?> clazz = webView.getClass();
            Method loadMethod = clazz.getMethod(methodName, paramTypes);
            loadMethod.invoke(webView, params);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
