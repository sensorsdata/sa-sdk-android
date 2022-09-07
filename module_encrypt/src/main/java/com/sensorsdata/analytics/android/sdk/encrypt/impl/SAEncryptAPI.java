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

package com.sensorsdata.analytics.android.sdk.encrypt.impl;

import android.net.Uri;

import org.json.JSONObject;

public interface SAEncryptAPI {
    /*
     * encrypt content
     */
    String encryptAES(String content);

    /*
     * decrypt content
     */
    String decryptAES(String content);

    /*
     * decrypt track event data
     */
    JSONObject encryptEventData(JSONObject jsonObject);

    /*
     * verify secret key from scan code
     */
    String verifySecretKey(Uri uri);

    /*
     * save secret key from remote config
     */
    void storeSecretKey(String secretKeyJson);

    /*
     * load secret key
     */
    String loadSecretKey();
}
