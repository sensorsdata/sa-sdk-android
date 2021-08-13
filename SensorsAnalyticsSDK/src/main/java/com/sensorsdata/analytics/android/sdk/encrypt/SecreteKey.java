/*
 * Created by dengshiwei on 2020/05/19.
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

public class SecreteKey {
    /**
     * 公钥秘钥
     */
    public String key;
    /**
     * 公钥秘钥版本
     */
    public int version;

    /**
     * 对称加密类型
     */
    public String symmetricEncryptType;

    /**
     * 非对称加密类型
     */
    public String asymmetricEncryptType;

    public SecreteKey(String secretKey, int secretVersion, String symmetricEncryptType, String asymmetricEncryptType) {
        this.key = secretKey;
        this.version = secretVersion;
        this.symmetricEncryptType = symmetricEncryptType;
        this.asymmetricEncryptType = asymmetricEncryptType;
    }

    @Override
    public String toString() {
        return "{\"key\":\"" + key +
                "\",\"version\":\"" + version +
                "\",\"symmetricEncryptType\":\"" + symmetricEncryptType +
                "\",\"asymmetricEncryptType\":\"" + asymmetricEncryptType + "\"}";

    }
}