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

import com.sensorsdata.analytics.android.sdk.SALog;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

/**
 * EC+AES 加密
 */
class SAECEncrypt implements SAEncryptListener {
    /**
     * 对称密钥
     */
    byte[] aesKey;

    /**
     * 加密后的对称密钥
     */
    String mEncryptKey;

    static {
        try {
            Class<?> provider = Class.forName("org.spongycastle.jce.provider.BouncyCastleProvider");
            Security.addProvider((Provider) provider.newInstance());
        } catch (Exception e) {
            SALog.i("SA.SAECEncrypt", e.toString());
        }
    }

    @Override
    public String symmetricEncryptType() {
        return "AES";
    }

    @Override
    public String encryptEvent(byte[] event) {
        return EncryptUtils.symmetricEncrypt(aesKey, event, SymmetricEncryptMode.AES);
    }

    @Override
    public String asymmetricEncryptType() {
        return "EC";
    }

    @Override
    public String encryptSymmetricKeyWithPublicKey(String publicKey) {
        if (mEncryptKey == null) {
            try {
                aesKey = EncryptUtils.generateSymmetricKey(SymmetricEncryptMode.AES);
                mEncryptKey = EncryptUtils.encryptAESKey(publicKey, aesKey, "EC");
            } catch (NoSuchAlgorithmException e) {
                SALog.printStackTrace(e);
                return null;
            }
        }
        return mEncryptKey;
    }
}
