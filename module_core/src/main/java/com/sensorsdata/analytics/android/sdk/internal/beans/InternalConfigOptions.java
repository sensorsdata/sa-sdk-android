/*
 * Created by dengshiwei on 2022/09/05.
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

package com.sensorsdata.analytics.android.sdk.internal.beans;

import android.content.Context;

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataGPSLocation;
import com.sensorsdata.analytics.android.sdk.SensorsDataTrackEventCallBack;

public class InternalConfigOptions {
    public Context context;
    // 客户设置的配置项
    public SAConfigOptions saConfigOptions;
    // 是否主线程
    public boolean isMainProcess = false;
    // 是否展示 Debug 弹窗
    public boolean isShowDebugView = true;
    // 是否请求网络
    public boolean isNetworkRequestEnable = true;
    // 是否禁用远程配置
    public boolean isDefaultRemoteConfigEnable = false;
    // 是否禁用采集 DeviceId
    public boolean isTrackDeviceId = false;
    // Session 时长
    public int sessionTime = 30 * 1000;
    // Cookie 设置
    public String cookie;
    // GPS 信息
    public SensorsDataGPSLocation gpsLocation;
    // Debug 模式选项
    public SensorsDataAPI.DebugMode debugMode = SensorsDataAPI.DebugMode.DEBUG_OFF;
    // EventCallBack
    public SensorsDataTrackEventCallBack sensorsDataTrackEventCallBack;
}
