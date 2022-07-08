package com.sensorsdata.analytics.android.sdk.visual;

import android.text.TextUtils;
import android.view.View;
import android.webkit.JavascriptInterface;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.util.ReflectUtil;

import java.lang.ref.WeakReference;

public class WebViewVisualInterface {

    private static final String TAG = "SA.Visual.WebViewVisualInterface";
    private WeakReference<View> mWebView;

    public WebViewVisualInterface(View webView) {
        this.mWebView = new WeakReference(webView);
    }

    /**
     * JS 给 App 提供 H5 页面数据（只有当 sensorsdata_visualized_mode = true 时 JS 才返回数据）
     *
     * @param msg H5 页面数据
     */
    @JavascriptInterface
    public void sensorsdata_hover_web_nodes(final String msg) {
        try {
            WebNodesManager.getInstance().handlerMessage(msg);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 提供给 JS 判断当前是否正在使用可视化埋点或者点击图
     *
     * @return true 表示正在进行可视化埋点
     */
    @JavascriptInterface
    public boolean sensorsdata_visualized_mode() {
        return VisualizedAutoTrackService.getInstance().isServiceRunning() || HeatMapService.getInstance().isServiceRunning();
    }

    @JavascriptInterface
    public void sensorsdata_visualized_alert_info(final String msg) {
        try {
            SALog.i(TAG, "sensorsdata_visualized_alert_info msg: " + msg);
            if (mWebView.get() != null) {
                mWebView.get().post(new Runnable() {
                    @Override
                    public void run() {
                        String url = ReflectUtil.callMethod(mWebView.get(), "getUrl");
                        if (!TextUtils.isEmpty(url)) {
                            SALog.i(TAG, "sensorsdata_visualized_alert_info url: " + url);
                            WebNodesManager.getInstance().handlerFailure(url, msg);
                        }
                    }
                });
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
