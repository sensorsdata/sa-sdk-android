/*
 * Created by dengshiwei on 2021/04/07.
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

package com.sensorsdata.analytics.android.sdk.data.adapter;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;

import org.json.JSONObject;

import java.io.File;

abstract class DataOperation {
    String TAG = "EventDataOperation";
    ContentResolver contentResolver;
    private final File mDatabaseFile;
    private Context mContext;

    DataOperation(Context context) {
        this.mContext = context;
        contentResolver = context.getContentResolver();
        mDatabaseFile = context.getDatabasePath(DbParams.DATABASE_NAME);
    }

    /**
     * 保存数据
     */
    abstract int insertData(Uri uri, JSONObject jsonObject);

    /**
     * 保存数据
     */
    abstract int insertData(Uri uri, ContentValues contentValues);

    /**
     * 查询数据
     */
    abstract String[] queryData(Uri uri, int limit);

    /**
     * 查询数据条数
     *
     * @param uri Uri
     * @return 条数
     */
    int queryDataCount(Uri uri) {
        return queryDataCount(uri, null, null, null, null);
    }

    /**
     * 查询数据条数
     */
    int queryDataCount(Uri uri, String[] projection, String selection,
                       String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder);
            if (cursor != null) {
                return cursor.getCount();
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    /**
     * 删除数据
     */
    void deleteData(Uri uri, String id) {
        try {
            if (DbParams.DB_DELETE_ALL.equals(id)) {
                contentResolver.delete(uri, null, null);
            } else {
                contentResolver.delete(uri, "_id <= ?", new String[]{id});
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    String parseData(String keyData) {
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
     * 数据库存满时删除数据
     *
     * @param uri URI
     * @return 正常返回 0
     */
    int deleteDataLowMemory(Uri uri) {
        if (belowMemThreshold()) {
            SALog.i(TAG, "There is not enough space left on the device to store events, so will delete 100 oldest events");
            String[] eventsData = queryData(uri, 100);
            if (eventsData == null) {
                return DbParams.DB_OUT_OF_MEMORY_ERROR;
            }

            final String lastId = eventsData[0];
            deleteData(uri, lastId);
            if (queryDataCount(uri) <= 0) {
                return DbParams.DB_OUT_OF_MEMORY_ERROR;
            }
        }
        return 0;
    }

    private long getMaxCacheSize(Context context) {
        try {
            return SensorsDataAPI.sharedInstance(context).getMaxCacheSize();
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return 32 * 1024 * 1024;
        }
    }

    private boolean belowMemThreshold() {
        if (mDatabaseFile.exists()) {
            return mDatabaseFile.length() >= getMaxCacheSize(mContext);
        }
        return false;
    }
}
