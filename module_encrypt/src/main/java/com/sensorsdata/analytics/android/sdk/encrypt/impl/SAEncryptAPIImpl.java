/*
 * Created by dengshiwei on 2022/08/10.
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

import android.net.Uri;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.encrypt.AESSecretManager;
import com.sensorsdata.analytics.android.sdk.encrypt.R;
import com.sensorsdata.analytics.android.sdk.encrypt.SAEncryptListener;
import com.sensorsdata.analytics.android.sdk.encrypt.SecreteKey;
import com.sensorsdata.analytics.android.sdk.encrypt.biz.SAEventEncryptTools;
import com.sensorsdata.analytics.android.sdk.encrypt.biz.SecretKeyManager;
import com.sensorsdata.analytics.android.sdk.util.SADisplayUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class SAEncryptAPIImpl implements SAEncryptAPI{

    private static final String TAG = "SA.EncryptAPIImpl";
    private final SAContextManager mSAContextManager;
    private final SAEventEncryptTools mSensorsDataEncrypt;
    private final SecretKeyManager mSecretKeyManager;

    public SAEncryptAPIImpl(SAContextManager contextManager) {
        this.mSAContextManager = contextManager;
        mSensorsDataEncrypt = new SAEventEncryptTools(contextManager);
        mSecretKeyManager = SecretKeyManager.getInstance(contextManager);
        if (contextManager.getInternalConfigs().saConfigOptions.getStorePlugins() != null &&
                !contextManager.getInternalConfigs().saConfigOptions.getStorePlugins().isEmpty()) {// 注册默认的 Plugin
            AESSecretManager.getInstance().initSecretKey(contextManager.getContext());
        }
    }

    public <T> T invokeModuleFunction(String methodName, Object... argv) {
        try {
            if ("encryptAES".equals(methodName)) {
                return (T) encryptAES((String) argv[0]);
            } else if ("decryptAES".equals(methodName)) {
                return (T) decryptAES((String) argv[0]);
            } else if ("verifySecretKey".equals(methodName)) {
                return (T) verifySecretKey((Uri) argv[0]);
            } else if ("encryptEventData".equals(methodName)) {
                return (T) encryptEventData((JSONObject) argv[0]);
            } else if ("storeSecretKey".equals(methodName)) {
                storeSecretKey((String) argv[0]);
            } else if ("loadSecretKey".equals(methodName)) {
                return (T) loadSecretKey();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    @Override
    public String encryptAES(String content) {
        return AESSecretManager.getInstance().encryptAES(content);
    }

    @Override
    public String decryptAES(String content) {
        return AESSecretManager.getInstance().decryptAES(content);
    }

    @Override
    public JSONObject encryptEventData(JSONObject jsonObject) {
        return mSensorsDataEncrypt.encryptTrackData(jsonObject);
    }

    @Override
    public String verifySecretKey(Uri uri) {
        String version = uri.getQueryParameter("v");
        String key = Uri.decode(uri.getQueryParameter("key"));
        String symmetricEncryptType = Uri.decode(uri.getQueryParameter("symmetricEncryptType"));
        String asymmetricEncryptType = Uri.decode(uri.getQueryParameter("asymmetricEncryptType"));
        SALog.i(TAG, "Encrypt, version = " + version
                + ", key = " + key
                + ", symmetricEncryptType = " + symmetricEncryptType
                + ", asymmetricEncryptType = " + asymmetricEncryptType);
        String tip;
        if (TextUtils.isEmpty(version) || TextUtils.isEmpty(key)) {
            tip = SADisplayUtil.getStringResource(mSAContextManager.getContext(), R.string.sensors_analytics_encrypt_fail);
        } else if (mSecretKeyManager != null) {
            tip = mSecretKeyManager.checkPublicSecretKey(mSAContextManager.getContext(), version, key, symmetricEncryptType, asymmetricEncryptType);
        } else {
            tip = SADisplayUtil.getStringResource(mSAContextManager.getContext(), R.string.sensors_analytics_encrypt_disable);
        }
        return tip;
    }

    @Override
    public void storeSecretKey(String secretKeyJson) {
        SecretKeyManager.getInstance(mSAContextManager).storeSecretKey(secretKeyJson);
    }

    @Override
    public String loadSecretKey() {
        try {
            SecreteKey secreteKey = mSecretKeyManager.loadSecretKey();
            SAEncryptListener mEncryptListener = mSecretKeyManager.getEncryptListener(secreteKey);
            if (mEncryptListener == null) {
                return "";
            }
            return secreteKey.toString();
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
        return null;
    }
}
