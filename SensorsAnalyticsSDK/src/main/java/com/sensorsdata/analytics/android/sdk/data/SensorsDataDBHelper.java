/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.sensorsdata.analytics.android.sdk.SALog;

/**
 * Created by 王灼洲 on 2017/5/5
 */

class SensorsDataDBHelper extends SQLiteOpenHelper {
    private static final String TAG = "SA.SQLiteOpenHelper";
    private static final String CREATE_EVENTS_TABLE =
            "CREATE TABLE " + DbParams.TABLE_EVENTS + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DbParams.KEY_DATA + " STRING NOT NULL, " +
                    DbParams.KEY_CREATED_AT + " INTEGER NOT NULL);";
    private static final String EVENTS_TIME_INDEX =
            "CREATE INDEX IF NOT EXISTS time_idx ON " + DbParams.TABLE_EVENTS +
                    " (" + DbParams.KEY_CREATED_AT + ");";

    SensorsDataDBHelper(Context context) {
        super(context, DbParams.DATABASE_NAME, null, DbParams.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        SALog.i(TAG, "Creating a new Sensors Analytics DB");

        db.execSQL(CREATE_EVENTS_TABLE);
        db.execSQL(EVENTS_TIME_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        SALog.i(TAG, "Upgrading app, replacing Sensors Analytics DB");

        db.execSQL("DROP TABLE IF EXISTS " + DbParams.TABLE_EVENTS);
        db.execSQL(CREATE_EVENTS_TABLE);
        db.execSQL(EVENTS_TIME_INDEX);
    }
}
