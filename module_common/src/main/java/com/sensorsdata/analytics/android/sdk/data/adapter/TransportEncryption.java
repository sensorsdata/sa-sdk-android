/*
 * Created by dengshiwei on 2023/03/14.
 * Copyright 2015－2023 Sensors Data Inc.
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
import android.net.Uri;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.core.business.instantevent.InstantEventUtils;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.core.mediator.SAModuleManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TransportEncryption extends EncryptDataOperation {
    private Object mSupportTransport = null;

    TransportEncryption(Context context) {
        super(context);
        mDbEncrypt = false;
    }

    @Override
    int insertData(Uri uri, JSONObject jsonObject) {
        try {
            if (deleteDataLowMemory(uri) != 0) {
                return DbParams.DB_OUT_OF_MEMORY_ERROR;
            }
            int instant_event = InstantEventUtils.isInstantEvent(jsonObject);
            ContentValues cv = new ContentValues();
            String eventJson = encryptValue(jsonObject.toString());
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
    String[] queryData(Uri uri, int limit) {
        return queryData(uri, false, limit);
    }

    @Override
    String[] queryData(Uri uri, boolean is_instant_event, int limit) {
        try {
            String[] eventsData = super.queryData(uri, is_instant_event, limit);
            if (eventsData != null && eventsData.length >= 3) {
                String gzipType = eventsData[2];

                if (DbParams.GZIP_DATA_EVENT.equals(gzipType) && isSupportTransport()) {
                    String eventIds = eventsData[0];
                    String data = SAModuleManager.getInstance().invokeModuleFunction(Modules.Encrypt.MODULE_NAME, Modules.Encrypt.METHOD_ENCRYPT_EVENT_DATA, eventsData[1]);
                    gzipType = DbParams.GZIP_TRANSPORT_ENCRYPT;
                    try {
                        if (!TextUtils.isEmpty(data) && data.contains("payloads")) {
                            JSONArray jsonArray = new JSONArray();
                            JSONObject dataJSON = new JSONObject(data);
                            dataJSON.put("flush_time", System.currentTimeMillis());
                            jsonArray.put(dataJSON);
                            return new String[]{eventIds, jsonArray.toString(), gzipType};
                        }
                    } catch (JSONException e) {
                        SALog.printStackTrace(e);
                    }
                }
            }
            return eventsData;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    private boolean isSupportTransport() {
        if (mSupportTransport == null) {
            mSupportTransport = SAModuleManager.getInstance().invokeModuleFunction(Modules.Encrypt.MODULE_NAME, Modules.Encrypt.METHOD_VERIFY_SUPPORT_TRANSPORT);
        }
        return mSupportTransport != null && (boolean) mSupportTransport;
    }

    private String encryptValue(String value) {
        String encryptValue = "";
        if (isSupportTransport()) {
            encryptValue = SAModuleManager.getInstance().invokeModuleFunction(Modules.Encrypt.MODULE_NAME, Modules.Encrypt.METHOD_STORE_EVENT, value);
        }
        return TextUtils.isEmpty(encryptValue) ? value : "{\"payloads\": \"" + encryptValue + "\"}";
    }
}
