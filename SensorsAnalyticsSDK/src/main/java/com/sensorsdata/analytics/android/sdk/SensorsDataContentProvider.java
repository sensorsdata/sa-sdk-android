/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.Future;

/**
 * Created by 王灼洲 on 2017/5/5
 */

public class SensorsDataContentProvider extends ContentProvider {
    private final static int EVENTS = 1;
    private final static int APP_START = 2;
    private final static int APP_START_TIME = 3;
    private final static int APP_END_STATE = 4;
    private final static int APP_END_DATA = 5;
    private final static int APP_PAUSED_TIME = 6;
    private final static int SESSION_INTERVAL_TIME = 7;
    private static UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        //uriMatcher.addURI("com.sensorsdata.analytics.android.sdk.ContentProvider", "events", EVENTS);
    }

    private SensorsDataDBHelper dbHelper;
    private ContentResolver contentResolver;
    private PersistentAppStart persistentAppStart;
    private PersistentAppStartTime persistentAppStartTime;
    private PersistentAppEndEventState persistentAppEndEventState;
    private PersistentAppEndData persistentAppEndData;
    private PersistentAppPaused persistentAppPaused;
    private PersistentSessionIntervalTime persistentSessionIntervalTime;
    private static final SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();
    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context != null) {
            String packageName = context.getApplicationContext().getPackageName();
            contentResolver = context.getContentResolver();
            uriMatcher.addURI(packageName + ".SensorsDataContentProvider", "events", EVENTS);
            uriMatcher.addURI(packageName + ".SensorsDataContentProvider", DbAdapter.Table.APPSTARTED.getName(), APP_START);
            uriMatcher.addURI(packageName + ".SensorsDataContentProvider", DbAdapter.Table.APPSTARTTIME.getName(), APP_START_TIME);
            uriMatcher.addURI(packageName + ".SensorsDataContentProvider", DbAdapter.Table.APPENDSTATE.getName(), APP_END_STATE);
            uriMatcher.addURI(packageName + ".SensorsDataContentProvider", DbAdapter.Table.APPENDDATA.getName(), APP_END_DATA);
            uriMatcher.addURI(packageName + ".SensorsDataContentProvider", DbAdapter.Table.APPPAUSED.getName(), APP_PAUSED_TIME);
            uriMatcher.addURI(packageName + ".SensorsDataContentProvider", DbAdapter.Table.SESSIONINTERVALTIME.getName(), SESSION_INTERVAL_TIME);
            dbHelper = new SensorsDataDBHelper(context);

            /**
             * 迁移数据，并删除老的数据库
             */
            try {
                File oldDatabase = context.getDatabasePath(packageName);
                if (oldDatabase.exists()) {
                    OldBDatabaseHelper oldBDatabaseHelper = new OldBDatabaseHelper(context, packageName);

                    JSONArray oldEvents = oldBDatabaseHelper.getAllEvents();
                    for (int i = 0; i< oldEvents.length(); i++) {
                        JSONObject jsonObject = oldEvents.getJSONObject(i);
                        final ContentValues cv = new ContentValues();
                        cv.put(DbAdapter.KEY_DATA, jsonObject.getString(DbAdapter.KEY_DATA));
                        cv.put(DbAdapter.KEY_CREATED_AT, jsonObject.getString(DbAdapter.KEY_CREATED_AT));

                        SQLiteDatabase database = dbHelper.getWritableDatabase();
                        database.insert(DbAdapter.Table.EVENTS.getName(), "_id", cv);
                    }
                }

                context.deleteDatabase(packageName);
            } catch (Exception e) {
                e.printStackTrace();
            }

            final SharedPreferencesLoader.OnPrefsLoadedListener listener =
                    new SharedPreferencesLoader.OnPrefsLoadedListener() {
                        @Override
                        public void onPrefsLoaded(SharedPreferences preferences) {
                        }
                    };

            final String prefsName = "com.sensorsdata.analytics.android.sdk.SensorsDataAPI";
            final Future<SharedPreferences> storedPreferences =
                    sPrefsLoader.loadPreferences(context, prefsName, listener);
            persistentAppStart = new PersistentAppStart(storedPreferences);
            persistentAppEndEventState = new PersistentAppEndEventState(storedPreferences);
            persistentAppEndData = new PersistentAppEndData(storedPreferences);
            persistentAppStartTime = new PersistentAppStartTime(storedPreferences);
            persistentAppPaused = new PersistentAppPaused(storedPreferences);
            persistentSessionIntervalTime = new PersistentSessionIntervalTime(storedPreferences);
        }
        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int id = 0;
        SQLiteDatabase database = null;
        try {
            database = dbHelper.getWritableDatabase();
            database.delete(DbAdapter.Table.EVENTS.getName(), "_id <= ?", selectionArgs);
            //contentResolver.notifyChange(uri, null);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
//            try {
//                if (database != null) {
//                    database.close();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
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
        Uri u = null;
        SQLiteDatabase database = null;
        try {
            if (uriMatcher.match(uri) == EVENTS) {
                database = dbHelper.getWritableDatabase();
                long d = database.insert(DbAdapter.Table.EVENTS.getName(), "_id", values);
                u = ContentUris.withAppendedId(uri, d);
                //contentResolver.notifyChange(u, null);
            } else if (uriMatcher.match(uri) == APP_START) {
                boolean state = values.getAsBoolean(DbAdapter.APP_STARTED);
                persistentAppStart.commit(state);
                contentResolver.notifyChange(uri,null);
            } else if (uriMatcher.match(uri) == APP_START_TIME) {
                long startTime = values.getAsLong(DbAdapter.APP_START_TIME);
                persistentAppStartTime.commit(startTime);
            } else if (uriMatcher.match(uri) == APP_END_STATE) {
                boolean state = values.getAsBoolean(DbAdapter.APP_END_STATE);
                persistentAppEndEventState.commit(state);
                if (state) {
                    contentResolver.notifyChange(uri, null);
                }
            } else if (uriMatcher.match(uri) == APP_END_DATA) {
                String data = values.getAsString(DbAdapter.APP_END_DATA);
                persistentAppEndData.commit(data);
            } else if (uriMatcher.match(uri) == APP_PAUSED_TIME) {
                long pausedTime = values.getAsLong(DbAdapter.APP_PAUSED_TIME);
                persistentAppPaused.commit(pausedTime);
            } else if (uriMatcher.match(uri) == SESSION_INTERVAL_TIME) {
                int sessionIntervalTime = values.getAsInteger(DbAdapter.SESSION_INTERVAL_TIME);
                persistentSessionIntervalTime.commit(sessionIntervalTime);
                contentResolver.notifyChange(uri,null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
//            try {
//                if (database != null) {
//                    database.close();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }
        return u;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        MatrixCursor matrixCursor;
        try {
            if (uriMatcher.match(uri) == EVENTS) {
                SQLiteDatabase database = dbHelper.getReadableDatabase();
                cursor = database.query(DbAdapter.Table.EVENTS.getName(), projection, selection, selectionArgs, null, null, sortOrder);
                //cursor.setNotificationUri(contentResolver, uri);
            } else if (uriMatcher.match(uri) == APP_START) {
                int state = persistentAppStart.get() ? 1 : 0;
                matrixCursor = new MatrixCursor(new String[]{DbAdapter.APP_STARTED});
                matrixCursor.addRow(new Object[]{state});
                return matrixCursor;
            } else if (uriMatcher.match(uri) == APP_START_TIME) {
                long startTime = persistentAppStartTime.get();
                matrixCursor = new MatrixCursor(new String[]{DbAdapter.APP_START_TIME});
                matrixCursor.addRow(new Object[]{startTime});
                return matrixCursor;
            }else if (uriMatcher.match(uri) == APP_END_STATE) {
                int state = persistentAppEndEventState.get() ? 1 : 0;
                matrixCursor = new MatrixCursor(new String[]{DbAdapter.APP_END_STATE});
                matrixCursor.addRow(new Object[]{state});
                return matrixCursor;
            } else if (uriMatcher.match(uri) == APP_END_DATA) {
                String data = persistentAppEndData.get();
                matrixCursor = new MatrixCursor(new String[]{DbAdapter.APP_END_DATA});
                matrixCursor.addRow(new Object[]{data});
                return matrixCursor;
            } else if (uriMatcher.match(uri) == APP_PAUSED_TIME) {
                long pausedTime = persistentAppPaused.get();
                matrixCursor = new MatrixCursor(new String[]{DbAdapter.APP_PAUSED_TIME});
                matrixCursor.addRow(new Object[]{pausedTime});
                return matrixCursor;
            } else if (uriMatcher.match(uri) == SESSION_INTERVAL_TIME) {
                int state = persistentSessionIntervalTime.get();
                matrixCursor = new MatrixCursor(new String[]{DbAdapter.SESSION_INTERVAL_TIME});
                matrixCursor.addRow(new Object[]{state});
                return matrixCursor;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
