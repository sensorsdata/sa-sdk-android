/*
 * Created by dengshiwei on 2021/04/08.
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

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentAppEndData;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentAppPaused;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentAppStartTime;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFlushDataState;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoginId;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

class SAProviderHelper {
    private ContentResolver contentResolver;
    private SQLiteOpenHelper mDbHelper;
    private PersistentAppStartTime persistentAppStartTime;
    private PersistentAppEndData persistentAppEndData;
    private PersistentAppPaused persistentAppPaused;
    private PersistentLoginId persistentLoginId;
    private PersistentFlushDataState persistentFlushDataState;
    private Context mContext;
    private boolean isDbWritable = true;
    private boolean isFirstProcessStarted = true;
    private int startActivityCount = 0;
    private int mSessionTime = 30 * 1000;

    public SAProviderHelper(Context context, SQLiteOpenHelper dbHelper) {
        try {
            this.mDbHelper = dbHelper;
            this.mContext = context;
            contentResolver = context.getContentResolver();
            PersistentLoader.initLoader(context);
            persistentAppEndData = (PersistentAppEndData) PersistentLoader.loadPersistent(DbParams.TABLE_APP_END_DATA);
            persistentAppStartTime = (PersistentAppStartTime) PersistentLoader.loadPersistent(DbParams.TABLE_APP_START_TIME);
            persistentAppPaused = (PersistentAppPaused) PersistentLoader.loadPersistent(DbParams.TABLE_APP_END_TIME);
            persistentLoginId = (PersistentLoginId) PersistentLoader.loadPersistent(DbParams.TABLE_LOGIN_ID);
            persistentFlushDataState = (PersistentFlushDataState) PersistentLoader.loadPersistent(DbParams.TABLE_SUB_PROCESS_FLUSH_DATA);
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
                            OldBDatabaseHelper oldBDatabaseHelper = new OldBDatabaseHelper(context, packageName);
                            SQLiteDatabase database = getWritableDatabase();
                            if (database != null) {
                                JSONArray oldEvents = oldBDatabaseHelper.getAllEvents();
                                for (int i = 0; i < oldEvents.length(); i++) {
                                    JSONObject jsonObject = oldEvents.getJSONObject(i);
                                    final ContentValues cv = new ContentValues();
                                    cv.put(DbParams.KEY_DATA, jsonObject.getString(DbParams.KEY_DATA));
                                    cv.put(DbParams.KEY_CREATED_AT, jsonObject.getString(DbParams.KEY_CREATED_AT));
                                    database.insert(DbParams.TABLE_EVENTS, "_id", cv);
                                }
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
            uriMatcher.addURI(authority, DbParams.TABLE_APP_END_DATA, URI_CODE.APP_END_DATA);
            uriMatcher.addURI(authority, DbParams.TABLE_APP_END_TIME, URI_CODE.APP_PAUSED_TIME);
            uriMatcher.addURI(authority, DbParams.TABLE_SESSION_INTERVAL_TIME, URI_CODE.SESSION_INTERVAL_TIME);
            uriMatcher.addURI(authority, DbParams.TABLE_LOGIN_ID, URI_CODE.LOGIN_ID);
            uriMatcher.addURI(authority, DbParams.TABLE_CHANNEL_PERSISTENT, URI_CODE.CHANNEL_PERSISTENT);
            uriMatcher.addURI(authority, DbParams.TABLE_SUB_PROCESS_FLUSH_DATA, URI_CODE.FLUSH_DATA);
            uriMatcher.addURI(authority, DbParams.TABLE_FIRST_PROCESS_START, URI_CODE.FIRST_PROCESS_START);
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
            if (database == null || !values.containsKey(DbParams.KEY_CHANNEL_EVENT_NAME)
                    || !values.containsKey(DbParams.KEY_CHANNEL_RESULT)) {
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
                    persistentAppStartTime.commit(values.getAsLong(DbParams.TABLE_APP_START_TIME));
                    break;
                case URI_CODE.APP_PAUSED_TIME:
                    persistentAppPaused.commit(values.getAsLong(DbParams.TABLE_APP_END_TIME));
                    break;
                case URI_CODE.APP_END_DATA:
                    persistentAppEndData.commit(values.getAsString(DbParams.TABLE_APP_END_DATA));
                    break;
                case URI_CODE.SESSION_INTERVAL_TIME:
                    mSessionTime = values.getAsInteger(DbParams.TABLE_SESSION_INTERVAL_TIME);
                    contentResolver.notifyChange(uri, null);
                    break;
                case URI_CODE.LOGIN_ID:
                    persistentLoginId.commit(values.getAsString(DbParams.TABLE_LOGIN_ID));
                    contentResolver.notifyChange(uri, null);
                    break;
                case URI_CODE.FLUSH_DATA:
                    persistentFlushDataState.commit(values.getAsBoolean(DbParams.TABLE_SUB_PROCESS_FLUSH_DATA));
                    break;
                case URI_CODE.FIRST_PROCESS_START:
                    isFirstProcessStarted = values.getAsBoolean(DbParams.TABLE_FIRST_PROCESS_START);
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
    public Cursor queryPersistent(int code) {
        try {
            String column = null;
            Object data = null;
            switch (code) {
                case URI_CODE.ACTIVITY_START_COUNT:
                    data = startActivityCount;
                    column = DbParams.TABLE_ACTIVITY_START_COUNT;
                    break;
                case URI_CODE.APP_START_TIME:
                    data = persistentAppStartTime.get();
                    column = DbParams.TABLE_APP_START_TIME;
                    break;
                case URI_CODE.APP_PAUSED_TIME:
                    data = persistentAppPaused.get();
                    column = DbParams.TABLE_APP_END_TIME;
                    break;
                case URI_CODE.APP_END_DATA:
                    data = persistentAppEndData.get();
                    column = DbParams.TABLE_APP_END_DATA;
                    break;
                case URI_CODE.SESSION_INTERVAL_TIME:
                    data = mSessionTime;
                    column = DbParams.TABLE_SESSION_INTERVAL_TIME;
                    break;
                case URI_CODE.LOGIN_ID:
                    data = persistentLoginId.get();
                    column = DbParams.TABLE_LOGIN_ID;
                    break;
                case URI_CODE.FLUSH_DATA:
                    synchronized (SensorsDataContentProvider.class) {
                        if (persistentFlushDataState.get()) {
                            data = 1;
                        } else {
                            data = 0;
                            persistentFlushDataState.commit(true);
                        }
                        column = DbParams.TABLE_SUB_PROCESS_FLUSH_DATA;
                    }
                    break;
                case URI_CODE.FIRST_PROCESS_START:
                    data = isFirstProcessStarted ? 1 : 0;
                    column = DbParams.TABLE_FIRST_PROCESS_START;
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

    /**
     * URI 对应的 Code
     */
    public interface URI_CODE {
        int EVENTS = 1;
        int ACTIVITY_START_COUNT = 2;
        int APP_START_TIME = 3;
        int APP_END_DATA = 4;
        int APP_PAUSED_TIME = 5;
        int SESSION_INTERVAL_TIME = 6;
        int LOGIN_ID = 7;
        int CHANNEL_PERSISTENT = 8;
        int FLUSH_DATA = 9;
        int FIRST_PROCESS_START = 10;
    }
}
