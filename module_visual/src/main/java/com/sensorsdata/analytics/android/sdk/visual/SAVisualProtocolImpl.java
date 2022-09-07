package com.sensorsdata.analytics.android.sdk.visual;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.ModuleConstants;
import com.sensorsdata.analytics.android.sdk.core.mediator.visual.SAVisualProtocol;
import com.sensorsdata.analytics.android.sdk.monitor.SensorsDataLifecycleMonitorManager;
import com.sensorsdata.analytics.android.sdk.util.H5Util;
import com.sensorsdata.analytics.android.sdk.util.SAViewUtils;
import com.sensorsdata.analytics.android.sdk.visual.model.ViewNode;
import com.sensorsdata.analytics.android.sdk.visual.property.VisualPropertiesManager;
import com.sensorsdata.analytics.android.sdk.visual.utils.AppStateManager;
import com.sensorsdata.analytics.android.sdk.visual.utils.VisualUtil;
import com.sensorsdata.analytics.android.sdk.visual.view.PairingCodeEditDialog;
import com.sensorsdata.analytics.android.sdk.visual.view.VisualDialog;

import org.json.JSONObject;

public class SAVisualProtocolImpl implements SAVisualProtocol {
    private boolean mEnable = false;
    private static final String TAG = "SA.SAVisualProtocolImpl";
    private SAContextManager mSAContextManager;

    @Override
    public void install(SAContextManager contextManager) {
        mSAContextManager = contextManager;
        if (!contextManager.getInternalConfigs().saConfigOptions.isDisableSDK()) {
            setModuleState(true);
        }
        SensorsDataLifecycleMonitorManager.getInstance().addActivityLifeCallback(new AppStateManager());
    }

    @Override
    public void setModuleState(boolean enable) {
        if (mEnable != enable) {
            mEnable = enable;
        }
        if (enable && mSAContextManager.getInternalConfigs().saConfigOptions.isVisualizedPropertiesEnabled()) {
            // 可视化自定义属性拉取配置
            VisualPropertiesManager.getInstance().requestVisualConfig(mSAContextManager);
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
    public int getPriority() {
        return 5;
    }

    @Override
    public void requestVisualConfig() {
        VisualPropertiesManager.getInstance().requestVisualConfig(mSAContextManager);
    }

    @Override
    public void mergeVisualProperties(JSONObject srcObject, View view) {
        try {
            Activity activity = SAViewUtils.getActivityOfView(view.getContext(), view);
            // append $element_path
            ViewNode viewNode = VisualUtil.addViewPathProperties(activity, view, srcObject);
            // append visual custom properties
            if (mSAContextManager.getInternalConfigs().saConfigOptions.isVisualizedPropertiesEnabled()) {
                VisualPropertiesManager.getInstance().mergeVisualProperties(VisualPropertiesManager.VisualEventType.APP_CLICK, srcObject, viewNode);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
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
