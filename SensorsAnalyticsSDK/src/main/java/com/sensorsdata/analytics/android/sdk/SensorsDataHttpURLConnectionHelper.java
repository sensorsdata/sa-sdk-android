package com.sensorsdata.analytics.android.sdk;

import android.text.TextUtils;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

class SensorsDataHttpURLConnectionHelper {
    /**
     * Numeric status code, 307
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
            //某些时候会省略host，只返回后面的path，所以需要补全url
            URL originUrl = new URL(path);
            location = originUrl.getProtocol() + "://"
                    + originUrl.getHost() + location;
        }
        return location;
    }
}
