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

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.encrypt.AESSecretManager;
import com.sensorsdata.analytics.android.sdk.encrypt.R;
import com.sensorsdata.analytics.android.sdk.encrypt.SAEncryptListener;
import com.sensorsdata.analytics.android.sdk.encrypt.SecreteKey;
import com.sensorsdata.analytics.android.sdk.encrypt.biz.SAEventEncryptTools;
import com.sensorsdata.analytics.android.sdk.encrypt.biz.SecretKeyManager;
import com.sensorsdata.analytics.android.sdk.util.SADisplayUtil;

import org.json.JSONException;

public class SAEncryptAPIImpl implements SAEncryptAPI {

    private static final String TAG = "SA.EncryptAPIImpl";
    private SAContextManager mSAContextManager;
    private SAEventEncryptTools mSensorsDataEncrypt;
    private SecretKeyManager mSecretKeyManager;

    public SAEncryptAPIImpl(SAContextManager contextManager) {
        try {
            this.mSAContextManager = contextManager;
            SAConfigOptions configOptions = contextManager.getInternalConfigs().saConfigOptions;
            if (configOptions.isEnableEncrypt() || configOptions.isTransportEncrypt()) {
                mSensorsDataEncrypt = new SAEventEncryptTools(contextManager);
                mSecretKeyManager = SecretKeyManager.getInstance(contextManager);
                AESSecretManager.getInstance().initSecretKey(contextManager.getContext());
            } else if (configOptions.getAdvertConfig() != null &&
                    configOptions.getAdvertConfig().secreteKey != null) {//配置广告上报加密
                mSensorsDataEncrypt = new SAEventEncryptTools(contextManager);
            }
            if (configOptions.getStorePlugins() != null && !configOptions.getStorePlugins().isEmpty()) {// 注册默认的 Plugin
                AESSecretManager.getInstance().initSecretKey(contextManager.getContext());
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public <T> T invokeModuleFunction(String methodName, Object... argv) {
        try {
            if (Modules.Encrypt.METHOD_ENCRYPT_AES.equals(methodName)) {
                return (T) encryptAES((String) argv[0]);
            } else if (Modules.Encrypt.METHOD_DECRYPT_AES.equals(methodName)) {
                return (T) decryptAES((String) argv[0]);
            } else if (Modules.Encrypt.METHOD_VERIFY_SECRET_KEY.equals(methodName)) {
                return (T) verifySecretKey((Uri) argv[0]);
            } else if (Modules.Encrypt.METHOD_ENCRYPT_EVENT_DATA.equals(methodName)) {
                return (T) encryptEventData(argv[0]);
            } else if (Modules.Encrypt.METHOD_ENCRYPT_EVENT_DATA_WITH_KEY.equals(methodName)) {
                return (T) encryptEventData(argv[0], (SecreteKey) argv[1]);
            } else if (Modules.Encrypt.METHOD_STORE_SECRET_KEY.equals(methodName)) {
                storeSecretKey((String) argv[0]);
            } else if (Modules.Encrypt.METHOD_LOAD_SECRET_KEY.equals(methodName)) {
                return (T) loadSecretKey();
            } else if (Modules.Encrypt.METHOD_VERIFY_SUPPORT_TRANSPORT.equals(methodName)) {
                return (T) mSecretKeyManager.isSupportTransportEncrypt();
            } else if (Modules.Encrypt.METHOD_STORE_EVENT.equals(methodName)) {
                SAEncryptListener encryptListener = mSensorsDataEncrypt.getEncryptListener();
                if (encryptListener instanceof AbsSAEncrypt) {
                    return (T) ((AbsSAEncrypt) encryptListener).encryptEventRecord((String) argv[0]);
                }
            } else if (Modules.Encrypt.METHOD_LOAD_EVENT.equals(methodName)) {
                SAEncryptListener encryptListener = mSensorsDataEncrypt.getEncryptListener();
                if (encryptListener instanceof AbsSAEncrypt) {
                    return (T) ((AbsSAEncrypt) encryptListener).decryptEventRecord((String) argv[0]);
                }
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
    public <T> T encryptEventData(T jsonObject) {
        return mSensorsDataEncrypt.encryptTrackData(jsonObject);
    }

    @Override
    public <T> T encryptEventData(T jsonObject, SecreteKey secreteKey) {
        if (mSensorsDataEncrypt == null) {//防止开启广告上报但未开启加密出现空指针
            return jsonObject;
        }
        return mSensorsDataEncrypt.encryptTrackData(jsonObject, secreteKey);
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
