package com.sensorsdata.analytics.android.sdk.util;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONUtils {

    public static String optionalStringKey(JSONObject o, String k) throws JSONException {
        if (o.has(k) && !o.isNull(k)) {
            return o.getString(k);
        }
        return null;
    }
}
