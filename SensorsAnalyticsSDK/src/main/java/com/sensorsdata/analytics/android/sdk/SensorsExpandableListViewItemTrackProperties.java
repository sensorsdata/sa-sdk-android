/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by 王灼洲 on 2016/11/30
 * ExpandableListView
 */

public interface SensorsExpandableListViewItemTrackProperties {
    /**
     * 点击 groupPosition、childPosition 处 item 的扩展属性
     * @param groupPosition int
     * @param childPosition int
     * @return JSONObject
     * @throws JSONException JSONException
     */
    JSONObject getSensorsChildItemTrackProperties(int groupPosition, int childPosition) throws JSONException;

    /**
     * 点击 groupPosition 处 item 的扩展属性
     * @param groupPosition int
     * @return JSONObject
     * @throws JSONException JSONException
     */
    JSONObject getSensorsGroupItemTrackProperties(int groupPosition) throws JSONException;
}
