/**Created by qingyou.ren on 2019/03/04.
 * Copyright © 2015－2019 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk;

import org.json.JSONObject;

public interface SensorsDataTrackEventCallBack {
     /**
      *
      * @param eventName 事件名称
      * @param eventProperties 要修改的事件属性
      * @return true 表示事件将入库， false 表示事件将被抛弃
      */
     boolean onTrackEvent(String eventName, JSONObject eventProperties);

}
