package com.sensorsdata.analytics.android.sdk;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by 王灼洲 on 2016/11/30
 * 获取 ListView、GridView position 位置 Item 的 properties
 */

public interface SensorsAdapterViewItemTrackProperties {
    /**
     * 点击 position 处 item 的扩展属性
     * @param position
     * @return
     * @throws JSONException
     */
    JSONObject getSensorsItemTrackProperties(int position) throws JSONException;
}