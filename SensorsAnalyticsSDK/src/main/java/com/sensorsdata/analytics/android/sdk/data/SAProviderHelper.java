/*
 * Created by dengshiwei on 2021/04/08.
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

package com.sensorsdata.analytics.android.sdk.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;
import com.sensorsdata.analytics.android.sdk.data.persistent.LoginIdKeyPersistent;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentAppEndData;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentAppExitData;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoginId;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentRemoteSDKConfig;
import com.sensorsdata.analytics.android.sdk.plugin.encrypt.SAStoreManager;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.data.persistent.UserIdentityPersistent;

import java.io.File;

class SAProviderHelper {
    private ContentResolver contentResolver;
    private SQLiteOpenHelper mDbHelper;
    private PersistentAppEndData persistentAppEndData;
    private PersistentAppExitData persistentAppExitData;
    private PersistentLoginId persistentLoginId;
    private PersistentRemoteSDKConfig persistentRemoteSDKConfig;
    private LoginIdKeyPersistent mLoginIdKeyPersistent;
    private UserIdentityPersistent mUserIdsPersistent;
    private Context mContext;
    private boolean isDbWritable = true;
    private boolean mIsFlushDataState = false;
    private int startActivityCount = 0;
    private long mAppStartTime = 0;
    private int mSessionTime = 30 * 1000;

    public SAProviderHelper(Context context, SQLiteOpenHelper dbHelper) {
        try {
            this.mDbHelper = dbHelper;
            this.mContext = context;
            contentResolver = context.getContentResolver();
            PersistentLoader.initLoader(context);
            persistentAppEndData = (PersistentAppEndData) PersistentLoader.loadPersistent(DbParams.PersistentName.APP_END_DATA);
            persistentAppExitData = (PersistentAppExitData) PersistentLoader.loadPersistent(DbParams.APP_EXIT_DATA);
            persistentLoginId = (PersistentLoginId) PersistentLoader.loadPersistent(DbParams.PersistentName.LOGIN_ID);
            persistentRemoteSDKConfig = (PersistentRemoteSDKConfig) PersistentLoader.loadPersistent(DbParams.PersistentName.REMOTE_CONFIG);
            mUserIdsPersistent = (UserIdentityPersistent) PersistentLoader.loadPersistent(DbParams.PersistentName.PERSISTENT_USER_ID);
            mLoginIdKeyPersistent = (LoginIdKeyPersistent) PersistentLoader.loadPersistent(DbParams.PersistentName.PERSISTENT_LOGIN_ID_KEY);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 迁移数据，并删除老的数据库
     *
     * @param context Context
     * @param packageName 包名
     */
    public void migratingDB(final Context context, final String packageName) {
        try {
            boolean isMigrating = AppInfoUtils.getAppInfoBundle(context).getBoolean("com.sensorsdata.analytics.android.EnableMigratingDB", true);
            if (!isMigrating) {
                return;
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        File oldDatabase = context.getDatabasePath(packageName);
                        if (oldDatabase.exists()) {
                            final OldBDatabaseHelper oldBDatabaseHelper = new OldBDatabaseHelper(context, packageName);
                            final SQLiteDatabase database = getWritableDatabase();
                            if (database != null) {
                                final ContentValues cv = new ContentValues();
                                oldBDatabaseHelper.getAllEvents(database, new QueryEventsListener() {
                                    @Override
                                    public void insert(String data, String keyCreated) {
                                        cv.put(DbParams.KEY_DATA, data);
                                        cv.put(DbParams.KEY_CREATED_AT, keyCreated);
                                        database.insert(DbParams.TABLE_EVENTS, "_id", cv);
                                        cv.clear();
                                    }
                                });
                            }
                        }
                        if (isDbWritable) {
                            context.deleteDatabase(packageName);
                        }
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            }).start();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    interface QueryEventsListener {
        void insert(String data, String keyCreated);
    }

    /**
     * 构建 Uri 类型
     *
     * @param uriMatcher UriMatcher
     * @param authority authority
     */
    public void appendUri(UriMatcher uriMatcher, String authority) {
        try {
            uriMatcher.addURI(authority, DbParams.TABLE_EVENTS, URI_CODE.EVENTS);
            uriMatcher.addURI(authority, DbParams.TABLE_ACTIVITY_START_COUNT, URI_CODE.ACTIVITY_START_COUNT);
            uriMatcher.addURI(authority, DbParams.TABLE_APP_START_TIME, URI_CODE.APP_START_TIME);
            uriMatcher.addURI(authority, DbParams.APP_EXIT_DATA, URI_CODE.APP_EXIT_DATA);
            uriMatcher.addURI(authority, DbParams.TABLE_SESSION_INTERVAL_TIME, URI_CODE.SESSION_INTERVAL_TIME);
            uriMatcher.addURI(authority, DbParams.PersistentName.LOGIN_ID, URI_CODE.LOGIN_ID);
            uriMatcher.addURI(authority, DbParams.TABLE_CHANNEL_PERSISTENT, URI_CODE.CHANNEL_PERSISTENT);
            uriMatcher.addURI(authority, DbParams.PersistentName.SUB_PROCESS_FLUSH_DATA, URI_CODE.FLUSH_DATA);
            uriMatcher.addURI(authority, DbParams.TABLE_FIRST_PROCESS_START, URI_CODE.FIRST_PROCESS_START);
            uriMatcher.addURI(authority, DbParams.TABLE_DATA_DISABLE_SDK, URI_CODE.DISABLE_SDK);
            uriMatcher.addURI(authority, DbParams.PersistentName.REMOTE_CONFIG, URI_CODE.REMOTE_CONFIG);
            uriMatcher.addURI(authority, DbParams.PersistentName.PERSISTENT_USER_ID, URI_CODE.USER_IDENTITY_ID);
            uriMatcher.addURI(authority, DbParams.PersistentName.PERSISTENT_LOGIN_ID_KEY, URI_CODE.LOGIN_ID_KEY);
            uriMatcher.addURI(authority, DbParams.PUSH_ID_KEY, URI_CODE.PUSH_ID_KEY);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 插入 Event 埋点数据
     *
     * @param uri Uri
     * @param values 数据
     * @return Uri
     */
    public Uri insertEvent(Uri uri, ContentValues values) {
        try {
            SQLiteDatabase database = getWritableDatabase();
            if (database == null || !values.containsKey(DbParams.KEY_DATA)
                    || !values.containsKey(DbParams.KEY_CREATED_AT)) {
                return uri;
            }
            long d = database.insert(DbParams.TABLE_EVENTS, "_id", values);
            return ContentUris.withAppendedId(uri, d);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return uri;
    }

    /**
     * 删除埋点数据
     *
     * @param selection 条件
     * @param selectionArgs 参数
     * @return 受影响数
     */
    public int deleteEvents(String selection, String[] selectionArgs) {
        if (!isDbWritable) {
            return 0;
        }
        try {
            SQLiteDatabase database = getWritableDatabase();
            if (database != null) {
                return database.delete(DbParams.TABLE_EVENTS, selection, selectionArgs);
            }
        } catch (SQLiteException e) {
            isDbWritable = false;
            SALog.printStackTrace(e);
        }
        return 0;
    }

    /**
     * 插入渠道信息
     *
     * @param uri Uri
     * @param values 数据
     * @return Uri
     */
    public Uri insertChannelPersistent(Uri uri, ContentValues values) {
        try {
            SQLiteDatabase database = getWritableDatabase();
            if (database == null || !values.containsKey(DbParams.KEY_CHANNEL_EVENT_NAME)) {
                return uri;
            }
            long d = database.insertWithOnConflict(DbParams.TABLE_CHANNEL_PERSISTENT, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            return ContentUris.withAppendedId(uri, d);
        } catch (Exception exception) {
            SALog.printStackTrace(exception);
        }
        return uri;
    }

    /**
     * insert 处理
     *
     * @param code Uri code
     * @param uri Uri
     * @param values ContentValues
     */
    public void insertPersistent(int code, Uri uri, ContentValues values) {
        try {
            switch (code) {
                case URI_CODE.ACTIVITY_START_COUNT:
                    startActivityCount = values.getAsInteger(DbParams.TABLE_ACTIVITY_START_COUNT);
                    break;
                case URI_CODE.APP_START_TIME:
                    mAppStartTime = values.getAsLong(DbParams.TABLE_APP_START_TIME);
                    break;
                case URI_CODE.APP_EXIT_DATA:
                    persistentAppExitData.commit(values.getAsString(DbParams.APP_EXIT_DATA));
                    break;
                case URI_CODE.SESSION_INTERVAL_TIME:
                    mSessionTime = values.getAsInteger(DbParams.TABLE_SESSION_INTERVAL_TIME);
                    contentResolver.notifyChange(uri, null);
                    break;
                case URI_CODE.LOGIN_ID:
                    persistentLoginId.commit(values.getAsString(DbParams.PersistentName.LOGIN_ID));
                    contentResolver.notifyChange(uri, null);
                    break;
                case URI_CODE.FLUSH_DATA:
                    mIsFlushDataState = values.getAsBoolean(DbParams.PersistentName.SUB_PROCESS_FLUSH_DATA);
                    break;
                case URI_CODE.REMOTE_CONFIG:
                    persistentRemoteSDKConfig.commit(values.getAsString(DbParams.PersistentName.REMOTE_CONFIG));
                    break;
                case URI_CODE.USER_IDENTITY_ID:
                    mUserIdsPersistent.commit(values.getAsString(DbParams.PersistentName.PERSISTENT_USER_ID));
                    contentResolver.notifyChange(uri, null);
                    break;
                case URI_CODE.LOGIN_ID_KEY:
                    mLoginIdKeyPersistent.commit(values.getAsString(DbParams.PersistentName.PERSISTENT_LOGIN_ID_KEY));
                    break;
                case URI_CODE.PUSH_ID_KEY:
                    SAStoreManager.getInstance().setString(values.getAsString(DbParams.PUSH_ID_KEY),
                            values.getAsString(DbParams.PUSH_ID_VALUE));
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 查询数据
     *
     * @param tableName 表名
     * @param projection 列明
     * @param selection 筛选条件
     * @param selectionArgs 筛选参数
     * @param sortOrder 排序
     * @return Cursor
     */
    public Cursor queryByTable(String tableName, String[] projection, String selection, String[]
            selectionArgs, String sortOrder) {
        if (!isDbWritable) {
            return null;
        }
        Cursor cursor = null;
        try {
            SQLiteDatabase liteDatabase = getWritableDatabase();
            if (liteDatabase != null) {
                cursor = liteDatabase.query(tableName, projection, selection, selectionArgs, null, null, sortOrder);
            }
        } catch (SQLiteException e) {
            isDbWritable = false;
            SALog.printStackTrace(e);
        }
        return cursor;
    }

    /**
     * query 处理
     *
     * @param code Uri code
     * @return Cursor
     */
    public Cursor queryPersistent(int code, Uri uri) {
        try {
            String column = null;
            Object data = null;
            switch (code) {
                case URI_CODE.ACTIVITY_START_COUNT:
                    data = startActivityCount;
                    column = DbParams.TABLE_ACTIVITY_START_COUNT;
                    break;
                case URI_CODE.APP_START_TIME:
                    data = mAppStartTime;
                    column = DbParams.TABLE_APP_START_TIME;
                    break;
                case URI_CODE.APP_EXIT_DATA:
                    String exitData = persistentAppExitData.get();
                    if(TextUtils.isEmpty(exitData)) {
                        exitData = persistentAppEndData.get();
                        persistentAppEndData.remove();
                    }
                    data = exitData;
                    column = DbParams.APP_EXIT_DATA;
                    break;
                case URI_CODE.SESSION_INTERVAL_TIME:
                    data = mSessionTime;
                    column = DbParams.TABLE_SESSION_INTERVAL_TIME;
                    break;
                case URI_CODE.LOGIN_ID:
                    data = persistentLoginId.get();
                    column = DbParams.PersistentName.LOGIN_ID;
                    break;
                case URI_CODE.FLUSH_DATA:
                    data = mIsFlushDataState ? 1 : 0;
                    column = DbParams.PersistentName.SUB_PROCESS_FLUSH_DATA;
                    break;
                case URI_CODE.REMOTE_CONFIG:
                    data = persistentRemoteSDKConfig.get();
                    break;
                case URI_CODE.USER_IDENTITY_ID:
                    data = mUserIdsPersistent.get();
                    column = DbParams.PersistentName.PERSISTENT_USER_ID;
                    break;
                case URI_CODE.LOGIN_ID_KEY:
                    data = mLoginIdKeyPersistent.get();
                    column = DbParams.PersistentName.PERSISTENT_LOGIN_ID_KEY;
                    break;
                case URI_CODE.PUSH_ID_KEY:
                    data = SAStoreManager.getInstance().getString(uri.getQueryParameter(DbParams.PUSH_ID_KEY), "");
                    column = DbParams.PUSH_ID_KEY;
                    break;
                default:
                    break;
            }

            MatrixCursor matrixCursor = new MatrixCursor(new String[]{column});
            matrixCursor.addRow(new Object[]{data});
            return matrixCursor;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    /**
     * 获取数据库
     *
     * @return SQLiteDatabase
     */
    private SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase database = null;
        try {
            if (!isDBExist()) {
                mDbHelper.close();
                isDbWritable = true;
            }
            database = mDbHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            SALog.printStackTrace(e);
            isDbWritable = false;
        }
        return database;
    }

    private boolean isDBExist() {
        return mContext.getDatabasePath(DbParams.DATABASE_NAME).exists();
    }

    public int removeSP(String key) {
        SAStoreManager.getInstance().remove(key);
        return 1;
    }

    /**
     * URI 对应的 Code
     */
    public interface URI_CODE {
        int EVENTS = 1;
        int ACTIVITY_START_COUNT = 2;
        int APP_START_TIME = 3;
        int APP_EXIT_DATA = 4;
        int APP_PAUSED_TIME = 5;
        int SESSION_INTERVAL_TIME = 6;
        int LOGIN_ID = 7;
        int CHANNEL_PERSISTENT = 8;
        int FLUSH_DATA = 9;
        int FIRST_PROCESS_START = 10;
        int DISABLE_SDK = 11;
        int REMOTE_CONFIG = 12;
        int USER_IDENTITY_ID = 13;
        int LOGIN_ID_KEY = 14;
        int PUSH_ID_KEY = 15;
    }
}
