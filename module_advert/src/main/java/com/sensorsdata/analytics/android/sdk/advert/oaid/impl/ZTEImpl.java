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
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;

import com.sensorsdata.analytics.android.sdk.advert.oaid.IRomOAID;
import com.sensorsdata.analytics.android.sdk.SALog;

import java.lang.reflect.Constructor;

/**
 * 中兴
 */
public class ZTEImpl implements IRomOAID {
    private static final String TAG = "SA.ZTEImpl";
    private static final String ZTE_MANAGER = "android.app.ZteDeviceIdentifyManager";
    private static final String ID_PACKAGE = "com.mdid.msa";
    private final Context mContext;
    private final OAIDService mService;

    ZTEImpl(Context context) {
        this.mContext = context;
        this.mService = new OAIDService();
    }

    @SuppressLint("PrivateApi")
    private static String getOAID30(Context context) {
        Constructor<?> declaredConstructor;
        try {
             Class<?> cls = Class.forName(ZTE_MANAGER);
            if (cls != null && (declaredConstructor = cls.getDeclaredConstructor(Context.class)) != null) {
                Object newInstance = declaredConstructor.newInstance(context);
                return (String) cls.getDeclaredMethod("getOAID", Context.class).invoke(newInstance, context);
            }
        } catch (Throwable th) {
            SALog.i(TAG, th);
        }
        return null;
    }

    private static void startMsaklServer(String str, Context context) {
        Intent intent = new Intent();
        intent.setClassName(ID_PACKAGE, "com.mdid.msa.service.MsaKlService");
        intent.setAction("com.bun.msa.action.start.service");
        intent.putExtra("com.bun.msa.param.pkgname", str);
        try {
            intent.putExtra("com.bun.msa.param.runinset", true);
            if (context.startService(intent) != null) {
            }
        } catch (Throwable th) {
            SALog.i(TAG, th);
        }
    }

    @Override
    public boolean isSupported() {
        try {
            if (Build.VERSION.SDK_INT <= 29) {
                mContext.getPackageManager().getPackageInfo(ID_PACKAGE, 0);
            }
            return true;
        } catch (Throwable t) {
            SALog.i(TAG, t);
        }
        return false;
    }

    @Override
    public String getRomOAID() {
        return bindZTEServiceGetOAID(mContext);
    }

    public String bindZTEServiceGetOAID(Context context) {
        if (Build.VERSION.SDK_INT <= 29) {
            return getOAID29(context);
        } else {
            return getOAID30(context);
        }
    }

    private String getOAID29(Context context) {
        String oaid = null;
        try {
            String packageName = context.getPackageName();
            startMsaklServer(packageName, context);
            Intent intent = new Intent();
            intent.setClassName("com.mdid.msa", "com.mdid.msa.service.MsaIdService");
            intent.setAction("com.bun.msa.action.bindto.service");
            intent.putExtra("com.bun.msa.param.pkgname", packageName);
            if (context.bindService(intent, mService, Context.BIND_AUTO_CREATE)) {
                oaid = new ZTEInterface(OAIDService.BINDER_QUEUE.take()).getOAID();
                try {
                    context.unbindService(this.mService);
                } catch (Throwable th) {
                    SALog.i(TAG, th);
                }
            } else {
                return oaid;
            }
        } catch (Throwable t) {
            SALog.i(TAG, t);
        }
        return oaid;
    }

    static class ZTEInterface implements IInterface {
        private final IBinder mIBinder;

        ZTEInterface(IBinder iBinder) {
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
                obtain.writeInterfaceToken("com.bun.lib.MsaIdInterface");
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
