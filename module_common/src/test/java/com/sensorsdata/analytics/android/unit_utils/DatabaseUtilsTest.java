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
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;

public class DatabaseUtilsTest {
    /**
     * 查询数据库中的数据
     *
     * @return Data
     */
    public static String loadEventFromDb(Application application) {
        final Uri uri = Uri.parse("content://" + application.getPackageName() + ".SensorsDataContentProvider/events");
        ContentResolver resolver = application.getContentResolver();
        Cursor cursor = resolver.query(uri, null, null, null, null);
        assertNotNull(cursor);
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        String data =  parseData(cursor.getString(cursor.getColumnIndexOrThrow(DbParams.KEY_DATA)));
        cursor.close();
        return data;
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
}
