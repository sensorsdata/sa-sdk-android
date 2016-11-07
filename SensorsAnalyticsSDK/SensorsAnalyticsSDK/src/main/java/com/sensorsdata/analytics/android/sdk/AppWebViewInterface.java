package com.sensorsdata.analytics.android.sdk;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by wangzhuozhou on 16/9/1
 */
/* package */ class AppWebViewInterface {
    private static final String LOGTAG = "SA.AppWebViewInterface";
    private Context mContext;
    private JSONObject properties;

    AppWebViewInterface(Context c, JSONObject p) {
        this.mContext = c;
        this.properties = p;
    }

    @JavascriptInterface
    public String sensorsdata_call_app() {
        try {
            if (properties == null) {
                properties = new JSONObject();
            }
            properties.put("type", "Android");
            String loginId = SensorsDataAPI.sharedInstance(mContext).getLoginId();
            if (!TextUtils.isEmpty(loginId)) {
                properties.put("distinct_id", loginId);
            } else {
                properties.put("distinct_id", SensorsDataAPI.sharedInstance(mContext).getAnonymousId());
            }
            return properties.toString();
        } catch (JSONException e) {
            Log.i(LOGTAG, e.getMessage());
        }
        return null;
    }
}
