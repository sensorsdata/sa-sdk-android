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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.encrypt.SensorsDataEncrypt;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

class EncryptDataOperation extends DataOperation {

    private SensorsDataEncrypt mSensorsDataEncrypt;

    EncryptDataOperation(Context context, SensorsDataEncrypt sensorsDataEncrypt) {
        super(context);
        this.mSensorsDataEncrypt = sensorsDataEncrypt;
    }

    @Override
    int insertData(Uri uri, JSONObject jsonObject) {
        try {
            if (deleteDataLowMemory(uri) != 0) {
                return DbParams.DB_OUT_OF_MEMORY_ERROR;
            }
            jsonObject = mSensorsDataEncrypt.encryptTrackData(jsonObject);
            ContentValues cv = new ContentValues();
            cv.put(DbParams.KEY_DATA, jsonObject.toString() + "\t" + jsonObject.toString().hashCode());
            cv.put(DbParams.KEY_CREATED_AT, System.currentTimeMillis());
            contentResolver.insert(uri, cv);
        } catch (Exception e) {
            SALog.printStackTrace(e);
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
        Cursor cursor = null;
        String data = null;
        String last_id = null;
        String gzipType = DbParams.GZIP_DATA_ENCRYPT;
        try {
            Map<String, JSONArray> dataEncryptMap = new HashMap<>();
            JSONArray dataJsonArray = new JSONArray();
            cursor = contentResolver.query(uri, null, null, null, DbParams.KEY_CREATED_AT + " ASC LIMIT " + limit);
            if (cursor != null) {
                String keyData;
                JSONObject jsonObject;
                final String EKEY = "ekey";
                final String KEY_VER = "pkv";
                final String PAYLOADS = "payloads";
                while (cursor.moveToNext()) {
                    if (cursor.isLast()) {
                        last_id = cursor.getString(cursor.getColumnIndex("_id"));
                    }
                    try {
                        keyData = cursor.getString(cursor.getColumnIndex(DbParams.KEY_DATA));
                        keyData = parseData(keyData);
                        if (TextUtils.isEmpty(keyData)) {
                            continue;
                        }

                        jsonObject = new JSONObject(keyData);
                        boolean isHasEkey = jsonObject.has(EKEY);
                        if (!isHasEkey) { // 如果没有包含 Ekey 字段，则重新进行加密
                            jsonObject = mSensorsDataEncrypt.encryptTrackData(jsonObject);
                        }

                        if (jsonObject.has(EKEY)) {
                            String key = jsonObject.getString(EKEY) + "$" + jsonObject.getInt(KEY_VER);
                            if (dataEncryptMap.containsKey(key)) {
                                dataEncryptMap.get(key).put(jsonObject.getString(PAYLOADS));
                            } else {
                                JSONArray jsonArray = new JSONArray();
                                jsonArray.put(jsonObject.getString(PAYLOADS));
                                dataEncryptMap.put(key, jsonArray);
                            }
                        } else {
                            dataJsonArray.put(jsonObject);
                        }
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
                JSONArray dataEncryptJsonArray = new JSONArray();
                for (String key : dataEncryptMap.keySet()) {
                    jsonObject = new JSONObject();
                    jsonObject.put(EKEY, key.substring(0, key.indexOf("$")));
                    jsonObject.put(KEY_VER, Integer.valueOf(key.substring(key.indexOf("$") + 1)));
                    jsonObject.put(PAYLOADS, dataEncryptMap.get(key));
                    jsonObject.put("flush_time", System.currentTimeMillis());
                    dataEncryptJsonArray.put(jsonObject);
                }
                if (dataEncryptJsonArray.length() > 0) {
                    data = dataEncryptJsonArray.toString();
                } else {
                    data = dataJsonArray.toString();
                    gzipType = DbParams.GZIP_DATA_EVENT;
                }
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (last_id != null) {
            return new String[]{last_id, data, gzipType};
        }
        return null;
    }

    @Override
    void deleteData(Uri uri, String id) {
        super.deleteData(uri, id);
    }
}
