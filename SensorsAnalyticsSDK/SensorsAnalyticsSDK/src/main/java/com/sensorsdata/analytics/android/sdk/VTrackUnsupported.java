package com.sensorsdata.analytics.android.sdk;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by 王灼洲 on 2017/4/10
 */

class VTrackUnsupported implements VTrack, DebugTracking {
    public VTrackUnsupported() {
    }

    @Override
    public void startUpdates() {
        // do NOTHING
    }

    @Override
    public void setEventBindings(JSONArray bindings) {
        // do NOTHING
    }

    @Override
    public void setVTrackServer(String serverUrl) {
        // do NOTHING
    }

    @Override
    public void enableEditingVTrack() {
        // do NOTHING
    }

    @Override
    public void disableActivity(String canonicalName) {
        // do NOTHING
    }

    @Override
    public void reportTrack(JSONObject eventJson) {
        // do NOTHING
    }
}
