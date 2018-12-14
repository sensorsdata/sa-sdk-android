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
import java.util.List;

/* package */ class DbAdapter {

    private static final String TAG = "SA.DbAdapter";
    private final File mDatabaseFile;
    private final Uri mUri, mAppStartUri, mAppStartTimeUri, mAppPausedUri, mAppEndStateUri, mAppEndDataUri, mSessionTimeUri;
    /** Session 时长间隔 */
    private int mSessionTime = 30 * 1000, mSavedSessionTime = 0;
    /** AppPaused 的时间戳 */
    private long mAppPausedTime = 0;
    /** AppEnd 事件是否发送，true 发送、false 未发送 */
    private boolean mAppEndState = true;
    /** App 是否启动到 onResume*/
    private boolean mAppStart = false;
    enum Table {
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

    static final String KEY_DATA = "data";
    static final String KEY_CREATED_AT = "created_at";
    static final String DATABASE_NAME = "sensorsdata";
    static final String APP_STARTED = "$app_started";
    static final String APP_START_TIME = "$app_start_time";
    static final String APP_END_STATE = "$app_end_state";
    static final String APP_END_DATA = "$app_end_data";
    static final String APP_PAUSED_TIME = "$app_paused_time";
    static final String SESSION_INTERVAL_TIME = "$session_interval_time";
    private static final int DB_UPDATE_ERROR = -1;
    static final int DB_OUT_OF_MEMORY_ERROR = -2;

    private final Context mContext;

    private long getMaxCacheSize(Context context) {
        try {
            return SensorsDataAPI.sharedInstance(context).getMaxCacheSize();
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
            return 32 * 1024 * 1024;
        }
    }

    private boolean belowMemThreshold() {
        if (mDatabaseFile.exists()) {
            return Math.max(
                    mDatabaseFile.getUsableSpace(),
                    getMaxCacheSize(mContext)
            ) < mDatabaseFile.length();
        }
        return false;
    }

    private ContentResolver contentResolver;

    public DbAdapter(Context context, String packageName) {
        mContext = context;
        contentResolver = mContext.getContentResolver();
        mDatabaseFile = context.getDatabasePath(DbAdapter.DATABASE_NAME);
        mUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + Table.EVENTS.getName());
        mAppStartUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + Table.APPSTARTED.getName());
        mAppStartTimeUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + Table.APPSTARTTIME.getName());
        mAppEndStateUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + Table.APPENDSTATE.getName());
        mAppEndDataUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + Table.APPENDDATA.getName());
        mAppPausedUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + Table.APPPAUSED.getName());
        mSessionTimeUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + Table.SESSIONINTERVALTIME.getName());
    }

    /**
     * Adds a JSON string representing an event with properties or a person record
     * to the SQLiteDatabase.
     *
     * @param j     the JSON to record
     * @return the number of rows in the table, or DB_OUT_OF_MEMORY_ERROR/DB_UPDATE_ERROR
     * on failure
     */
    int addJSON(JSONObject j) {
        // we are aware of the race condition here, but what can we do..?
        int count = DB_UPDATE_ERROR;
        Cursor c = null;
        try {
            if (belowMemThreshold()) {
                SALog.i(TAG, "There is not enough space left on the device to store events, so will delete some old events");
                String[] eventsData = generateDataString(DbAdapter.Table.EVENTS, 100);
                if (eventsData == null) {
                    return DB_OUT_OF_MEMORY_ERROR;
                }

                final String lastId = eventsData[0];
                count = cleanupEvents(lastId);
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
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return count;
    }

    /**
     * Adds a JSON string representing an event with properties or a person record
     * to the SQLiteDatabase.
     *
     * @param eventsList  the JSON to record
     * @return the number of rows in the table, or DB_OUT_OF_MEMORY_ERROR/DB_UPDATE_ERROR
     * on failure
     */
    int addJSON(List<JSONObject> eventsList) {
        // we are aware of the race condition here, but what can we do..?
        int count = DB_UPDATE_ERROR;
        Cursor c = null;
        try {
            if (belowMemThreshold()) {
                SALog.i(TAG, "There is not enough space left on the device to store events, so will delete some old events");
                String[] eventsData = generateDataString(DbAdapter.Table.EVENTS, 100);
                if (eventsData == null) {
                    return DB_OUT_OF_MEMORY_ERROR;
                }
                final String lastId = eventsData[0];
                count = cleanupEvents(lastId);
                if (count <= 0) {
                    return DB_OUT_OF_MEMORY_ERROR;
                }
            }
            ContentValues[] contentValues = new ContentValues[eventsList.size()];
            ContentValues cv;
            int index = 0;
            for(JSONObject j : eventsList) {
                cv = new ContentValues();
                cv.put(KEY_DATA, j.toString() + "\t" + j.toString().hashCode());
                cv.put(KEY_CREATED_AT, System.currentTimeMillis());
                contentValues[index++] = cv;
            }
            contentResolver.bulkInsert(mUri, contentValues);
            c = contentResolver.query(mUri, null, null, null, null);
            if (c != null) {
                count = c.getCount();
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
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
    void deleteAllEvents() {
        try {
            contentResolver.delete(mUri, null, new String[]{});
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    /**
     * Removes events with an _id <= last_id from table
     *
     * @param last_id the last id to delete
     * @return the number of rows in the table
     */
    int cleanupEvents(String last_id) {
        Cursor c = null;
        int count = DB_UPDATE_ERROR;

        try {
            contentResolver.delete(mUri, "_id <= ?", new String[]{last_id});
            c = contentResolver.query(mUri, null, null, null, null);
            if (c != null) {
                count = c.getCount();
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
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
    void commitAppStart(boolean appStart){
        ContentValues contentValues = new ContentValues();
        contentValues.put(APP_STARTED, appStart);
        contentResolver.insert(mAppStartUri, contentValues);
        mAppStart = appStart;
    }

    /**
     * return the state of Activity start
     * @return Activity count
     */
    boolean getAppStart(){
        return mAppStart;
    }

    /**
     * add the Activity start time to the SharedPreferences
     * @param appStartTime the Activity start time
     */
    void commitAppStartTime(long appStartTime){
        ContentValues contentValues = new ContentValues();
        contentValues.put(APP_START_TIME, appStartTime);
        contentResolver.insert(mAppStartTimeUri, contentValues);
    }

    /**
     * return the time of Activity start
     * @return
     */
    long getAppStartTime(){
        long startTime = 0;
        Cursor cursor = contentResolver.query(mAppStartTimeUri, new String[]{APP_START_TIME},null,null,null);
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
    void commitAppPausedTime(long appPausedTime){
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(APP_PAUSED_TIME, appPausedTime);
            contentResolver.insert(mAppPausedUri, contentValues);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        mAppPausedTime = appPausedTime;
    }

    /**
     * return the time of Activity Paused
     * @return Activity End state
     */
    long getAppPausedTime(){
        if (System.currentTimeMillis() - mAppPausedTime > mSessionTime) {
            Cursor cursor = null;
            try {
                cursor = contentResolver.query(mAppPausedUri, new String[]{APP_PAUSED_TIME}, null, null, null);
                if (cursor != null && cursor.getCount() > 0) {
                    while (cursor.moveToNext()) {
                        mAppPausedTime = cursor.getLong(0);
                    }
                }
            } catch (Exception e) {
                com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
            } finally {
                if(cursor != null){
                    cursor.close();
                }
            }
        }
        return mAppPausedTime;
    }

    /**
     * add the Activity End to the SharedPreferences
     * @param appEndState the Activity end state
     */
    void commitAppEndState(boolean appEndState){
        if (appEndState == mAppEndState)return;
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(APP_END_STATE, appEndState);
            contentResolver.insert(mAppEndStateUri, contentValues);
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }

        mAppEndState = appEndState;
    }

    /**
     * return the state of $AppEnd
     * @return Activity End state
     */
    boolean getAppEndState(){
        Cursor cursor = null;
        if (mAppEndState) {
            try {
                cursor = contentResolver.query(mAppEndStateUri, new String[]{APP_END_STATE}, null, null, null);
                if (cursor != null && cursor.getCount() > 0) {
                    while (cursor.moveToNext()) {
                        mAppEndState = cursor.getInt(0) > 0;
                    }
                }
            } catch (Exception e) {
                com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        SALog.d(TAG,"getAppEndState:" + mAppEndState);
        return mAppEndState;
    }

    /**
     * add the Activity End Data to the SharedPreferences
     * @param appEndData $AppEnd
     */
    void commitAppEndData(String appEndData){
        ContentValues contentValues = new ContentValues();
        contentValues.put(APP_END_DATA, appEndData);
        contentResolver.insert(mAppEndDataUri, contentValues);
    }

    /**
     * return the $AppEnd
     * @return Activity count
     */
    String getAppEndData(){
        String data = "";
        Cursor cursor = contentResolver.query(mAppEndDataUri, new String[]{APP_END_DATA},null,null,null);
        if(cursor != null && cursor.getCount() > 0){
            while(cursor.moveToNext()){
                data = cursor.getString(0);
            }
        }

        if (cursor != null) {
            cursor.close();
        }
        SALog.d(TAG,"getAppEndData:" + data);
        return data;
    }


    /**
     * add the session interval time to the SharedPreferences
     * @param sessionIntervalTime session interval time
     */
    void commitSessionIntervalTime(int sessionIntervalTime){
        if (sessionIntervalTime == mSavedSessionTime)return;
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(SESSION_INTERVAL_TIME, sessionIntervalTime);
            contentResolver.insert(mSessionTimeUri, contentValues);
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
        mSavedSessionTime = sessionIntervalTime;
    }

    /**
     * return the $AppEnd
     * @return Activity count
     */
    int getSessionIntervalTime(){
        if (mSessionTime != mSavedSessionTime) {
            Cursor cursor = null;
            try {
                cursor = contentResolver.query(mSessionTimeUri, new String[]{SESSION_INTERVAL_TIME}, null, null, null);
                if (cursor != null && cursor.getCount() > 0) {
                    while (cursor.moveToNext()) {
                        mSessionTime = cursor.getInt(0);
                    }
                }
            } catch (Exception e) {
                com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        mSavedSessionTime = mSessionTime;
        SALog.d(TAG,"getSessionIntervalTime:" + mSessionTime);
        return mSessionTime;
    }

    String[] generateDataString(Table table, int limit) {
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
            return new String[]{last_id, data};
        }
        return null;
    }

    Uri getAppStartUri() {
        return mAppStartUri;
    }

    Uri getIntervalTimeUri() {
        return mSessionTimeUri;
    }

    Uri getAppEndStateUri() {
        return mAppEndStateUri;
    }
}
