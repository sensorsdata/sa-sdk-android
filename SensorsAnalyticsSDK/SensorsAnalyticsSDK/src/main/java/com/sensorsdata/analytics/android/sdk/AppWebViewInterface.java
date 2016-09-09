package com.sensorsdata.analytics.android.sdk;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by wangzhuozhou on 16/9/1
 */
public class AppWebViewInterface {
    private static final String LOGTAG = "SA.AppWebViewInterface";
    private Context mContext;

    AppWebViewInterface(Context c) {
        mContext = c;
    }

    @JavascriptInterface
    public String sensorsdata_call_app() {
        try {
            JSONObject object = new JSONObject();
            object.put("type", "Android");
            object.put("distinct_id", SensorsDataAPI.sharedInstance(mContext).getDistinctId());
            return object.toString();
        } catch (JSONException e) {
            Log.i(LOGTAG, e.getMessage());
        }
        return null;
    }
}
