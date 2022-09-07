/*
 * Created by dengshiwei on 2020/05/18.
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

package com.sensorsdata.analytics.android.sdk.encrypt.biz;

import android.content.Context;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.encrypt.IPersistentSecretKey;
import com.sensorsdata.analytics.android.sdk.encrypt.R;
import com.sensorsdata.analytics.android.sdk.encrypt.SAEncryptListener;
import com.sensorsdata.analytics.android.sdk.encrypt.SecreteKey;
import com.sensorsdata.analytics.android.sdk.encrypt.encryptor.SAECEncrypt;
import com.sensorsdata.analytics.android.sdk.encrypt.encryptor.SARSAEncrypt;
import com.sensorsdata.analytics.android.sdk.encrypt.utils.EncryptUtils;
import com.sensorsdata.analytics.android.sdk.plugin.encrypt.SAStoreManager;
import com.sensorsdata.analytics.android.sdk.util.SADisplayUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class SecretKeyManager {
    private static final String TAG = "SA.SecretKeyManager";
    private static final String SP_SECRET_KEY = "secret_key";
    private static final int KEY_VERSION_DEFAULT = 0;
    private final IPersistentSecretKey mPersistentSecretKey;
    private final SAContextManager mSAContextManager;
    private final List<SAEncryptListener> mListeners;
    private static SecretKeyManager INSTANCE;

    private SecretKeyManager(SAContextManager contextManager) {
        mSAContextManager = contextManager;
        this.mPersistentSecretKey = contextManager.getInternalConfigs().saConfigOptions.getPersistentSecretKey();
        this.mListeners = contextManager.getInternalConfigs().saConfigOptions.getEncryptors();
        mListeners.add(new SARSAEncrypt());
        if (EncryptUtils.isECEncrypt()) {
            mListeners.add(new SAECEncrypt());
        }
    }

    public static SecretKeyManager getInstance(SAContextManager contextManager) {
        if (INSTANCE == null) {
            INSTANCE = new SecretKeyManager(contextManager);
        }
        return INSTANCE;
    }

    public void storeSecretKey(String configJSON) {
        try {
            JSONObject configObject = new JSONObject(configJSON);
            String configs = configObject.optString("configs");
            configObject = new JSONObject(configs);
            SecreteKey secreteKey = new SecreteKey("", -1, "", "");
            List<SAEncryptListener> encryptListeners = mSAContextManager.getInternalConfigs().saConfigOptions.getEncryptors();
            if (encryptListeners != null && !encryptListeners.isEmpty()) {
                JSONObject keyObject = configObject.optJSONObject("key_v2");
                if (keyObject != null) {
                    String[] types = keyObject.optString("type").split("\\+");
                    if (types.length == 2) {
                        String asymmetricType = types[0];
                        String symmetricType = types[1];
                        for (SAEncryptListener encryptListener : encryptListeners) {
                            if (asymmetricType.equals(encryptListener.asymmetricEncryptType())
                                    && symmetricType.equals(encryptListener.symmetricEncryptType())) {
                                secreteKey.key = keyObject.optString("public_key");
                                secreteKey.version = keyObject.optInt("pkv");
                                secreteKey.asymmetricEncryptType = asymmetricType;
                                secreteKey.symmetricEncryptType = symmetricType;
                            }
                        }
                    }
                }
                if (TextUtils.isEmpty(secreteKey.key)) {
                    parseSecreteKey(configObject.optJSONObject("key"), secreteKey);
                }
            }
            // real store key
            storeSecretKey(secreteKey);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载密钥
     *
     * @throws JSONException 异常
     */
    public SecreteKey loadSecretKey() throws JSONException {
        if (mPersistentSecretKey != null) {
            return readAppKey();
        } else {
            return readLocalKey();
        }
    }

    public SAEncryptListener getEncryptListener(SecreteKey secreteKey) {
        if (!isSecretKeyNull(secreteKey)) {
            for (SAEncryptListener listener : mListeners) {
                if (isMatchEncryptType(listener, secreteKey)) {
                    return listener;
                }
            }
        }
        return null;
    }

    public boolean isSecretKeyNull(SecreteKey secreteKey) {
        return secreteKey == null || TextUtils.isEmpty(secreteKey.key) || secreteKey.version == KEY_VERSION_DEFAULT;
    }

    /**
     * 检查公钥密钥信息是否和本地一致
     *
     * @param activity Context
     * @param version 版本号
     * @param key 密钥信息
     * @param symmetricEncryptType 对称加密类型
     * @param asymmetricEncryptType 非对称加密类型
     * @return -1 是本地密钥信息为空，-2 是相同，其它是不相同
     */
    public String checkPublicSecretKey(Context activity, String version, String key, String symmetricEncryptType, String asymmetricEncryptType) {
        try {
            SecreteKey secreteKey = loadSecretKey();
            if (secreteKey == null || TextUtils.isEmpty(secreteKey.key)) {
                return SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_encrypt_key_null);
            } else if (version.equals(secreteKey.version + "")
                    && disposeECPublicKey(key).equals(disposeECPublicKey(secreteKey.key))) {
                if ((symmetricEncryptType == null || asymmetricEncryptType == null)
                        || (symmetricEncryptType.equals(secreteKey.symmetricEncryptType) && asymmetricEncryptType.equals(secreteKey.asymmetricEncryptType))) {
                    return SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_encrypt_pass);
                } else {
                    return String.format(SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_encrypt_verify_fail_type),
                            symmetricEncryptType, asymmetricEncryptType, secreteKey.symmetricEncryptType, secreteKey.asymmetricEncryptType);
                }
            } else {
                return String.format(SADisplayUtil.getStringResource(activity, R.string.sensors_analytics_encrypt_verify_fail_version), version, secreteKey.version);
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return "";
    }

    /**
     * 删除公钥前面的 EC 标识
     *
     * @param key 公钥
     * @return 返回 publicKey
     */
    private String disposeECPublicKey(String key) {
        if (TextUtils.isEmpty(key) || !key.startsWith("EC:")) {
            return key;
        } else {
            return key.substring(key.indexOf(":") + 1);
        }
    }

    /**
     * 从 App 端读取密钥
     */
    private SecreteKey readAppKey() {
        String publicKey = "";
        int keyVersion = 0;
        String symmetricEncryptType = "";
        String asymmetricEncryptType = "";
        SecreteKey rsaPublicKeyVersion = mPersistentSecretKey.loadSecretKey();
        if (rsaPublicKeyVersion != null) {
            publicKey = rsaPublicKeyVersion.key;
            keyVersion = rsaPublicKeyVersion.version;
            symmetricEncryptType = rsaPublicKeyVersion.symmetricEncryptType;
            asymmetricEncryptType = rsaPublicKeyVersion.asymmetricEncryptType;
        }
        SALog.i(TAG, "readAppKey [key = " + publicKey + " ,v = " + keyVersion + " ,symmetricEncryptType = " + symmetricEncryptType + " ,asymmetricEncryptType = " + asymmetricEncryptType + "]");
        return new SecreteKey(publicKey, keyVersion, symmetricEncryptType, asymmetricEncryptType);
    }

    /**
     * 从 SDK 端读取密钥
     *
     * @throws JSONException 异常
     */
    private SecreteKey readLocalKey() throws JSONException {
        String publicKey = "";
        int keyVersion = 0;
        String symmetricEncryptType = "";
        String asymmetricEncryptType = "";
        String secretKey = SAStoreManager.getInstance().getString(SP_SECRET_KEY, "");
        if (!TextUtils.isEmpty(secretKey)) {
            JSONObject jsonObject = new JSONObject(secretKey);
            publicKey = jsonObject.optString("key", "");
            keyVersion = jsonObject.optInt("version", KEY_VERSION_DEFAULT);
            symmetricEncryptType = jsonObject.optString("symmetricEncryptType", "");
            asymmetricEncryptType = jsonObject.optString("asymmetricEncryptType", "");
        }
        SALog.i(TAG, "readLocalKey [key = " + publicKey + " ,v = " + keyVersion + " ,symmetricEncryptType = " + symmetricEncryptType + " ,asymmetricEncryptType = " + asymmetricEncryptType + "]");
        return new SecreteKey(publicKey, keyVersion, symmetricEncryptType, asymmetricEncryptType);
    }

    /**
     * 检测加密插件加密类型是否一致
     *
     * @param listener 加密插件
     * @param secreteKey 加密信息
     * @return 类型是否一致 true:一致 false:不一致
     */
    private boolean isMatchEncryptType(SAEncryptListener listener, SecreteKey secreteKey) {
        return listener != null && !isSecretKeyNull(secreteKey) && !isEncryptorTypeNull(listener) && listener.asymmetricEncryptType().equals(secreteKey.asymmetricEncryptType)
                && listener.symmetricEncryptType().equals(secreteKey.symmetricEncryptType);
    }

    private boolean isEncryptorTypeNull(SAEncryptListener saEncryptListener) {
        return TextUtils.isEmpty(saEncryptListener.asymmetricEncryptType())
                || TextUtils.isEmpty(saEncryptListener.symmetricEncryptType());
    }

    private void parseSecreteKey(JSONObject keyObject, SecreteKey secreteKey) {
        if (keyObject != null) {
            try {
                if (keyObject.has("key_ec") && EncryptUtils.isECEncrypt()) {
                    String key_ec = keyObject.optString("key_ec");
                    if (!TextUtils.isEmpty(key_ec)) {
                        keyObject = new JSONObject(key_ec);
                    }
                }

                secreteKey.key = keyObject.optString("public_key");
                secreteKey.symmetricEncryptType = "AES";
                if (keyObject.has("type")) {
                    String type = keyObject.optString("type");
                    secreteKey.key = type + ":" + secreteKey.key;
                    secreteKey.asymmetricEncryptType = type;
                } else {
                    secreteKey.asymmetricEncryptType = "RSA";
                }
                secreteKey.version = keyObject.optInt("pkv");
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    private void storeSecretKey(SecreteKey secreteKey) {
        try {
            SALog.i(TAG, "[saveSecretKey] publicKey = " + secreteKey.toString());
            SAEncryptListener encryptListener = getEncryptListener(secreteKey);
            if (encryptListener != null) {
                if (mPersistentSecretKey != null) {
                    mPersistentSecretKey.saveSecretKey(secreteKey);
                    // 同时删除本地的密钥
                    SAStoreManager.getInstance().setString(SP_SECRET_KEY, "");
                } else {
                    SAStoreManager.getInstance().setString(SP_SECRET_KEY, secreteKey.toString());
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
