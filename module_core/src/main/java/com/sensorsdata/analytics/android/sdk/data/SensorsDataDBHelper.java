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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;

class SensorsDataDBHelper extends SQLiteOpenHelper {
    private static final String TAG = "SA.SQLiteOpenHelper";
    private static final String CREATE_EVENTS_TABLE =
            String.format("CREATE TABLE %s (_id INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT NOT NULL, %s INTEGER NOT NULL);", DbParams.TABLE_EVENTS, DbParams.KEY_DATA, DbParams.KEY_CREATED_AT);
    private static final String EVENTS_TIME_INDEX =
            String.format("CREATE INDEX IF NOT EXISTS time_idx ON %s (%s);", DbParams.TABLE_EVENTS, DbParams.KEY_CREATED_AT);
    private static final String CHANNEL_EVENT_PERSISTENT_TABLE = String.format("CREATE TABLE %s (%s TEXT PRIMARY KEY, %s INTEGER)",
            DbParams.TABLE_CHANNEL_PERSISTENT, DbParams.KEY_CHANNEL_EVENT_NAME, DbParams.KEY_CHANNEL_RESULT);

    SensorsDataDBHelper(Context context) {
        super(context, DbParams.DATABASE_NAME, null, DbParams.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        SALog.i(TAG, "Creating a new Sensors Analytics DB");

        db.execSQL(CREATE_EVENTS_TABLE);
        db.execSQL(EVENTS_TIME_INDEX);
        db.execSQL(CHANNEL_EVENT_PERSISTENT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        SALog.i(TAG, "Upgrading app, replacing Sensors Analytics DB");

        db.execSQL(String.format("DROP TABLE IF EXISTS %s", DbParams.TABLE_EVENTS));
        db.execSQL(CREATE_EVENTS_TABLE);
        db.execSQL(EVENTS_TIME_INDEX);
        db.execSQL(CHANNEL_EVENT_PERSISTENT_TABLE);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
