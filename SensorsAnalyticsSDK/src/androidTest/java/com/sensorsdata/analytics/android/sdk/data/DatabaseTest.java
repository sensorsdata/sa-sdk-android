/*
 * Created by zhangwei on 2019/04/30.
 * Copyright 2015－2021 Sensors Data Inc.
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * 测试数据库相关的逻辑
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseTest {
    private static SensorsDataDBHelper dbHelper;
    private static Context context;

    @BeforeClass
    public static void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(DbParams.DATABASE_NAME);
        dbHelper = new SensorsDataDBHelper(context);
    }

    @Test
    public void crudTest() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DbParams.TABLE_EVENTS, null);
        assertEquals(3, cursor.getColumnCount());
        assertEquals(0, cursor.getCount());
        cursor.close();

        long currentTime = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(DbParams.KEY_DATA, "foobar");
        values.put(DbParams.KEY_CREATED_AT, currentTime);
        long id = db.insert(DbParams.TABLE_EVENTS, "_id", values);
        assertNotEquals(-1, id);

        cursor = db.rawQuery("SELECT * FROM " + DbParams.TABLE_EVENTS, null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        String data = cursor.getString(cursor.getColumnIndex(DbParams.KEY_DATA));
        long time = cursor.getLong(cursor.getColumnIndex(DbParams.KEY_CREATED_AT));
        long dbId = cursor.getLong(cursor.getColumnIndex("_id"));
        assertEquals(id, dbId);
        assertEquals(currentTime, time);
        assertEquals(data, "foobar");
        cursor.close();
        context.deleteDatabase(DbParams.DATABASE_NAME);
    }
}
