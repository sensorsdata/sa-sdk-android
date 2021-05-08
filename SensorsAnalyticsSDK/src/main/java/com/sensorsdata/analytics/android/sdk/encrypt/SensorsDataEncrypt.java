/*
 * Created by dengshiwei on 2020/05/18.
 * Copyright 2015－2021 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.encrypt;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class SensorsDataEncrypt {
    private static final String SP_SECRET_KEY = "secret_key";
    private static final int KEY_VERSION_DEFAULT = 0;
    private static final String TAG = "SensorsDataEncrypt";
    private List<SAEncryptListener> mListeners;
    private SecreteKey mSecreteKey;

    private IPersistentSecretKey mPersistentSecretKey;
    private Context mContext;
    private SAEncryptListener mEncryptListener;


    public SensorsDataEncrypt(Context context, IPersistentSecretKey persistentSecretKey, List<SAEncryptListener> listeners) {
        this.mPersistentSecretKey = persistentSecretKey;
        this.mContext = context;
        this.mListeners = listeners;
        if (mListeners == null) {
            mListeners = new ArrayList<>();
        }
        mListeners.add(new SARSAEncrypt());
        mListeners.add(new SAECEncrypt());
    }

    /**
     * 检测是否集成 EC 算法
     *
     * @return 是否集成 EC 算法
     */
    public static boolean isECEncrypt() {
        try {
            Class.forName("org.spongycastle.jce.provider.BouncyCastleProvider");
            return true;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return false;
    }

    /**
     * 针对数据进行加密
     *
     * @param jsonObject，需要加密的数据
     * @return 加密后的数据
     */
    public JSONObject encryptTrackData(JSONObject jsonObject) {
        try {
            if (isSecretKeyNull(mSecreteKey)) {
                mSecreteKey = loadSecretKey();
                if (isSecretKeyNull(mSecreteKey)) {
                    return jsonObject;
                }
            }

            if (!isMatchEncryptType(mEncryptListener, mSecreteKey)) {
                mEncryptListener = getEncryptListener(mSecreteKey);
            }

            if (mEncryptListener == null) {
                return jsonObject;
            }

            //获取公钥加密后的对称密钥
            String publicKey = mSecreteKey.key;
            if (publicKey.startsWith("EC:")) {
                publicKey = publicKey.substring(publicKey.indexOf(":") + 1);
            }

            String encryptedKey = mEncryptListener.encryptSymmetricKeyWithPublicKey(publicKey);

            if (TextUtils.isEmpty(encryptedKey)) {
                return jsonObject;
            }

            String encryptData = mEncryptListener.encryptEvent(gzipEventData(jsonObject.toString()));
            if (TextUtils.isEmpty(encryptData)) {
                return jsonObject;
            }
            JSONObject dataJson = new JSONObject();
            dataJson.put("ekey", encryptedKey);
            dataJson.put("pkv", mSecreteKey.version);
            dataJson.put("payloads", encryptData);
            return dataJson;
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return jsonObject;
    }

    /**
     * 保存密钥
     *
     * @param secreteKey 密钥信息
     */
    public void saveSecretKey(SecreteKey secreteKey) {
        try {
            SALog.i(TAG, "[saveSecretKey] publicKey = " + secreteKey.toString());

            SAEncryptListener encryptListener = getEncryptListener(secreteKey);
            if (isMatchEncryptType(encryptListener, secreteKey)) {
                if (mPersistentSecretKey != null) {
                    mPersistentSecretKey.saveSecretKey(secreteKey);
                    // 同时删除本地的密钥
                    saveLocalSecretKey("");
                } else {
                    saveLocalSecretKey(secreteKey.toString());
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 公钥是否为空
     *
     * @return true，为空。false，不为空
     */
    public boolean isPublicSecretKeyNull() {
        try {
            SecreteKey secreteKey = loadSecretKey();
            return TextUtils.isEmpty(secreteKey.key);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return true;
    }

    /**
     * 检测加密插件加密类型是否一致
     *
     * @param listener 加密插件
     * @param secreteKey 加密信息
     * @return 类型是否一致 true:一致 false:不一致
     */
    boolean isMatchEncryptType(SAEncryptListener listener, SecreteKey secreteKey) {
        return listener != null && listener.asymmetricEncryptType().equals(secreteKey.asymmetricEncryptType)
                && listener.symmetricEncryptType().equals(secreteKey.symmetricEncryptType);
    }

    /**
     * 检查公钥密钥信息是否和本地一致
     *
     * @param version 版本号
     * @param key 密钥信息
     * @return -1 是本地密钥信息为空，-2 是相同，其它是不相同
     */
    public String checkPublicSecretKey(String version, String key) {
        try {
            SecreteKey secreteKey = loadSecretKey();
            if (secreteKey == null || TextUtils.isEmpty(secreteKey.key)) {
                return "密钥验证不通过，App 端密钥为空";
            } else if (version.equals(secreteKey.version + "") && key.equals(secreteKey.key)) {
                return "密钥验证通过，所选密钥与 App 端密钥相同";
            } else {
                return "密钥验证不通过，所选密钥与 App 端密钥不相同。所选密钥版本:" + version +
                        "，App 端密钥版本:" + secreteKey.version;
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return "";
    }

    /**
     * 压缩事件
     *
     * @param record 压缩
     * @return 压缩后事件
     */
    private byte[] gzipEventData(String record) {
        GZIPOutputStream gzipOutputStream = null;
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            gzipOutputStream = new GZIPOutputStream(buffer);
            gzipOutputStream.write(record.getBytes());
            gzipOutputStream.finish();
            return buffer.toByteArray();
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
            return null;
        } finally {
            if (gzipOutputStream != null) {
                try {
                    gzipOutputStream.close();
                } catch (Exception ex) {
                    SALog.printStackTrace(ex);
                }
            }
        }
    }

    /**
     * 存储密钥
     *
     * @param key 密钥
     */
    private void saveLocalSecretKey(String key) {
        SharedPreferences preferences = SensorsDataUtils.getSharedPreferences(mContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SP_SECRET_KEY, key);
        editor.apply();
    }

    /**
     * 加载密钥
     *
     * @throws JSONException 异常
     */
    private SecreteKey loadSecretKey() throws JSONException {
        if (mPersistentSecretKey != null) {
            return readAppKey();
        } else {
            return readLocalKey();
        }
    }

    /**
     * 从 App 端读取密钥
     */
    private SecreteKey readAppKey() {
        String publicKey = null;
        int keyVersion = 0;
        String symmetricEncryptType = null;
        String asymmetricEncryptType = null;
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
        String publicKey = null;
        int keyVersion = 0;
        String symmetricEncryptType = null;
        String asymmetricEncryptType = null;
        final SharedPreferences preferences = SensorsDataUtils.getSharedPreferences(mContext);
        String secretKey = preferences.getString(SP_SECRET_KEY, "");
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

    private boolean isSecretKeyNull(SecreteKey secreteKey) {
        return secreteKey == null || TextUtils.isEmpty(secreteKey.key) || secreteKey.version == KEY_VERSION_DEFAULT;
    }

    SAEncryptListener getEncryptListener(SecreteKey secreteKey) {
        for (SAEncryptListener listener : mListeners) {
            if (listener != null && isMatchEncryptType(listener, secreteKey)) {
                return listener;
            }
        }
        return null;
    }
}