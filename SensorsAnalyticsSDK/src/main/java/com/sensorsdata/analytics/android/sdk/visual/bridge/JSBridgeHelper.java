/*
 * Created by zhangxiangwei on 2021/07/30.
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

package com.sensorsdata.analytics.android.sdk.visual.bridge;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Base64;
import android.view.View;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.listener.SAJSListener;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class JSBridgeHelper implements WebViewJavascriptBridge {

    private final static String CALLBACK_ID_FORMAT = "JAVA_CB_%s";
    private final static String CALL_TYPE_GET_VISUAL_PROPERTIES = "getJSVisualProperties";
    private Map<String, OnBridgeCallback> mCallbacks = new ArrayMap<>();
    private SAJSListener mSAJSListener;

    public void addSAJSListener() {
        if (mSAJSListener == null) {
            mSAJSListener = new SAJSListener() {
                @Override
                public void onReceiveJSMessage(WeakReference<View> view, String message) {
                    try {
                        JSONObject obj = new JSONObject(message);
                        String callType = obj.optString("callType");
                        if (TextUtils.equals(CALL_TYPE_GET_VISUAL_PROPERTIES, callType)) {
                            String messageId = obj.optString("message_id");
                            if (!TextUtils.isEmpty(messageId)) {
                                OnBridgeCallback function = mCallbacks.remove(messageId);
                                if (function != null) {
                                    JSONObject dataObj = obj.optJSONObject("data");
                                    if (dataObj != null) {
                                        function.onCallBack(dataObj.toString());
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            };
            SensorsDataAPI.sharedInstance().addSAJSListener(mSAJSListener);
        }
    }

    @Override
    public synchronized void sendToWeb(final View webView, final String methodName, Object data, OnBridgeCallback responseCallback) {
        try {
            if (TextUtils.isEmpty(methodName)) {
                return;
            }
            JSRequest request = new JSRequest();
            request.methodName = methodName;
            if (responseCallback != null) {
                String messageId = String.format(CALLBACK_ID_FORMAT, SystemClock.currentThreadTimeMillis());
                mCallbacks.put(messageId, responseCallback);
                request.messageId = messageId;
            }
            JSONObject object = null;
            if (data instanceof String) {
                object = new JSONObject((String) data);
            } else if (data instanceof JSONObject) {
                object = new JSONObject();
                object.put("message_id", request.messageId);
                object.put("platform", "Android");
                SensorsDataUtils.mergeJSONObject((JSONObject) data, object);
            }
            final JSONObject obj = object;
            if (obj == null) {
                return;
            }
            if (webView != null) {
                webView.post(new Runnable() {
                    @Override
                    public void run() {
                        String result = "'" + methodName + "','" + Base64.encodeToString(obj.toString().getBytes(), Base64.DEFAULT) + "'";
                        invokeWebViewLoad(webView, "loadUrl", new Object[]{"javascript:window.sensorsdata_app_call_js(" + result + ")"}, new Class[]{String.class});
                    }
                });
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void sendToWeb(View webView, String methodName, Object data) {
        sendToWeb(webView, methodName, data, null);
    }

    private static void invokeWebViewLoad(View webView, String methodName, Object[] params, Class[] paramTypes) {
        try {
            Class<?> clazz = webView.getClass();
            Method loadMethod = clazz.getMethod(methodName, paramTypes);
            loadMethod.invoke(webView, params);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
