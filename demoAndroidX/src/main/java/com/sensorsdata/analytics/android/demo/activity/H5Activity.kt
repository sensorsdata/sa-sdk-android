/*
 * Created by chenru on 2019/06/20.
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

package com.sensorsdata.analytics.android.demo.activity

import android.os.Bundle
import android.webkit.JavascriptInterface
import com.sensorsdata.analytics.android.demo.R
import com.sensorsdata.analytics.android.demo.widget.AAWebView
import com.sensorsdata.analytics.android.demo.widget.AAX5WebView
import com.sensorsdata.analytics.android.demo.widget.AWebView
import com.sensorsdata.analytics.android.demo.widget.AX5WebView
import com.sensorsdata.analytics.android.sdk.SALog
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebView
import kotlinx.android.synthetic.main.activity_h5.*
import org.json.JSONObject

class H5Activity : BaseActivity() {
    private val TAG: String = "H5Activity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_h5)
        initWebView()

        x5WebView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView, title: String) {
                supportActionBar!!.title = title
            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                SALog.i(TAG, "current progress: $newProgress")
            }
        }

        x5WebView.loadUrl("file:///android_asset/new_h5_test/index.html")
        //x5WebView.addJavascriptInterface(JsObject(), "sensorsDataObj")
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
//        }
        //SensorsDataAPI.sharedInstance().showUpX5WebView(x5WebView, true)


        //SensorsDataAPI.sharedInstance().showUpWebView(androidWebView, true,true)
        //androidWebView.loadUrl("https://869359954.github.io/App_H5_traffic_sdk/index.html")
        //本地新版 H5
        androidWebView.loadUrl("file:///android_asset/new_h5_test/index.html")
        //旧版 H5
        //androidWebView.loadUrl("https://fengandyun.github.io/apph5select/index.html")

        //SensorsDataAPI.sharedInstance().showUpWebView(androidWebView, true, true)

    }

    private fun webview() {
        run {
            val webView = AWebView(this)
            val testUrl1 = "hhh"
            webView.loadUrl(testUrl1 + "uiuiuiuiuiuiui")
            webView.loadData("data1", "html", "utf8")
            webView.loadUrl("https://www.ss.cn", mapOf("header" to "h1"))
            webView.loadDataWithBaseURL("http://www.base.cn", "h1=5", "text", "utf8", "http://www.hisotory.cn")
            webView.postUrl("sss" , null)
        }

        run {
            val aaWebView = AAWebView(this)
            val testUrl1 = "hhh"
            aaWebView.loadUrl(testUrl1 + "uiuiuiuiuiuiui")
            aaWebView.loadData("data1", "html", "utf8")
            aaWebView.loadUrl("https://www.ss.cn", mapOf("header" to "h1"))
            aaWebView.loadDataWithBaseURL("http://www.base.cn", "h1=5", "text", "utf8", "http://www.hisotory.cn")
        }

        run {
            val webView = AX5WebView(this)
            val testUrl1 = "hhh"
            webView.loadUrl(testUrl1 + "uiuiuiuiuiuiui")

            webView.loadData("data1", "html", "utf8")
            webView.loadUrl("https://www.ss.cn", mapOf("header" to "h1"))
            webView.loadDataWithBaseURL("http://www.base.cn", "h1=5", "text", "utf8", "http://www.hisotory.cn")
        }

        run {
            val webView = AAX5WebView(this)
            val testUrl1 = "hhh"
            webView.loadUrl(testUrl1 + "uiuiuiuiuiuiui")
            webView.loadData("data1", "html", "utf8")
            webView.loadUrl("https://www.ss.cn", mapOf("header" to "h1"))
            webView.loadDataWithBaseURL("http://www.base.cn", "h1=5", "text", "utf8", "http://www.hisotory.cn")
        }

    }

    private class JsObject {
        @JavascriptInterface
        fun track(obj: JSONObject) {
            SALog.i("JsObject", "from h5: $obj")
        }
    }

    private fun initWebView() {
        val settings = x5WebView.settings
        settings.javaScriptEnabled = true
    }
}