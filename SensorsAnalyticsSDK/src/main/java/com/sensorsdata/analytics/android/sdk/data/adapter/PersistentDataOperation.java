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
import android.net.Uri;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;

import org.json.JSONObject;

class PersistentDataOperation extends DataOperation {

    PersistentDataOperation(Context context) {
        super(context);
    }

    @Override
    String[] queryData(Uri uri, int limit) {
        return handleQueryUri(uri);
    }

    @Override
    int insertData(Uri uri, JSONObject jsonObject) {
        return handleInsertUri(uri, jsonObject);
    }

    @Override
    int insertData(Uri uri, ContentValues contentValues) {
        contentResolver.insert(uri, contentValues);
        return 0;
    }

    private int handleInsertUri(Uri uri, JSONObject jsonObject) {
        if (uri == null) return -1;

        try {
            ContentValues contentValues = new ContentValues();
            String path = uri.getPath();
            if (!TextUtils.isEmpty(path)) {
                path = path.substring(1);
                switch (path) {
                    case DbParams.TABLE_ACTIVITY_START_COUNT:
                        contentValues.put(DbParams.TABLE_ACTIVITY_START_COUNT, jsonObject.optInt(DbParams.VALUE));
                        break;
                    case DbParams.APP_EXIT_DATA:
                        contentValues.put(DbParams.APP_EXIT_DATA, jsonObject.optString(DbParams.VALUE));
                        break;
                    case DbParams.TABLE_APP_START_TIME:
                        contentValues.put(DbParams.TABLE_APP_START_TIME, jsonObject.optLong(DbParams.VALUE));
                        break;
                    case DbParams.TABLE_SESSION_INTERVAL_TIME:
                        contentValues.put(DbParams.TABLE_SESSION_INTERVAL_TIME, jsonObject.optLong(DbParams.VALUE));
                        break;
                    case DbParams.PersistentName.LOGIN_ID:
                        contentValues.put(DbParams.PersistentName.LOGIN_ID, jsonObject.optString(DbParams.VALUE));
                        break;
                    case DbParams.PersistentName.SUB_PROCESS_FLUSH_DATA:
                        contentValues.put(DbParams.PersistentName.SUB_PROCESS_FLUSH_DATA, jsonObject.optBoolean(DbParams.VALUE));
                        break;
                    case DbParams.TABLE_FIRST_PROCESS_START:
                        contentValues.put(DbParams.TABLE_FIRST_PROCESS_START, jsonObject.optBoolean(DbParams.VALUE));
                        break;
                    case DbParams.PersistentName.REMOTE_CONFIG:
                        contentValues.put(DbParams.PersistentName.REMOTE_CONFIG, jsonObject.optString(DbParams.VALUE));
                        break;
                    case DbParams.PersistentName.PERSISTENT_USER_ID:
                        contentValues.put(DbParams.PersistentName.PERSISTENT_USER_ID, jsonObject.optString(DbParams.VALUE));
                        break;
                    case DbParams.PersistentName.PERSISTENT_LOGIN_ID_KEY:
                        contentValues.put(DbParams.PersistentName.PERSISTENT_LOGIN_ID_KEY, jsonObject.optString(DbParams.VALUE));
                        break;
                    case DbParams.PUSH_ID_KEY:
                        contentValues.put(DbParams.PUSH_ID_KEY, jsonObject.optString(DbParams.PUSH_ID_KEY));
                        contentValues.put(DbParams.PUSH_ID_VALUE, jsonObject.optString(DbParams.PUSH_ID_VALUE));
                        break;
                    default:
                        return -1;
                }
                contentResolver.insert(uri, contentValues);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return 0;
    }

    private String[] handleQueryUri(Uri uri) {
        if (uri == null) return null;
        String path = uri.getPath();
        if (TextUtils.isEmpty(path)) return null;
        Cursor cursor = null;
        try {
            path = path.substring(1);
            cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToNext();
                switch (path) {
                    case DbParams.TABLE_ACTIVITY_START_COUNT:
                    case DbParams.PersistentName.SUB_PROCESS_FLUSH_DATA:
                    case DbParams.TABLE_FIRST_PROCESS_START:
                        return new String[]{String.valueOf(cursor.getInt(0))};
                    case DbParams.APP_EXIT_DATA:
                    case DbParams.PersistentName.LOGIN_ID:
                    case DbParams.PersistentName.REMOTE_CONFIG:
                    case DbParams.PersistentName.PERSISTENT_USER_ID:
                    case DbParams.PersistentName.PERSISTENT_LOGIN_ID_KEY:
                    case DbParams.PUSH_ID_KEY:
                        return new String[]{cursor.getString(0)};
                    case DbParams.TABLE_SESSION_INTERVAL_TIME:
                    case DbParams.TABLE_APP_START_TIME:
                        return new String[]{String.valueOf(cursor.getLong(0))};
                    default:
                        return null;
                }
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    @Override
    void deleteData(Uri uri, String key) {
        contentResolver.delete(uri.buildUpon().appendQueryParameter(DbParams.REMOVE_SP_KEY, key).build(),
                null, null);
    }
}
