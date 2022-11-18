package com.sensorsdata.analytics.android.sdk.visual;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.autotrack.aop.SensorsDataAutoTrackHelper;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.business.visual.SAVisualTools;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.visual.property.VisualPropertiesManager;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class SensorsDataVisualUnitTest {
    Application mApplication = ApplicationProvider.getApplicationContext();
    SensorsDataAPI mSensorsDataAPI = null;
    VisualActivity mActivity = Robolectric.setupActivity(VisualActivity.class);
    H5VisualTestActivity mH5VisualTestActivity = Robolectric.setupActivity(H5VisualTestActivity.class);
    String featureCode = "sdgce7b04ba0xTt4WxYh8";
    String postUrl = "http://10.120.111.143:8107/api/v2/sdg/sdk/qr_code/upload?project=default";

    @Test
    public void showOpenVisualizedAutoTrackDialog() {
        setUp();
        SAVisualTools.showOpenVisualizedAutoTrackDialog(mActivity, featureCode, postUrl);
    }

    @Test
    public void showOpenHeatMapDialog() {
        setUp();
        SAVisualTools.showOpenHeatMapDialog(mActivity, featureCode, postUrl);
    }

    @Test
    public void showPairingCodeInputDialog() {
        setUp();
        SAVisualTools.showPairingCodeInputDialog(mActivity.getBaseContext());
    }

    @Test
    public void startVisual() {
        setUp();
        VisualizedAutoTrackService.getInstance().start(mActivity, featureCode, postUrl);
        VisualizedAutoTrackService.getInstance().start(mH5VisualTestActivity, featureCode, postUrl);
    }

    @Test
    public void startHeatMap() {
        setUp();
        HeatMapService.getInstance().start(mActivity, featureCode, postUrl);
        VisualizedAutoTrackService.getInstance().start(mH5VisualTestActivity, featureCode, postUrl);
    }

    @Test
    public void stopVisual() {
        setUp();
        VisualizedAutoTrackService.getInstance().stop();
    }

    @Test
    public void stopHeatMap() {
        setUp();
        HeatMapService.getInstance().stop();
    }

    @Test
    public void resumeVisual() {
        setUp();
        VisualizedAutoTrackService.getInstance().resume();
    }

    @Test
    public void resumeHeatMap() {
        setUp();
        HeatMapService.getInstance().resume();
    }

    @Test
    public void requestVisualConfig() {
        setUp();
        VisualPropertiesManager.getInstance().requestVisualConfig(SensorsDataAPI.sharedInstance().getSAContextManager());
    }

    @Test
    public void trackViewOnClick() {
        setUp();
        View view = new View(mActivity.getBaseContext());
        SensorsDataAutoTrackHelper.trackViewOnClick(view, true);
    }

    @Test
    public void trackH5ViewOnClick() {
        setUp();
        View view = new View(mH5VisualTestActivity.getBaseContext());
        SensorsDataAutoTrackHelper.trackViewOnClick(view, true);
    }

    public SensorsDataAPI setUp() {
        mSensorsDataAPI = null;
        mSensorsDataAPI = SAHelper.initSensors(mApplication);
        return mSensorsDataAPI;
    }

    public static class VisualActivity extends Activity {
        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setTheme(android.R.style.Theme_Material);
        }
    }

    public static class H5VisualTestActivity extends Activity {

        private WebView mWebView;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setTheme(android.R.style.Theme_Material);
            mWebView = new WebView(this);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        view.loadUrl(request.getUrl().toString());
                    }
                    return true;
                }
            });
            mWebView.loadUrl("https://869359954.github.io/sadefine/vue/index.html");
        }

        @Override
        protected void onDestroy() {
            if (mWebView != null) {
                mWebView.clearHistory();
                ((ViewGroup) mWebView.getParent()).removeView(mWebView);
                mWebView.destroy();
                mWebView = null;
            }
            super.onDestroy();
        }

        @Override
        protected void onNewIntent(Intent intent) {
            super.onNewIntent(intent);
            SensorsDataUtils.handleSchemeUrl(this, intent);
        }
    }
}