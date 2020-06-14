/*
 * Created by chenru on 2019/12/31.
 * Copyright 2015－2020 Sensors Data Inc.
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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.data.DbAdapter;
import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;

import org.json.JSONArray;
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
    private static final String SHARED_PREF_CHANNEL_EVENT = "sensorsdata.channel.event";
    private static final String IS_FIRST_DEEPLINK_ACTIVITY_KEY = "is_first_deeplink_activity";

    private static HashSet<String> sChannelSourceKeySet = new HashSet<>();
    private static Set<String> channelEvents = new HashSet<>();
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
     * @param maps uri 中的 query 信息
     * @return true 包含；false 不包含
     */
    private static boolean hasLinkUtmProperties(Map<String, String> maps) {
        if (maps == null || maps.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, String> entry : UTM_LINK_MAP.entrySet()) {
            if (entry != null) {
                if (null != maps.get(entry.getValue())) {
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
     * @return 拼接的渠道追踪设置信息
     */
    public static String getDeviceInfo(Context mContext, String androidId) {
        return String.format("android_id=%s##imei=%s##imei_old=%s##imei_slot1=%s##imei_slot2=%s##imei_meid=%s##mac=%s##oaid=%s",
                androidId,
                SensorsDataUtils.getIMEI(mContext),
                SensorsDataUtils.getIMEIOld(mContext),
                SensorsDataUtils.getSlot(mContext, 0),
                SensorsDataUtils.getSlot(mContext, 1),
                SensorsDataUtils.getMEID(mContext),
                SensorsDataUtils.getMacAddress(mContext),
                SADeviceUtils.getOAID(mContext));
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

    /**
     * 解析 Uri 并返回是否包含 utm 属性
     *
     * @param activity 浏览页面
     * @param isStart 是否 ActivityStarted
     * @param isSaveDeepLinkInfo 是否保存本地
     * @return 是否包含 utm 属性
     */
    @TargetApi(11)
    public static boolean parseUtmFromActivity(Activity activity, boolean isStart, boolean isSaveDeepLinkInfo) {
        boolean hasUtmProperties = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            return false;
        }
        Intent intent = activity.getIntent();
        if (intent != null
                && Intent.ACTION_VIEW.equals(intent.getAction())
                && intent.getBooleanExtra(IS_FIRST_DEEPLINK_ACTIVITY_KEY, true)) {
            Uri uri = intent.getData();
            if (uri == null) {
                return false;
            }
            Set<String> parameterNames = uri.getQueryParameterNames();
            if (parameterNames != null && parameterNames.size() > 0) {
                Map<String, String> uriParams = new HashMap<>();
                for (String name : parameterNames) {
                    String value = uri.getQueryParameter(name);
                    uriParams.put(name, TextUtils.isEmpty(value) ? "" : value);
                }
                hasUtmProperties = hasLinkUtmProperties(uriParams);
                if (hasUtmProperties) {
                    sUtmProperties.clear();
                    sLatestUtmProperties.clear();
                    for (Map.Entry<String, String> entry : UTM_LINK_MAP.entrySet()) {
                        String utmKey = entry.getValue();
                        String value = uriParams.get(utmKey);
                        if (!TextUtils.isEmpty(value)) {
                            sUtmProperties.put(UTM_MAP.get(entry.getKey()), value);
                            sLatestUtmProperties.put(LATEST_UTM_MAP.get(entry.getKey()), value);
                        }
                    }
                    for (String sourceKey : sChannelSourceKeySet) {
                        try {
                            //检测 key 的值,非正常 key 值直接跳过.
                            assertKey(sourceKey);
                            String value = uri.getQueryParameter(sourceKey);
                            if (!TextUtils.isEmpty(value)) {
                                sUtmProperties.put(sourceKey, value);
                                sLatestUtmProperties.put("_latest_" + sourceKey, value);
                            }
                        } catch (InvalidDataException e) {
                            SALog.printStackTrace(e);
                        }
                    }
                    intent.putExtra(IS_FIRST_DEEPLINK_ACTIVITY_KEY, isStart);
                    if (isSaveDeepLinkInfo) {
                        saveDeepLinkInfo(activity.getApplicationContext());
                    }
                }
            }
        }
        return hasUtmProperties;
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
    public static void clearLocalDeepLinkInfo(Context context) {
        try {
            SharedPreferences utmPref = getSharedPreferences(context);
            utmPref.edit().putString(SHARED_PREF_UTM_FILE, "").apply();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
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
    private static void saveDeepLinkInfo(Context context) {
        try {
            if (sLatestUtmProperties.size() > 0) {
                SharedPreferences utmPref = getSharedPreferences(context);
                utmPref.edit().putString(SHARED_PREF_UTM_FILE, sLatestUtmProperties.toString()).apply();
            } else {
                clearLocalDeepLinkInfo(context);
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
                boolean isFirst = DbAdapter.getInstance().isFirstChannelEvent(eventName);
                properties.put("$is_channel_callback_event", isFirst);
                if (isFirst && context != null && !ChannelUtils.hasUtmProperties(properties)) {
                    ChannelUtils.mergeUtmByMetaData(context, properties);
                    DbAdapter.getInstance().addChannelEvent(eventName);
                }
                properties.put("$channel_device_info", "1");
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
        }
        return properties;
    }
}
