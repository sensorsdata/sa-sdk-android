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
     * @return
     * @throws JSONException
     */
    JSONObject getSensorsTrackProperties() throws JSONException;
}