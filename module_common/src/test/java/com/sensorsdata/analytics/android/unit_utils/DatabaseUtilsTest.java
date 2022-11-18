/*
 * Created by dengshiwei on 2022/06/08.
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

package com.sensorsdata.analytics.android.unit_utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;

import java.lang.reflect.Constructor;

public class DatabaseUtilsTest {
    private static SQLiteDatabase mDB;

    /**
     * 查询数据库中的数据
     *
     * @return Data
     */
    public static String loadEventFromDb(Application application) {
        try {
            String dbPath = application.getDatabasePath(DbParams.DATABASE_NAME).getAbsolutePath();
            openDB(application.getApplicationContext(), dbPath);
            if (mDB != null) {
                Cursor cursor = mDB.rawQuery("select * from events", null);
                assertNotNull(cursor);
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                String data = parseData(cursor.getString(cursor.getColumnIndexOrThrow(DbParams.KEY_DATA)));
                cursor.close();
                return data;
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return "";
    }

    static String parseData(String keyData) {
        try {
            if (TextUtils.isEmpty(keyData)) return "";
            int index = keyData.lastIndexOf("\t");
            if (index > -1) {
                String crc = keyData.substring(index).replaceFirst("\t", "");
                keyData = keyData.substring(0, index);
                if (TextUtils.isEmpty(keyData) || TextUtils.isEmpty(crc)
                        || !crc.equals(String.valueOf(keyData.hashCode()))) {
                    return "";
                }
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return keyData;
    }
    /**
     * 打开数据库
     */
    private static void openDB(Context context, String dbPath) throws Exception {
        if (mDB == null || !mDB.isOpen()) {
            Class dbHelperClazz = Class.forName("com.sensorsdata.analytics.android.sdk.data.SensorsDataDBHelper");
            Constructor contextStruct = dbHelperClazz.getDeclaredConstructor(Context.class);
            contextStruct.setAccessible(true);
            Object dbHelper = contextStruct.newInstance(context);
            mDB = ((SQLiteOpenHelper)dbHelper).getReadableDatabase();
//            mDB = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
        }
    }
}
