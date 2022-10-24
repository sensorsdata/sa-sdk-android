package com.sensorsdata.analytics.android.sdk.useridentity.h5identity;


import com.sensorsdata.analytics.android.sdk.useridentity.Identities;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class H5UserIdentityAPI {

    //代码 h5 传递过来的 identities
    protected JSONObject mIdentityJson;
    protected JSONObject mEventObject;


    public void init(JSONObject identityJson, JSONObject eventObject) {
        this.mIdentityJson = identityJson;
        this.mEventObject = eventObject;
    }

    public boolean process(JSONObject identityJson, JSONObject eventObject) {
        init(identityJson, eventObject);
        //具体操作步骤：
        //1、App 的 identities 更新、App 的 identities 同 h5 的 identities 合并
        //2、业务 ID 处理：App 对业务ID 清除等操作
        return updateIdentities();
    }


    public boolean updateIdentities() {
        return true;
    }

    protected void mergeIdentities(JSONObject identities) throws JSONException {
        if (identities != null && mIdentityJson != null) {
            JSONUtils.mergeJSONObject(identities, mIdentityJson);
        }
        if (mIdentityJson == null && identities != null) {
            mIdentityJson = new JSONObject(identities.toString());
        }
        mEventObject.put(Identities.IDENTITIES_KEY, mIdentityJson);
    }
}
