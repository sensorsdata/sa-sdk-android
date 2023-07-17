/*
 * Created by dengshiwei on 2023/06/12.
 * Copyright 2015－2023 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.encrypt.impl;

import com.sensorsdata.analytics.android.sdk.encrypt.SAEncryptListener;

public abstract class AbsSAEncrypt implements SAEncryptListener {

    /**
     * 用于处理传输加密存储的埋点数据
     *
     * @param eventJson 埋点数据
     * @return 处理后的埋点数据
     */
    public String encryptEventRecord(String eventJson) {
        return eventJson;
    }

    /**
     * 用于处理传输加密读取的埋点数据
     *
     * @param encryptEvent 埋点数据
     * @return 处理后的埋点数据
     */
    public String decryptEventRecord(String encryptEvent) {
        return encryptEvent;
    }
}
