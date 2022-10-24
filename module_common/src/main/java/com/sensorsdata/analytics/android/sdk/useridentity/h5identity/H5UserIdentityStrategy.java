package com.sensorsdata.analytics.android.sdk.useridentity.h5identity;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.useridentity.Identities;
import com.sensorsdata.analytics.android.sdk.useridentity.UserIdentityAPI;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;

import org.json.JSONObject;

public class H5UserIdentityStrategy {


    private final UserIdentityAPI userIdentityAPI;

    public H5UserIdentityStrategy(UserIdentityAPI userIdentityAPI) {
        this.userIdentityAPI = userIdentityAPI;
    }

    public boolean processH5UserIdentity(EventType eventType, JSONObject eventObject) {
        try {
            H5UserIdentityAPI h5UserIdentityAPI;
            // 先判断 H5 事件中是否存在 identities
            JSONObject identityJson;
            String identities = eventObject.optString(Identities.IDENTITIES_KEY);
            if (!TextUtils.isEmpty(identities)) {
                identityJson = new JSONObject(identities);
            } else {
                identityJson = new JSONObject();
            }
            if (EventType.TRACK_SIGNUP == eventType) {
                specialIDProcess(identityJson);
                h5UserIdentityAPI = new SignUpH5UserIdentityAPI(userIdentityAPI, eventType);
            } else if (EventType.TRACK_ID_BIND == eventType) {
                specialIDProcess(identityJson);
                h5UserIdentityAPI = new BindIDH5UserIdentityAPI(userIdentityAPI);
            } else if (EventType.TRACK_ID_UNBIND == eventType && identityJson != null) {
                h5UserIdentityAPI = new UnbindIDH5UserIdentityAPI(userIdentityAPI);
            } else {
                specialIDProcess(identityJson);
                h5UserIdentityAPI = new CommonUserIdentityAPI(userIdentityAPI);
            }
            return h5UserIdentityAPI.process(identityJson, eventObject);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return false;
    }

    private void specialIDProcess(JSONObject identityJson) {
        identityJson.remove(Identities.ANDROID_ID);
        identityJson.remove(Identities.ANONYMOUS_ID);
        identityJson.remove(Identities.ANDROID_UUID);
    }
}
