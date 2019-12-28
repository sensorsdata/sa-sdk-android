/*
 * Created by chenru on 2019/06/20.
 * Copyright 2015－2020 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.demo.activity

import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.sensorsdata.analytics.android.demo.R
import com.sensorsdata.analytics.android.sdk.SALog
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI
import kotlinx.android.synthetic.main.activity_h5.*
import org.json.JSONObject

class H5Activity : BaseActivity() {
    private val TAG: String = "H5Activity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_h5)
        initWebView()

        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView, title: String) {
                supportActionBar!!.title = title
            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                SALog.i(TAG, "current progress: $newProgress")
            }
        }

        webView.loadUrl("file:///android_asset/index.html")
        webView.addJavascriptInterface(JsObject(), "sensorsDataObj")
        SensorsDataAPI.sharedInstance().showUpWebView(webView, false, true)
    }

    private class JsObject {
        @JavascriptInterface
        fun track(obj: JSONObject) {
            SALog.i("JsObject", "from h5: $obj")
        }
    }

    private fun initWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true

    }
}