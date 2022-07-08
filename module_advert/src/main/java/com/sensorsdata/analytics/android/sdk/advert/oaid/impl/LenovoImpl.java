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
import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;

import com.sensorsdata.analytics.android.sdk.advert.oaid.IRomOAID;
import com.sensorsdata.analytics.android.sdk.SALog;

/**
 * 联想、乐檬、摩托罗拉
 */
class LenovoImpl implements IRomOAID {
    private final Context mContext;
    private static final String TAG = "SA.LenovoImpl";
    private final OAIDService mService;

    public LenovoImpl(Context context) {
        this.mContext = context;
        this.mService = new OAIDService();
    }

    @Override
    public boolean isSupported() {
        try {
            PackageInfo pi = mContext.getPackageManager().getPackageInfo("com.zui.deviceidservice", 0);
            return pi != null;
        } catch (Throwable th) {
            SALog.i(TAG, th);
            return false;
        }
    }

    @Override
    public String getRomOAID() {
        Intent intent = new Intent();
        intent.setClassName("com.zui.deviceidservice", "com.zui.deviceidservice.DeviceidService");
        String oaid = null;
        try {
            if (mContext.bindService(intent, mService, Context.BIND_AUTO_CREATE)) {
                LenovoInterface anInterface = new LenovoInterface(OAIDService.BINDER_QUEUE.take());
                oaid = anInterface.getOAID();
                mContext.unbindService(mService);
            }
        } catch (Throwable th) {
            SALog.i(TAG, th);
        }
        return oaid;
    }

    static final class LenovoInterface implements IInterface {
        private final IBinder iBinder;

        private LenovoInterface(IBinder iBinder2) {
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
                obtain.writeInterfaceToken("com.zui.deviceidservice.IDeviceidInterface");
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
