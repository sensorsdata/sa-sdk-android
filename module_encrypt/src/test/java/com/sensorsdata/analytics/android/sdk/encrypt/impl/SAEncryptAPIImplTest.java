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
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.encrypt.SAHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class SAEncryptAPIImplTest {
    Application mApplication = ApplicationProvider.getApplicationContext();
    @Test
    public void invokeModuleFunction() {
        SAHelper.initSensors(mApplication);
        SAEncryptAPIImpl encryptAPIImpl = new SAEncryptAPIImpl(SensorsDataAPI.sharedInstance(mApplication).getSAContextManager());
        encryptAPIImpl.invokeModuleFunction(Modules.Encrypt.METHOD_LOAD_SECRET_KEY);
    }

    @Test
    public void encryptAES() {
        SAHelper.initSensors(mApplication);
        SAEncryptAPIImpl encryptAPIImpl = new SAEncryptAPIImpl(SensorsDataAPI.sharedInstance(mApplication).getSAContextManager());
        final String Identity = "Identity";
        String encrypt = encryptAPIImpl.encryptAES(Identity);
        Assert.assertEquals(encryptAPIImpl.decryptAES(encrypt), Identity);
    }

    @Test
    public void decryptAES() {
        SAHelper.initSensors(mApplication);
        SAEncryptAPIImpl encryptAPIImpl = new SAEncryptAPIImpl(SensorsDataAPI.sharedInstance(mApplication).getSAContextManager());
        final String Identity = "Identity";
        String encrypt = encryptAPIImpl.encryptAES(Identity);
        Assert.assertEquals(encryptAPIImpl.decryptAES(encrypt), Identity);
    }

    @Test
    public void encryptEventData() {
        SAHelper.initSensors(mApplication);
        SAEncryptAPIImpl encryptAPIImpl = new SAEncryptAPIImpl(SensorsDataAPI.sharedInstance(mApplication).getSAContextManager());
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("_track_id", "123213");
            jsonObject.put("properties", "property");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        encryptAPIImpl.encryptEventData(jsonObject);
    }

    @Test
    public void verifySecretKey() {
        Uri uri = Uri.parse("sace7b04ba://encrypt?v=25&key=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiX+GcdDPF1In7cvBSvKYrCEGnC3jU+ADrFvvGQFcxloEFTGs6JOMdi56y5l4XJqDLKX/gp4wdIFiV89S0JnOF7V2Q0FvN+1ReaUvI93Fwus9BEnCUchdTHL6jZWBbe1Gq4oTuVv2PcASy+hBplnf1D7HXds6SAf08ZNEX5LGEp+1sIVm8SuH0VXE5rIxZSIz/NB5lDztmOwDWlVUjPi+xURG5L9HrDGgi4DelgrWm9Yq0vmASJ8COItLOMyrr3W3SgdyWR9dJD4Kutq1rflywa32IzdxEFR9HW7p9EAPypTnY2YhW2S/sA/Vz1xPLL8zh/6GL68tnYDz+wva1tUNzQIDAQAB&symmetricEncryptType=AES&asymmetricEncryptType=RSA");
        SAHelper.initSensors(mApplication);
        SAEncryptAPIImpl encryptAPIImpl = new SAEncryptAPIImpl(SensorsDataAPI.sharedInstance(mApplication).getSAContextManager());
        Assert.assertEquals("密钥验证不通过，App 端密钥为空", encryptAPIImpl.verifySecretKey(uri));
    }

    @Test
    public void storeSecretKey() {
        SAHelper.initSensors(mApplication);
        String key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiX+GcdDPF1In7cvBSvKYrCEGnC3jU+ADrFvvGQFcxloEFTGs6JOMdi56y5l4XJqDLKX/gp4wdIFiV89S0JnOF7V2Q0FvN+1ReaUvI93Fwus9BEnCUchdTHL6jZWBbe";
        JSONObject keyJson = new JSONObject();
        JSONObject remoteJson = new JSONObject();
        try {
            keyJson.put("public_key", key);
            keyJson.put("pkv", "1");
            remoteJson.put("configs", keyJson.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        SAEncryptAPIImpl encryptAPIImpl = new SAEncryptAPIImpl(SensorsDataAPI.sharedInstance(mApplication).getSAContextManager());
        encryptAPIImpl.storeSecretKey(remoteJson.toString());
    }

    @Test
    public void loadSecretKey() {
        SAHelper.initSensors(mApplication);
        SAEncryptAPIImpl encryptAPIImpl = new SAEncryptAPIImpl(SensorsDataAPI.sharedInstance(mApplication).getSAContextManager());
        encryptAPIImpl.loadSecretKey();
    }
}