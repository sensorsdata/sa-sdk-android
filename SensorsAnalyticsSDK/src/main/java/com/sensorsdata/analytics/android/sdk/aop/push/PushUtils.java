/*
 * Created by yuejz on 2021/02/19.
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

package com.sensorsdata.analytics.android.sdk.aop.push;

public class PushUtils {

    /**
     * 获取极光推送的手机厂商
     *
     * @param whichPushSDK 极光推送内部字段
     * @return 手机厂商名称
     */
    public static String getJPushSDKName(byte whichPushSDK) {
        String name;
        switch (whichPushSDK) {
            case 1:
                name = "Xiaomi";
                break;
            case 2:
                name = "HUAWEI";
                break;
            case 3:
                name = "Meizu";
                break;
            case 4:
                name = "OPPO";
                break;
            case 5:
                name = "vivo";
                break;
            default:
                name = null;
        }
        return name;
    }
}
