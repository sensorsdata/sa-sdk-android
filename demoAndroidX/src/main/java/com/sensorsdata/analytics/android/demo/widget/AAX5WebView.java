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

import java.security.PKCS12Attribute;
import java.util.Map;

public class AAX5WebView extends AX5WebView {

    public AAX5WebView(Context context) {
        super(context);
    }

    public AAX5WebView(Context context, boolean b) {
        super(context, b);
    }

    public AAX5WebView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public AAX5WebView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public AAX5WebView(Context context, AttributeSet attributeSet, int i, boolean b) {
        super(context, attributeSet, i, b);
    }

    public void customMethod(String url){
        //do something
        loadUrl(url);
    }

    @Override
    public void loadUrl(String s, Map<String, String> map) {
        loadData("","","");
    }

    @Override
    public void loadUrl(String s) {
        super.loadUrl(s);
    }

    @Override
    public void postUrl(String s, byte[] bytes) {
        super.postUrl(s, bytes);
    }

    @Override
    public void loadData(String s, String s1, String s2) {
        super.loadData(s, s1, s2);
    }

    @Override
    public void loadDataWithBaseURL(String s, String s1, String s2, String s3, String s4) {
        super.loadDataWithBaseURL(s, s1, s2, s3, s4);
    }
}
