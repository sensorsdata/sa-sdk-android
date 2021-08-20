/*
 * Created by dengshiwei on 2020/11/26.
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

package com.sensorsdata.analytics.android.sdk.internal.rpc;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;

/**
 * 用于跨进程业务的数据通信
 */
public class SensorsDataContentObserver extends ContentObserver {
    public static boolean isEnableFromObserver = false;
    public static boolean isDisableFromObserver = false;
    public static boolean isLoginFromObserver = false;

    public SensorsDataContentObserver() {
        super(new Handler(Looper.getMainLooper()));
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        try {
            if (DbParams.getInstance().getDataCollectUri().equals(uri)) {
                SensorsDataAPI.sharedInstance().enableDataCollect();
            } else if (DbParams.getInstance().getSessionTimeUri().equals(uri)) {
                SensorsDataAPI.sharedInstance().setSessionIntervalTime(DbAdapter.getInstance().getSessionIntervalTime());
            } else if (DbParams.getInstance().getLoginIdUri().equals(uri)) {
                String loginId = DbAdapter.getInstance().getLoginId();
                if (TextUtils.isEmpty(loginId)) {
                    SensorsDataAPI.sharedInstance().logout();
                } else {
                    isLoginFromObserver = true;
                    SensorsDataAPI.sharedInstance().login(loginId);
                }
            } else if (DbParams.getInstance().getDisableSDKUri().equals(uri)) {
                if (!SensorsDataAPI.getConfigOptions().isDisableSDK()) {
                    isDisableFromObserver = true;
                    SensorsDataAPI.disableSDK();
                }
            } else if (DbParams.getInstance().getEnableSDKUri().equals(uri)) {
                if (SensorsDataAPI.getConfigOptions().isDisableSDK()) {
                    isEnableFromObserver = true;
                    SensorsDataAPI.enableSDK();
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}