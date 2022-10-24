package com.sensorsdata.analytics.android.sdk.useridentity.h5identity;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.useridentity.Identities;
import com.sensorsdata.analytics.android.sdk.useridentity.UserIdentityAPI;

import java.util.Iterator;

public class UnbindIDH5UserIdentityAPI extends H5UserIdentityAPI {

    private final UserIdentityAPI mUserIdentityApi;

    public UnbindIDH5UserIdentityAPI(UserIdentityAPI userIdentityAPI) {
        this.mUserIdentityApi = userIdentityAPI;
    }

    @Override
    public boolean updateIdentities() {
        try {
            Iterator<String> iteratorKeys = mIdentityJson.keys();
            while (iteratorKeys.hasNext()) {
                String key = iteratorKeys.next();
                mUserIdentityApi.getIdentitiesInstance().remove(key, mIdentityJson.optString(key));
            }
            mEventObject.put(Identities.IDENTITIES_KEY, mIdentityJson);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return super.updateIdentities();
    }
}
