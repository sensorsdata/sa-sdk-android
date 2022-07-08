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

import com.sensorsdata.analytics.android.sdk.advert.oaid.IRomOAID;
import com.sensorsdata.analytics.android.sdk.SALog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 小米、红米、黑鲨
 */
public class XiaomiImpl implements IRomOAID {
    private final Context mContext;
    private Class<?> mIdProviderClass;
    private Object mIdProviderImpl;
    private static final String TAG = "SA.OAIDFactory";


    @SuppressLint("PrivateApi")
    public XiaomiImpl(Context context) {
        this.mContext = context;
        try {
            mIdProviderClass = Class.forName("com.android.id.impl.IdProviderImpl");
            mIdProviderImpl = mIdProviderClass.newInstance();
        } catch (Throwable th) {
            SALog.i(TAG, th);
        }
    }

    @Override
    public boolean isSupported() {
        return mIdProviderImpl != null;
    }

    @Override
    public String getRomOAID() {
        if (mIdProviderClass == null || mIdProviderImpl == null) {
            return null;
        }
        String oaid = null;
        try {
            oaid = getOAID();
        } catch (Throwable th) {
            SALog.i(TAG, th);
        }
        return oaid;
    }

    private String getOAID() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = mIdProviderClass.getMethod("getOAID", Context.class);
        return (String) method.invoke(mIdProviderImpl, mContext);
    }
}
