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

public class SymmetricEncryptModeTest {

    @Test
    public void values() {
        SymmetricEncryptMode[] modes = SymmetricEncryptMode.values();
        Assert.assertEquals(modes[0], SymmetricEncryptMode.AES);
        Assert.assertEquals(modes[1], SymmetricEncryptMode.SM4);
    }

    @Test
    public void valueOf() {
        Assert.assertEquals(SymmetricEncryptMode.valueOf("AES"), SymmetricEncryptMode.AES);
        Assert.assertEquals(SymmetricEncryptMode.valueOf("SM4"), SymmetricEncryptMode.SM4);
    }
}