package com.sensorsdata.analytics.android.demo;

import android.app.Application;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;

/**
 * Created by 王灼洲 on 2016/11/12
 */

public class MyApplication extends Application {
    /**
     * Sensors Analytics 采集数据的地址
     */
    private final static String SA_SERVER_URL = "http://test-zouyuhan.cloud.sensorsdata.cn:8006/sa?project=wangzhuozhou&token=db52d13749514676";

    /**
     * Sensors Analytics 配置分发的地址
     */
    private final static String SA_CONFIGURE_URL = "http://test-zouyuhan.cloud.sensorsdata.cn:8006/config/?project=wangzhuozhou";

    /**
     * Sensors Analytics DEBUG 模式
     * SensorsDataAPI.DebugMode.DEBUG_OFF - 关闭 Debug 模式
     * SensorsDataAPI.DebugMode.DEBUG_ONLY - 打开 Debug 模式，校验数据，但不进行数据导入
     * SensorsDataAPI.DebugMode.DEBUG_AND_TRACK - 打开 Debug 模式，校验数据，并将数据导入到 Sensors Analytics 中
     * 注意！请不要在正式发布的 App 中使用 Debug 模式！
     */
    private final SensorsDataAPI.DebugMode SA_DEBUG_MODE = SensorsDataAPI.DebugMode.DEBUG_AND_TRACK;

    @Override
    public void onCreate() {
        super.onCreate();
        initSensorsDataAPI();
    }

    /**
     * 初始化 Sensors Analytics SDK
     */
    private void initSensorsDataAPI() {
        SensorsDataAPI.sharedInstance(
                this,                               // 传入 Context
                SA_SERVER_URL,                      // 数据接收的 URL
                SA_CONFIGURE_URL,                   // 配置分发的 URL
                SA_DEBUG_MODE);                     // Debug 模式选项
        SensorsDataAPI.sharedInstance(this).enableAutoTrack();
        //16MB
        SensorsDataAPI.sharedInstance(this).setMaxCacheSize(16 * 1024 * 1024);
    }
}
