/*
 * Created by dengshiwei on 2019/07/25.
 * Copyright 2015－2019 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.util.Base64Coder;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SensorsDataEncrypt {
    /**
     * AES 加密算法
     */
    private static final String KEY_ALGORITHM_AES = "AES";
    private static final String KEY_ALGORITHM_AES_CIPHPER = "AES/CBC/PKCS5Padding";

    /**
     * 算法
     */
    private static final String ALGORITHM = "RSA";

    private static final String KEY_ALGORITHM_RSA_CIPHPER = "RSA/None/PKCS1Padding";

    /**
     * RSA最大加密明文大小
     * 1024 位公钥：117
     * 2048 为公钥：245
     */
    private static final int MAX_ENCRYPT_BLOCK = 245;

    /**
     * AES 秘钥加密
     */
    private static final String EKEY = "ekey";
    /**
     * RSA 公钥名称
     */
    private static final String KEY_VER = "pkv";
    /**
     * 加密后的数据
     */
    private static final String PAYLOAD = "payloads";

    private static byte[] aesKeyValue;
    /**
     * EAS 加密后的值
     */
    private static String mEkey;
    /**
     * RSA 公钥
     */
    private static String mRSAPublicKey;
    /**
     * 针对数据进行加密
     * @param jsonObject，需要加密的数据
     * @return 加密后的数据
     */
    public static JSONObject encryptTrackData(JSONObject jsonObject) {
        try {
            if (TextUtils.isEmpty(mRSAPublicKey)) {
                mRSAPublicKey = SensorsDataAPI.sharedInstance().getRsaPublicKey();
            }

            if (TextUtils.isEmpty(mRSAPublicKey)) {
                return jsonObject;
            }

            if (aesKeyValue == null || aesKeyValue.length == 0) {
                generateAESKey();
            }

            if (TextUtils.isEmpty(mEkey)) {// 如果为空，则尝试再次加密
                mEkey = rsaEncrypt(mRSAPublicKey, aesKeyValue);
                if (TextUtils.isEmpty(mEkey)) {
                    return jsonObject;
                }
            }

            String encryptData = aesEncrypt(aesKeyValue, jsonObject.toString());

            JSONObject dataJson = new JSONObject();
            dataJson.put(EKEY, mEkey);
            dataJson.put(KEY_VER, SensorsDataAPI.sharedInstance().getPkv());
            dataJson.put(PAYLOAD, encryptData);
            return dataJson;
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return jsonObject;
    }

    /**
     * AES 加密
     * @param key AES 加密秘钥
     * @param content 加密内容
     * @return AES 加密后的数据
     */
    private static String aesEncrypt(byte[] key, String content) {
        try {
            Random random = new Random();
            // 随机生成初始化向量
            byte[] ivBytes = new byte[16];
            random.nextBytes(ivBytes);
            byte[] contentBytes = gzipEventData(content);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, KEY_ALGORITHM_AES);
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM_AES_CIPHPER);
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
     * RSA 加密
     * @param rsaPublicKey，公钥秘钥
     * @param content，加密内容
     * @return 加密后的数据
     */
    private static String rsaEncrypt(String rsaPublicKey, byte[] content) {
        if (TextUtils.isEmpty(rsaPublicKey)) {
            return null;
        }
        try {
            byte[] keyBytes = Base64Coder.decode(rsaPublicKey);
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            Key publicKey = keyFactory.generatePublic(x509EncodedKeySpec);
            Cipher cipher = Cipher.getInstance(KEY_ALGORITHM_RSA_CIPHPER);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            int contentLen = content.length;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int offSet = 0;
            byte[] cache;
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
     * @param record 压缩
     * @return 压缩后事件
     */
    private static byte[] gzipEventData(String record){
        try{
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(buffer);
            gzipOutputStream.write(record.getBytes());
            gzipOutputStream.finish();
            return buffer.toByteArray();
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
            return null;
        }
    }

    /**
     * 随机生成 AES 加密秘钥
     */
    private static void generateAESKey(){
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(KEY_ALGORITHM_AES);
            keyGen.init(128);
            SecretKey secretKey = keyGen.generateKey();
            aesKeyValue = secretKey.getEncoded();
            if (TextUtils.isEmpty(mRSAPublicKey)) {
                mRSAPublicKey = SensorsDataAPI.sharedInstance().getRsaPublicKey();
            }
            mEkey = rsaEncrypt(mRSAPublicKey, aesKeyValue);
        } catch (NoSuchAlgorithmException e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 持续化存储 RSA 公钥
     */
    public interface PersistentSecretKey {
        /**
         * 存储公钥
         * @param secreteKey 密钥
         */
        void saveSecretKey(SecreteKey secreteKey);

        /**
         * 获取公钥
         * @return 密钥
         */
        SecreteKey loadSecretKey();
    }

    public static class SecreteKey{
        /**公钥秘钥*/
        public String key;
        /**公钥秘钥版本*/
        public int version;

        public SecreteKey(String secretKey, int secretVersion) {
            this.key = secretKey;
            this.version = secretVersion;
        }
    }
}

