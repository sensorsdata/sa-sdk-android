/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;

import org.json.JSONObject;

import java.io.File;
import java.util.List;

public class DbAdapter {
    private static final String TAG = "SA.DbAdapter";
    private static DbAdapter instance;
    private final File mDatabaseFile;
    private final DbParams mDbParams;
    /** Session 时长间隔 */
    private int mSessionTime = 30 * 1000, mSavedSessionTime = 0;
    /** AppPaused 的时间戳 */
    private long mAppPausedTime = 0;
    /** AppEnd 事件是否发送，true 发送、false 未发送 */
    private boolean mAppEndState = true;
    /** App 是否启动到 onResume*/
    private boolean mAppStart = false;
    private ContentResolver contentResolver;
    private final Context mContext;


    public static DbAdapter getInstance(Context context, String packageName) {
        if (instance == null) {
            instance = new DbAdapter(context, packageName);
        }
        return instance;
    }

    public static DbAdapter getInstance() {
        if (instance == null) {
            throw new IllegalStateException("The static method getInstance(Context context, String packageName) should be called before calling getInstance()");
        }
        return instance;
    }

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

    private DbAdapter(Context context, String packageName) {
        mContext = context;
        contentResolver = mContext.getContentResolver();
        mDatabaseFile = context.getDatabasePath(DbParams.DATABASE_NAME);
        mDbParams = DbParams.getInstance(packageName);
    }

