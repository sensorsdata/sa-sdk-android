/*
 * Created by dengshiwei on 2022/07/05.
 * Copyright 2015－2021 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sensorsdata.analytics.android.sdk.core.rpc;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPIEmptyImplementation;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;
import com.sensorsdata.analytics.android.sdk.useridentity.LoginIDAndKey;

/**
 * 用于跨进程业务的数据通信
 */
public class SensorsDataContentObserver extends ContentObserver {
    public SensorsDataContentObserver() {
        super(new Handler(Looper.getMainLooper()));
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        try {
            if (DbParams.getInstance().getSessionTimeUri().equals(uri)) {
                SensorsDataAPI.sharedInstance().setSessionIntervalTime(DbAdapter.getInstance().getSessionIntervalTime());
            } else if (DbParams.getInstance().getLoginIdUri().equals(uri)) {
                //临时方案兼容跨进程 getLognID 立即获取接口
                String loginIDKey = DbAdapter.getInstance().getLoginIdKey();
                String loginID = DbAdapter.getInstance().getLoginId();
                boolean flag = TextUtils.isEmpty(loginID) || loginIDKey.equals(LoginIDAndKey.LOGIN_ID_KEY_DEFAULT);
                flag = flag && TextUtils.isEmpty(loginID);
                if (flag) {
                    if (State.LOGOUT.isDid) {
                        State.LOGOUT.isDid = false;
                        return;
                    }
                    State.LOGOUT.isObserverCalled = true;
                    SensorsDataAPI.sharedInstance().logout();
                } else {
                    if (State.LOGIN.isDid) {
                        State.LOGIN.isDid = false;
                        return;
                    }
                    SensorsDataAPI.sharedInstance().loginWithKey(loginIDKey, loginID);
                }
            } else if (DbParams.getInstance().getDisableSDKUri().equals(uri)) {
                if (State.DISABLE_SDK.isDid) {
                    State.DISABLE_SDK.isDid = false;
                    return;
                }
                State.DISABLE_SDK.isObserverCalled = true;
                SensorsDataAPI.disableSDK();
            } else if (DbParams.getInstance().getEnableSDKUri().equals(uri)) {
                if (State.ENABLE_SDK.isDid) {
                    State.ENABLE_SDK.isDid = false;
                    return;
                }
                State.ENABLE_SDK.isObserverCalled = true;
                SensorsDataAPI.enableSDK();
            } else if (DbParams.getInstance().getUserIdentities().equals(uri)) {
                SensorsDataAPI sensorsDataAPI = SensorsDataAPI.sharedInstance();
                if (sensorsDataAPI instanceof SensorsDataAPIEmptyImplementation) {
                    return;
                }
                SAContextManager saContextManager = sensorsDataAPI.getSAContextManager();
                if (saContextManager != null) {
                    saContextManager.getUserIdentityAPI().getIdentitiesInstance().updateIdentities();
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public enum State {
        LOGOUT(false, false),
        LOGIN(false, false),
        ENABLE_SDK(false, false),
        DISABLE_SDK(false, false),
        ;
        public boolean isDid;
        public boolean isObserverCalled;

        State(boolean isInterfaceDid, boolean isObserverCalled) {
            this.isDid = isInterfaceDid;
            this.isObserverCalled = isObserverCalled;
        }
    }
}