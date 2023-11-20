/*
 * Created by wangzhuozhou on 2019/02/01.
 * Copyright 2015－2022 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.data.persistent;

import android.content.Context;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;

public class PersistentLoader {
    private final Context mContext;
    private volatile static PersistentLoader INSTANCE;
    private final PersistentAppEndData mAppEndDataPst;
    private final PersistentAppExitData mAppExitDataPst;
    private final PersistentLoginId mLoginIdPst;
    private final PersistentDistinctId mAnonymousIdPst;
    private final PersistentRemoteSDKConfig mRemoteSDKConfig;
    private final LoginIdKeyPersistent mLoginIdKeyPst;
    private final UserIdentityPersistent mUserIdsPst;
    private final PersistentFirstStart mFirstStartPst;
    private final PersistentFirstDay mFirstDayPst;
    private final PersistentDailyDate mDayDatePst;
    private final PersistentSuperProperties mSuperPropertiesPst;
    private final PersistentVisualConfig mVisualConfigPst;
    private final PersistentFirstTrackInstallation mFirstInstallationPst;
    private final PersistentFirstTrackInstallationWithCallback mFirstInstallationWithCallbackPst;

    private PersistentLoader(Context context) {
        this.mContext = context.getApplicationContext();
        mAppEndDataPst = (PersistentAppEndData) loadPersistent(DbParams.PersistentName.APP_END_DATA);
        mAppExitDataPst = (PersistentAppExitData) loadPersistent(DbParams.APP_EXIT_DATA);
        mLoginIdPst = (PersistentLoginId) loadPersistent(DbParams.PersistentName.LOGIN_ID);
        mRemoteSDKConfig = (PersistentRemoteSDKConfig) loadPersistent(DbParams.PersistentName.REMOTE_CONFIG);
        mUserIdsPst = (UserIdentityPersistent) loadPersistent(DbParams.PersistentName.PERSISTENT_USER_ID);
        mLoginIdKeyPst = (LoginIdKeyPersistent) loadPersistent(DbParams.PersistentName.PERSISTENT_LOGIN_ID_KEY);
        mAnonymousIdPst = (PersistentDistinctId) loadPersistent(DbParams.PersistentName.DISTINCT_ID);
        mFirstStartPst = (PersistentFirstStart) loadPersistent(DbParams.PersistentName.FIRST_START);
        mFirstDayPst = (PersistentFirstDay) loadPersistent(DbParams.PersistentName.FIRST_DAY);
        mSuperPropertiesPst = (PersistentSuperProperties) loadPersistent(DbParams.PersistentName.SUPER_PROPERTIES);
        mVisualConfigPst = (PersistentVisualConfig) loadPersistent(DbParams.PersistentName.VISUAL_PROPERTIES);
        mFirstInstallationPst = (PersistentFirstTrackInstallation) loadPersistent(DbParams.PersistentName.FIRST_INSTALL);
        mFirstInstallationWithCallbackPst = (PersistentFirstTrackInstallationWithCallback) loadPersistent(DbParams.PersistentName.FIRST_INSTALL_CALLBACK);
        mDayDatePst = (PersistentDailyDate) loadPersistent(DbParams.PersistentName.PERSISTENT_DAY_DATE);
    }

    public static void preInit(Context context) {
        if (INSTANCE == null) {
            synchronized (PersistentLoader.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PersistentLoader(context);
                }
            }
        }
    }

    public static PersistentLoader getInstance() {
        return INSTANCE;
    }

    private PersistentIdentity<?> loadPersistent(String persistentKey) {
        if (TextUtils.isEmpty(persistentKey)) {
            return null;
        }
        switch (persistentKey) {
            case DbParams.PersistentName.APP_END_DATA:
                return new PersistentAppEndData();
            case DbParams.PersistentName.DISTINCT_ID:
                return new PersistentDistinctId(mContext);
            case DbParams.PersistentName.FIRST_DAY:
                return new PersistentFirstDay();
            case DbParams.PersistentName.FIRST_INSTALL:
                return new PersistentFirstTrackInstallation();
            case DbParams.PersistentName.FIRST_INSTALL_CALLBACK:
                return new PersistentFirstTrackInstallationWithCallback();
            case DbParams.PersistentName.FIRST_START:
                return new PersistentFirstStart();
            case DbParams.PersistentName.LOGIN_ID:
                return new PersistentLoginId();
            case DbParams.PersistentName.REMOTE_CONFIG:
                return new PersistentRemoteSDKConfig();
            case DbParams.PersistentName.SUPER_PROPERTIES:
                return new PersistentSuperProperties();
            case DbParams.PersistentName.VISUAL_PROPERTIES:
                return new PersistentVisualConfig();
            case DbParams.PersistentName.PERSISTENT_USER_ID:
                return new UserIdentityPersistent();
            case DbParams.PersistentName.PERSISTENT_LOGIN_ID_KEY:
                return new LoginIdKeyPersistent();
            case DbParams.APP_EXIT_DATA:
                return new PersistentAppExitData();
            case DbParams.PersistentName.PERSISTENT_DAY_DATE:
                return new PersistentDailyDate();
            default:
                return null;
        }
    }

    public PersistentAppEndData getAppEndDataPst() {
        return mAppEndDataPst;
    }

    public PersistentAppExitData getAppExitDataPst() {
        return mAppExitDataPst;
    }

    public PersistentLoginId getLoginIdPst() {
        return mLoginIdPst;
    }

    public PersistentDistinctId getAnonymousIdPst() {
        return mAnonymousIdPst;
    }

    public PersistentRemoteSDKConfig getRemoteSDKConfig() {
        return mRemoteSDKConfig;
    }

    public LoginIdKeyPersistent getLoginIdKeyPst() {
        return mLoginIdKeyPst;
    }

    public UserIdentityPersistent getUserIdsPst() {
        return mUserIdsPst;
    }

    public PersistentFirstStart getFirstStartPst() {
        return mFirstStartPst;
    }

    public PersistentFirstDay getFirstDayPst() {
        return mFirstDayPst;
    }

    public PersistentSuperProperties getSuperPropertiesPst() {
        return mSuperPropertiesPst;
    }

    public PersistentVisualConfig getVisualConfigPst() {
        return mVisualConfigPst;
    }

    public PersistentFirstTrackInstallation getFirstInstallationPst() {
        return mFirstInstallationPst;
    }

    public PersistentFirstTrackInstallationWithCallback getFirstInstallationWithCallbackPst() {
        return mFirstInstallationWithCallbackPst;
    }

    public PersistentDailyDate getDayDatePst() {
        return mDayDatePst;
    }
}
