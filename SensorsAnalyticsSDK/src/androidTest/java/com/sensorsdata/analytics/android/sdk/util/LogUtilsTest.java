package com.sensorsdata.analytics.android.sdk.util;

/*
 * Created by zxwei on 2019/10/15.
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


import android.util.Log;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class LogUtilsTest {
    private static final String TAG = "LogUtilsTest";

    /**
     * 打印内容中含 '/' 字符
     */
    @Test
    public void logPrintTest() {
        String text = "{\"$url\":\"file:\\/\\/\\/android_asset\\/index.html\",\"$url_path\":\"\\/android_asset\\/index.html\",\"$title\":\"H5打通\"}";
        Log.d(TAG,JSONUtils.formatJson(text));
    }
}

