/*
 * Created by luweibin on 2022/01/04.
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

package com.sensorsdata.analytics.android.demo.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SensorsAnalyticsAutoTrackEventType;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;

import java.util.List;

public class TestMultiProcessMainActivity extends Activity {
    TextView textView;
    Button bt1, bt2, bt3, bt4, initSdk;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_process_test);
        textView = findViewById(R.id.show_value);
        initSdk = findViewById(R.id.init_sdk);
        bt1 = findViewById(R.id.set_value);
        bt2 = findViewById(R.id.get_value);
        bt3 = findViewById(R.id.go_to_sub_process);
        bt4 = findViewById(R.id.track_a_event);
        bt4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SensorsDataAPI.sharedInstance().track("test_event");
            }
        });
        initSdk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initSensorsDataAPI();
                refreshValue();
            }
        });
        bt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SensorsDataAPI.sharedInstance().login("a" + System.currentTimeMillis());
                refreshValue();
            }
        });
        bt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshValue();
            }
        });
        bt3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TestMultiProcessMainActivity.this, TestMultiProcessSubActivity.class));
            }
        });
    }

    private void refreshValue() {
        String text =
                "processName:   " + getCurrentProcessNameByActivityManager(this) + "\n" +
                        "loginId:       " + SensorsDataAPI.sharedInstance().getLoginId() + "\n" +
                        "distinctId:    " + SensorsDataAPI.sharedInstance().getDistinctId() + "\n" +
                        "anonymousId:   " + SensorsDataAPI.sharedInstance().getAnonymousId() + "\n";
        textView.setText(text);
    }

    private final static String SA_SERVER_URL = "https://sdkdebugtest.datasink.sensorsdata.cn/sa?project=default&token=cfb8b60e42e0ae9b";

    /**
     * 初始化 Sensors Analytics SDK
     */
    private void initSensorsDataAPI() {
        SAConfigOptions configOptions = new SAConfigOptions(SA_SERVER_URL);
        // 打开自动采集, 并指定追踪哪些 AutoTrack 事件
        configOptions.setAutoTrackEventType(SensorsAnalyticsAutoTrackEventType.APP_START |
                SensorsAnalyticsAutoTrackEventType.APP_END |
                SensorsAnalyticsAutoTrackEventType.APP_VIEW_SCREEN |
                SensorsAnalyticsAutoTrackEventType.APP_CLICK)
                .enableTrackAppCrash()
                .enableJavaScriptBridge(true)
                .enableVisualizedAutoTrack(true);
        SensorsDataAPI.startWithConfigOptions(this, configOptions);
        SensorsDataAPI.sharedInstance(this).trackFragmentAppViewScreen();
    }

    public static String getCurrentProcessNameByActivityManager(@NonNull Context context) {
        int pid = Process.myPid();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            List<ActivityManager.RunningAppProcessInfo> runningAppList = am.getRunningAppProcesses();
            if (runningAppList != null) {
                for (ActivityManager.RunningAppProcessInfo processInfo : runningAppList) {
                    if (processInfo.pid == pid) {
                        return processInfo.processName;
                    }
                }
            }
        }
        return null;
    }
}
