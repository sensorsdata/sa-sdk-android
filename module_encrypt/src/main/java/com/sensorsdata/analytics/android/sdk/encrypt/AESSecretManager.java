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

package com.sensorsdata.analytics.android.sdk.encrypt;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.plugin.encrypt.SAStoreManager;
import com.sensorsdata.analytics.android.sdk.util.Base64Coder;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES 加密工具，针对 SP、t_channel 数据表、push File 文件加密
 * SF SDK 会根据此类获取秘钥，谨慎修改
 */
public class AESSecretManager {
    private static final String TAG = "SA.AESSecretManager";
    private static final byte[] ZERO_IV = new byte[16];
    private static final String CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";
    private static final String ALGORITHM = "AES";
    private static final String CHARSET_NAME = "UTF-8";
    private static final String SECRET_KEY_FILE = "com.sensorsdata.analytics.android.sdk.other";
    private String mAESSecret;

    private AESSecretManager() {

    }

    private static class SingletonHolder{
        private static final AESSecretManager INSTANCE = new AESSecretManager();
    }

    public static AESSecretManager getInstance(){
        return SingletonHolder.INSTANCE;
    }

    /**
     *  获取秘钥
     *
     * @param context Context
     */
    public void initSecretKey(Context context){
        SharedPreferences secretSp = context.getSharedPreferences(SECRET_KEY_FILE, Context.MODE_PRIVATE);
        mAESSecret = secretSp.getString(Base64Coder.encodeString(SAStoreManager.SECRET_KEY), "");
        if (TextUtils.isEmpty(mAESSecret)) {
            mAESSecret = generateAESKey();
            secretSp.edit().putString(Base64Coder.encodeString(SAStoreManager.SECRET_KEY), mAESSecret).apply();
        }
    }

    /**
     * 获得一个密钥长度为 128 位的 AES 密钥，
     *
     * @return 返回经 BASE64 处理之后的密钥字符串
     */
    private String generateAESKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(128);
            SecretKey secretKey = keyGen.generateKey();
            return new String(Base64Coder.encode(secretKey.getEncoded()));
        } catch (Exception e) {
            SALog.i(TAG, "generateAESKey fail, msg: " + e);
        }
        return "";
    }

    /**
     * 将使用 Base64 加密后的字符串类型的 secretKey 转为 SecretKey
     *
     * @param strKey 待转换的字符串 secretKey
     * @return SecretKey
     */
    private SecretKey strKey2SecretKey(String strKey) {
        byte[] bytes = Base64Coder.decode(strKey);
        return new SecretKeySpec(bytes, ALGORITHM);
    }

    /**
     * 加密，SF SDK 会调用此方法
     *
     * @param content 待加密内容
     * @return 加密后的密文
     */
    public String encryptAES(String content) {
        try {
            if (content == null || TextUtils.isEmpty(mAESSecret)) {
                return content;
            }
            Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
            IvParameterSpec zeroIv = new IvParameterSpec(ZERO_IV);
            cipher.init(Cipher.ENCRYPT_MODE, strKey2SecretKey(mAESSecret), zeroIv);
            return new String(Base64Coder.encode(cipher.doFinal(content.getBytes(CHARSET_NAME))));
        } catch (Exception e) {
            SALog.i(TAG, "encryptAES fail, msg: " + e);
        }
        return "";
    }

    /**
     * 解密，SF SDK 会调用此类
     *
     * @param content 待解密内容
     * @return 解密后的明文
     */
    public String decryptAES(String content) {
        try {
            if (content == null || TextUtils.isEmpty(mAESSecret)) {
                return content;
            }
            Cipher cipher = Cipher.getInstance(CBC_PKCS5_PADDING);
            IvParameterSpec zeroIv = new IvParameterSpec(ZERO_IV);
            cipher.init(Cipher.DECRYPT_MODE, strKey2SecretKey(mAESSecret), zeroIv);
            return new String(cipher.doFinal(Base64Coder.decode(content)), CHARSET_NAME);
        } catch (Exception e) {
            SALog.i(TAG, "decryptAES fail, msg: " + e);
        }
        return "";
    }
}