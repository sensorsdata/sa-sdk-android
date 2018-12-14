/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by 王灼洲 on 2016/11/30
 * 获取 ListView、GridView position 位置 Item 的 properties
 */

public interface SensorsObjectTrackProperties {
    /**
     * Object 扩展属性
     * @return JSONObject
     * @throws JSONException JSONException
     */
    JSONObject getSensorsTrackProperties() throws JSONException;
}
