/*
 * Created by qingyou.ren on 2019/03/04.
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

import org.json.JSONObject;

public interface SensorsDataTrackEventCallBack {
    /**
     * 事件回调接口
     *
     * @param eventName 事件名称
     * @param eventProperties 要修改的事件属性
     * @return true 表示事件将入库， false 表示事件将被抛弃
     */

    boolean onTrackEvent(String eventName, JSONObject eventProperties);

}
