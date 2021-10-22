/*
 * Created by yuejianzhong on 2021/05/10.
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

package com.sensorsdata.analytics.android.sdk.dialog;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPIEmptyImplementation;
import com.sensorsdata.analytics.android.sdk.SensorsDataIgnoreTrackAppViewScreen;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

@SensorsDataIgnoreTrackAppViewScreen
public class SchemeActivity extends Activity {

    private static final String TAG = "SA.SchemeActivity";

    /**
     * 是否是弹窗扫码
     */
    public static boolean isPopWindow = false;

    /**
     * 是否拉起 App
     */
    private boolean isStartApp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SALog.i(TAG, "onCreate");
        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                setTheme(android.R.style.Theme_DeviceDefault_Light);
            } else {
                setTheme(android.R.style.Theme_Light);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        //未初始化 SDK 时，直接拉起 LaunchActivity
        if (SensorsDataAPI.sharedInstance() instanceof SensorsDataAPIEmptyImplementation) {
            SensorsDataDialogUtils.startLaunchActivity(this);
        } else {
            SensorsDataUtils.handleSchemeUrl(this, this.getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //未初始化 SDK 时，直接拉起 LaunchActivity
        if (SensorsDataAPI.sharedInstance() instanceof SensorsDataAPIEmptyImplementation) {
            SensorsDataDialogUtils.startLaunchActivity(this);
        } else {
            SensorsDataUtils.handleSchemeUrl(this, this.getIntent());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SALog.i(TAG, "onResume");
        if (isStartApp) {
            isStartApp = false;
            SensorsDataDialogUtils.startLaunchActivity(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        SALog.i(TAG, "onPause");
        if (isPopWindow) {
            isPopWindow = false;
            isStartApp = true;
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}