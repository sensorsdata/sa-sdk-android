/*
 * Created by dengshiwei on 2021/04/07.
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

package com.sensorsdata.analytics.android.sdk.data.adapter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteBlobTooBigException;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.core.business.instantevent.InstantEventUtils;

import org.json.JSONArray;
import org.json.JSONObject;


class EventDataOperation extends DataOperation {

    EventDataOperation(Context context) {
        super(context);
        TAG = "EventDataOperation";
    }

    @Override
    int insertData(Uri uri, JSONObject jsonObject) {
        try {
            if (deleteDataLowMemory(uri) != 0) {
                return DbParams.DB_OUT_OF_MEMORY_ERROR;
            }
            int instant_event = InstantEventUtils.isInstantEvent(jsonObject);
            ContentValues cv = new ContentValues();
            String eventJson = jsonObject.toString();
            cv.put(DbParams.KEY_DATA, eventJson + "\t" + eventJson.hashCode());
            cv.put(DbParams.KEY_CREATED_AT, System.currentTimeMillis());
            cv.put(DbParams.KEY_IS_INSTANT_EVENT, instant_event);
            contentResolver.insert(uri, cv);
        } catch (Throwable e) {
            SALog.i(TAG, e.getMessage());
        }
        return 0;
    }

    @Override
    int insertData(Uri uri, ContentValues contentValues) {
        try {
            if (deleteDataLowMemory(uri) != 0) {
                return DbParams.DB_OUT_OF_MEMORY_ERROR;
            }
            contentResolver.insert(uri, contentValues);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return 0;
    }

    @Override
    String[] queryData(Uri uri, int limit) {
        return queryData(uri, false, limit);
    }

    @Override
    String[] queryData(Uri uri, boolean is_instant_event, int limit) {
        try {
            return queryDataInner(uri, is_instant_event, limit);
        } catch (SQLiteBlobTooBigException bigException) {
            SALog.i(TAG, "Could not pull records for SensorsData out of database events. SQLiteBlobTooBigException ", bigException);
            return handleBigException(uri, is_instant_event);
        } catch (final SQLiteException e) {
            SALog.i(TAG, "Could not pull records for SensorsData out of database events. Waiting to send.", e);
        }
        return null;
    }

    @Override
    void deleteData(Uri uri, String id) {
        super.deleteData(uri, id);
    }

    private String[] queryDataInner(Uri uri, boolean is_instant_event, int limit) {
        Cursor cursor = null;
        String data = null;
        String eventIds = null;
        JSONArray idArray = new JSONArray();
        try {
            String instant_event = "0";
            if (is_instant_event) {
                instant_event = "1";
            }
            cursor = contentResolver.query(uri, null, DbParams.KEY_IS_INSTANT_EVENT + "=?", new String[]{instant_event}, DbParams.KEY_CREATED_AT + " ASC LIMIT " + limit);
            if (cursor != null) {
                StringBuilder dataBuilder = new StringBuilder();
                final String flush_time = ",\"_flush_time\":";
                String suffix = ",";
                dataBuilder.append("[");
                String keyData;
                while (cursor.moveToNext()) {
                    if (cursor.isLast()) {
                        suffix = "]";
                    }
                    String eventId = cursor.getString(cursor.getColumnIndexOrThrow("_id"));
                    idArray.put(eventId);
                    try {
                        keyData = cursor.getString(cursor.getColumnIndexOrThrow(DbParams.KEY_DATA));
                        keyData = parseData(keyData);
                        if (!TextUtils.isEmpty(keyData)) {
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
                if (idArray.length() > 0) {
                    eventIds = idArray.toString();
                }
            }
        } catch (Throwable error) {
            SALog.i(TAG, error.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (eventIds != null) {
            return new String[]{eventIds, data, DbParams.GZIP_DATA_EVENT};
        }
        return null;
    }

    /*
     用于处理 SQLiteBlobTooBigException 的场景，出现此类情况时，只读取一条数据上报
     */
    private String[] handleBigException(Uri uri, boolean is_instant_event) {
        try {
            return queryDataInner(uri, is_instant_event, 1);
        } catch (SQLiteBlobTooBigException bigException) {//说明第一条数据就是 SQLiteBlobTooBigException，该条数据一直无法上报，所以删除处理
            deleteData(uri, getFirstRowId(uri, is_instant_event ? "1" : "0"));
            SALog.printStackTrace(bigException);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }
}
