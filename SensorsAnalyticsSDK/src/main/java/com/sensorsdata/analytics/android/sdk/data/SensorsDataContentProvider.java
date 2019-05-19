/*
 * Created by wangzhuozhou on 2017/5/5.
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

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentAppEndData;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentAppEndEventState;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentAppPaused;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentAppStart;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentAppStartTime;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentSessionIntervalTime;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

public class SensorsDataContentProvider extends ContentProvider {
    private final static int EVENTS = 1;
    private final static int APP_START = 2;
    private final static int APP_START_TIME = 3;
    private final static int APP_END_STATE = 4;
    private final static int APP_END_DATA = 5;
    private final static int APP_PAUSED_TIME = 6;
    private final static int SESSION_INTERVAL_TIME = 7;
    private static UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private SensorsDataDBHelper dbHelper;
    private ContentResolver contentResolver;
    private PersistentAppStart persistentAppStart;
    private PersistentAppStartTime persistentAppStartTime;
    private PersistentAppEndEventState persistentAppEndEventState;
    private PersistentAppEndData persistentAppEndData;
    private PersistentAppPaused persistentAppPaused;
    private PersistentSessionIntervalTime persistentSessionIntervalTime;

    private boolean isDbWritable = true;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context != null) {
            String packageName = context.getApplicationContext().getPackageName();
            String authority = packageName + ".SensorsDataContentProvider";
            contentResolver = context.getContentResolver();
            uriMatcher.addURI(authority, DbParams.TABLE_EVENTS, EVENTS);
            uriMatcher.addURI(authority, DbParams.TABLE_APPSTARTED, APP_START);
            uriMatcher.addURI(authority, DbParams.TABLE_APPSTARTTIME, APP_START_TIME);
            uriMatcher.addURI(authority, DbParams.TABLE_APPENDSTATE, APP_END_STATE);
            uriMatcher.addURI(authority, DbParams.TABLE_APPENDDATA, APP_END_DATA);
            uriMatcher.addURI(authority, DbParams.TABLE_APPPAUSEDTIME, APP_PAUSED_TIME);
            uriMatcher.addURI(authority, DbParams.TABLE_SESSIONINTERVALTIME, SESSION_INTERVAL_TIME);
            dbHelper = new SensorsDataDBHelper(context);

            /**
             * 迁移数据，并删除老的数据库
             */
            try {
                File oldDatabase = context.getDatabasePath(packageName);
                if (oldDatabase.exists()) {
                    OldBDatabaseHelper oldBDatabaseHelper = new OldBDatabaseHelper(context, packageName);

                    JSONArray oldEvents = oldBDatabaseHelper.getAllEvents();
                    for (int i = 0; i < oldEvents.length(); i++) {
                        JSONObject jsonObject = oldEvents.getJSONObject(i);
                        final ContentValues cv = new ContentValues();
                        cv.put(DbParams.KEY_DATA, jsonObject.getString(DbParams.KEY_DATA));
                        cv.put(DbParams.KEY_CREATED_AT, jsonObject.getString(DbParams.KEY_CREATED_AT));

                        try {
                            SQLiteDatabase database = dbHelper.getWritableDatabase();
                            database.insert(DbParams.TABLE_EVENTS, "_id", cv);
                        } catch (SQLiteException e) {
                            isDbWritable = false;
                            SALog.printStackTrace(e);
                        }
                    }
                }
                if (isDbWritable) {
                    context.deleteDatabase(packageName);
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
            PersistentLoader.initLoader(context);
            persistentAppStart = (PersistentAppStart) PersistentLoader.loadPersistent(DbParams.TABLE_APPSTARTED);
            persistentAppEndEventState = (PersistentAppEndEventState) PersistentLoader.loadPersistent(DbParams.TABLE_APPENDSTATE);
            persistentAppEndData = (PersistentAppEndData) PersistentLoader.loadPersistent(DbParams.TABLE_APPENDDATA);
            persistentAppStartTime = (PersistentAppStartTime) PersistentLoader.loadPersistent(DbParams.TABLE_APPSTARTTIME);
            persistentAppPaused = (PersistentAppPaused) PersistentLoader.loadPersistent(DbParams.TABLE_APPPAUSEDTIME);
            persistentSessionIntervalTime = (PersistentSessionIntervalTime) PersistentLoader.loadPersistent(DbParams.TABLE_SESSIONINTERVALTIME);
        }
        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (!isDbWritable) {
            return 0;
        }
        int id = 0;
        SQLiteDatabase database = null;
        try {
            try {
                database = dbHelper.getWritableDatabase();
            } catch (SQLiteException e) {
                isDbWritable = false;
                SALog.printStackTrace(e);
            }
            database.delete(DbParams.TABLE_EVENTS, "_id <= ?", selectionArgs);
            //contentResolver.notifyChange(uri, null);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        } finally {
//            try {
//                if (database != null) {
//                    database.close();
//                }
//            } catch (Exception e) {
//                com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
//            }
        }
        return id;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (!isDbWritable) {
            return uri;
        }
        try {
            int code = uriMatcher.match(uri);
            switch (code) {
                case EVENTS:
                    SQLiteDatabase database = null;
                    try {
                        database = dbHelper.getWritableDatabase();
                    } catch (SQLiteException e) {
                        isDbWritable = false;
                        SALog.printStackTrace(e);
                        return uri;
                    }
                    long d = database.insert(DbParams.TABLE_EVENTS, "_id", values);
                    return ContentUris.withAppendedId(uri, d);
                default:
                    insert(code, uri, values);
                    return uri;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        } finally {
//            try {
//                if (database != null) {
//                    database.close();
//                }
//            } catch (Exception e) {
//                com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
//            }
        }
        return uri;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        if (!isDbWritable) {
            return 0;
        }
        int numValues;
        SQLiteDatabase database = null;
        try {
            try {
                database = dbHelper.getWritableDatabase();
            } catch (SQLiteException e) {
                isDbWritable = false;
                SALog.printStackTrace(e);
                return 0;
            }
            database.beginTransaction();
            numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                insert(uri, values[i]);
            }
            database.setTransactionSuccessful();
        } finally {
            if (database != null) {
                database.endTransaction();
            }
        }
        return numValues;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (!isDbWritable) {
            return null;
        }
        Cursor cursor = null;
        try {
            int code = uriMatcher.match(uri);
            switch (code) {
                case EVENTS:
                    SQLiteDatabase database;
                    try {
                        database = dbHelper.getWritableDatabase();
                    } catch (SQLiteException e) {
                        isDbWritable = false;
                        SALog.printStackTrace(e);
                        return null;
                    }
                    cursor = database.query(DbParams.TABLE_EVENTS, projection, selection, selectionArgs, null, null, sortOrder);
                    break;
                default:
                    return query(code);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    /**
     * insert 处理
     *
     * @param code Uri code
     * @param uri Uri
     * @param values ContentValues
     */
    private void insert(int code, Uri uri, ContentValues values) {
        boolean state;
        switch (code) {
            case APP_START:
                state = values.getAsBoolean(DbParams.TABLE_APPSTARTED);
                persistentAppStart.commit(values.getAsBoolean(DbParams.TABLE_APPSTARTED));
                if (state) {
                    contentResolver.notifyChange(uri, null);
                }
                break;
            case APP_START_TIME:
                persistentAppStartTime.commit(values.getAsLong(DbParams.TABLE_APPSTARTTIME));
                break;
            case APP_PAUSED_TIME:
                persistentAppPaused.commit(values.getAsLong(DbParams.TABLE_APPPAUSEDTIME));
                break;
            case APP_END_STATE:
                state = values.getAsBoolean(DbParams.TABLE_APPENDSTATE);
                persistentAppEndEventState.commit(state);
                break;
            case APP_END_DATA:
                persistentAppEndData.commit(values.getAsString(DbParams.TABLE_APPENDDATA));
                break;
            case SESSION_INTERVAL_TIME:
                persistentSessionIntervalTime.commit(values.getAsInteger(DbParams.TABLE_SESSIONINTERVALTIME));
                contentResolver.notifyChange(uri, null);
                break;
            default:
                break;
        }
    }

    /**
     * query 处理
     *
     * @param code Uri code
     * @return Cursor
     */
    private Cursor query(int code) {
        String column = null;
        Object data = null;
        switch (code) {
            case APP_START:
                data = persistentAppStart.get() ? 1 : 0;
                column = DbParams.TABLE_APPSTARTED;
                break;
            case APP_START_TIME:
                data = persistentAppStartTime.get();
                column = DbParams.TABLE_APPSTARTTIME;
                break;
            case APP_PAUSED_TIME:
                data = persistentAppPaused.get();
                column = DbParams.TABLE_APPPAUSEDTIME;
                break;
            case APP_END_STATE:
                data = persistentAppEndEventState.get() ? 1 : 0;
                column = DbParams.TABLE_APPENDSTATE;
                break;
            case APP_END_DATA:
                data = persistentAppEndData.get();
                column = DbParams.TABLE_APPENDDATA;
                break;
            case SESSION_INTERVAL_TIME:
                data = persistentSessionIntervalTime.get();
                column = DbParams.TABLE_SESSIONINTERVALTIME;
                break;
            default:
                break;
        }

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{column});
        matrixCursor.addRow(new Object[]{data});
        return matrixCursor;
    }
}
