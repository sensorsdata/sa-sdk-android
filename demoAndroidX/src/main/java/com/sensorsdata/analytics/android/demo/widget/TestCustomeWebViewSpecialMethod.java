/*
 * Created by zhangwei on 2020/08/15.
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
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.Nullable;

class TestCustomeWebViewSpecialMethod extends View {

    public TestCustomeWebViewSpecialMethod(Context context) {
        super(context);
    }

    public TestCustomeWebViewSpecialMethod(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TestCustomeWebViewSpecialMethod(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //需要能够正常插入代码
    public void loadUrl(String str){
        WebView webView = null;
        webView.loadUrl(str);
    }
}
