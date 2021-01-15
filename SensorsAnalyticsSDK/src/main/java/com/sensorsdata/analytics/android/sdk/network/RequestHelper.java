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

import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import static com.sensorsdata.analytics.android.sdk.util.Base64Coder.CHARSET_UTF8;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;

public class RequestHelper {
    //重定向 URL
    private boolean isRedirected = false;

    /**
     * 网络请求
     *
     * @param url url
     * @param paramsMap 键值对参数
     * @param headerMap 请求头键值对
     * @param retryCount 重试次数
     * @param callBack 请求回调
     */
    private RequestHelper(HttpMethod method, String url, Map<String, String> paramsMap, Map<String, String> headerMap, int retryCount, HttpCallback callBack) {
        switch (method) {
            case GET:
                urlHttpGet(url, paramsMap, headerMap, retryCount, callBack);
                break;
            case POST:
                urlHttpPost(url, paramsMap, "", headerMap, retryCount, callBack);
                break;
        }
    }

    /**
     * POST 请求
     *
     * @param url url
     * @param jsonData json 格式参数
     * @param headerMap 请求头键值对
     * @param retryCount 重试次数
     * @param callBack 请求回调
     */
    private RequestHelper(String url, String jsonData, Map<String, String> headerMap, int retryCount, HttpCallback callBack) {
        urlHttpPost(url, null, jsonData, headerMap, retryCount, callBack);
    }

    /**
     * GET 请求
     *
     * @param url url
     * @param paramsMap 键值对参数
     * @param headerMap 请求头键值对
     * @param retryCount 重试次数
     * @param callBack 请求回调
     */
    private void urlHttpGet(final String url, final Map<String, String> paramsMap, final Map<String, String> headerMap, final int retryCount, final HttpCallback callBack) {
        final int requestCount = retryCount - 1;
        HttpTaskManager.execute(new Runnable() {
            @Override
            public void run() {
                RealResponse response = new RealRequest().getData(getUrl(url, paramsMap), headerMap);
                if (response.code == HTTP_OK || response.code == HTTP_NO_CONTENT) {
                    if (callBack != null) {
                        callBack.onSuccess(response);
                    }
                } else if (!isRedirected && HttpUtils.needRedirects(response.code)) {
                    isRedirected = true;
                    urlHttpGet(response.location, paramsMap, headerMap, retryCount, callBack);
                } else {
                    if (requestCount != 0) {
                        urlHttpGet(url, paramsMap, headerMap, requestCount, callBack);
                    } else {
                        if (callBack != null) {
                            callBack.onError(response);
                        }
                    }
                }
            }
        });
    }

    /**
     * POST 请求
     *
     * @param url url
     * @param paramsMap 键值对参数
     * @param jsonData json 格式参数
     * @param headerMap 请求头键值对
     * @param retryCount 重试次数
     * @param callBack 请求回调
     */
    private void urlHttpPost(final String url, final Map<String, String> paramsMap,
                             final String jsonData, final Map<String, String> headerMap,
                             final int retryCount, final HttpCallback callBack) {
        final int requestCount = retryCount - 1;
        HttpTaskManager.execute(new Runnable() {
            @Override
            public void run() {
                RealResponse response = new RealRequest().postData(url, getPostBody(paramsMap, jsonData), getPostBodyType(paramsMap, jsonData), headerMap);
                if (response.code == HTTP_OK || response.code == HTTP_NO_CONTENT) {
                    if (callBack != null) {
                        callBack.onSuccess(response);
                    }
                } else if (!isRedirected && HttpUtils.needRedirects(response.code)) {
                    isRedirected = true;
                    urlHttpPost(response.location, paramsMap, jsonData, headerMap, retryCount, callBack);
                } else {
                    if (requestCount != 0) {
                        urlHttpPost(url, paramsMap, jsonData, headerMap, requestCount, callBack);
                    } else {
                        if (callBack != null) {
                            callBack.onError(response);
                        }
                    }
                }
            }
        });
    }

    /**
     * GET 请求 url 拼接
     *
     * @param path 请求地址
     * @param paramsMap 参数键值对参数
     * @return GET 请求 url 链接
     */
    private String getUrl(String path, Map<String, String> paramsMap) {
        if (path != null && paramsMap != null) {
            if (!path.contains("?")) {
                path = path + "?";
            } else {
                path = path + ("&");
            }
            for (String key : paramsMap.keySet()) {
                path = path + key + "=" + paramsMap.get(key) + "&";
            }
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * 根据参数得到 body
     *
     * @param params 键值对参数
     * @param jsonStr json 格式参数
     * @return 请求 body
     */
    private String getPostBody(Map<String, String> params, String jsonStr) {
        if (params != null) {
            return getPostBodyFormParamsMap(params);
        } else if (!TextUtils.isEmpty(jsonStr)) {
            return jsonStr;
        }
        return null;
    }

    /**
     * 根据键值对参数得到 body
     *
     * @param params 键值对参数
     * @return 请求 body
     */
    private String getPostBodyFormParamsMap(Map<String, String> params) {
        if (params != null) {
            StringBuilder result = new StringBuilder();
            boolean first = true;
            try {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (first) {
                        first = false;
                    } else {
                        result.append("&");
                    }
                    result.append(URLEncoder.encode(entry.getKey(), CHARSET_UTF8));
                    result.append("=");
                    result.append(URLEncoder.encode(entry.getValue(), CHARSET_UTF8));
                }
                return result.toString();
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取请求的 Content-Type
     *
     * @param paramsMap 请求参数
     * @param jsonStr 请求参数 json 字符串
     * @return Content-Type
     */
    private String getPostBodyType(Map<String, String> paramsMap, String jsonStr) {
        if (paramsMap != null) {
            return null;
        } else if (!TextUtils.isEmpty(jsonStr)) {
            return "application/json;charset=utf-8";
        }
        return null;
    }

    public static class Builder {

        private HttpMethod httpMethod;
        private String httpUrl;
        private Map<String, String> paramsMap;
        private String jsonData;
        private Map<String, String> headerMap;
        private HttpCallback callBack;
        private int retryCount = 1;

        public Builder(HttpMethod method, String url) {
            this.httpMethod = method;
            this.httpUrl = url;
        }

        public Builder params(Map<String, String> paramsMap) {
            this.paramsMap = paramsMap;
            return this;
        }

        public Builder jsonData(String data) {
            this.jsonData = data;
            return this;
        }

        public Builder header(Map<String, String> headerMap) {
            this.headerMap = headerMap;
            return this;
        }

        public Builder callback(HttpCallback callBack) {
            this.callBack = callBack;
            return this;
        }

        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public void execute() {
            if (httpMethod == HttpMethod.POST && paramsMap == null) {
                new RequestHelper(httpUrl, jsonData, headerMap, retryCount, callBack);
            } else {
                new RequestHelper(httpMethod, httpUrl, paramsMap, headerMap, retryCount, callBack);
            }
        }
    }
}