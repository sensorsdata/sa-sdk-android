/*
 * Created by zhangwei on 2019/05/05.
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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.provider.ProviderTestRule;

import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SensorsDataContentProviderTest {

    private static Context context;
    private static DbParams dbParams;
    private static String mPackageName;

    @Rule
    public ProviderTestRule mProviderRule =
            new ProviderTestRule.Builder(SensorsDataContentProvider.class, "com.sensorsdata.analytics.android.sdk.test.SensorsDataContentProvider")
                    .build();

    @BeforeClass
    public static void beforeClass() {
        context = ApplicationProvider.getApplicationContext();
        dbParams = DbParams.getInstance(context.getPackageName());
        mPackageName = context.getPackageName();
    }

    @Before
    public void setUp() {
    }

    @Test
    public void testCRUD() {
        //clear data
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.sensorsdata.analytics.android.sdk.SensorsDataAPI", Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
        mProviderRule.runDatabaseCommands(DbParams.DATABASE_NAME, "DROP TABLE IF EXISTS " + DbParams.TABLE_EVENTS);
        Uri eventUri = Uri.parse("content://" + mPackageName + ".SensorsDataContentProvider/events");
        ContentResolver resolver = mProviderRule.getResolver();
        assertNotNull(resolver);
        resolver.delete(eventUri, null, new String[]{});
        {
            // events test
            Cursor cursor = resolver.query(eventUri, null, null, null, null);
            if (cursor != null) {
                assertEquals(0, cursor.getCount());
                cursor.close();
            }

            //添加一条数据
            ContentValues contentValues = new ContentValues();
            contentValues.put(DbParams.KEY_DATA, "foobar");
            contentValues.put(DbParams.KEY_CREATED_AT, System.currentTimeMillis());
            Uri uri = resolver.insert(eventUri, contentValues);
            assertNotNull(uri);
            long id = ContentUris.parseId(uri);
            assertTrue(id > 0);

            cursor = resolver.query(eventUri, null, null, null, null);
            assertTrue(cursor.moveToFirst());
            assertEquals(1, cursor.getCount());
            String data = cursor.getString(cursor.getColumnIndex(DbParams.KEY_DATA));
            assertEquals("foobar", data);
            cursor.close();

            int deleteRows = resolver.delete(eventUri, null, null);
            assertEquals(1, deleteRows);
            cursor = resolver.query(eventUri, null, null, null, null);
            assertEquals(0, cursor.getCount());
            assertFalse(cursor.moveToNext());
            cursor.close();

            //批量添加数据
            ContentValues[] valuesArray = new ContentValues[]{new ContentValues(), new ContentValues()};
            valuesArray[0].put(DbParams.KEY_DATA, "foo");
            valuesArray[0].put(DbParams.KEY_CREATED_AT, System.currentTimeMillis());
            valuesArray[1].put(DbParams.KEY_DATA, "bar");
            valuesArray[1].put(DbParams.KEY_CREATED_AT, System.currentTimeMillis() + 1000);
            int insertCount = resolver.bulkInsert(eventUri, valuesArray);
            assertEquals(valuesArray.length, insertCount);

            deleteRows = resolver.delete(eventUri, null, null);
            assertEquals(2, deleteRows);
            cursor = resolver.query(eventUri, null, null, null, null);
            assertEquals(0, cursor.getCount());
            assertFalse(cursor.moveToNext());
            cursor.close();

            //测试空 ContentValues
            contentValues = new ContentValues();
            uri = resolver.insert(eventUri, contentValues);
            assertNotNull(uri);
            cursor = resolver.query(eventUri, null, null, null, null);
            assertEquals(0, cursor.getCount());
            cursor.close();

            //测试 null ContentValues
            uri = resolver.insert(eventUri, null);
            assertNotNull(uri);
            cursor = resolver.query(eventUri, null, null, null, null);
            assertEquals(0, cursor.getCount());
            cursor.close();
        }

        {
            //app start test
            ContentValues contentValues = new ContentValues();
            contentValues.put(DbParams.TABLE_ACTIVITY_START_COUNT, true);
            Uri uri = resolver.insert(dbParams.getActivityStartCountUri(), contentValues);
            assertEquals(uri, dbParams.getActivityStartCountUri());
            Cursor cursor = resolver.query(uri, null, null, null, null);
            assertNotNull(cursor);
            assertTrue(cursor.moveToNext());
            int tmp = cursor.getInt(0);
            assertEquals(1, tmp);
            cursor.close();

            int deleteResult = resolver.delete(dbParams.getActivityStartCountUri(), null, null);
            assertEquals(0, deleteResult);

            //测试空 ContentValues
            contentValues = new ContentValues();
            contentValues.put(DbParams.TABLE_ACTIVITY_START_COUNT, false);
            uri = resolver.insert(dbParams.getActivityStartCountUri(), contentValues);
            assertEquals(uri, dbParams.getActivityStartCountUri());
            cursor = resolver.query(uri, null, null, null, null);
            assertNotNull(cursor);
            assertTrue(cursor.moveToNext());
            tmp = cursor.getInt(0);
            assertEquals(0, tmp);
            cursor.close();

            //测试 null 值
            uri = resolver.insert(dbParams.getActivityStartCountUri(), null);
            assertEquals(uri, dbParams.getActivityStartCountUri());
            cursor = resolver.query(uri, null, null, null, null);
            assertNotNull(cursor);
            assertTrue(cursor.moveToNext());
            tmp = cursor.getInt(0);
            assertEquals(0, tmp);
            cursor.close();
        }

        {
            //session time test
            long sessionIntervalTime = 100;
            ContentValues contentValues = new ContentValues();
            contentValues.put(DbParams.TABLE_SESSION_INTERVAL_TIME, sessionIntervalTime);
            Uri uri = resolver.insert(dbParams.getSessionTimeUri(), contentValues);

            assertEquals(uri, dbParams.getSessionTimeUri());
            Cursor cursor = resolver.query(uri, null, null, null, null);
            assertNotNull(cursor);
            assertTrue(cursor.moveToNext());
            long tmp = cursor.getInt(0);
            assertEquals(sessionIntervalTime, tmp);
            cursor.close();

            int deleteResult = resolver.delete(dbParams.getSessionTimeUri(), null, null);
            assertEquals(0, deleteResult);

            //测试空 ContentValues
            contentValues = new ContentValues();
            uri = resolver.insert(dbParams.getSessionTimeUri(), contentValues);
            assertEquals(uri, dbParams.getSessionTimeUri());
            cursor = resolver.query(uri, null, null, null, null);
            assertNotNull(cursor);
            assertTrue(cursor.moveToNext());
            tmp = cursor.getInt(0);
            assertEquals(sessionIntervalTime, tmp);
            cursor.close();

            //测试 null 值
            uri = resolver.insert(dbParams.getSessionTimeUri(), null);
            cursor = resolver.query(uri, null, null, null, null);
            assertNotNull(cursor);
            assertTrue(cursor.moveToNext());
            tmp = cursor.getInt(0);
            assertEquals(sessionIntervalTime, tmp);
            cursor.close();

        }

        {
            // login id test
            String userId = "sensors_data_cn";
            ContentValues contentValues = new ContentValues();
            contentValues.put("value", userId);
            Uri uri = resolver.insert(dbParams.getLoginIdUri(), contentValues);

            assertEquals(uri, dbParams.getLoginIdUri());
            Cursor cursor = resolver.query(uri, null, null, null, null);
            assertNotNull(cursor);
            assertTrue(cursor.moveToNext());
            String tmpLoginId = cursor.getString(0);
            assertEquals(userId, tmpLoginId);
            cursor.close();

            int deleteResult = resolver.delete(dbParams.getLoginIdUri(), null, null);
            assertEquals(0, deleteResult);

            //测试空 ContentValues
            contentValues = new ContentValues();
            uri = resolver.insert(dbParams.getLoginIdUri(), contentValues);
            assertEquals(uri, dbParams.getLoginIdUri());
            cursor = resolver.query(uri, null, null, null, null);
            assertNotNull(cursor);
            assertTrue(cursor.moveToNext());
            tmpLoginId = cursor.getString(0);
            assertEquals(tmpLoginId, userId);
            cursor.close();

            //测试 null 值
            uri = resolver.insert(dbParams.getLoginIdUri(), null);
            cursor = resolver.query(uri, null, null, null, null);
            assertNotNull(cursor);
            assertTrue(cursor.moveToNext());
            tmpLoginId = cursor.getString(0);
            assertEquals(tmpLoginId, userId);
            cursor.close();
        }
    }

}
