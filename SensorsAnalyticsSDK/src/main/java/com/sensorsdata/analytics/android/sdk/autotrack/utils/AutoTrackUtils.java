/*
 * Created by dengshiwei on 2021/08/15.
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

package com.sensorsdata.analytics.android.sdk.autotrack.utils;

public class AutoTrackUtils {
    private static String sLastScreenUrl = "";

    /**
     * 前向页面地址
     *
     * @return 前向页面地址
     */
    public static String getLastScreenUrl() {
        return sLastScreenUrl;
    }

    /**
     * 前向页面地址
     *
     * @param lastScreenUrl 前向页面地址
     */
    public static void setLastScreenUrl(String lastScreenUrl) {
        sLastScreenUrl = lastScreenUrl;
    }
}
