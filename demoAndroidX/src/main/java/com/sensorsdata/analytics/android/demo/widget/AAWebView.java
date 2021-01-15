/*
 * Created by zhangwei on 2020/03/26.
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

package com.sensorsdata.analytics.android.demo.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import java.util.Map;

public class AAWebView extends AWebView {

    public AAWebView(Context context) {
        super(context);
    }

    public AAWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AAWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AAWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void customMethod(String url){
        //do something
        loadUrl(url);
    }

    @Override
    public void loadUrl(String s) {
        loadUrl(s, null);
    }

    @Override
    public void loadUrl(String url, Map<String, String> additionalHttpHeaders) {
        super.loadUrl(url, additionalHttpHeaders);
    }

    @Override
    public void postUrl(String url, byte[] postData) {
        super.postUrl(url, postData);
    }

    @Override
    public void loadDataWithBaseURL(@Nullable String baseUrl, String data, @Nullable String mimeType, @Nullable String encoding, @Nullable String historyUrl) {
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }
}
