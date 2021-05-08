/*
 * Created by chenru on 2021/03/19.
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

public interface SAEncryptListener {
    /**
     * 返回指定的对称加密类型
     *
     * @return 当前对称加密类型，例如 “AES”
     */
    String symmetricEncryptType();

    /**
     * 通过对称加密对事件信息进行加密
     *
     * @param event gzip 后的埋点信息
     * @return 对称加密后的埋点信息
     */
    String encryptEvent(byte[] event);

    /**
     * 返回指定的非对称加密类型
     *
     * @return 当前非对称加密类型，例如 “RSA”
     */
    String asymmetricEncryptType();

    /**
     * 通过非对称加密算法加密对称加密算法密钥
     *
     * @param publicKey 公钥
     * @return 加密后的对称密钥
     */
    String encryptSymmetricKeyWithPublicKey(String publicKey);
}
