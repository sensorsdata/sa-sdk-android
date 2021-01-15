/*
 * Created by dengshiwei on 2019/06/03.
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

package com.sensorsdata.analytics.android.sdk;

/**
 * 网络类型
 */
public interface SensorsNetworkType {
    // NULL
    int TYPE_NONE = 0;
    // 2G
    int TYPE_2G = 1;
    // 3G
    int TYPE_3G = 1 << 1;
    // 4G
    int TYPE_4G = 1 << 2;
    // WIFI
    int TYPE_WIFI = 1 << 3;
    // 5G
    int TYPE_5G = 1 << 4;
    // ALL
    int TYPE_ALL = 0xFF;
}
