/*
 * Created by chenru on 2019/12/31.
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

package com.sensorsdata.analytics.android.sdk.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.sensorsdata.analytics.android.sdk.util.SADataHelper.assertKey;
import static com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils.getSharedPreferences;

public class ChannelUtils {
    private static final String UTM_SOURCE_KEY = "SENSORS_ANALYTICS_UTM_SOURCE";
    private static final String UTM_MEDIUM_KEY = "SENSORS_ANALYTICS_UTM_MEDIUM";
    private static final String UTM_TERM_KEY = "SENSORS_ANALYTICS_UTM_TERM";
    private static final String UTM_CONTENT_KEY = "SENSORS_ANALYTICS_UTM_CONTENT";
    private static final String UTM_CAMPAIGN_KEY = "SENSORS_ANALYTICS_UTM_CAMPAIGN";
    private static final String SHARED_PREF_UTM_FILE = "sensorsdata.utm";
    private static final String SHARED_PREF_CORRECT_TRACK_INSTALLATION = "sensorsdata.correct.track.installation";

    private static HashSet<String> sChannelSourceKeySet = new HashSet<>();
    private static final HashMap<String, String> UTM_MAP = new HashMap<String, String>() {{
        put(UTM_SOURCE_KEY, "$utm_source");
        put(UTM_MEDIUM_KEY, "$utm_medium");
        put(UTM_TERM_KEY, "$utm_term");
        put(UTM_CONTENT_KEY, "$utm_content");
        put(UTM_CAMPAIGN_KEY, "$utm_campaign");
    }};

    private static final HashMap<String, String> UTM_LINK_MAP = new HashMap<String, String>() {{
        put(UTM_SOURCE_KEY, "utm_source");
        put(UTM_MEDIUM_KEY, "utm_medium");
        put(UTM_TERM_KEY, "utm_term");
        put(UTM_CONTENT_KEY, "utm_content");
        put(UTM_CAMPAIGN_KEY, "utm_campaign");
    }};

    private static final Map<String, String> LATEST_UTM_MAP = new HashMap<String, String>() {{
        put(UTM_SOURCE_KEY, "$latest_utm_source");
        put(UTM_MEDIUM_KEY, "$latest_utm_medium");
        put(UTM_TERM_KEY, "$latest_utm_term");
        put(UTM_CONTENT_KEY, "$latest_utm_content");
        put(UTM_CAMPAIGN_KEY, "$latest_utm_campaign");
    }};

    private static Map<String, String> sUtmProperties = new HashMap<>();

    private static Map<String, String> sLatestUtmProperties = new HashMap<>();

    public static JSONObject getUtmProperties() {
        if (sUtmProperties.size() > 0) {
            return new JSONObject(sUtmProperties);
        }
        return new JSONObject();
    }

    public static JSONObject getLatestUtmProperties() {
        if (sLatestUtmProperties.size() > 0) {
            return new JSONObject(sLatestUtmProperties);
        }
        return new JSONObject();
    }

    public static void mergeUtmToEndData(JSONObject source, JSONObject dest) {
        try {
            if (source == null || dest == null) {
                return;
            }
            String latestKey;
            Iterator<String> keys = source.keys();
            while (keys.hasNext()) {
                latestKey = keys.next();
                if (latestKey.startsWith("$latest") || latestKey.startsWith("_latest")) {
                    dest.put(latestKey, source.getString(latestKey));
                }
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 判断是否包含 Utm 属性，trackInstallation 和 trackChannelEvent 中用到.
     *
     * @param properties 属性
     * @return true 包含；false 不包含
     */
    public static boolean hasUtmProperties(JSONObject properties) {
        if (properties == null) {
            return false;
        }
        for (Map.Entry<String, String> entry : UTM_MAP.entrySet()) {
            if (entry != null) {
                if (properties.has(entry.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断是否包含 Utm 属性
     *
     * @param parameterNames uri 中的参数名
     * @return true 包含；false 不包含
     */
    public static boolean hasLinkUtmProperties(Set<String> parameterNames) {
        if (parameterNames == null || parameterNames.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, String> entry : UTM_LINK_MAP.entrySet()) {
            if (entry != null) {
                if (parameterNames.contains(entry.getValue())) {
                    return true;
                }
            }
        }
        for (String key : sChannelSourceKeySet) {
            if (!TextUtils.isEmpty(key)) {
                if (sChannelSourceKeySet.contains(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取渠道追踪设置信息
     *
     * @param mContext Context
     * @param androidId androidId
     * @param oaid OAID
     * @return 拼接的渠道追踪设置信息
     */
    public static String getDeviceInfo(Context mContext, String androidId, String oaid) {
        return String.format("android_id=%s##imei=%s##imei_old=%s##imei_slot1=%s##imei_slot2=%s##imei_meid=%s##mac=%s##oaid=%s",
                androidId,
                SensorsDataUtils.getIMEI(mContext),
                SensorsDataUtils.getIMEIOld(mContext),
                SensorsDataUtils.getSlot(mContext, 0),
                SensorsDataUtils.getSlot(mContext, 1),
                SensorsDataUtils.getMEID(mContext),
                SensorsDataUtils.getMacAddress(mContext),
                oaid);
    }

    public static void mergeUtmByMetaData(Context context, JSONObject properties) throws JSONException {
        if (properties == null) {
            return;
        }
        for (Map.Entry<String, String> entry : UTM_MAP.entrySet()) {
            if (entry != null) {
                String utmValue = getApplicationMetaData(context, entry.getKey());
                if (!TextUtils.isEmpty(utmValue)) {
                    properties.put(entry.getValue(), utmValue);
                }
            }
        }
    }

    public static void setSourceChannelKeys(String... sourceChannelKeys) {
        sChannelSourceKeySet.clear();
        if (sourceChannelKeys != null && sourceChannelKeys.length > 0) {
            for (String key : sourceChannelKeys) {
                if (!TextUtils.isEmpty(key)) {
                    sChannelSourceKeySet.add(key);
                }
            }
        }
    }

    public static void parseParams(Map<String, String> params) {
        if (params != null && params.size() > 0) {
            for (Map.Entry<String, String> entry : UTM_LINK_MAP.entrySet()) {
                String utmKey = entry.getValue();
                String value = params.get(utmKey);
                if (!TextUtils.isEmpty(value)) {
                    sUtmProperties.put(UTM_MAP.get(entry.getKey()), value);
                    sLatestUtmProperties.put(LATEST_UTM_MAP.get(entry.getKey()), value);
                }
            }
            for (String sourceKey : sChannelSourceKeySet) {
                try {
                    //检测 key 的值,非正常 key 值直接跳过.
                    assertKey(sourceKey);
                    String value = params.get(sourceKey);
                    if (!TextUtils.isEmpty(value)) {
                        sUtmProperties.put(sourceKey, value);
                        sLatestUtmProperties.put("_latest_" + sourceKey, value);
                    }
                } catch (InvalidDataException e) {
                    SALog.printStackTrace(e);
                }
            }
        }
    }

    /**
     * 初始化 utm 数据,从本地文件
     *
     * @param context Context
     */
    public static void loadUtmByLocal(Context context) {
        try {
            SharedPreferences utmPref = getSharedPreferences(context);
            sLatestUtmProperties.clear();
            String channelJson = utmPref.getString(SHARED_PREF_UTM_FILE, "");
            if (!TextUtils.isEmpty(channelJson)) {
                JSONObject jsonObject = new JSONObject(channelJson);
                for (Map.Entry<String, String> entry : LATEST_UTM_MAP.entrySet()) {
                    String utmKey = entry.getValue();
                    if (jsonObject.has(utmKey)) {
                        sLatestUtmProperties.put(utmKey, jsonObject.optString(utmKey));
                    }
                }
                String latestSourceKey;
                for (String sourceKey : sChannelSourceKeySet) {
                    latestSourceKey = "_latest_" + sourceKey;
                    if (jsonObject.has(latestSourceKey)) {
                        sLatestUtmProperties.put(latestSourceKey, jsonObject.optString(latestSourceKey));
                    }
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 清除本地保存的 utm 属性
     *
     * @param context Context
     */
    public static void clearLocalUtm(Context context) {
        try {
            SharedPreferences utmPref = getSharedPreferences(context);
            utmPref.edit().putString(SHARED_PREF_UTM_FILE, "").apply();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 清除内存中的 utm 属性
     */
    public static void clearMemoryUtm() {
        sUtmProperties.clear();
        sLatestUtmProperties.clear();
    }

    /**
     * 清除本地保存和内存中的 utm 属性
     *
     * @param context Context
     */
    public static void clearUtm(Context context) {
        clearMemoryUtm();
        clearLocalUtm(context);
    }

    /**
     * 需要清除 utm 属性的 JSONObject
     *
     * @param jsonObject 事件属性
     */
    public static void removeDeepLinkInfo(JSONObject jsonObject) {
        try {
            if (jsonObject == null) {
                return;
            }
            String latestKey;
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                latestKey = keys.next();
                if (latestKey.startsWith("$latest") || latestKey.startsWith("_latest")) {
                    keys.remove();
                }
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 保存的 utm 属性到本地
     *
     * @param context Context
     */
    public static void saveDeepLinkInfo(Context context) {
        try {
            if (sLatestUtmProperties.size() > 0) {
                SharedPreferences utmPref = getSharedPreferences(context);
                utmPref.edit().putString(SHARED_PREF_UTM_FILE, sLatestUtmProperties.toString()).apply();
            } else {
                clearLocalUtm(context);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private static String getApplicationMetaData(Context mContext, String metaKey) {
        try {
            ApplicationInfo appInfo = mContext.getApplicationContext().getPackageManager()
                    .getApplicationInfo(mContext.getApplicationContext().getPackageName(),
                            PackageManager.GET_META_DATA);
            String value = appInfo.metaData.getString(metaKey);
            int iValue = -1;
            if (value == null) {
                iValue = appInfo.metaData.getInt(metaKey, -1);
            }
            if (iValue != -1) {
                value = String.valueOf(iValue);
            }
            return value;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 判断是否需要添加渠道回调事件，如果需要则添加。
     *
     * @param isAutoAddChannelCallbackEvent 是否开启
     * @param eventName 事件名
     * @param properties 属性
     * @param context Context
     * @return JSONObject
     */
    public static JSONObject checkOrSetChannelCallbackEvent(boolean isAutoAddChannelCallbackEvent, String eventName, JSONObject properties, Context context) {
        if (isAutoAddChannelCallbackEvent) {
            if (properties == null) {
                properties = new JSONObject();
            }
            try {
                boolean isFirst = isFirstChannelEvent(eventName);
                properties.put("$is_channel_callback_event", isFirst);
                if (context != null && !ChannelUtils.hasUtmProperties(properties)) {
                    ChannelUtils.mergeUtmByMetaData(context, properties);
                }
                properties.put("$channel_device_info", "1");
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
        }
        return properties;
    }

    /**
     * 是否是首次触发的渠道事件
     *
     * @param eventName 事件名称
     * @return 是否是首次触发
     */
    public static boolean isFirstChannelEvent(String eventName) {
        boolean isFirst = DbAdapter.getInstance().isFirstChannelEvent(eventName);
        if (isFirst) {
            DbAdapter.getInstance().addChannelEvent(eventName);
        }
        return isFirst;
    }

    /**
     * meta 中是否包含 Utm 属性
     *
     * @param context Context
     * @return meta 中是否包含 Utm 属性
     */
    public static boolean hasUtmByMetaData(Context context) {
        if (context == null) {
            return false;
        }
        for (Map.Entry<String, String> entry : UTM_MAP.entrySet()) {
            if (entry != null) {
                String utmValue = getApplicationMetaData(context, entry.getKey());
                if (!TextUtils.isEmpty(utmValue)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 是否获取到设备信息
     *
     * @param context Context
     * @param androidId AndroidID
     * @param oaid 移动联通设备标识
     * @return 是否获取到设备信息（macAddress 除外）
     */
    public static boolean isGetDeviceInfo(Context context, String androidId, String oaid) {
        try {
            return !TextUtils.isEmpty(androidId) ||
                    !TextUtils.isEmpty(oaid) ||
                    !TextUtils.isEmpty(SensorsDataUtils.getIMEI(context)) ||
                    !TextUtils.isEmpty(SensorsDataUtils.getIMEIOld(context)) ||
                    !TextUtils.isEmpty(SensorsDataUtils.getSlot(context, 0)) ||
                    !TextUtils.isEmpty(SensorsDataUtils.getSlot(context, 1)) ||
                    !TextUtils.isEmpty(SensorsDataUtils.getMEID(context));
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return false;
    }


    /**
     * 是否触发过 trackInstallation 事件
     *
     * @param context Context
     * @return 是否触发过 trackInstallation 事件
     */
    public static boolean isTrackInstallation(Context context) {
        try {
            SharedPreferences sp = getSharedPreferences(context);
            return sp.contains(SHARED_PREF_CORRECT_TRACK_INSTALLATION);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return false;
    }

    /**
     * 是否正确触发过 trackInstallation 事件
     *
     * @param context Context
     * @return 是否正确触发过 trackInstallation 事件
     */
    public static boolean isCorrectTrackInstallation(Context context) {
        try {
            SharedPreferences sp = getSharedPreferences(context);
            return sp.getBoolean(SHARED_PREF_CORRECT_TRACK_INSTALLATION, false);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return false;
    }

    /**
     * 保存 trackInstallation 事件是否正确触发标识
     *
     * @param context Context
     * @param isCorrectTrackInstallation trackInstallation 事件是否正确触发标识
     */
    public static void saveCorrectTrackInstallation(Context context, boolean isCorrectTrackInstallation) {
        try {
            SharedPreferences sp = getSharedPreferences(context);
            sp.edit().putBoolean(SHARED_PREF_CORRECT_TRACK_INSTALLATION, isCorrectTrackInstallation).apply();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 设备信息是否匹配
     *
     * @param context Context
     * @param deviceInfo 设备信息字符串
     * @return 是否匹配
     */
    public static boolean checkDeviceInfo(Context context, String deviceInfo) {
        if (context == null || TextUtils.isEmpty(deviceInfo)) {
            return false;
        }
        String[] codes = deviceInfo.split("##");
        Map<String, String> deviceMaps = new HashMap<>();
        if (codes.length == 0) {
            return false;
        }
        for (String code : codes) {
            String[] keyValue = code.trim().split("=");
            if (keyValue.length != 2) {
                continue;
            }
            deviceMaps.put(keyValue[0], keyValue[1]);
        }
        if (deviceMaps.isEmpty()) {
            return false;
        }
        return (deviceMaps.containsKey("oaid")//防止都为 null 返回 true
                        && TextUtils.equals(deviceMaps.get("oaid"), OaidHelper.getOAID(context))) ||
                       (deviceMaps.containsKey("imei") &&
                                TextUtils.equals(deviceMaps.get("imei"), SensorsDataUtils.getIMEI(context))) ||
                       (deviceMaps.containsKey("imei_old") &&
                                TextUtils.equals(deviceMaps.get("imei_old"), SensorsDataUtils.getIMEIOld(context))) ||
                       (deviceMaps.containsKey("imei_slot1") &&
                                TextUtils.equals(deviceMaps.get("imei_slot1"), SensorsDataUtils.getSlot(context, 0))) ||
                       (deviceMaps.containsKey("imei_slot2") &&
                                TextUtils.equals(deviceMaps.get("imei_slot2"), SensorsDataUtils.getSlot(context, 1))) ||
                       (deviceMaps.containsKey("imei_meid") &&
                                TextUtils.equals(deviceMaps.get("imei_meid"), SensorsDataUtils.getMEID(context))) ||
                       (deviceMaps.containsKey("android_id") &&
                                TextUtils.equals(deviceMaps.get("android_id"), SensorsDataUtils.getAndroidID(context))) ||
                       (deviceMaps.containsKey("mac") &&
                                TextUtils.equals(deviceMaps.get("mac"), SensorsDataUtils.getMacAddress(context)));
    }
}
