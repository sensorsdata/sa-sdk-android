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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.provider.Settings;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.advert.oaid.IRomOAID;
import com.sensorsdata.analytics.android.sdk.SALog;

/**
 * 华为、荣耀
 */
public class HuaweiImpl implements IRomOAID {
    private final Context mContext;
    private final OAIDService mService;
    private static final String TAG = "SA.HuaweiImpl";
    private String mPackageName;

    public HuaweiImpl(Context context) {
        this.mContext = context;
        this.mService = new OAIDService();
    }

    @Override
    public boolean isSupported() {
        boolean ret = false;
        try {
            PackageManager pm = mContext.getPackageManager();
            if (pm.getPackageInfo("com.huawei.hwid", 0) != null) {
                mPackageName = "com.huawei.hwid";
                ret = true;
            } else if (pm.getPackageInfo("com.huawei.hwid.tv", 0) != null) {
                mPackageName = "com.huawei.hwid.tv";
                ret = true;
            } else {
                mPackageName = "com.huawei.hms";
                ret = pm.getPackageInfo(mPackageName, 0) != null;
            }
        } catch (Throwable t) {
            SALog.i(TAG, t);
        }
        return ret;
    }

    @Override
    public String getRomOAID() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                String oaid = Settings.Global.getString(mContext.getContentResolver(), "pps_oaid");
                if (!TextUtils.isEmpty(oaid)) {
                    SALog.i(TAG, "Get oaid from global settings");
                    return oaid;
                }
            } catch (Throwable t) {
                SALog.i(TAG, t);
            }
        }
        if (TextUtils.isEmpty(mPackageName) && !isSupported()) {
            SALog.i(TAG, "Huawei Advertising ID not available");
            return null;
        }
        Intent intent = new Intent("com.uodis.opendevice.OPENIDS_SERVICE");
        intent.setPackage(mPackageName);
        String oaid = null;
        try {
            if (mContext.bindService(intent, mService, Context.BIND_AUTO_CREATE)) {
                HuaWeiInterface anInterface = new HuaWeiInterface(OAIDService.BINDER_QUEUE.take());
                oaid = anInterface.getOAID();
            }
        } catch (Throwable t) {
            SALog.i(TAG, t);
        }
        return oaid;
    }

    static final class HuaWeiInterface implements IInterface {
        private final IBinder iBinder;

        private HuaWeiInterface(IBinder iBinder2) {
            this.iBinder = iBinder2;
        }

        public IBinder asBinder() {
            return this.iBinder;
        }

        public String getOAID() {
            Parcel obtain;
            Parcel obtain2;
            String str = null;
            try {
                obtain = Parcel.obtain();
                obtain2 = Parcel.obtain();
                obtain.writeInterfaceToken("com.uodis.opendevice.aidl.OpenDeviceIdentifierService");
                this.iBinder.transact(1, obtain, obtain2, 0);
                obtain2.readException();
                str = obtain2.readString();
                obtain.recycle();
                obtain2.recycle();
            } catch (Throwable th) {
                SALog.i(TAG, th);
            }
            return str;
        }
    }
}
