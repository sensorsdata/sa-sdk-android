/*
 * Created by chenru on 2020/06/22.
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

package com.sensorsdata.analytics.android.sdk.network;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.sensorsdata.analytics.android.sdk.util.Base64Coder.CHARSET_UTF8;

class HttpUtils {
    /**
     * HTTP 状态码 307
     */
    private static final int HTTP_307 = 307;

    static boolean needRedirects(int responseCode) {
        return responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HTTP_307;
    }

    static String getLocation(HttpURLConnection connection, String path) throws MalformedURLException {
        if (connection == null || TextUtils.isEmpty(path)) {
            return null;
        }
        String location = connection.getHeaderField("Location");
        if (TextUtils.isEmpty(location)) {
            location = connection.getHeaderField("location");
        }
        if (TextUtils.isEmpty(location)) {
            return null;
        }
        if (!(location.startsWith("http://") || location
                .startsWith("https://"))) {
            //某些时候会省略 host，只返回后面的 path，所以需要补全 url
            URL originUrl = new URL(path);
            location = originUrl.getProtocol() + "://"
                    + originUrl.getHost() + location;
        }
        return location;
    }

    static String getRetString(InputStream is) {
        String buf;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is, CHARSET_UTF8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            is.close();
            buf = sb.toString();
            return buf;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                    reader = null;
                } catch (IOException e) {
                    SALog.printStackTrace(e);
                }
            }
            if (is != null) {
                try {
                    is.close();
                    is = null;
                } catch (IOException e) {
                    SALog.printStackTrace(e);
                }
            }
        }
        return "";
    }
}
