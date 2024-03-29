/*
 * Created by chenru on 2022/4/25 下午5:05(format year/.
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

package com.sensorsdata.analytics.android.sdk.advert.oaid.impl;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.advert.oaid.IRomOAID;

/**
 * 魅族
 */
class MeizuImpl implements IRomOAID {
    private final Context mContext;
    private static final String TAG = "SA.MeizuImpl";

    public MeizuImpl(Context context) {
        this.mContext = context;
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public String getRomOAID() {
        Uri uri = Uri.parse("content://com.meizu.flyme.openidsdk/");
        String oaid = null;
        try {
            Cursor cursor = mContext.getContentResolver().query(uri, null, null,
                    new String[]{"oaid"}, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex("value");
                oaid = cursor.getString(index);
                SALog.i(TAG, "OAID query success: " + oaid);
                cursor.close();
            }
        } catch (Throwable th) {
            SALog.i(TAG, th);
        }
        return oaid;
    }
}
