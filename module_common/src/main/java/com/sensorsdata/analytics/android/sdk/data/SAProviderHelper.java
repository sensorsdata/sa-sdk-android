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
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.plugin.encrypt.SAStoreManager;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;

import java.io.File;

public class SAProviderHelper {
    private static final String TAG = "SA.ProviderHelper";
    private static SAProviderHelper INSTANCE;
    private ContentResolver contentResolver;
    private SQLiteOpenHelper mDbHelper;
    private Context mContext;
    private boolean isDbWritable = true;
    private boolean mIsFlushDataState = false;
    private int startActivityCount = 0;
    private long mAppStartTime = 0;
    private int mSessionTime = 30 * 1000;

    private SAProviderHelper(Context context) {
        try {
            this.mDbHelper = new SensorsDataDBHelper(context);
            this.mContext = context.getApplicationContext();
            contentResolver = context.getContentResolver();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static synchronized SAProviderHelper getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new SAProviderHelper(context);
        }
        return INSTANCE;
    }

    /**
     * 迁移数据，并删除老的数据库
     *
     * @param context Context
     */
    public void migratingDB(final Context context) {
        try {
            boolean isMigrating = AppInfoUtils.getAppInfoBundle(context).getBoolean("com.sensorsdata.analytics.android.EnableMigratingDB", true);
            if (!isMigrating) {
                SALog.i(TAG, "The migrating DB operation is false");
                return;
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String packageName = context.getPackageName();
                        File oldDatabase = context.getDatabasePath(packageName);
                        if (oldDatabase.exists()) {
                            SALog.i(TAG, "start migratingDB");
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

    public int bulkInsert(Uri uri, ContentValues[] values) {
        int numValues;
        SQLiteDatabase database = null;
        try {
            try {
                database = mDbHelper.getWritableDatabase();
            } catch (SQLiteException e) {
                SALog.printStackTrace(e);
                return 0;
            }
            database.beginTransaction();
            numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                insertEvent(uri, values[i]);
            }
            database.setTransactionSuccessful();
        } finally {
            if (database != null) {
                database.endTransaction();
            }
        }
        return numValues;
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
                    PersistentLoader.getInstance().getAppExitDataPst().commit(values.getAsString(DbParams.APP_EXIT_DATA));
                    break;
                case URI_CODE.SESSION_INTERVAL_TIME:
                    mSessionTime = values.getAsInteger(DbParams.TABLE_SESSION_INTERVAL_TIME);
                    contentResolver.notifyChange(uri, null);
                    break;
                case URI_CODE.LOGIN_ID:
                    PersistentLoader.getInstance().getLoginIdPst().commit(values.getAsString(DbParams.PersistentName.LOGIN_ID));
                    contentResolver.notifyChange(uri, null);
                    break;
                case URI_CODE.FLUSH_DATA:
                    mIsFlushDataState = values.getAsBoolean(DbParams.PersistentName.SUB_PROCESS_FLUSH_DATA);
                    break;
                case URI_CODE.REMOTE_CONFIG:
                    PersistentLoader.getInstance().getRemoteSDKConfig().commit(values.getAsString(DbParams.PersistentName.REMOTE_CONFIG));
                    break;
                case URI_CODE.USER_IDENTITY_ID:
                    PersistentLoader.getInstance().getUserIdsPst().commit(values.getAsString(DbParams.PersistentName.PERSISTENT_USER_ID));
                    contentResolver.notifyChange(uri, null);
                    break;
                case URI_CODE.LOGIN_ID_KEY:
                    PersistentLoader.getInstance().getLoginIdKeyPst().commit(values.getAsString(DbParams.PersistentName.PERSISTENT_LOGIN_ID_KEY));
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
                    String exitData = PersistentLoader.getInstance().getAppExitDataPst().get();
                    if(TextUtils.isEmpty(exitData)) {
                        exitData = PersistentLoader.getInstance().getAppEndDataPst().get();
                        PersistentLoader.getInstance().getAppEndDataPst().remove();
                    }
                    data = exitData;
                    column = DbParams.APP_EXIT_DATA;
                    break;
                case URI_CODE.SESSION_INTERVAL_TIME:
                    data = mSessionTime;
                    column = DbParams.TABLE_SESSION_INTERVAL_TIME;
                    break;
                case URI_CODE.LOGIN_ID:
                    data = PersistentLoader.getInstance().getLoginIdPst().get();
                    column = DbParams.PersistentName.LOGIN_ID;
                    break;
                case URI_CODE.FLUSH_DATA:
                    data = mIsFlushDataState ? 1 : 0;
                    column = DbParams.PersistentName.SUB_PROCESS_FLUSH_DATA;
                    break;
                case URI_CODE.REMOTE_CONFIG:
                    data = PersistentLoader.getInstance().getRemoteSDKConfig().get();
                    break;
                case URI_CODE.USER_IDENTITY_ID:
                    data = PersistentLoader.getInstance().getUserIdsPst().get();
                    column = DbParams.PersistentName.PERSISTENT_USER_ID;
                    break;
                case URI_CODE.LOGIN_ID_KEY:
                    data = PersistentLoader.getInstance().getLoginIdKeyPst().get();
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
