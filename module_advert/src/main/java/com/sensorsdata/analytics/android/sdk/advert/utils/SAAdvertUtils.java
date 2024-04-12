/*
 * Created by chenru on 2022/4/25 下午5:05(format year/.
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

package com.sensorsdata.analytics.android.sdk.advert.utils;

import static com.sensorsdata.analytics.android.sdk.util.Base64Coder.CHARSET_UTF8;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.exceptions.ConnectErrorException;
import com.sensorsdata.analytics.android.sdk.exceptions.ResponseErrorException;
import com.sensorsdata.analytics.android.sdk.plugin.encrypt.SAStoreManager;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SAAdvertUtils {

    private static final String TAG = "SA.SAAdvert";

    /**
     * 获取是否激活标记位
     *
     * @param disableCallback 是否回传事件 callback
     * @return 是否已触发过激活事件
     */
    public static boolean isFirstTrackInstallation(boolean disableCallback) {
        if (disableCallback) {
            return PersistentLoader.getInstance().getFirstInstallationWithCallbackPst().get();
        }
        return PersistentLoader.getInstance().getFirstInstallationPst().get();
    }

    /**
     * 设置激活标记位
     *
     * @param disableCallback 是否回传事件 callback
     */
    public static void setTrackInstallation(boolean disableCallback) {
        if (disableCallback) {
            PersistentLoader.getInstance().getFirstInstallationWithCallbackPst().commit(false);
        }
        PersistentLoader.getInstance().getFirstInstallationPst().commit(false);
    }

    /**
     * 获取 AndroidID
     *
     * @return AndroidID
     */
    public static String getIdentifier(Context context) {
        return SensorsDataUtils.getIdentifier(context);
    }

    /**
     * 获取 IMEI
     *
     * @return IMEI 拼接数据
     */
    public static String getInstallSource(Context context) {
        return String.format("imei=%s##imei_old=%s##imei_slot1=%s##imei_slot2=%s##imei_meid=%s",
                "", "", "", "", "");
    }


    public static void sendData(Context context, String path, JSONObject sendData, String rawMessage) {
        if (!SensorsDataAPI.sharedInstance().isNetworkRequestEnable()) {
            SALog.i(TAG, "NetworkRequest is disabled");
            return;
        }
        //无网络
        if (!NetworkUtils.isNetworkAvailable(context)) {
            return;
        }
        if (sendData != null && sendData.length() > 0) {
            String gzip = DbParams.GZIP_DATA_EVENT;
            try {
                JSONArray jsonArray = new JSONArray();
                jsonArray.put(sendData);
                String data = jsonArray.toString();
                if (sendData.has("ekey")) {
                    gzip = DbParams.GZIP_DATA_ENCRYPT;
                } else {
                    data = SADataHelper.gzipData(data);
                }
                sendData.put("_flush_time", System.currentTimeMillis());
                sendHttpRequest(path, String.valueOf(data.hashCode()), gzip, data, rawMessage);
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
        }
    }

    // 是否触发过激活
    public static boolean isInstallationTracked() {
        return SAStoreManager.getInstance().isExists(DbParams.PersistentName.FIRST_INSTALL) || SAStoreManager.getInstance().isExists(DbParams.PersistentName.FIRST_INSTALL_CALLBACK);
    }

    private synchronized static void sendHttpRequest(String path, String crc, String gzip, String data, String rawMessage) throws ConnectErrorException, ResponseErrorException {
        HttpURLConnection connection = null;
        InputStream in = null;
        OutputStream out = null;
        BufferedOutputStream bout = null;
        try {
            final URL url = new URL(path);
            connection = (HttpURLConnection) url.openConnection();
            if (connection == null) {
                SALog.i(TAG, String.format("can not connect %s, it shouldn't happen", url));
                return;
            }

            connection.setInstanceFollowRedirects(false);

            Uri.Builder builder = new Uri.Builder();
            //添加 crc
            if (!TextUtils.isEmpty(crc)) {
                builder.appendQueryParameter("crc", crc);
            }

            builder.appendQueryParameter("gzip", gzip);
            builder.appendQueryParameter("data_list", data);
            builder.appendQueryParameter("sink_name", "mirror");

            String query = builder.build().getEncodedQuery();
            if (TextUtils.isEmpty(query)) {
                return;
            }

            connection.setFixedLengthStreamingMode(query.getBytes(CHARSET_UTF8).length);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            //设置连接超时时间
            connection.setConnectTimeout(30 * 1000);
            //设置读取超时时间
            connection.setReadTimeout(30 * 1000);
            out = connection.getOutputStream();
            bout = new BufferedOutputStream(out);
            bout.write(query.getBytes(CHARSET_UTF8));
            bout.flush();

            int responseCode = connection.getResponseCode();
            SALog.i(TAG, "responseCode: " + responseCode);
            try {
                in = connection.getInputStream();
            } catch (FileNotFoundException e) {
                in = connection.getErrorStream();
            }
            byte[] responseBody = SADataHelper.slurp(in);
            in.close();
            in = null;

            String response = new String(responseBody, CHARSET_UTF8);
            if (SALog.isLogEnabled()) {
                String jsonMessage = JSONUtils.formatJson(rawMessage);
                // 状态码 200 - 300 间都认为正确
                if (responseCode >= HttpURLConnection.HTTP_OK &&
                        responseCode < HttpURLConnection.HTTP_MULT_CHOICE) {
                    SALog.i(TAG, "sat valid message: \n" + jsonMessage);
                } else {
                    SALog.i(TAG, "sat invalid message: \n" + jsonMessage);
                    SALog.i(TAG, String.format(TimeUtils.SDK_LOCALE, "ret_code: %d", responseCode));
                    SALog.i(TAG, String.format(TimeUtils.SDK_LOCALE, "ret_content: %s", response));
                }
            }
        } catch (IOException e) {
            throw new ConnectErrorException(e);
        } finally {
            SADataHelper.closeStream(bout, out, in, connection);
        }
    }
}
