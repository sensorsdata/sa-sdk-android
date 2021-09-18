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

package com.sensorsdata.analytics.android.sdk.visual.property;

import android.text.TextUtils;
import android.util.Base64;
import android.util.SparseArray;
import android.view.View;

import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.listener.SAEventListener;
import com.sensorsdata.analytics.android.sdk.util.Base64Coder;
import com.sensorsdata.analytics.android.sdk.visual.ViewTreeStatusObservable;
import com.sensorsdata.analytics.android.sdk.visual.bridge.JSBridgeHelper;
import com.sensorsdata.analytics.android.sdk.visual.bridge.OnBridgeCallback;
import com.sensorsdata.analytics.android.sdk.visual.bridge.WebViewJavascriptBridge;
import com.sensorsdata.analytics.android.sdk.visual.model.ViewNode;
import com.sensorsdata.analytics.android.sdk.visual.model.VisualConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class VisualPropertiesH5Helper implements WebViewJavascriptBridge {

    private JSBridgeHelper mJSBridgeHelper;
    private SAEventListener mSAEventListener;
    private SparseArray<JSONArray> mSparseArray = new SparseArray<>();

    public VisualPropertiesH5Helper() {
        mJSBridgeHelper = new JSBridgeHelper();
    }

    void mergeJSVisualProperties(final JSONObject srcObject, HashSet<String> hashSet, String eventName) {
        try {
            if (hashSet == null || hashSet.size() == 0) {
                return;
            }
            Iterator<String> entries = hashSet.iterator();
            final CountDownLatch latch = new CountDownLatch(hashSet.size());
            while (entries.hasNext()) {
                String webViewElementPath = entries.next();
                ViewNode viewNode = ViewTreeStatusObservable.getInstance().getViewNode(webViewElementPath);
                if (viewNode != null && viewNode.getView() != null) {
                    View view = viewNode.getView().get();
                    if (view != null) {
                        getJSVisualProperties(view, viewNode.getViewPath(), eventName, new OnBridgeCallback() {
                            @Override
                            public void onCallBack(String data) {
                                try {
                                    JSONObject obj = new JSONObject(data);
                                    Iterator<String> iterator = obj.keys();
                                    while (iterator.hasNext()) {
                                        String key = iterator.next();
                                        String value = obj.optString(key);
                                        // WebClick 自定义属性优先级最高
                                        if (!TextUtils.isEmpty(key)) {
                                            srcObject.put(key, value);
                                        }
                                    }
                                } catch (JSONException e) {
                                    SALog.printStackTrace(e);
                                } finally {
                                    latch.countDown();
                                }
                            }
                        });
                    }
                }
            }
            try {
                latch.await(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                SALog.printStackTrace(e);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private void getJSVisualProperties(View webView, String elementPath, String eventName, OnBridgeCallback onBridgeCallback) {
        try {
            JSONArray array = VisualPropertiesManager.getInstance().getVisualPropertiesCache().getH5JsonArrayFromCache(eventName, elementPath);
            if (array == null) {
                return;
            }
            JSONObject obj = new JSONObject();
            try {
                obj.put("sensorsdata_js_visual_properties", array);
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
            sendToWeb(webView, "getJSVisualProperties", obj, onBridgeCallback);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 注册监听
     */
    public void registerListeners() {
        try {
            mJSBridgeHelper.addSAJSListener();
            addSAEventListener();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private void addSAEventListener() {
        if (mSAEventListener == null) {
            mSAEventListener = new SAEventListener() {
                @Override
                public void trackEvent(JSONObject jsonObject) {
                    try {
                        String eventType = jsonObject.optString("event");
                        if (!TextUtils.equals(AopConstants.WEB_CLICK_EVENT_NAME, eventType)) {
                            return;
                        }
                        JSONObject propertiesObj = jsonObject.optJSONObject("properties");
                        if (propertiesObj == null) {
                            return;
                        }

                        if (propertiesObj.has("sensorsdata_web_visual_eventName")) {
                            mSparseArray.put(jsonObject.hashCode(), propertiesObj.optJSONArray("sensorsdata_web_visual_eventName"));
                            propertiesObj.remove("sensorsdata_web_visual_eventName");
                        }

                        String base64Message = propertiesObj.optString("sensorsdata_app_visual_properties");
                        propertiesObj.remove("sensorsdata_app_visual_properties");
                        if (TextUtils.isEmpty(base64Message)) {
                            return;
                        }
                        String appVisualProperties = Base64Coder.decodeString(base64Message);
                        if (TextUtils.isEmpty(appVisualProperties)) {
                            return;
                        }
                        try {
                            JSONArray array = new JSONArray(appVisualProperties);
                            if (array.length() > 0) {
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject obj = array.getJSONObject(i);
                                    VisualConfig.VisualProperty visualProperty = new VisualConfig.VisualProperty();
                                    visualProperty.elementPath = obj.optString("element_path");
                                    visualProperty.elementPosition = obj.optString("element_position");
                                    visualProperty.screenName = obj.optString("screen_name");
                                    visualProperty.name = obj.optString("name");
                                    visualProperty.regular = obj.optString("regular");
                                    visualProperty.isH5 = obj.optBoolean("h5");
                                    visualProperty.type = obj.optString("type");
                                    visualProperty.webViewElementPath = obj.optString("webview_element_path");
                                    VisualPropertiesManager.getInstance().mergeAppVisualProperty(visualProperty, null, propertiesObj, null);
                                }
                            }
                        } catch (JSONException e) {
                            SALog.printStackTrace(e);
                        }
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }

                @Override
                public void login() {

                }

                @Override
                public void logout() {

                }

                @Override
                public void identify() {

                }

                @Override
                public void resetAnonymousId() {

                }
            };
            SensorsDataAPI.sharedInstance().addEventListener(mSAEventListener);
        }
    }

    public JSONArray getEventName(int hashCode) {
        try {
            return mSparseArray.get(hashCode);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    public void clearCache(int hashCode) {
        try {
            mSparseArray.remove(hashCode);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void sendToWeb(View webView, String methodName, Object data, OnBridgeCallback
            responseCallback) {
        mJSBridgeHelper.sendToWeb(webView, methodName, data, responseCallback);
    }

    @Override
    public void sendToWeb(View webView, String methodName, Object data) {
        mJSBridgeHelper.sendToWeb(webView, methodName, data);
    }

    private static String Base642string(String s) {
        return new String(Base64.decode(s.getBytes(), Base64.DEFAULT));
    }
}