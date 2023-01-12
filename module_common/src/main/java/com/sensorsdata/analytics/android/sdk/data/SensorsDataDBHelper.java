/*
 * Created by wangzhuozhou on 2015/08/01.
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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;

class SensorsDataDBHelper extends SQLiteOpenHelper {
    private static final String TAG = "SA.SQLiteOpenHelper";
    private static final String CREATE_EVENTS_TABLE =
            String.format("CREATE TABLE IF NOT EXISTS %s (_id INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT NOT NULL, %s INTEGER NOT NULL, %s INTEGER NOT NULL DEFAULT 0);", DbParams.TABLE_EVENTS, DbParams.KEY_DATA, DbParams.KEY_CREATED_AT, DbParams.KEY_IS_INSTANT_EVENT);
    private static final String EVENTS_TIME_INDEX =
            String.format("CREATE INDEX IF NOT EXISTS time_idx ON %s (%s);", DbParams.TABLE_EVENTS, DbParams.KEY_CREATED_AT);
    private static final String CHANNEL_EVENT_PERSISTENT_TABLE = String.format("CREATE TABLE IF NOT EXISTS %s (%s TEXT PRIMARY KEY, %s INTEGER)",
            DbParams.TABLE_CHANNEL_PERSISTENT, DbParams.KEY_CHANNEL_EVENT_NAME, DbParams.KEY_CHANNEL_RESULT);

    SensorsDataDBHelper(Context context) {
        super(context, DbParams.DATABASE_NAME, null, DbParams.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        SALog.i(TAG, "Creating a new Sensors Analytics DB");
        createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        SALog.i(TAG, "Upgrading app, replacing Sensors Analytics DB, oldVersion:" + oldVersion + ", newVersion:" + newVersion);
        try {
            if (oldVersion < 4) {
                db.execSQL(String.format("DROP TABLE IF EXISTS %s", DbParams.TABLE_EVENTS));
            }
            createTable(db);
            if (oldVersion >= 4 && oldVersion <= 5) {
                //不存在则创建字段，避免数据库升级，又降级，又升级有此字段导致的字段重复添加异常
                if (!checkColumnExist(db, DbParams.TABLE_EVENTS, DbParams.KEY_IS_INSTANT_EVENT)) {
                    String sql = "ALTER TABLE " + DbParams.TABLE_EVENTS + " ADD COLUMN  " + DbParams.KEY_IS_INSTANT_EVENT + " INTEGER NOT NULL DEFAULT 0";
                    db.execSQL(sql);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private void createTable(SQLiteDatabase db) {
        db.execSQL(CREATE_EVENTS_TABLE);
        db.execSQL(EVENTS_TIME_INDEX);
        db.execSQL(CHANNEL_EVENT_PERSISTENT_TABLE);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    /**
     * 检查某表中某列是否存在
     *
     * @param db 数据库
     * @param tableName 表名
     * @param columnName 列名
     * @return 列表某列是否存在
     */
    private boolean checkColumnExist(SQLiteDatabase db, String tableName
            , String columnName) {
        boolean result = false;
        Cursor cursor = null;
        try {
            //查询一行
            cursor = db.rawQuery("SELECT * FROM " + tableName + " LIMIT 0"
                    , null);
            result = cursor != null && cursor.getColumnIndex(columnName) != -1;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        } finally {
            try {
                if (null != cursor && !cursor.isClosed()) {
                    cursor.close();
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
        return result;
    }
}
