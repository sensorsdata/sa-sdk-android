/*
 * Created by chenru on 2020/06/22.
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

package com.sensorsdata.analytics.android.sdk.network;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class HttpCallback<T> {
    static Handler sMainHandler = new Handler(Looper.getMainLooper());

    void onError(final RealResponse response) {
        final String errorMessage;
        if (!TextUtils.isEmpty(response.result)) {
            errorMessage = response.result;
        } else if (!TextUtils.isEmpty(response.errorMsg)) {
            errorMessage = response.errorMsg;
        } else if (response.exception != null) {
            errorMessage = response.exception.toString();
        } else {
            errorMessage = "unknown error";
        }
        sMainHandler.post(new Runnable() {
            @Override
            public void run() {
                onFailure(response.code, errorMessage);
                onAfter();
            }
        });
    }

    void onSuccess(RealResponse response) {
        final T obj;
        obj = onParseResponse(response.result);
        sMainHandler.post(new Runnable() {
            @Override
            public void run() {
                onResponse(obj);
                onAfter();
            }
        });
    }

    /**
     * 解析 Response，执行在子线程
     *
     * @param result 网络请求返回信息
     * @return T
     */
    public abstract T onParseResponse(String result);

    /**
     * 访问网络失败后被调用，执行在 UI 线程
     *
     * @param code 请求返回的错误 code
     * @param errorMessage 错误信息
     */
    public abstract void onFailure(int code, String errorMessage);

    /**
     * 访问网络成功后被调用，执行在 UI 线程
     *
     * @param response 处理后的对象
     */
    public abstract void onResponse(T response);

    /**
     * 访问网络成功或失败后调用
     */
    public abstract void onAfter();

    public static abstract class StringCallback extends HttpCallback<String> {
        @Override
        public String onParseResponse(String result) {
            return result;
        }
    }

    public static abstract class JsonCallback extends HttpCallback<JSONObject> {
        @Override
        public JSONObject onParseResponse(String result) {
            try {
                if (!TextUtils.isEmpty(result)) {
                    return new JSONObject(result);
                }
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
            return null;
        }

        @Override
        public void onAfter() {

        }
    }
}