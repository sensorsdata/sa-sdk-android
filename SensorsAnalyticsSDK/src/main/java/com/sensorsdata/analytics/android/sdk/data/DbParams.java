/**Created by wangzhuozhou on 2019/02/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
package com.sensorsdata.analytics.android.sdk.data;

import android.net.Uri;

public class DbParams {
    private static DbParams instance;
    private final Uri mUri, mAppStartUri, mAppStartTimeUri, mAppPausedUri, mAppEndStateUri, mAppEndDataUri, mSessionTimeUri;
    /** 数据库名称 */
    public static final String DATABASE_NAME = "sensorsdata";
    /** 数据库版本号 */
    public static final int DATABASE_VERSION = 4;
    /** 数据库中的表名 */
    public static final String TABLE_EVENTS = "events";
    public static final String TABLE_APPSTARTED = "app_started";
    public static final String TABLE_APPSTARTTIME = "app_start_time";
    public static final String TABLE_APPPAUSEDTIME = "app_paused_time";
    public static final String TABLE_APPENDSTATE = "app_end_state";
    public static final String TABLE_APPENDDATA = "app_end_data";
    public static final String TABLE_SESSIONINTERVALTIME = "session_interval_time";

    /** Event 表字段 */
    public static final String KEY_DATA = "data";
    public static final String KEY_CREATED_AT = "created_at";

    /** 数据库状态 */
    public static final int DB_UPDATE_ERROR = -1;
    public static final int DB_OUT_OF_MEMORY_ERROR = -2;

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

    private DbParams(String packageName) {
        mUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_EVENTS);
        mAppStartUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_APPSTARTED);
        mAppStartTimeUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_APPSTARTTIME);
        mAppEndStateUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_APPENDSTATE);
        mAppEndDataUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_APPENDDATA);
        mAppPausedUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_APPPAUSEDTIME);
        mSessionTimeUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_SESSIONINTERVALTIME);
    }

    /**
     * 获取 Event Uri
     * @return Uri
     */
    public Uri gemUri() {
        return mUri;
    }

    /**
     * 获取 AppStart Uri
     * @return Uri
     */
    public Uri getAppStartUri() {
        return mAppStartUri;
    }

    /**
     * 获取 AppStartTime Uri
     * @return Uri
     */
    public Uri getAppStartTimeUri() {
        return mAppStartTimeUri;
    }

    /**
     * 获取 AppPausedTime Uri
     * @return uri
     */
    public Uri getAppPausedUri() {
        return mAppPausedUri;
    }

    /**
     * 获取 AppEndState Uri
     * @return Uri
     */
    public Uri getAppEndStateUri() {
        return mAppEndStateUri;
    }

    /**
     * 获取 AppEndData Uri
     * @return Uri
     */
    public Uri getAppEndDataUri() {
        return mAppEndDataUri;
    }

    /**
     * 获取 SessionTime Uri
     * @return Uri
     */
    public Uri getSessionTimeUri() {
        return mSessionTimeUri;
    }
}
