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

package com.sensorsdata.analytics.android.sdk.encrypt.biz;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.encrypt.SAEncryptListener;
import com.sensorsdata.analytics.android.sdk.encrypt.SecreteKey;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

public class SAEventEncryptTools {
    private SecreteKey mSecreteKey;
    private final SecretKeyManager mSecretKeyManager;

    public SAEventEncryptTools(SAContextManager contextManager) {
        this.mSecretKeyManager = SecretKeyManager.getInstance(contextManager);
    }

    /**
     * 针对数据进行加密
     *
     * @param jsonObject，需要加密的数据
     * @return 加密后的数据
     */
    public JSONObject encryptTrackData(JSONObject jsonObject) {
        try {
            if (mSecretKeyManager.isSecretKeyNull(mSecreteKey)) {
                mSecreteKey = mSecretKeyManager.loadSecretKey();
                if (mSecretKeyManager.isSecretKeyNull(mSecreteKey)) {
                    return jsonObject;
                }
            }

            SAEncryptListener mEncryptListener = mSecretKeyManager.getEncryptListener(mSecreteKey);
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
}