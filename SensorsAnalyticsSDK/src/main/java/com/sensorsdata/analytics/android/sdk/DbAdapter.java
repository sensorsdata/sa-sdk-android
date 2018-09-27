/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/* package */ class DbAdapter {

    private static final String TAG = "SA.DbAdapter";
    private final File mDatabaseFile;
    private Uri mUri, mAppStart, mAppStartTime, mAppPaused, mAppEndState, mAppEndData, mSessionIntervalTime;

    public enum Table {
        EVENTS("events"),
        APPSTARTED("app_started"),
        APPSTARTTIME("app_start_time"),
        APPPAUSED("app_paused_time"),
        APPENDSTATE("app_end_state"),
        APPENDDATA("app_end_data"),
        SESSIONINTERVALTIME("session_interval_time");

        Table(String name) {
            mTableName = name;
        }

        public String getName() {
            return mTableName;
        }

        private final String mTableName;
    }

    public static final String KEY_DATA = "data";
    public static final String KEY_CREATED_AT = "created_at";
    public static final String DATABASE_NAME = "sensorsdata";
    public static final String APP_STARTED = "$app_started";
    public static final String APP_START_TIME = "$app_start_time";
    public static final String APP_END_STATE = "$app_end_state";
    public static final String APP_END_DATA = "$app_end_data";
    public static final String APP_PAUSED_TIME = "$app_paused_time";
    public static final String SESSION_INTERVAL_TIME = "$session_interval_time";
    public static final int DB_UPDATE_ERROR = -1;
    public static final int DB_OUT_OF_MEMORY_ERROR = -2;

    private final Context mContext;

    private long getMaxCacheSize(Context context) {
        try {
            return SensorsDataAPI.sharedInstance(context).getMaxCacheSize();
        } catch (Exception e) {
            e.printStackTrace();
            return 32 * 1024 * 1024;
        }
    }

    private boolean belowMemThreshold() {
        if (mDatabaseFile.exists()) {
            return Math.max(
                    mDatabaseFile.getUsableSpace(),
                    getMaxCacheSize(mContext)
            ) >= mDatabaseFile.length();
        }
        return true;
    }

    private ContentResolver contentResolver;

    public DbAdapter(Context context, String packageName) {
        mContext = context;
        contentResolver = mContext.getContentResolver();
        mDatabaseFile = context.getDatabasePath(DbAdapter.DATABASE_NAME);
        mUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + Table.EVENTS.getName());
        mAppStart = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + Table.APPSTARTED.getName());
        mAppStartTime = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + Table.APPSTARTTIME.getName());
        mAppEndState = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + Table.APPENDSTATE.getName());
        mAppEndData = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + Table.APPENDDATA.getName());
        mAppPaused = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + Table.APPPAUSED.getName());
        mSessionIntervalTime = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + Table.SESSIONINTERVALTIME.getName());
    }

    /**
     * Adds a JSON string representing an event with properties or a person record
     * to the SQLiteDatabase.
     *
     * @param j     the JSON to record
     * @param table the table to insert into, either "events" or "people"
     * @return the number of rows in the table, or DB_OUT_OF_MEMORY_ERROR/DB_UPDATE_ERROR
     * on failure
     */
    public int addJSON(JSONObject j, Table table) {
        // we are aware of the race condition here, but what can we do..?
        int count = DB_UPDATE_ERROR;
        Cursor c = null;
        try {
            if (!belowMemThreshold()) {
                SALog.i(TAG, "There is not enough space left on the device to store events, so will delete some old events");
                String[] eventsData = generateDataString(DbAdapter.Table.EVENTS, 100);
                if (eventsData == null) {
                    return DB_OUT_OF_MEMORY_ERROR;
                }
                final String lastId = eventsData[0];
                count = cleanupEvents(lastId, DbAdapter.Table.EVENTS);
                if (count <= 0) {
                    return DB_OUT_OF_MEMORY_ERROR;
                }
            }

            final ContentValues cv = new ContentValues();
            cv.put(KEY_DATA, j.toString() + "\t" + j.toString().hashCode());
            cv.put(KEY_CREATED_AT, System.currentTimeMillis());
            contentResolver.insert(mUri, cv);
            c = contentResolver.query(mUri, null, null, null, null);
            if (c != null) {
                count = c.getCount();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } finally {

            }
        }
        return count;
    }

    /**
     * Removes all events from table
     *
     */
    public void deleteAllEvents() {
        try {
            contentResolver.delete(mUri, null, new String[]{});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes events with an _id <= last_id from table
     *
     * @param last_id the last id to delete
     * @param table   the table to remove events from
     * @return the number of rows in the table
     */
    public int cleanupEvents(String last_id, Table table) {
        Cursor c = null;
        int count = DB_UPDATE_ERROR;

        try {
            contentResolver.delete(mUri, "_id <= ?", new String[]{last_id});
            c = contentResolver.query(mUri, null, null, null, null);
            if (c != null) {
                count = c.getCount();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } finally {

            }
        }
        return count;
    }

    /**
     * add the ActivityStart state to the SharedPreferences
     * @param appStart the ActivityState
     */
    public void commitAppStart(boolean appStart){
        ContentValues contentValues = new ContentValues();
        contentValues.put(APP_STARTED, appStart);
        contentResolver.insert(mAppStart, contentValues);
    }

    /**
     * return the state of Activity start
     * @return Activity count
     */
    public boolean getAppStart(){
        boolean state = true;
        Cursor cursor = contentResolver.query(mAppStart, new String[]{APP_STARTED},null,null,null);
        if(cursor != null && cursor.getCount() > 0){
            while(cursor.moveToNext()){
                state = cursor.getInt(0) > 0;
            }
        }

        if(cursor != null){
            cursor.close();
        }
        SALog.d(TAG,"getAppStart:" + state);
        return state;
    }

    /**
     * add the Activity start time to the SharedPreferences
     * @param appStartTime the Activity start time
     */
    public void commitAppStartTime(long appStartTime){
        ContentValues contentValues = new ContentValues();
        contentValues.put(APP_START_TIME, appStartTime);
        contentResolver.insert(mAppStartTime, contentValues);
    }

    /**
     * return the time of Activity start
     * @return
     */
    public long getAppStartTime(){
        long startTime = 0;
        Cursor cursor = contentResolver.query(mAppStartTime, new String[]{APP_START_TIME},null,null,null);
        if(cursor != null && cursor.getCount() > 0){
            while(cursor.moveToNext()){
                startTime = cursor.getLong(0);
            }
        }

        if(cursor != null){
            cursor.close();
        }
        SALog.d(TAG,"getAppStartTime:" + startTime);
        return startTime;
    }

    /**
     * add the Activity Paused time to the SharedPreferences
     * @param appPausedTime the Activity paused time
     */
    public void commitAppPausedTime(long appPausedTime){
        ContentValues contentValues = new ContentValues();
        contentValues.put(APP_PAUSED_TIME, appPausedTime);
        contentResolver.insert(mAppPaused, contentValues);
    }

    /**
     * return the time of Activity Paused
     * @return Activity End state
     */
    public long getAppPausedTime(){
        long pausedTime = 0;
        Cursor cursor = contentResolver.query(mAppPaused, new String[]{APP_PAUSED_TIME},null,null,null);
        if(cursor != null && cursor.getCount() > 0){
            while(cursor.moveToNext()){
                pausedTime = cursor.getLong(0);
            }
        }

        if(cursor != null){
            cursor.close();
        }
        return pausedTime;
    }

    /**
     * add the Activity End to the SharedPreferences
     * @param appEndState the Activity end state
     */
    public void commitAppEndState(boolean appEndState){
        ContentValues contentValues = new ContentValues();
        contentValues.put(APP_END_STATE, appEndState);
        contentResolver.insert(mAppEndState, contentValues);
    }

    /**
     * return the state of $AppEnd
     * @return Activity End state
     */
    public boolean getAppEndState(){
        boolean state = true;
        Cursor cursor = contentResolver.query(mAppEndState, new String[]{APP_END_STATE},null,null,null);
        if(cursor != null && cursor.getCount() > 0){
            while(cursor.moveToNext()){
                state = cursor.getInt(0) > 0;
            }
        }

        if(cursor != null){
            cursor.close();
        }
        SALog.d(TAG,"getAppEndState:" + state);
        return state;
    }

    /**
     * add the Activity End Data to the SharedPreferences
     * @param appEndData $AppEnd
     */
    public void commitAppEndData(String appEndData){
        ContentValues contentValues = new ContentValues();
        contentValues.put(APP_END_DATA, appEndData);
        contentResolver.insert(mAppEndData, contentValues);
    }

    /**
     * return the $AppEnd
     * @return Activity count
     */
    public String getAppEndData(){
        String data = "";
        Cursor cursor = contentResolver.query(mAppEndData, new String[]{APP_END_DATA},null,null,null);
        if(cursor != null && cursor.getCount() > 0){
            while(cursor.moveToNext()){
                data = cursor.getString(0);
            }
        }

        if(cursor != null){
            cursor.close();
        }
        SALog.d(TAG,"getAppEndData:" + data);
        return data;
    }


    /**
     * add the session interval time to the SharedPreferences
     * @param sessionIntervalTime session interval time
     */
    public void commitSessionIntervalTime(int sessionIntervalTime){
        ContentValues contentValues = new ContentValues();
        contentValues.put(SESSION_INTERVAL_TIME, sessionIntervalTime);
        contentResolver.insert(mSessionIntervalTime, contentValues);
    }

    /**
     * return the $AppEnd
     * @return Activity count
     */
    public int getSessionIntervalTime(){
        int sessionIntervalTime = 30 * 1000;
        Cursor cursor = contentResolver.query(mSessionIntervalTime, new String[]{SESSION_INTERVAL_TIME},null,null,null);
        if(cursor != null && cursor.getCount() > 0){
            while(cursor.moveToNext()){
                sessionIntervalTime = cursor.getInt(0);
            }
        }

        if(cursor != null){
            cursor.close();
        }
        SALog.d(TAG,"getSessionIntervalTime:" + sessionIntervalTime);
        return sessionIntervalTime;
    }

    public String[] generateDataString(Table table, int limit) {
        Cursor c = null;
        String data = null;
        String last_id = null;
        final String tableName = table.getName();
        try {
            c = contentResolver.query(mUri, null, null, null, KEY_CREATED_AT + " ASC LIMIT " + String.valueOf(limit));
            final JSONArray arr = new JSONArray();

            if (c != null) {
                while (c.moveToNext()) {
                    if (c.isLast()) {
                        last_id = c.getString(c.getColumnIndex("_id"));
                    }
                    try {
                        String keyData = c.getString(c.getColumnIndex(KEY_DATA));
                        if (!TextUtils.isEmpty(keyData)) {
                            JSONObject j = null;
                            int index = keyData.lastIndexOf("\t");
                            if (index > -1) {
                                String crc = keyData.substring(index).replaceFirst("\t", "");
                                String content = keyData.substring(0, index);
                                if (!TextUtils.isEmpty(content) && !TextUtils.isEmpty(crc)) {
                                    if (crc.equals(String.valueOf(content.hashCode()))) {
                                        j = new JSONObject(content);
                                    }
                                }
                            } else {
                                j = new JSONObject(keyData);
                            }
                            if (j != null) {
                                j.put("_flush_time", System.currentTimeMillis());
                                arr.put(j);
                            }
                        }
                    } catch (final JSONException e) {
                        // Ignore this object
                    }
                }

                if (arr.length() > 0) {
                    data = arr.toString();
                }
            }
        } catch (final SQLiteException e) {
            SALog.i(TAG, "Could not pull records for SensorsData out of database " + tableName
                    + ". Waiting to send.", e);
            last_id = null;
            data = null;
        } finally {
            if (c != null) {
                c.close();
            }
        }

        if (last_id != null && data != null) {
            final String[] ret = {last_id, data};
            return ret;
        }
        return null;
    }

    public Uri getAppStartUri() {
        return mAppStart;
    }

    public Uri getIntervalTimeUri() {
        return mSessionIntervalTime;
    }

    public Uri getAppEndStateUri() {
        return mAppEndState;
    }
}
