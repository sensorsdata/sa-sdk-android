package com.sensorsdata.analytics.android.sdk.visual;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.core.mediator.ModuleConstants;
import com.sensorsdata.analytics.android.sdk.core.mediator.visual.SAVisualProtocol;
import com.sensorsdata.analytics.android.sdk.util.H5Util;
import com.sensorsdata.analytics.android.sdk.visual.model.ViewNode;
import com.sensorsdata.analytics.android.sdk.visual.property.VisualPropertiesManager;
import com.sensorsdata.analytics.android.sdk.visual.view.PairingCodeEditDialog;

import org.json.JSONObject;

public class SAVisualProtocolImpl implements SAVisualProtocol {
    private boolean mEnable = false;
    private static final String TAG = "SA.SAVisualProtocolImpl";
    private SAConfigOptions options;

    @Override
    public void install(Context context, SAConfigOptions options) {
        this.options = options;
        if (!options.isDisableSDK()) {
            setModuleState(true);
            //在刚初次初始化的时候状态值不能满足；初始化之后状态值才能满足。
            if (options.isVisualizedPropertiesEnabled()) {
                VisualPropertiesManager.getInstance().requestVisualConfig(context, true);
            }
        }
    }

    @Override
    public void setModuleState(boolean enable) {
        if (mEnable != enable) {
            mEnable = enable;
        }
        if (enable && options.isVisualizedPropertiesEnabled()) {
            // 可视化自定义属性拉取配置
            requestVisualConfig();
        }
    }

    @Override
    public String getModuleName() {
        return ModuleConstants.ModuleName.VISUAL_NAME;
    }

    @Override
    public boolean isEnable() {
        return mEnable;
    }

    @Override
    public void requestVisualConfig() {
        VisualPropertiesManager.getInstance().requestVisualConfig();
    }

    @Override
    public void mergeVisualProperties(JSONObject srcObject, ViewNode viewNode) {
        VisualPropertiesManager.getInstance().mergeVisualProperties(VisualPropertiesManager.VisualEventType.APP_CLICK, srcObject, viewNode);
    }

    @Override
    public String getAppVisualConfig() {
        try {
            VisualPropertiesManager.getInstance().getVisualPropertiesH5Helper().registerListeners();
            String visualCache = VisualPropertiesManager.getInstance().getVisualPropertiesCache().getVisualCache();
            if (!TextUtils.isEmpty(visualCache)) {
                return Base64.encodeToString(visualCache.getBytes(), Base64.DEFAULT);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    @Override
    public void resumeVisualService() {
        VisualizedAutoTrackService.getInstance().resume();
    }

    @Override
    public void stopVisualService() {
        VisualizedAutoTrackService.getInstance().stop();
    }

    @Override
    public void addVisualJavascriptInterface(View webView) {
        if (webView != null && webView.getTag(R.id.sensors_analytics_tag_view_webview_visual) == null) {
            webView.setTag(R.id.sensors_analytics_tag_view_webview_visual, new Object());
            H5Util.addJavascriptInterface(webView, new WebViewVisualInterface(webView), "SensorsData_App_Visual_Bridge");
        }
    }

    @Override
    public void resumeHeatMapService() {
        HeatMapService.getInstance().resume();
    }

    @Override
    public void stopHeatMapService() {
        HeatMapService.getInstance().stop();
    }

    @Override
    public void showPairingCodeInputDialog(final Context context) {
        if (context == null) {
            SALog.i(TAG, "The argument context can't be null");
            return;
        }
        if (!(context instanceof Activity)) {
            SALog.i(TAG, "The static method showPairingCodeEditDialog(Context context) only accepts Activity as a parameter");
            return;
        }
        Activity activity = (Activity) context;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final PairingCodeEditDialog dialog = new PairingCodeEditDialog(context);
                dialog.show();
            }
        });
    }

    @Override
    public void showOpenHeatMapDialog(final Activity context, final String featureCode, final String postUrl) {
        VisualDialog.showOpenHeatMapDialog(context, featureCode, postUrl);
    }

    @Override
    public void showOpenVisualizedAutoTrackDialog(final Activity context, final String featureCode, final String postUrl) {
        VisualDialog.showOpenVisualizedAutoTrackDialog(context, featureCode, postUrl);
    }
}