    /**
     * Adds a JSON string representing an event with properties or a person record
     * to the SQLiteDatabase.
     *
     * @param j     the JSON to record
     * @return the number of rows in the table, or DB_OUT_OF_MEMORY_ERROR/DB_UPDATE_ERROR
     * on failure
     */
    public int addJSON(JSONObject j) {
        // we are aware of the race condition here, but what can we do..?
        int count = DbParams.DB_UPDATE_ERROR;
        Cursor c = null;
        try {
            if (belowMemThreshold()) {
                SALog.i(TAG, "There is not enough space left on the device to store events, so will delete some old events");
                String[] eventsData = generateDataString(DbParams.TABLE_EVENTS, 100);
                if (eventsData == null) {
                    return DbParams.DB_OUT_OF_MEMORY_ERROR;
                }

                final String lastId = eventsData[0];
                count = cleanupEvents(lastId);
                if (count <= 0) {
                    return DbParams.DB_OUT_OF_MEMORY_ERROR;
                }
            }

            final ContentValues cv = new ContentValues();
            cv.put(DbParams.KEY_DATA, j.toString() + "\t" + j.toString().hashCode());
            cv.put(DbParams.KEY_CREATED_AT, System.currentTimeMillis());
            contentResolver.insert(mDbParams.gemUri(), cv);
            c = contentResolver.query(mDbParams.gemUri(), null, null, null, null);
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
    public int addJSON(List<JSONObject> eventsList) {
        // we are aware of the race condition here, but what can we do..?
        int count = DbParams.DB_UPDATE_ERROR;
        Cursor c = null;
        try {
            if (belowMemThreshold()) {
                SALog.i(TAG, "There is not enough space left on the device to store events, so will delete some old events");
                String[] eventsData = generateDataString(DbParams.TABLE_EVENTS, 100);
                if (eventsData == null) {
                    return DbParams.DB_OUT_OF_MEMORY_ERROR;
                }
                final String lastId = eventsData[0];
                count = cleanupEvents(lastId);
                if (count <= 0) {
                    return DbParams.DB_OUT_OF_MEMORY_ERROR;
                }
            }
            ContentValues[] contentValues = new ContentValues[eventsList.size()];
            ContentValues cv;
            int index = 0;
            for(JSONObject j : eventsList) {
                cv = new ContentValues();
                cv.put(DbParams.KEY_DATA, j.toString() + "\t" + j.toString().hashCode());
                cv.put(DbParams.KEY_CREATED_AT, System.currentTimeMillis());
                contentValues[index++] = cv;
            }
            contentResolver.bulkInsert(mDbParams.gemUri(), contentValues);
            c = contentResolver.query(mDbParams.gemUri(), null, null, null, null);
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
    public void deleteAllEvents() {
        try {
            contentResolver.delete(mDbParams.gemUri(), null, new String[]{});
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    /**
     * Removes events with an _id &lt;= last_id from table
     *
     * @param last_id the last id to delete
     * @return the number of rows in the table
     */
    public int cleanupEvents(String last_id) {
        Cursor c = null;
        int count = DbParams.DB_UPDATE_ERROR;

        try {
            contentResolver.delete(mDbParams.gemUri(), "_id <= ?", new String[]{last_id});
            c = contentResolver.query(mDbParams.gemUri(), null, null, null, null);
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
    public void commitAppStart(boolean appStart){
        ContentValues contentValues = new ContentValues();
        contentValues.put(DbParams.TABLE_APPSTARTED, appStart);
        contentResolver.insert(mDbParams.getAppStartUri(), contentValues);
        mAppStart = appStart;
    }

    /**
     * return the state of Activity start
     * @return Activity count
     */
    public boolean getAppStart(){
        return mAppStart;
    }

    /**
     * add the Activity start time to the SharedPreferences
     * @param appStartTime the Activity start time
     */
    public void commitAppStartTime(long appStartTime){
        ContentValues contentValues = new ContentValues();
        contentValues.put(DbParams.TABLE_APPSTARTTIME, appStartTime);
        contentResolver.insert(mDbParams.getAppStartTimeUri(), contentValues);
    }

    /**
     * return the time of Activity start
     * @return getAppStartTime
     */
    public long getAppStartTime(){
        long startTime = 0;
        Cursor cursor = contentResolver.query(mDbParams.getAppStartTimeUri(), new String[]{DbParams.TABLE_APPSTARTTIME},null,null,null);
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
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DbParams.TABLE_APPPAUSEDTIME, appPausedTime);
            contentResolver.insert(mDbParams.getAppPausedUri(), contentValues);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        mAppPausedTime = appPausedTime;
    }

    /**
     * return the time of Activity Paused
     * @return Activity End state
     */
    public long getAppPausedTime(){
        if (System.currentTimeMillis() - mAppPausedTime > mSessionTime) {
            Cursor cursor = null;
            try {
                cursor = contentResolver.query(mDbParams.getAppPausedUri(), new String[]{DbParams.TABLE_APPPAUSEDTIME}, null, null, null);
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
    public void commitAppEndState(boolean appEndState){
        if (appEndState == mAppEndState)return;
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DbParams.TABLE_APPENDSTATE, appEndState);
            contentResolver.insert(mDbParams.getAppEndStateUri(), contentValues);
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }

        mAppEndState = appEndState;
    }

    /**
     * return the state of $AppEnd
     * @return Activity End state
     */
    public boolean getAppEndState(){
        Cursor cursor = null;
        if (mAppEndState) {
            try {
                cursor = contentResolver.query(mDbParams.getAppEndStateUri(), new String[]{DbParams.TABLE_APPENDSTATE}, null, null, null);
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
    public void commitAppEndData(String appEndData){
        ContentValues contentValues = new ContentValues();
        contentValues.put(DbParams.TABLE_APPENDDATA, appEndData);
        contentResolver.insert(mDbParams.getAppEndDataUri(), contentValues);
    }

    /**
     * return the $AppEnd
     * @return Activity count
     */
    public String getAppEndData(){
        String data = "";
        Cursor cursor = contentResolver.query(mDbParams.getAppEndDataUri(), new String[]{DbParams.TABLE_APPENDDATA},null,null,null);
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
    public void commitSessionIntervalTime(int sessionIntervalTime){
        if (sessionIntervalTime == mSavedSessionTime)return;
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DbParams.TABLE_SESSIONINTERVALTIME, sessionIntervalTime);
            contentResolver.insert(mDbParams.getSessionTimeUri(), contentValues);
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
        mSavedSessionTime = sessionIntervalTime;
    }

    /**
     * return the $AppEnd
     * @return Activity count
     */
    public int getSessionIntervalTime(){
        if (mSessionTime != mSavedSessionTime) {
            Cursor cursor = null;
            try {
                cursor = contentResolver.query(mDbParams.getSessionTimeUri(), new String[]{DbParams.TABLE_SESSIONINTERVALTIME}, null, null, null);
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

    public String[] generateDataString(String tableName, int limit) {
        Cursor c = null;
        String data = null;
        String last_id = null;
        try {
            c = contentResolver.query(mDbParams.gemUri(), null, null, null, DbParams.KEY_CREATED_AT + " ASC LIMIT " + String.valueOf(limit));

            if (c != null) {
                StringBuilder dataBuilder = new StringBuilder();
                final String flush_time = ",\"_flush_time\":";
                String suffix = ",";
                dataBuilder.append("[");
                String keyData, crc, content;
                while (c.moveToNext()) {
                    if (c.isLast()) {
                        suffix = "]";
                        last_id = c.getString(c.getColumnIndex("_id"));
                    }
                    try {
                        keyData = c.getString(c.getColumnIndex(DbParams.KEY_DATA));
                        if (!TextUtils.isEmpty(keyData)) {
                            int index = keyData.lastIndexOf("\t");
                            if (index > -1) {
                                crc = keyData.substring(index).replaceFirst("\t", "");
                                content = keyData.substring(0, index);
                                if (TextUtils.isEmpty(content) || TextUtils.isEmpty(crc)
                                        || !crc.equals(String.valueOf(content.hashCode()))) {
                                    continue;
                                }
                                keyData = content;
                            }
                            dataBuilder.append(keyData, 0, keyData.length()-1)
                                    .append(flush_time)
                                    .append(System.currentTimeMillis())
                                    .append("}").append(suffix);
                        }
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
                data = dataBuilder.toString();
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

        if (last_id != null) {
            return new String[]{last_id, data};
        }
        return null;
    }
}
