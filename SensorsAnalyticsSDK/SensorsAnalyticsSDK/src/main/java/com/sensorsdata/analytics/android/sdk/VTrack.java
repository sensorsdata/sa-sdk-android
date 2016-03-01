package com.sensorsdata.analytics.android.sdk;

import org.json.JSONArray;

/**
 * SDK内部接口
 **/
public interface VTrack {

  void startUpdates();

  void setEventBindings(JSONArray bindings);

}
