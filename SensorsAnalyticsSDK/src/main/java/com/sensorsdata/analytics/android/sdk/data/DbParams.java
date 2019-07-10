/*
 * Created by wangzhuozhou on 2019/02/01.
 * Copyright 2015－2019 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.data;

import android.net.Uri;

public class DbParams {
    /* 数据库中的表名 */
    public static final String TABLE_EVENTS = "events";
    public static final int DB_OUT_OF_MEMORY_ERROR = -2;
    /* 数据库名称 */
    static final String DATABASE_NAME = "sensorsdata";
    /* 数据库版本号 */
    static final int DATABASE_VERSION = 4;
    static final String TABLE_APP_STARTED = "app_started";
    static final String TABLE_APP_START_TIME = "app_start_time";
    static final String TABLE_APP_PAUSED_TIME = "app_paused_time";
    static final String TABLE_APP_END_STATE = "app_end_state";
    static final String TABLE_APP_END_DATA = "app_end_data";
    static final String TABLE_SESSION_INTERVAL_TIME = "session_interval_time";
    static final String TABLE_LOGIN_ID = "events_login_id";
    /* Event 表字段 */
    static final String KEY_DATA = "data";
    static final String KEY_CREATED_AT = "created_at";
    /* 数据库状态 */
    static final int DB_UPDATE_ERROR = -1;
    private static DbParams instance;
    private final Uri mUri, mAppStartUri, mAppStartTimeUri, mAppPausedUri, mAppEndStateUri,
            mAppEndDataUri, mSessionTimeUri, mLoginIdUri;

    private DbParams(String packageName) {
        mUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_EVENTS);
        mAppStartUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_APP_STARTED);
        mAppStartTimeUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_APP_START_TIME);
        mAppEndStateUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_APP_END_STATE);
        mAppEndDataUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_APP_END_DATA);
        mAppPausedUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_APP_PAUSED_TIME);
        mSessionTimeUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_SESSION_INTERVAL_TIME);
        mLoginIdUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_LOGIN_ID);
    }

    public static DbParams getInstance(String packageName) {
        if (instance == null) {
            instance = new DbParams(packageName);
        }
        return instance;
    }

    public static DbParams getInstance() {
        if (instance == null) {
            throw new IllegalStateException("The static method getInstance(String packageName) should be called before calling getInstance()");
        }
        return instance;
    }

    /**
     * 获取 Event Uri
     *
     * @return Uri
     */
    Uri getEventUri() {
        return mUri;
    }

    /**
     * 获取 AppStart Uri
     *
     * @return Uri
     */
    public Uri getAppStartUri() {
        return mAppStartUri;
    }

    /**
     * 获取 AppStartTime Uri
     *
     * @return Uri
     */
    Uri getAppStartTimeUri() {
        return mAppStartTimeUri;
    }

    /**
     * 获取 AppPausedTime Uri
     *
     * @return uri
     */
    Uri getAppPausedUri() {
        return mAppPausedUri;
    }

    /**
     * 获取 AppEndState Uri
     *
     * @return Uri
     */
    Uri getAppEndStateUri() {
        return mAppEndStateUri;
    }

    /**
     * 获取 AppEndData Uri
     *
     * @return Uri
     */
    Uri getAppEndDataUri() {
        return mAppEndDataUri;
    }

    /**
     * 获取 SessionTime Uri
     *
     * @return Uri
     */
    public Uri getSessionTimeUri() {
        return mSessionTimeUri;
    }

    /**
     * 获取 LoginId 的 Uri
     *
     * @return Uri
     */
    Uri getLoginIdUri() {
        return mLoginIdUri;
    }
}
