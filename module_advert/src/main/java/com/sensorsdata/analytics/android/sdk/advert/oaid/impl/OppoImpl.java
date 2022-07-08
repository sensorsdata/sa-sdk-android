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

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;

import com.sensorsdata.analytics.android.sdk.advert.oaid.IRomOAID;
import com.sensorsdata.analytics.android.sdk.SALog;

import java.security.MessageDigest;

/**
 * 欧珀、真我、一加
 */
class OppoImpl implements IRomOAID {
    private final Context mContext;
    private final OAIDService mService;
    private static final String TAG = "SA.OppoImpl";
    private String mSign;

    public OppoImpl(Context context) {
        this.mContext = context;
        this.mService = new OAIDService();
    }

    @Override
    public boolean isSupported() {
        try {
            PackageInfo pi = mContext.getPackageManager().getPackageInfo("com.heytap.openid", 0);
            return pi != null;
        } catch (Throwable th) {
            SALog.i(TAG, th);
            return false;
        }
    }

    @Override
    public String getRomOAID() {
        Intent intent = new Intent("action.com.heytap.openid.OPEN_ID_SERVICE");
        intent.setComponent(new ComponentName("com.heytap.openid", "com.heytap.openid.IdentifyService"));
        String oaid = null;
        try {
            if (mContext.bindService(intent, mService, Context.BIND_AUTO_CREATE)) {
                oaid = realGetOUID();
                mContext.unbindService(mService);
            }
        } catch (Throwable th) {
            SALog.i(TAG, th);
        }
        return oaid;
    }

    @SuppressLint("PackageManagerGetSignatures")
    private String realGetOUID() {
        String pkgName = mContext.getPackageName();
        try {
            if (mSign == null) {
                Signature[] signatures = mContext.getPackageManager().getPackageInfo(pkgName,
                        PackageManager.GET_SIGNATURES).signatures;
                byte[] byteArray = signatures[0].toByteArray();
                MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
                byte[] digest = messageDigest.digest(byteArray);
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(Integer.toHexString((b & 255) | 256).substring(1, 3));
                }
                mSign = sb.toString();
                return getSerId(pkgName, mSign);
            }
            return getSerId(pkgName, mSign);
        } catch (Throwable throwable) {
            SALog.i(TAG, throwable);
        }
        return null;
    }

    private String getSerId(String pkgName, String sign) throws InterruptedException {
        OppoInterface anInterface = new OppoInterface(OAIDService.BINDER_QUEUE.take());
        return anInterface.getSerID(pkgName, sign, "OUID");
    }

    static class OppoInterface implements IInterface {
        private final IBinder mIBinder;

        OppoInterface(IBinder iBinder) {
            this.mIBinder = iBinder;
        }

        public IBinder asBinder() {
            return this.mIBinder;
        }

        public String getSerID(String packageName, String sign, String str) {
            Parcel obtain;
            Parcel obtain2;
            String str4 = null;
            try {
                obtain = Parcel.obtain();
                obtain2 = Parcel.obtain();
                obtain.writeInterfaceToken("com.heytap.openid.IOpenID");
                obtain.writeString(packageName);
                obtain.writeString(sign);
                obtain.writeString(str);
                this.mIBinder.transact(1, obtain, obtain2, 0);
                obtain2.readException();
                str4 = obtain2.readString();
                obtain.recycle();
                obtain2.recycle();
            } catch (Throwable th) {
                SALog.i(TAG, th);
            }
            return str4;
        }
    }
}
