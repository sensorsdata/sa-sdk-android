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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;

import com.sensorsdata.analytics.android.sdk.advert.oaid.IRomOAID;
import com.sensorsdata.analytics.android.sdk.SALog;

/**
 * 华硕
 */
class AsusImpl implements IRomOAID {
    private final Context mContext;
    private static final String TAG = "SA.AsusImpl";
    private final OAIDService mService;

    public AsusImpl(Context context) {
        this.mContext = context;
        mService = new OAIDService();
    }

    @Override
    public boolean isSupported() {
        try {
            PackageInfo pi = mContext.getPackageManager().getPackageInfo("com.asus.msa.SupplementaryDID", 0);
            return pi != null;
        } catch (Throwable th) {
            SALog.i(TAG, th);
            return false;
        }
    }

    @Override
    public String getRomOAID() {
        Intent intent = new Intent("com.asus.msa.action.ACCESS_DID");
        ComponentName componentName = new ComponentName("com.asus.msa.SupplementaryDID", "com.asus.msa.SupplementaryDID.SupplementaryDIDService");
        intent.setComponent(componentName);
        String oaid = null;
        try {
            if (mContext.bindService(intent, mService, Context.BIND_AUTO_CREATE)) {
                AsusInterface anInterface = new AsusInterface(OAIDService.BINDER_QUEUE.take());
                oaid = anInterface.getOAID();
                mContext.unbindService(mService);
            }
        } catch (Throwable th) {
            SALog.i(TAG, th);
        }
        return oaid;
    }

    static class AsusInterface implements IInterface {
        private final IBinder mIBinder;

        AsusInterface(IBinder iBinder) {
            this.mIBinder = iBinder;
        }

        public IBinder asBinder() {
            return this.mIBinder;
        }

        public String getOAID() {
            Parcel obtain;
            Parcel obtain2;
            String str = null;
            try {
                obtain = Parcel.obtain();
                obtain2 = Parcel.obtain();
                obtain.writeInterfaceToken("com.asus.msa.SupplementaryDID.IDidAidlInterface");
                this.mIBinder.transact(3, obtain, obtain2, 0);
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
