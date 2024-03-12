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

package com.sensorsdata.analytics.android.sdk.jsbridge;

import android.text.TextUtils;
import android.view.View;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPIEmptyImplementation;
import com.sensorsdata.analytics.android.sdk.core.SACoreHelper;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.internal.beans.ServerUrl;
import com.sensorsdata.analytics.android.sdk.listener.SAJSListener;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.concurrent.CopyOnWriteArrayList;

public class H5Helper {
    private static CopyOnWriteArrayList<SAJSListener> mSAJSListeners;

    public static void addJavascriptInterface(View webView, Object obj, String interfaceName) {
        try {
            Class<?> clazz = webView.getClass();
            try {
                Method getSettingsMethod = clazz.getMethod("getSettings");
                Object settings = getSettingsMethod.invoke(webView);
                if (settings != null) {
                    Method setJavaScriptEnabledMethod = settings.getClass().getMethod("setJavaScriptEnabled", boolean.class);
                    setJavaScriptEnabledMethod.invoke(settings, true);
                }
            } catch (Exception e) {
                //ignore
            }
            Method addJSMethod = clazz.getMethod("addJavascriptInterface", Object.class, String.class);
            addJSMethod.invoke(webView, obj, interfaceName);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static boolean verifyEventFromH5(String eventInfo) {
        try {
            SALog.i("SA.AppWebViewInterface", "verifyEventFromH5 h5 eventInfo = " + eventInfo);
            if (TextUtils.isEmpty(eventInfo)) {
                return false;
            }
            JSONObject eventObject = new JSONObject(eventInfo);

            String serverUrl = eventObject.optString("server_url");
            SALog.i("SA.AppWebViewInterface", "verifyEventFromH5 h5 serverUrl = " + serverUrl);
            if (!TextUtils.isEmpty(serverUrl)) {
                if (!(new ServerUrl(serverUrl).check(new ServerUrl(SensorsDataAPI.getConfigOptions().getServerUrl())))) {
                    return false;
                }
                trackEvent(eventInfo);
                return true;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return false;
    }

    public static void trackEventFromH5(String eventInfo, boolean enableVerify) {
        try {
            if (TextUtils.isEmpty(eventInfo)) {
                return;
            }

            JSONObject eventObject = new JSONObject(eventInfo);
            SALog.i("SA.AppWebViewInterface", "trackEventFromH5 h5 enableVerify = " + enableVerify);
            if (enableVerify) {
                String serverUrl = eventObject.optString("server_url");
                SALog.i("SA.AppWebViewInterface", "trackEventFromH5 h5 serverUrl = " + serverUrl);
                if (!TextUtils.isEmpty(serverUrl)) {
                    if (!(new ServerUrl(serverUrl).check(new ServerUrl(SensorsDataAPI.getConfigOptions().getServerUrl())))) {
                        return;
                    }
                } else {
                    //防止 H5 集成的 JS SDK 版本太老，没有发 server_url
                    return;
                }
            }
            trackEvent(eventInfo);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void addSAJSListener(final SAJSListener listener) {
        try {
            if (mSAJSListeners == null) {
                mSAJSListeners = new CopyOnWriteArrayList<>();
            }
            if (!mSAJSListeners.contains(listener)) {
                mSAJSListeners.add(listener);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 移除 JS 消息
     *
     * @param listener JS 监听
     */
    public static void removeSAJSListener(final SAJSListener listener) {
        try {
            if (mSAJSListeners != null && mSAJSListeners.contains(listener)) {
                mSAJSListeners.remove(listener);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void handleJsMessage(WeakReference<View> view, final String message) {
        if (mSAJSListeners != null && mSAJSListeners.size() > 0) {
            for (final SAJSListener listener : mSAJSListeners) {
                try {
                    if (listener != null) {
                        listener.onReceiveJSMessage(view, message);
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        }
    }

    private static void trackEvent(final String eventInfo) {
        if (SensorsDataAPI.sharedInstance() instanceof SensorsDataAPIEmptyImplementation) {
            SALog.i("SA.AppWebViewInterface", "trackEvent SensorsDataAPIEmptyImplementation");
            return;
        }
        SACoreHelper.getInstance().trackQueueEvent(new Runnable() {
            @Override
            public void run() {
                SACoreHelper.getInstance().trackEvent(new InputData().setExtras(eventInfo));
            }
        });
    }
}
