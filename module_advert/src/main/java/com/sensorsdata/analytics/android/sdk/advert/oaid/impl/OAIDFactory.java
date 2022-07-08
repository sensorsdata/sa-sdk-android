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

import android.app.Application;
import android.content.Context;

import com.sensorsdata.analytics.android.sdk.advert.oaid.IRomOAID;
import com.sensorsdata.analytics.android.sdk.advert.oaid.OAIDRom;
import com.sensorsdata.analytics.android.sdk.SALog;

public final class OAIDFactory {
    private static IRomOAID ioaid;
    private static final String TAG = "SA.OAIDFactory";

    public static IRomOAID create(Context context) {
        if (context != null && !(context instanceof Application)) {
            context = context.getApplicationContext();
        }
        if (ioaid != null) {
            return ioaid;
        }
        ioaid = createManufacturerImpl(context);
        if (ioaid != null && ioaid.isSupported()) {
            SALog.i(TAG, "Manufacturer interface has been found: " + ioaid.getClass().getName());
            return ioaid;
        }
        ioaid = new DefaultImpl();
        return ioaid;
    }

    private static IRomOAID createManufacturerImpl(Context context) {
        if (OAIDRom.isHuawei() || OAIDRom.isEmui()) {
            return new HuaweiImpl(context);
        }
        if (OAIDRom.isXiaomi() || OAIDRom.isMiui() || OAIDRom.isBlackShark()) {
            return new XiaomiImpl(context);
        }
        if (OAIDRom.isVivo()) {
            return new VivoImpl(context);
        }
        if (OAIDRom.isOppo() || OAIDRom.isOnePlus()) {
            return new OppoImpl(context);
        }
        if (OAIDRom.isMeizu()) {
            return new MeizuImpl(context);
        }
        if (OAIDRom.isSamsung()) {
            return new SamsungImpl(context);
        }
        if (OAIDRom.isNubia()) {
            return new NubiaImpl(context);
        }
        if (OAIDRom.isASUS()) {
            return new AsusImpl(context);
        }
        if (OAIDRom.isLenovo() || OAIDRom.isMotolora()) {
            return new LenovoImpl(context);
        }
        if (OAIDRom.isZTE()) {
            return new ZTEImpl(context);
        }
        if (OAIDRom.isCoolpad(context)) {
            return new CoolpadImpl(context);
        }
        return null;
    }
}
