/*
 * Created by dengshiwei on 2022/11/22.
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

package com.sensorsdata.analytics.android.sdk.encrypt.impl;

import static org.junit.Assert.*;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.encrypt.SAHelper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class SAEncryptProtocolImplTest {
    Application mApplication = ApplicationProvider.getApplicationContext();
    @Test
    public void install() {
        SAHelper.initSensors(mApplication);
        SAEncryptProtocolImpl encryptProtocol = new SAEncryptProtocolImpl();
        encryptProtocol.install(SensorsDataAPI.sharedInstance(mApplication).getSAContextManager());
    }

    @Test
    public void setModuleState() {
        SAHelper.initSensors(mApplication);
        SAEncryptProtocolImpl encryptProtocol = new SAEncryptProtocolImpl();
        encryptProtocol.install(SensorsDataAPI.sharedInstance(mApplication).getSAContextManager());
        Assert.assertTrue(encryptProtocol.isEnable());
        encryptProtocol.setModuleState(false);
        Assert.assertFalse(encryptProtocol.isEnable());
    }

    @Test
    public void getModuleName() {
        SAHelper.initSensors(mApplication);
        SAEncryptProtocolImpl encryptProtocol = new SAEncryptProtocolImpl();
        encryptProtocol.install(SensorsDataAPI.sharedInstance(mApplication).getSAContextManager());
        Assert.assertEquals(encryptProtocol.getModuleName(), Modules.Encrypt.MODULE_NAME);
    }

    @Test
    public void isEnable() {
        SAHelper.initSensors(mApplication);
        SAEncryptProtocolImpl encryptProtocol = new SAEncryptProtocolImpl();
        encryptProtocol.install(SensorsDataAPI.sharedInstance(mApplication).getSAContextManager());
        Assert.assertTrue(encryptProtocol.isEnable());
    }

    @Test
    public void getPriority() {
        SAHelper.initSensors(mApplication);
        SAEncryptProtocolImpl encryptProtocol = new SAEncryptProtocolImpl();
        encryptProtocol.install(SensorsDataAPI.sharedInstance(mApplication).getSAContextManager());
        Assert.assertEquals(6, encryptProtocol.getPriority());
    }

    @Test
    public void invokeModuleFunction() {
        SAHelper.initSensors(mApplication);
        SAEncryptProtocolImpl encryptProtocol = new SAEncryptProtocolImpl();
        encryptProtocol.install(SensorsDataAPI.sharedInstance(mApplication).getSAContextManager());
        encryptProtocol.invokeModuleFunction(Modules.Encrypt.METHOD_LOAD_SECRET_KEY);
    }
}