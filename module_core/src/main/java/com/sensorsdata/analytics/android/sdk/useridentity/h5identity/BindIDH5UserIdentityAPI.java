package com.sensorsdata.analytics.android.sdk.useridentity.h5identity;


import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.useridentity.Identities;
import com.sensorsdata.analytics.android.sdk.useridentity.UserIdentityAPI;

import org.json.JSONObject;

public class BindIDH5UserIdentityAPI extends H5UserIdentityAPI {
    private final UserIdentityAPI mUserIdentityApi;

    public BindIDH5UserIdentityAPI(UserIdentityAPI userIdentityAPI) {
        this.mUserIdentityApi = userIdentityAPI;
    }

    @Override
    public boolean updateIdentities() {
        try {
            if (mIdentityJson != null) {
                //1、合并 h5 传递过来的 identities 并且保存到本地 identities
                JSONObject h5IdentityTmp = mIdentityJson;
                h5IdentityTmp.remove(Identities.COOKIE_ID);
                mUserIdentityApi.getIdentitiesInstance().mergeIdentities(h5IdentityTmp);
            }
            //2、合并 h5 和本地 identities 并且进行数据发送
            mergeIdentities(mUserIdentityApi.getIdentitiesInstance().getIdentities(Identities.State.DEFAULT));
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return super.updateIdentities();
    }
}
