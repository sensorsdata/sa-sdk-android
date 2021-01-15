/*
 * Created by wangzhuozhou on 2016/11/30.
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

import org.json.JSONException;
import org.json.JSONObject;

public interface SensorsExpandableListViewItemTrackProperties {
    /**
     * 点击 groupPosition、childPosition 处 item 的扩展属性
     *
     * @param groupPosition int
     * @param childPosition int
     * @return JSONObject
     * @throws JSONException JSONException
     */
    JSONObject getSensorsChildItemTrackProperties(int groupPosition, int childPosition) throws JSONException;

    /**
     * 点击 groupPosition 处 item 的扩展属性
     *
     * @param groupPosition int
     * @return JSONObject
     * @throws JSONException JSONException
     */
    JSONObject getSensorsGroupItemTrackProperties(int groupPosition) throws JSONException;
}
