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
import android.os.Build;

import com.sensorsdata.analytics.android.sdk.advert.oaid.IRomOAID;
import com.sensorsdata.analytics.android.sdk.advert.oaid.OAIDRom;
import com.sensorsdata.analytics.android.sdk.SALog;

/**
 * 维沃、爱酷
 */
class VivoImpl implements IRomOAID {
    private final Context mContext;
    private static final String TAG = "SA.VivoImpl";

    public VivoImpl(Context context) {
        this.mContext = context;
    }

    @Override
    public boolean isSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return false;
        }
        return OAIDRom.sysProperty("persist.sys.identifierid.supported", "0").equals("1");
    }

    @Override
    public String getRomOAID() {
        Uri uri = Uri.parse("content://com.vivo.vms.IdProvider/IdentifierId/OAID");
        String oaid = null;
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(uri, null, null,
                    null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex("value");
                oaid = cursor.getString(index);
                if (oaid == null || oaid.length() == 0) {
                    SALog.i(TAG, "OAID query failed");
                }
            }
        } catch (Throwable th) {
            SALog.i(TAG, th);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return oaid;
    }
}
