/*
 * Created by dengshiwei on 2022/12/26.
 * Copyright 2015－2022 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.advert.deeplink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.SAHelper;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataTrackEventCallBack;
import com.sensorsdata.analytics.android.sdk.advert.utils.ChannelUtils;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;
import com.sensorsdata.analytics.android.sdk.plugin.encrypt.SAStoreManager;
import com.sensorsdata.analytics.android.sdk.plugin.property.SAPropertyPlugin;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertiesFetcher;
import com.sensorsdata.analytics.android.sdk.plugin.property.beans.SAPropertyFilter;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class DeepLinkManagerTest {
    Application mApplication = ApplicationProvider.getApplicationContext();
    SensorsDataAPI mSensorsDataAPI = SAHelper.initSensors(mApplication);
    @Test
    public void parseDeepLink() {
        Activity activity = Robolectric.setupActivity(DeepActivity.class);
        DeepLinkManager.parseDeepLink(activity, true);
    }

    @Test
    public void mergeDeepLinkProperty() {
        JSONObject jsonObject = new JSONObject();
        DeepLinkManager.mergeDeepLinkProperty(jsonObject);
    }

    @Test
    public void resetDeepLinkProcessor() {
        DeepLinkManager.resetDeepLinkProcessor();
    }

    @Test
    public void setDeepLinkCallback() {
        DeepLinkManager.setDeepLinkCallback(null);
    }

    @Test
    public void setDeferredDeepLinkCallback() {
        DeepLinkManager.setDeferredDeepLinkCallback(null);
    }

    @Test
    public void enableDeepLinkInstallSource() {
        DeepLinkManager.enableDeepLinkInstallSource(true);
    }

    @Test
    public void mergeCacheProperties() {
        DeepLinkManager.mergeCacheProperties(null);
    }

    @Test
    public void requestDeferredDeepLink() {
        SAStoreManager.getInstance().remove(DbParams.PersistentName.REQUEST_DEFERRER_DEEPLINK);
        Assert.assertFalse(ChannelUtils.isExistRequestDeferredDeeplink());
        Assert.assertTrue(ChannelUtils.isRequestDeferredDeeplink());
        SensorsDataAPI.sharedInstance().requestDeferredDeepLink(null);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(ChannelUtils.isExistRequestDeferredDeeplink());
        Assert.assertFalse(ChannelUtils.isRequestDeferredDeeplink());
    }

    @Test
    public void testTrackDeeplinkLaunch() throws InterruptedException {
        final CountDownLatch downLatch = new CountDownLatch(1);
        mSensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                if ("$AppDeeplinkLaunch".equals(eventName)) {
                    assertEquals(eventProperties.opt("$deeplink_url"), "www.baidu.com");
                    String iosInstallSource = eventProperties.optString("$ios_install_source");
                    assertTrue(iosInstallSource.contains("custom_oaid"));
                    downLatch.countDown();
                }
                return false;
            }
        });
        mSensorsDataAPI.trackDeepLinkLaunch("www.baidu.com", "custom_oaid");
        downLatch.await(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void createSlink() throws InterruptedException {
        //环境失效，先注销
//        final CountDownLatch downLatch = new CountDownLatch(1);
//        SASlinkCreator creator = new SASlinkCreator(
//                "82F540B23BCA40",
//                "自定义测试",
//                "https://calendar1.slinking.cn/slink/common/short-link",
//                "ETKQ7v3SOL48K105XW1Y2IzeuekewJ1u"
//        );
//        creator.createSLink(mApplication, new SensorsDataCreateSLinkCallback() {
//            @Override
//            public void onReceive(SASlinkResponse response) {
//                assertTrue(TextUtils.isEmpty(response.slink));
////                assertEquals(0, response.statusCode);
//                downLatch.countDown();
//            }
//        });
//        downLatch.await(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void channelCallback() throws InterruptedException {
        final CountDownLatch downLatch = new CountDownLatch(1);
        mSensorsDataAPI.getSAContextManager().getPluginManager().registerPropertyPlugin(new SAPropertyPlugin() {
            @Override
            public boolean isMatchedWithFilter(SAPropertyFilter filter) {
                return filter.getEvent().equals("channel_callback");
            }

            @Override
            public void properties(SAPropertiesFetcher fetcher) {
                assertTrue(fetcher.getProperties().optBoolean("$is_channel_callback_event"));
                assertEquals(fetcher.getProperties().opt("$channel_device_info"), "1");
                downLatch.countDown();
            }
        });
        SensorsDataAPI.sharedInstance().track("channel_callback");
        downLatch.await(1000, TimeUnit.MILLISECONDS);
    }

    public static class DeepActivity extends Activity {
    }
}