/*
 * Created by dengshiwei on 2023/10/08.
 * Copyright 2015－2023 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.advert.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SAAdvertisingConfig;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.advert.SAAdvertConstants;
import com.sensorsdata.analytics.android.sdk.advert.deeplink.DeepLinkManager;
import com.sensorsdata.analytics.android.sdk.advert.oaid.SAOaidHelper;
import com.sensorsdata.analytics.android.sdk.core.SACoreHelper;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentDailyDate;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;

import org.json.JSONObject;

public class SAAdvertMarketHelper {

    /**
     * 处理在营销
     *
     * @param activity Activity
     * @param saAdvertisingConfig SAAdvertisingConfig
     */
    public static void handleAdMarket(final Activity activity, final SAAdvertisingConfig saAdvertisingConfig) {
        try {
            if (saAdvertisingConfig != null && saAdvertisingConfig.isEnableRemarketing() && isDailyFirst()) {
                SACoreHelper.getInstance().trackQueueEvent(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Intent uriIntent = null;
                            if (!TextUtils.isEmpty(saAdvertisingConfig.getWakeupUrl())) {
                                uriIntent = Intent.parseUri(saAdvertisingConfig.getWakeupUrl(), Intent.URI_INTENT_SCHEME);
                            }
                            // 触发埋点事件
                            Context context = activity.getApplicationContext();
                            JSONObject propertyJson = new JSONObject();
                            propertyJson.put("$ios_install_source",
                                    ChannelUtils.getDeviceInfo(context,
                                            SensorsDataUtils.getIdentifier(context),
                                            SAOaidHelper.getOpenAdIdentifier(context),
                                            SAOaidHelper.getOpenAdIdentifierByReflection(context)));
                            propertyJson.put("$sat_awake_from_deeplink", DeepLinkManager.isDeepLink(activity.getIntent()) || DeepLinkManager.isDeepLink(uriIntent));
                            propertyJson.put("$sat_has_installed_app", SAAdvertUtils.isInstallationTracked());
                            SACoreHelper.getInstance().trackEvent(new InputData().setEventType(EventType.TRACK)
                                    .setEventName(SAAdvertConstants.EventName.APP_INTERACT).setProperties(propertyJson));
                        } catch (Exception e) {
                            SALog.printStackTrace(e);
                        }
                    }
                });
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static boolean isDailyFirst() {
        PersistentDailyDate dailyDate = PersistentLoader.getInstance().getDayDatePst();
        String currentDate = TimeUtils.formatTime(System.currentTimeMillis(), TimeUtils.YYYY_MM_DD);
        if (!currentDate.equals(dailyDate.get())) {
            dailyDate.commit(currentDate);
            return true;
        }
        return false;
    }
}
