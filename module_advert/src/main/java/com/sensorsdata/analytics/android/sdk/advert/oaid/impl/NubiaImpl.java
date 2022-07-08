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

import android.content.ContentProviderClient;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.sensorsdata.analytics.android.sdk.advert.oaid.IRomOAID;
import com.sensorsdata.analytics.android.sdk.SALog;

/**
 * 努比亚
 */
class NubiaImpl implements IRomOAID {
    private final Context mContext;
    private static final String TAG = "SA.NubiaImpl";

    public NubiaImpl(Context context) {
        this.mContext = context;
    }

    @Override
    public boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    @Override
    public String getRomOAID() {
        if (!isSupported()) {
            SALog.i(TAG, "Only supports Android 10.0 and above for Nubia");
            return null;
        }
        String oaid = null;
        try {
            Uri uri = Uri.parse("content://cn.nubia.identity/identity");
            ContentProviderClient client = mContext.getContentResolver().acquireContentProviderClient(uri);
            if (client == null) {
                return null;
            }
            Bundle bundle = client.call("getOAID", null, null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                client.close();
            } else {
                client.release();
            }
            if (bundle == null) {
                SALog.i(TAG, "OAID query failed: bundle is null");
                return null;
            }
            if (bundle.getInt("code", -1) == 0) {
                oaid = bundle.getString("id");
            }
            SALog.i(TAG, "OAID query success: " + oaid);
        } catch (Throwable th) {
            SALog.i(TAG, th);
        }
        return oaid;
    }
}
