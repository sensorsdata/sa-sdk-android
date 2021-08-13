/*
 * Created by chenru on 2021/03/22.
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

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.util.Base64Coder;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 加密工具类
 */
class EncryptUtils {

    private static final String TAG = "SensorsDataEncrypt";

    /**
     * 随机生成 AES/SM4 加密秘钥
     *
     * @return AES/SM4 密钥
     */
    static byte[] generateSymmetricKey(SymmetricEncryptMode mode) throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(mode.algorithm);
        keyGen.init(128);
        SecretKey aesKey = keyGen.generateKey();
        return aesKey.getEncoded();
    }

    static String encryptAESKey(String publicKey, byte[] aesKey, String type) {
        return publicKeyEncrypt(publicKey, type, aesKey);
    }

    /**
     * 使用服务端公钥对 AES 密钥加密
     *
     * @param publicKey，公钥秘钥
     * @param type 加密类型 EC or RSA
     * @param content，加密内容
     * @return 加密后的数据
     */
    private static String publicKeyEncrypt(String publicKey, String type, byte[] content) {
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
     * 使用 AES/SM4 密钥对埋点数据加密
     *
     * @param key AES/SM4 加密秘钥
     * @param contentBytes gzip 后的加密内容
     * @param mode {@link SymmetricEncryptMode} 同步加密类型
     * @return AES/SM4 加密后的数据
     */
    static String symmetricEncrypt(byte[] key, byte[] contentBytes, SymmetricEncryptMode mode) {
        if (key == null || contentBytes == null) {
            return null;
        }
        try {
            SecureRandom random = new SecureRandom();
            // 随机生成初始化向量
            byte[] ivBytes = new byte[16];
            random.nextBytes(ivBytes);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, mode.algorithm);
            Cipher cipher = Cipher.getInstance(mode.transformation);
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
}
