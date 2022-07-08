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

public class CoolpadImpl implements IRomOAID {
    private final Context context;
    private static final String TAG = "SA.CoolpadImpl";
    private final OAIDService service;

    public CoolpadImpl(Context context) {
        this.context = context;
        this.service = new OAIDService();
    }

    @Override
    public boolean isSupported() {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo("com.coolpad.deviceidsupport", 0);
            return pi != null;
        } catch (Throwable throwable) {
            SALog.i(TAG, throwable);
            return false;
        }
    }

    @Override
    public String getRomOAID() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.coolpad.deviceidsupport", "com.coolpad.deviceidsupport.DeviceIdService"));
        String oaid = null;
        try {
            if (context.bindService(intent, service, Context.BIND_AUTO_CREATE)) {
                CoolpadInterface anInterface = new CoolpadInterface(OAIDService.BINDER_QUEUE.take());
                oaid = anInterface.getOAID(context.getPackageName());
                context.unbindService(service);
            }
        } catch (Throwable throwable) {
            SALog.i(TAG, throwable);
        }
        return oaid;
    }

    static class CoolpadInterface implements IInterface {
        private final IBinder mIBinder;

        CoolpadInterface(IBinder iBinder) {
            this.mIBinder = iBinder;
        }

        public IBinder asBinder() {
            return this.mIBinder;
        }

        public String getOAID(String packageName) {
            Parcel obtain;
            Parcel obtain2;
            String str = null;
            try {
                obtain = Parcel.obtain();
                obtain2 = Parcel.obtain();
                obtain.writeInterfaceToken("com.coolpad.deviceidsupport.IDeviceIdManager");
                obtain.writeString(packageName);
                this.mIBinder.transact(2, obtain, obtain2, 0);
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
