package com.sensorsdata.analytics.android.sdk;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

/**
 * Created by 王灼洲 on 2017/5/5
 */

public class SensorsDataContentProvider extends ContentProvider {
    private final static int EVENTS = 1;
    private static UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        //uriMatcher.addURI("com.sensorsdata.analytics.android.sdk.ContentProvider", "events", EVENTS);
    }

    private SensorsDataDBHelper dbHelper;
    private ContentResolver contentResolver;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context != null) {
            String packageName = context.getApplicationContext().getPackageName();
            contentResolver = context.getContentResolver();
            uriMatcher.addURI(packageName + ".SensorsDataContentProvider", "events", EVENTS);
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
        }
        return true;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
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

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        Uri u = null;
        SQLiteDatabase database = null;
        try {
            if (uriMatcher.match(uri) == EVENTS) {
                database = dbHelper.getWritableDatabase();
                long d = database.insert(DbAdapter.Table.EVENTS.getName(), "_id", values);
                u = ContentUris.withAppendedId(uri, d);
                //contentResolver.notifyChange(u, null);
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

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Cursor cursor = null;
        try {
            if (uriMatcher.match(uri) == EVENTS) {
                SQLiteDatabase database = dbHelper.getReadableDatabase();
                cursor = database.query(DbAdapter.Table.EVENTS.getName(), projection, selection, selectionArgs, null, null, sortOrder);
                //cursor.setNotificationUri(contentResolver, uri);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cursor;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
