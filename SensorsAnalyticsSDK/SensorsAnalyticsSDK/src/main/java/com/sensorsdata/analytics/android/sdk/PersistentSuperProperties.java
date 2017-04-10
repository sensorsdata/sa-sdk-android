package com.sensorsdata.analytics.android.sdk;

import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Future;

/**
 * Created by 王灼洲 on 2017/4/10
 */

class PersistentSuperProperties extends PersistentIdentity<JSONObject> {
    PersistentSuperProperties(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "super_properties", new PersistentSerializer<JSONObject>() {
            @Override
            public JSONObject load(String value) {
                try {
                    return new JSONObject(value);
                } catch (JSONException e) {
                    Log.e("Persistent", "failed to load SuperProperties from SharedPreferences.", e);
                    return null;
                }
            }

            @Override
            public String save(JSONObject item) {
                return item.toString();
            }

            @Override
            public JSONObject create() {
                return new JSONObject();
            }
        });
    }
}
