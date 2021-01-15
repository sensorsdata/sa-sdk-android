/*
 * Created by dengshiwei on 2019/04/18.
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

public interface SensorsAnalyticsAutoTrackEventType {
    /**
     * 空类型
     */
    int TYPE_NONE = 0;
    /**
     * App 启动事件
     */
    int APP_START = 1;

    /**
     * App 退出事件
     */
    int APP_END = 1 << 1;

    /**
     * 控件点击事件
     */
    int APP_CLICK = 1 << 2;

    /**
     * 页面浏览事件
     */
    int APP_VIEW_SCREEN = 1 << 3;
}
