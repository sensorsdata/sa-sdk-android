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
import com.sensorsdata.analytics.android.sdk.core.business.instantevent.InstantEventUtils;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.core.mediator.SAModuleManager;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

class EncryptDataOperation extends DataOperation {
    protected boolean mDbEncrypt;

    EncryptDataOperation(Context context) {
        super(context);
        mDbEncrypt = true;
    }

    @Override
    int insertData(Uri uri, JSONObject jsonObject) {
        try {
            if (deleteDataLowMemory(uri) != 0) {
                return DbParams.DB_OUT_OF_MEMORY_ERROR;
            }
            int instant_event = InstantEventUtils.isInstantEvent(jsonObject);
            JSONObject jsonEncrypt = SAModuleManager.getInstance().invokeModuleFunction(Modules.Encrypt.MODULE_NAME, Modules.Encrypt.METHOD_ENCRYPT_EVENT_DATA, jsonObject);
            if (jsonEncrypt != null) {
                jsonObject = jsonEncrypt;
            }
            ContentValues cv = new ContentValues();
            cv.put(DbParams.KEY_DATA, jsonObject.toString() + "\t" + jsonObject.toString().hashCode());
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
        Cursor cursor = null;
        String data = null;
        String eventIds = null;
        JSONArray idEncryptArray = new JSONArray();
        JSONArray idArray = new JSONArray();
        String gzipType = DbParams.GZIP_DATA_ENCRYPT;
        try {
            Map<String, JSONArray> dataEncryptMap = new HashMap<>();
            JSONArray dataJsonArray = new JSONArray();

            String instant_event = "0";
            if (is_instant_event) {
                instant_event = "1";
            }
            cursor = contentResolver.query(uri, null, DbParams.KEY_IS_INSTANT_EVENT + "=?", new String[]{instant_event}, DbParams.KEY_CREATED_AT + " ASC LIMIT " + limit);
            if (cursor != null) {
                String keyData;
                JSONObject jsonObject;
                final String EKEY = "ekey";
                final String KEY_VER = "pkv";
                final String PAYLOADS = "payloads";
                while (cursor.moveToNext()) {
                    try {
                        String eventId = cursor.getString(cursor.getColumnIndexOrThrow("_id"));
                        keyData = cursor.getString(cursor.getColumnIndexOrThrow(DbParams.KEY_DATA));
                        keyData = parseData(keyData);
                        if (TextUtils.isEmpty(keyData)) {
                            continue;
                        }

                        if (!JSONUtils.isJson(keyData)) {
                            SALog.i(TAG, "Error is not json data, v = " + keyData);
                            continue;
                        }

                        jsonObject = new JSONObject(keyData);
                        boolean isHasEkey = jsonObject.has(EKEY);
                        if (jsonObject.has(PAYLOADS) && !isHasEkey) {// 传输加密
                            jsonObject = new JSONObject(decryptValue(jsonObject.optString(PAYLOADS)));
                        }

                        if (!isHasEkey && mDbEncrypt) { // 如果没有包含 Ekey 字段，则重新进行加密
                            JSONObject jsonEncrypt = SAModuleManager.getInstance().invokeModuleFunction(Modules.Encrypt.MODULE_NAME, Modules.Encrypt.METHOD_ENCRYPT_EVENT_DATA, jsonObject);
                            if (jsonEncrypt != null) {
                                jsonObject = jsonEncrypt;
                            }
                        }

                        if (jsonObject.has(EKEY)) {
                            String key = jsonObject.getString(EKEY) + "$" + jsonObject.getInt(KEY_VER);
                            if (dataEncryptMap.containsKey(key)) {
                                dataEncryptMap.get(key).put(jsonObject.optString(PAYLOADS));
                            } else {
                                JSONArray jsonArray = new JSONArray();
                                jsonArray.put(jsonObject.optString(PAYLOADS));
                                dataEncryptMap.put(key, jsonArray);
                            }
                            idEncryptArray.put(eventId);
                        } else {
                            jsonObject.put("_flush_time", System.currentTimeMillis());
                            dataJsonArray.put(jsonObject);
                            idArray.put(eventId);
                        }
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
                if (dataEncryptMap.size() > 0) {// 埋点加密
                    JSONArray dataEncryptJsonArray = new JSONArray();
                    for (String key : dataEncryptMap.keySet()) {
                        jsonObject = new JSONObject();
                        jsonObject.put(EKEY, key.substring(0, key.indexOf("$")));
                        jsonObject.put(KEY_VER, Integer.valueOf(key.substring(key.indexOf("$") + 1)));
                        jsonObject.put(PAYLOADS, dataEncryptMap.get(key));
                        jsonObject.put("flush_time", System.currentTimeMillis());
                        dataEncryptJsonArray.put(jsonObject);
                    }
                    data = dataEncryptJsonArray.toString();
                    eventIds = idEncryptArray.toString();
                } else if (dataJsonArray.length() > 0) {// 明文数据
                    data = dataJsonArray.toString();
                    eventIds = idArray.toString();
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
        if (eventIds != null) {
            return new String[]{eventIds, data, gzipType};
        }
        return null;
    }

    @Override
    void deleteData(Uri uri, String id) {
        super.deleteData(uri, id);
    }


    private String decryptValue(String value) {
        String decryptValue = SAModuleManager.getInstance().invokeModuleFunction(Modules.Encrypt.MODULE_NAME, Modules.Encrypt.METHOD_LOAD_EVENT, value);
        return TextUtils.isEmpty(decryptValue) ? value : decryptValue;
    }
}
