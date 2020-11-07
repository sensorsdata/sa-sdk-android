/*
 * Created by dengshiwei on 2020/05/18.
 * Copyright 2015－2020 Sensors Data Inc.
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
import com.sensorsdata.analytics.android.sdk.util.Base64Coder;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SensorsDataEncrypt {
    private static final String SP_SECRET_KEY = "secret_key";
    private static final int KEY_VERSION_DEFAULT = 0;
    private static final String TAG = "SensorsDataEncrypt";
    private byte[] aesKeyValue;
    private SecreteKey mSecreteKey;
    /**
     * RSA 加密 AES 密钥后的值
     */
    private String mEkey;
    private IPersistentSecretKey mPersistentSecretKey;
    private Context mContext;

    static {
        try {
            Class<?> provider = Class.forName("org.spongycastle.jce.provider.BouncyCastleProvider");
            Security.addProvider((Provider) provider.newInstance());
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public SensorsDataEncrypt(Context context, IPersistentSecretKey persistentSecretKey) {
        this.mPersistentSecretKey = persistentSecretKey;
        this.mContext = context;
    }

    /**
     * 检测是否集成 EC 算法
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

            generateAESKey(mSecreteKey);

            if (TextUtils.isEmpty(mEkey)) {
                return jsonObject;
            }

            String encryptData = aesEncrypt(aesKeyValue, jsonObject.toString());
            JSONObject dataJson = new JSONObject();
            dataJson.put("ekey", mEkey);
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
     * @param publicKey 公钥
     * @param version 密钥版本号
     */
    public void saveSecretKey(String publicKey, int version) {
        try {
            SecreteKey secreteKey = new SecreteKey(publicKey, version);
            SALog.i(TAG, "[saveSecretKey] publicKey = " + publicKey + " ,v = " + version);
            if (mPersistentSecretKey != null) {
                mPersistentSecretKey.saveSecretKey(secreteKey);
                // 同时删除本地的密钥
                saveLocalSecretKey("");
            } else {
                saveLocalSecretKey(secreteKey.toString());
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
     * 检查公钥密钥信息是否和本地一致
     *
     * @param version 版本号
     * @param key 密钥信息
     * @return -1 是本地密钥信息为空，-2 是相同，其它是不相同
     */
    public String checkPublicSecretKey(String version, String key) {
        String tip = "";
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
        return tip;
    }

    /**
     * 使用 AES 密钥对埋点数据加密
     *
     * @param key AES 加密秘钥
     * @param content 加密内容
     * @return AES 加密后的数据
     */
    private String aesEncrypt(byte[] key, String content) {
        try {
            Random random = new Random();
            // 随机生成初始化向量
            byte[] ivBytes = new byte[16];
            random.nextBytes(ivBytes);
            byte[] contentBytes = gzipEventData(content);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(ivBytes));

            byte[] encryptedBytes = cipher.doFinal(contentBytes);
            ByteBuffer byteBuffer = ByteBuffer.allocate(ivBytes.length + encryptedBytes.length);
            byteBuffer.put(ivBytes);
            byteBuffer.put(encryptedBytes);
            byte[] cipherMessage = byteBuffer.array();
            return new String(Base64Coder.encode(cipherMessage));
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return null;
    }

    /**
     * 使用服务端公钥对 AES 密钥加密
     *
     * @param publicKey，公钥秘钥
     * @param content，加密内容
     * @return 加密后的数据
     */
    private String publicKeyEncrypt(String publicKey, String type, byte[] content) {
        if (TextUtils.isEmpty(publicKey)) {
            SALog.i(TAG, "PublicKey is null.");
            return null;
        }
        try {
            byte[] keyBytes = Base64Coder.decode(publicKey);
            KeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
            Cipher cipher;
            if ("EC".equals(type)) {
                KeyFactory keyFactory = KeyFactory.getInstance("EC", "SC");
                ECPublicKey ecPublicKey = (ECPublicKey) keyFactory.generatePublic(x509EncodedKeySpec);
                cipher = Cipher.getInstance("ECIES", "SC");
                cipher.init(Cipher.ENCRYPT_MODE, ecPublicKey);
            } else {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                Key rsaPublicKey = keyFactory.generatePublic(x509EncodedKeySpec);
                cipher = Cipher.getInstance("RSA/None/PKCS1Padding");
                cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
            }

            int contentLen = content.length;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int offSet = 0;
            byte[] cache;
            /*
             * RSA 最大加密明文大小：1024 位公钥：117，2048 为公钥：245
             */
            int MAX_ENCRYPT_BLOCK = 245;
            while (contentLen - offSet > 0) {
                if (contentLen - offSet > MAX_ENCRYPT_BLOCK) {
                    cache = cipher.doFinal(content, offSet, MAX_ENCRYPT_BLOCK);
                } else {
                    cache = cipher.doFinal(content, offSet, contentLen - offSet);
                }
                outputStream.write(cache, 0, cache.length);
                offSet += MAX_ENCRYPT_BLOCK;
            }
            byte[] encryptedData = outputStream.toByteArray();
            outputStream.close();
            return new String(Base64Coder.encode(encryptedData));
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return null;
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
     * 随机生成 AES 加密秘钥
     */
    private void generateAESKey(SecreteKey secreteKey) throws NoSuchAlgorithmException {
        if (TextUtils.isEmpty(mEkey) || aesKeyValue == null) {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128);
            SecretKey aesKey = keyGen.generateKey();
            aesKeyValue = aesKey.getEncoded();
            mEkey = publicKeyEncrypt(parsePublicKey(secreteKey.key), parseType(secreteKey.key), aesKeyValue);
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
        SecreteKey rsaPublicKeyVersion = mPersistentSecretKey.loadSecretKey();
        if (rsaPublicKeyVersion != null) {
            publicKey = rsaPublicKeyVersion.key;
            keyVersion = rsaPublicKeyVersion.version;
        }
        SALog.i(TAG, "readAppKey [key = " + publicKey + " ,v = " + keyVersion + "]");
        return new SecreteKey(publicKey, keyVersion);
    }

    /**
     * 从 SDK 端读取密钥
     *
     * @throws JSONException 异常
     */
    private SecreteKey readLocalKey() throws JSONException {
        String publicKey = null;
        int keyVersion = 0;
        final SharedPreferences preferences = SensorsDataUtils.getSharedPreferences(mContext);
        String secretKey = preferences.getString(SP_SECRET_KEY, "");
        if (!TextUtils.isEmpty(secretKey)) {
            JSONObject jsonObject = new JSONObject(secretKey);
            publicKey = jsonObject.optString("key", "");
            keyVersion = jsonObject.optInt("version", KEY_VERSION_DEFAULT);
        }
        SALog.i(TAG, "readLocalKey [key = " + publicKey + " ,v = " + keyVersion + "]");
        return new SecreteKey(publicKey, keyVersion);
    }

    private boolean isSecretKeyNull(SecreteKey secreteKey) {
        return secreteKey == null || TextUtils.isEmpty(secreteKey.key) || secreteKey.version == KEY_VERSION_DEFAULT;
    }

    /**
     * 从公钥中解析出密钥类型
     *
     * @param secretKey 拼接后的密钥
     * @return 公钥类型
     */
    private String parseType(String secretKey) {
        try {
            // 公钥中的拼接格式: type:key，老的 SA 版本中没有 type 字段，只有 EC 加密有
            if (!TextUtils.isEmpty(secretKey)) {
                int index = secretKey.indexOf(":");
                if (index != -1) {
                    return secretKey.substring(0, index);
                }
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return "RSA";
    }

    /**
     * 从拼接字符串中解析出密钥
     *
     * @param secretKey 拼接后的密钥
     * @return 公钥
     */
    private String parsePublicKey(String secretKey) {
        try {
            // 公钥中的拼接格式: type:key，老的 SA 版本中没有 type 字段，只有 EC 加密有
            if (!TextUtils.isEmpty(secretKey)) {
                int index = secretKey.indexOf(":");
                if (index != -1) {
                    return secretKey.substring(index + 1);
                }
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return secretKey;
    }
}