package com.sensorsdata.analytics.android.sdk.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.webkit.WebSettings;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class SensorsDataUtils {

    public static String operatorToCarrier(String operator) {
        String other = "其他";
        if (TextUtils.isEmpty(operator)) {
            return other;
        }

        if (sCarrierMap.containsKey(operator)) {
            return sCarrierMap.get(operator);
        } else {
            return other;
        }
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        final String sharedPrefsName = SHARED_PREF_EDITS_FILE;
        return context.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
    }

    public static void cleanUserAgent(Context context) {
        try {
            final SharedPreferences preferences = getSharedPreferences(context);
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putString(SHARED_PREF_USER_AGENT_KEY, null);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void mergeJSONObject(final JSONObject source, JSONObject dest)
            throws JSONException {
        Iterator<String> superPropertiesIterator = source.keys();
        while (superPropertiesIterator.hasNext()) {
            String key = superPropertiesIterator.next();
            Object value = source.get(key);
            if (value instanceof Date) {
                synchronized (mDateFormat) {
                    dest.put(key, mDateFormat.format((Date) value));
                }
            } else {
                dest.put(key, value);
            }
        }
    }

    /**
     * 获取 UA 值
     * @param context Context
     * @return 当前 UA 值
     */
    public static String getUserAgent(Context context) {
        try {
            final SharedPreferences preferences = getSharedPreferences(context);
            String userAgent = preferences.getString(SHARED_PREF_USER_AGENT_KEY, null);
            if (TextUtils.isEmpty(userAgent)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    try {
                        Class webSettingsClass = Class.forName("android.webkit.WebSettings");
                        Method getDefaultUserAgentMethod = webSettingsClass.getMethod("getDefaultUserAgent");
                        if (getDefaultUserAgentMethod != null) {
                            userAgent = WebSettings.getDefaultUserAgent(context);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        final Class<?> webSettingsClassicClass = Class.forName("android.webkit.WebSettingsClassic");
                        final Constructor<?> constructor = webSettingsClassicClass.getDeclaredConstructor(Context.class, Class.forName("android.webkit.WebViewClassic"));
                        constructor.setAccessible(true);
                        final Method method = webSettingsClassicClass.getMethod("getUserAgentString");
                        userAgent = (String) method.invoke(constructor.newInstance(context, null));
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            if (TextUtils.isEmpty(userAgent)) {
                userAgent = System.getProperty("http.agent");
            }

            if (!TextUtils.isEmpty(userAgent)) {
                final SharedPreferences.Editor editor = preferences.edit();
                editor.putString(SHARED_PREF_USER_AGENT_KEY, userAgent);
                editor.apply();
            }

            return userAgent;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getDeviceID(Context context) {
        final SharedPreferences preferences = getSharedPreferences(context);
        String storedDeviceID = preferences.getString(SHARED_PREF_DEVICE_ID_KEY, null);

        if (storedDeviceID == null) {
            storedDeviceID = UUID.randomUUID().toString();
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putString(SHARED_PREF_DEVICE_ID_KEY, storedDeviceID);
            editor.apply();
        }

        return storedDeviceID;
    }

    public static boolean isInEmulator() {
        if (!Build.HARDWARE.equals("goldfish")) {
            return false;
        }

        if (!Build.BRAND.startsWith("generic")) {
            return false;
        }

        if (!Build.DEVICE.startsWith("generic")) {
            return false;
        }

        if (!Build.PRODUCT.contains("sdk")) {
            return false;
        }

        if (!Build.MODEL.toLowerCase(Locale.US).contains("sdk")) {
            return false;
        }

        return true;
    }

    public static String networkType(Context context) {
        // Wifi
        ConnectivityManager manager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager != null) {
            NetworkInfo networkInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
                return "WIFI";
            }
        }

        // Mobile network
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context
                .TELEPHONY_SERVICE);

        int networkType = telephonyManager.getNetworkType();
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
        }

        // disconnected to the internet
        return "NULL";
    }

    public static boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取IMEI
     * @param mContext Context
     * @return IMEI
     */
    public static String getIMEI(Context mContext) {
        String imei = "";
        try {
            if (mContext.checkCallingOrSelfPermission("android.permission.READ_PHONE_STATE") != PackageManager.PERMISSION_GRANTED) {
                return imei;
            }
            TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                imei = tm.getDeviceId();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imei;
    }

    /**
     * 获取 Android ID
     * @param mContext Context
     * @return androidID
     */
    public static String getAndroidID(Context mContext) {
        String androidID = "";
        try {
            androidID = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return androidID;
    }

    public static String getApplicationMetaData(Context mContext, String metaKey) {
        try {
            ApplicationInfo appInfo = mContext.getApplicationContext().getPackageManager()
                    .getApplicationInfo(mContext.getApplicationContext().getPackageName(),
                            PackageManager.GET_META_DATA);
            return appInfo.metaData.getString(metaKey);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 获取手机的MAC地址
     * @return mac address
     */
    public static String getMacAddress() {
        String str = "";
        String macSerial = "";
        try {
            Process pp = Runtime.getRuntime().exec(
                    "cat /sys/class/net/wlan0/address ");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);

            for (; null != str; ) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim();// 去空格
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if ("".equals(macSerial)) {
            try {
                return loadFileAsString("/sys/class/net/eth0/address")
                        .toUpperCase().substring(0, 17);
            } catch (Exception e) {
                e.printStackTrace();

            }

        }
        return macSerial;
    }

    private static String loadFileAsString(String fileName) throws Exception {
        try {
            FileReader reader = new FileReader(fileName);
            String text = loadReaderAsString(reader);
            reader.close();
            return text;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String loadReaderAsString(Reader reader) throws Exception {
        try {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[4096];
            int readLength = reader.read(buffer);
            while (readLength >= 0) {
                builder.append(buffer, 0, readLength);
                readLength = reader.read(buffer);
            }
            return builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static boolean hasUtmProperties(JSONObject properties) {
        if (properties == null) {
            return false;
        }

        return properties.has("$utm_source") ||
                properties.has("$utm_medium") ||
                properties.has("$utm_term") ||
                properties.has("$utm_content") ||
                properties.has("$utm_campaign");
    }

    private static final String SHARED_PREF_EDITS_FILE = "sensorsdata";
    private static final String SHARED_PREF_DEVICE_ID_KEY = "sensorsdata.device.id";
    private static final String SHARED_PREF_USER_AGENT_KEY = "sensorsdata.user.agent";

    private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"
            + ".SSS", Locale.CHINA);
    private static final Map<String, String> sCarrierMap = new HashMap<String, String>() {
        {
            //中国移动
            put("46000", "中国移动");
            put("46002", "中国移动");
            put("46007", "中国移动");
            put("46008", "中国移动");

            //中国联通
            put("46001", "中国联通");
            put("46006", "中国联通");
            put("46009", "中国联通");

            //中国电信
            put("46003", "中国电信");
            put("46005", "中国电信");
            put("46011", "中国电信");
        }
    };

    private static final String LOGTAG = "SA.SensorsDataUtils";
}
