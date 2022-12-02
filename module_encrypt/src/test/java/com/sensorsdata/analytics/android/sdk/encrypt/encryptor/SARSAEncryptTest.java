/*
 * Created by dengshiwei on 2022/11/22.
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

package com.sensorsdata.analytics.android.sdk.encrypt.encryptor;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

public class SARSAEncryptTest {

    @Test
    public void symmetricEncryptType() {
        SARSAEncrypt rsaEncrypt = new SARSAEncrypt();
        Assert.assertEquals("AES", rsaEncrypt.symmetricEncryptType());
    }

    @Test
    public void asymmetricEncryptType() {
        SARSAEncrypt rsaEncrypt = new SARSAEncrypt();
        Assert.assertEquals("RSA", rsaEncrypt.asymmetricEncryptType());
    }
}