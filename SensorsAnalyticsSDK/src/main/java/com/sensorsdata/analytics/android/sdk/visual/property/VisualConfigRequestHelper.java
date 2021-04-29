/*
 * Created by zhangxiangwei on 2021/01/28.
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

import android.content.Context;
import android.net.Uri;
import android.os.CountDownTimer;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.network.HttpCallback;
import com.sensorsdata.analytics.android.sdk.network.HttpMethod;
import com.sensorsdata.analytics.android.sdk.network.RequestHelper;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;


/**
 * 可视化全埋点自定义属性配置请求
 */
public class VisualConfigRequestHelper {

    private static final String TAG = "SA.VP.VisualConfigRequestHelper";
    // 每次启动 App 时，最多尝试三次
    private CountDownTimer mCountDownTimer;

    public interface IApiCallback {
        void onSuccess(String message);
    }

    public void requestVisualConfig(final Context context, final String version, final IApiCallback callback) {
        if (!SensorsDataAPI.sharedInstance().isNetworkRequestEnable()) {
            SALog.i(TAG, "Close network request");
            return;
        }
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
        mCountDownTimer = new CountDownTimer(90 * 1000, 30 * 1000) {
            @Override
            public void onTick(long l) {
                try {
                    String serverUrl = SensorsDataAPI.sharedInstance().getServerUrl();
                    if (TextUtils.isEmpty(serverUrl)) {
                        SALog.i(TAG, "visualConfigRequest server url is null and return");
                        return;
                    }
                    final String requestUrl = getRequestUrl(context, version);
                    if (TextUtils.isEmpty(requestUrl)) {
                        SALog.i(TAG, "visualConfigRequest request url is null and return");
                        return;
                    }
                    new RequestHelper.Builder(HttpMethod.GET, requestUrl)
                            .callback(new HttpCallback.StringCallback() {
                                @Override
                                public void onFailure(int code, String errorMessage) {
                                    if (code == 304 || code == 404 || code == 205) {
                                        resetTimer();
                                        // 后端开关被禁用时需要清除本地缓存
                                        if (code == 205) {
                                            VisualPropertiesManager.getInstance().save2Cache("");
                                        }
                                        SALog.i(TAG, "requestVisualConfig return 304 Or 404");
                                    }
                                }

                                @Override
                                public void onResponse(String response) {
                                    resetTimer();
                                    if (callback != null) {
                                        callback.onSuccess(response);
                                    }
                                    SALog.i(TAG, "requestVisualConfig success response is " + response);
                                }

                                @Override
                                public void onAfter() {
                                }
                            }).execute();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }

            @Override
            public void onFinish() {

            }

        };
        mCountDownTimer.start();
    }

    private void resetTimer() {
        try {
            if (mCountDownTimer != null) {
                mCountDownTimer.cancel();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        } finally {
            mCountDownTimer = null;
        }
    }

    private String getRequestUrl(Context context, String version) {
        if (context == null) {
            SALog.i(TAG, "getRequestUrl context is null and return");
            return null;
        }
        String serverUrl = SensorsDataAPI.sharedInstance().getServerUrl();
        if (TextUtils.isEmpty(serverUrl)) {
            SALog.i(TAG, "visualConfigRequest server url is null and return");
            return null;
        }

        String baseUrl = null;
        int pathPrefix = serverUrl.lastIndexOf("/");
        if (pathPrefix != -1) {
            baseUrl = serverUrl.substring(0, pathPrefix) + "/config/visualized/Android.conf";
        }

        if (!TextUtils.isEmpty(baseUrl)) {
            Uri configUri = Uri.parse(baseUrl);
            Uri.Builder builder = configUri.buildUpon();
            if (!TextUtils.isEmpty(version)) {
                builder.appendQueryParameter("v", version);
            }
            Uri uri = Uri.parse(serverUrl);
            String project = uri.getQueryParameter("project");
            if (!TextUtils.isEmpty(project)) {
                builder.appendQueryParameter("project", project);
            }

            String appId = AppInfoUtils.getProcessName(context);
            if (!TextUtils.isEmpty(appId)) {
                builder.appendQueryParameter("app_id", appId);
            }
            return builder.build().toString();
        }
        return null;
    }
}
