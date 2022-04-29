package com.sensorsdata.analytics.android.sdk.useridentity.h5identity;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.useridentity.Identities;
import com.sensorsdata.analytics.android.sdk.useridentity.LoginIDAndKey;
import com.sensorsdata.analytics.android.sdk.useridentity.UserIdentityAPI;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;

import java.util.Iterator;

public class SignUpH5UserIdentityAPI extends H5UserIdentityAPI {
    private final UserIdentityAPI mUserIdentityApi;
    private final EventType eventType;

    public SignUpH5UserIdentityAPI(UserIdentityAPI userIdentityAPI, EventType eventType) {
        this.mUserIdentityApi = userIdentityAPI;
        this.eventType = eventType;
    }

    @Override
    public boolean updateIdentities() {
        try {
            String h5_identities = mEventObject.optString(Identities.IDENTITIES_KEY);
            if (TextUtils.isEmpty(h5_identities)) {
                //考虑 1.0、2.0 版本升级上来的场景
                String h5login_id = mEventObject.optString("distinct_id");
                if (!mUserIdentityApi.loginWithKeyBack(LoginIDAndKey.LOGIN_ID_KEY_DEFAULT, h5login_id)) {
                    return false;
                }
            } else {
                if (mIdentityJson.has(mUserIdentityApi.getIdentitiesInstance().getLoginIDKey())) {
                    String login_id = mIdentityJson.optString(mUserIdentityApi.getIdentitiesInstance().getLoginIDKey());
                    if (!mUserIdentityApi.loginWithKeyBack(mUserIdentityApi.getIdentitiesInstance().getLoginIDKey(), login_id)) {
                        return false;
                    }
                } else {
                    //考虑的是 h5 新登录，App 后登录，App 端无法感知登录与绑定的场景
                    String h5login_id = mEventObject.optString("login_id");
                    if (!TextUtils.isEmpty(h5login_id)) {
                        String[] keyAndValue = h5login_id.split("\\+");
                        if (keyAndValue.length == 2) {
                            String key = keyAndValue[0];
                            String value = keyAndValue[1];
                            String h5_value = mIdentityJson.optString(key);
                            if (mIdentityJson.has(key) && !TextUtils.isEmpty(h5_value) && h5_value.equals(value)) {
                                if (!mUserIdentityApi.loginWithKeyBack(key, value)) {
                                    return false;
                                }
                            }
                        } else {
                            if (!traversalSearch(h5login_id)) {
                                return false;
                            }
                        }
                    }
                }
            }
            String new_loginId = mUserIdentityApi.getIdentitiesInstance().getLoginId();
            if (TextUtils.isEmpty(new_loginId)) {
                mEventObject.put("login_id", new_loginId);
            }
            mergeIdentities(mUserIdentityApi.getIdentities(eventType));
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return false;
        }
        return true;
    }

    private boolean traversalSearch(String h5login_id) {
        String loginKey = "";
        int count = 0;
        Iterator<String> iterator = mIdentityJson.keys();
        while (iterator.hasNext()) {
            String tmp_key = iterator.next();
            if (!TextUtils.isEmpty(tmp_key)) {
                String tmp_value = mIdentityJson.optString(tmp_key);
                if (!TextUtils.isEmpty(tmp_value) && tmp_value.equals(h5login_id)) {
                    loginKey = tmp_key;
                    count++;
                }
            }
        }
        if (count == 1) {
            return mUserIdentityApi.loginWithKeyBack(loginKey, h5login_id);
        }
        return false;
    }
}
