/*
 * Created by wangzhuozhou on 2015/08/01.
 * Copyright 2015－2020 Sensors Data Inc.
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
    private final Context mContext;
    /* Session 时长间隔 */
    private int mSessionTime = 30 * 1000;
    /* $AppEnd 事件触发的时间戳 */
    private long mAppEndTime = 0;
    private ContentResolver contentResolver;

    private DbAdapter(Context context, String packageName) {
        mContext = context.getApplicationContext();
        contentResolver = mContext.getContentResolver();
        mDatabaseFile = context.getDatabasePath(DbParams.DATABASE_NAME);
        mDbParams = DbParams.getInstance(packageName);
    }

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
            return mDatabaseFile.length() >= getMaxCacheSize(mContext);
        }
        return false;
    }

    /**
     * Adds a JSON string representing an event with properties or a person record
     * to the SQLiteDatabase.
     *
     * @param j the JSON to record
     * @return the number of rows in the table, or DB_OUT_OF_MEMORY_ERROR/DB_UPDATE_ERROR
     * on failure
     */
    public int addJSON(JSONObject j) {
        // we are aware of the race condition here, but what can we do..?
        int count = DbParams.DB_UPDATE_ERROR;
        Cursor c = null;
        try {
            if (belowMemThreshold()) {
                SALog.i(TAG, "There is not enough space left on the device to store events, so will delete 100 oldest events");
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
            contentResolver.insert(mDbParams.getEventUri(), cv);
            c = contentResolver.query(mDbParams.getEventUri(), null, null, null, null);
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
     * @param eventsList the JSON to record
     * @return the number of rows in the table, or DB_OUT_OF_MEMORY_ERROR/DB_UPDATE_ERROR
     * on failure
     */
    public int addJSON(List<JSONObject> eventsList) {
        // we are aware of the race condition here, but what can we do..?
        int count = DbParams.DB_UPDATE_ERROR;
        Cursor c = null;
        try {
            if (belowMemThreshold()) {
                SALog.i(TAG, "There is not enough space left on the device to store events, so will delete 100 oldest events");
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
            for (JSONObject j : eventsList) {
                cv = new ContentValues();
                cv.put(DbParams.KEY_DATA, j.toString() + "\t" + j.toString().hashCode());
                cv.put(DbParams.KEY_CREATED_AT, System.currentTimeMillis());
                contentValues[index++] = cv;
            }
            contentResolver.bulkInsert(mDbParams.getEventUri(), contentValues);
            c = contentResolver.query(mDbParams.getEventUri(), null, null, null, null);
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
     */
    public void deleteAllEvents() {
        try {
            contentResolver.delete(mDbParams.getEventUri(), null, null);
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
            contentResolver.delete(mDbParams.getEventUri(), "_id <= ?", new String[]{last_id});
            c = contentResolver.query(mDbParams.getEventUri(), null, null, null, null);
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
            } catch (Exception ex) {
                // ignore
            }
        }
        return count;
    }

    /**
     * 保存启动的页面个数
     *
     * @param activityCount 页面个数
     */
    public void commitActivityCount(int activityCount) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DbParams.TABLE_ACTIVITY_START_COUNT, activityCount);
        contentResolver.insert(mDbParams.getActivityStartCountUri(), contentValues);
    }

    /**
     * 获取存储的页面个数
     *
     * @return 存储的页面个数
     */
    public int getActivityCount() {
        int activityCount = 0;
        Cursor cursor = contentResolver.query(mDbParams.getActivityStartCountUri(), null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                activityCount = cursor.getInt(0);
            }
        }

        if (cursor != null) {
            cursor.close();
        }
        return activityCount;
    }

    /**
     * 设置 Activity Start 的时间戳
     *
     * @param appStartTime Activity Start 的时间戳
     */
    public void commitAppStartTime(long appStartTime) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DbParams.TABLE_APP_START_TIME, appStartTime);
        contentResolver.insert(mDbParams.getAppStartTimeUri(), contentValues);
    }

    /**
     * 获取 Activity Start 的时间戳
     *
     * @return Activity Start 的时间戳
     */
    public long getAppStartTime() {
        long startTime = 0;
        Cursor cursor = contentResolver.query(mDbParams.getAppStartTimeUri(), null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                startTime = cursor.getLong(0);
            }
        }

        if (cursor != null) {
            cursor.close();
        }
        SALog.d(TAG, "getAppStartTime:" + startTime);
        return startTime;
    }

    /**
     * 设置 Activity Pause 的时间戳
     *
     * @param appPausedTime Activity Pause 的时间戳
     */
    public void commitAppEndTime(long appPausedTime) {
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DbParams.TABLE_APP_END_TIME, appPausedTime);
            contentResolver.insert(mDbParams.getAppPausedUri(), contentValues);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        mAppEndTime = appPausedTime;
    }

    /**
     * 获取 Activity Pause 的时间戳
     *
     * @return Activity Pause 的时间戳
     */
    public long getAppEndTime() {
        if (System.currentTimeMillis() - mAppEndTime > mSessionTime) {
            Cursor cursor = null;
            try {
                cursor = contentResolver.query(mDbParams.getAppPausedUri(), null, null, null, null);
                if (cursor != null && cursor.getCount() > 0) {
                    while (cursor.moveToNext()) {
                        mAppEndTime = cursor.getLong(0);
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
        return mAppEndTime;
    }

    /**
     * 设置 Activity End 的信息
     *
     * @param appEndData Activity End 的信息
     */
    public void commitAppEndData(String appEndData) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DbParams.TABLE_APP_END_DATA, appEndData);
        contentResolver.insert(mDbParams.getAppEndDataUri(), contentValues);
    }

    /**
     * 获取 Activity End 的信息
     *
     * @return Activity End 的信息
     */
    public String getAppEndData() {
        String data = "";
        Cursor cursor = contentResolver.query(mDbParams.getAppEndDataUri(), null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                data = cursor.getString(0);
            }
        }

        if (cursor != null) {
            cursor.close();
        }
        SALog.d(TAG, "getAppEndData:" + data);
        return data;
    }

    /**
     * 存储 LoginId
     *
     * @param loginId 登录 Id
     */
    public void commitLoginId(String loginId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DbParams.TABLE_LOGIN_ID, loginId);
        contentResolver.insert(mDbParams.getLoginIdUri(), contentValues);
    }

    /**
     * 获取 LoginId
     *
     * @return LoginId
     */
    public String getLoginId() {
        String data = "";
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(mDbParams.getLoginIdUri(), null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    data = cursor.getString(0);
                }
            }
            SALog.d(TAG, "getLoginId:" + data);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return data;
    }

    /**
     * 设置 Session 的时长
     *
     * @param sessionIntervalTime Session 的时长
     */
    public void commitSessionIntervalTime(int sessionIntervalTime) {
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DbParams.TABLE_SESSION_INTERVAL_TIME, sessionIntervalTime);
            contentResolver.insert(mDbParams.getSessionTimeUri(), contentValues);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 获取 Session 的时长
     *
     * @return Session 的时长
     */
    public int getSessionIntervalTime() {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(mDbParams.getSessionTimeUri(), null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    mSessionTime = cursor.getInt(0);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        SALog.d(TAG, "getSessionIntervalTime:" + mSessionTime);
        return mSessionTime;
    }

    /**
     * 查询表中是否有对应的事件
     *
     * @param eventName 事件名
     * @return false 表示已存在，true 表示不存在，是首次
     */
    public boolean isFirstChannelEvent(String eventName) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(mDbParams.getChannelPersistentUri(), null, DbParams.KEY_CHANNEL_EVENT_NAME + " = ? ", new String[]{eventName}, null);
            if (cursor != null && cursor.getCount() > 0) {
                return false;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return true;
    }

    /**
     * 添加渠道事件
     *
     * @param eventName 事件名
     */
    public void addChannelEvent(String eventName) {
        ContentValues values = new ContentValues();
        values.put(DbParams.KEY_CHANNEL_EVENT_NAME, eventName);
        values.put(DbParams.KEY_CHANNEL_RESULT, true);
        contentResolver.insert(mDbParams.getChannelPersistentUri(), values);
    }

    /**
     * 从 Event 表中读取上报数据
     *
     * @param tableName 表名
     * @param limit 条数限制
     * @return 数据
     */
    public String[] generateDataString(String tableName, int limit) {
        Cursor c = null;
        String data = null;
        String last_id = null;
        try {
            c = contentResolver.query(mDbParams.getEventUri(), null, null, null, DbParams.KEY_CREATED_AT + " ASC LIMIT " + limit);

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
                            dataBuilder.append(keyData, 0, keyData.length() - 1)
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