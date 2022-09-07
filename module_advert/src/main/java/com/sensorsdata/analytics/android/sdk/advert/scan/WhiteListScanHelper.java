/*
 * Created by chenru on 2022/7/5 下午6:03.
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

package com.sensorsdata.analytics.android.sdk.advert.scan;

import android.app.Activity;
import android.net.Uri;
import android.text.TextUtils;

import com.sensorsdata.analytics.advert.R;
import com.sensorsdata.analytics.android.sdk.advert.oaid.SAOaidHelper;
import com.sensorsdata.analytics.android.sdk.advert.utils.SAAdvertUtils;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.ServerUrl;
import com.sensorsdata.analytics.android.sdk.dialog.SensorsDataDialogUtils;
import com.sensorsdata.analytics.android.sdk.network.HttpCallback;
import com.sensorsdata.analytics.android.sdk.network.HttpMethod;
import com.sensorsdata.analytics.android.sdk.network.RequestHelper;
import com.sensorsdata.analytics.android.sdk.util.SADisplayUtil;
import com.sensorsdata.analytics.android.sdk.util.ToastUtil;

import org.json.JSONObject;

public class WhiteListScanHelper implements IAdvertScanListener {
    @Override
    public void handlerScanUri(Activity activity, Uri uri) {
        String platform = uri.getQueryParameter("device_type");
        if (!"2".equals(platform)) {// iOS：1，Android：2
            ToastUtil.showLong(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_whitelist_platform_error));
            SensorsDataDialogUtils.startLaunchActivity(activity);
            return ;
        }
        String url = uri.getQueryParameter("apiurl");
        String info_id = uri.getQueryParameter("info_id");
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(info_id)) {
            ToastUtil.showLong(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_whitelist_request_falied));
            SensorsDataDialogUtils.startLaunchActivity(activity);
            return ;
        }
        String project = uri.getQueryParameter("project");
        if (TextUtils.isEmpty(project)) {
            project = "default";
        }
        ServerUrl serverUrl = new ServerUrl(SensorsDataAPI.sharedInstance().getServerUrl());
        if (!project.equals(serverUrl.getProject())) {
            ToastUtil.showLong(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_whitelist_project_error));
            SensorsDataDialogUtils.startLaunchActivity(activity);
            return ;
        }
        updateWhitelist(activity, url, info_id, project);
    }

    private static void updateWhitelist(final Activity activity, final String url, final String info_id, final String project) {
        new Thread() {
            @Override
            public void run() {
                try {
                    JSONObject json = new JSONObject();
                    json.put("android_oaid", SAOaidHelper.getOpenAdIdentifier(activity));
                    json.put("android_id", SAAdvertUtils.getIdentifier(activity));
                    json.put("android_imei", SAAdvertUtils.getInstallSource(activity));
                    json.put("info_id", info_id);
                    json.put("project_name", project);
                    json.put("device_type", "2");
                    new RequestHelper.Builder(HttpMethod.POST, url)
                            .jsonData(json.toString()).callback(new HttpCallback.JsonCallback() {
                        @Override
                        public void onFailure(int code, String errorMessage) {
                            ToastUtil.showLong(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_whitelist_request_falied));
                        }

                        @Override
                        public void onResponse(JSONObject response) {
                            int code = response.optInt("code", -1);
                            if (code == 0) {
                                ToastUtil.showLong(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_whitelist_request_success));
                            } else {
                                ToastUtil.showLong(activity, SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_ad_whitelist_request_falied));
                            }
                        }

                        @Override
                        public void onAfter() {
                            SensorsDataDialogUtils.startLaunchActivity(activity);
                        }
                    }).execute();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        }.start();
    }
}
