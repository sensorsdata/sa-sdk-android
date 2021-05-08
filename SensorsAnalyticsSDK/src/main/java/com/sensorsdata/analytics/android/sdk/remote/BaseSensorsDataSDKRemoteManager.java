/*
 * Created by yuejianzhong on 2020/11/04.
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

package com.sensorsdata.analytics.android.sdk.remote;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Patterns;

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.encrypt.SecreteKey;
import com.sensorsdata.analytics.android.sdk.encrypt.SensorsDataEncrypt;
import com.sensorsdata.analytics.android.sdk.network.HttpCallback;
import com.sensorsdata.analytics.android.sdk.network.HttpMethod;
import com.sensorsdata.analytics.android.sdk.network.RequestHelper;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public abstract class BaseSensorsDataSDKRemoteManager {

    protected static final String TAG = "SA.SensorsDataSDKRemoteConfigBase";
    protected Context mContext;
    protected SAConfigOptions mSAConfigOptions;
    protected SensorsDataEncrypt mSensorsDataEncrypt;
    protected boolean mDisableDefaultRemoteConfig;

    protected static SensorsDataSDKRemoteConfig mSDKRemoteConfig;
    protected SensorsDataAPI mSensorsDataAPI;

    protected BaseSensorsDataSDKRemoteManager(SensorsDataAPI sensorsDataAPI) {
        this.mSensorsDataAPI = sensorsDataAPI;
        this.mContext = sensorsDataAPI.getContext();
        this.mSAConfigOptions = sensorsDataAPI.getConfigOptions();
        this.mSensorsDataEncrypt = sensorsDataAPI.getSensorsDataEncrypt();
        this.mDisableDefaultRemoteConfig = sensorsDataAPI.isDisableDefaultRemoteConfig();
    }


    public abstract void pullSDKConfigFromServer();

    public abstract void requestRemoteConfig(RandomTimeType randomTimeType, final boolean enableConfigV);

    public abstract void resetPullSDKConfigTimer();

    /**
     * 从本地缓存中读取最新的 SDK 配置信息
     */
    public abstract void applySDKConfigFromCache();

    protected abstract void setSDKRemoteConfig(SensorsDataSDKRemoteConfig sdkRemoteConfig);

    public boolean ignoreEvent(String eventName) {
        if (mSDKRemoteConfig != null && mSDKRemoteConfig.getEventBlacklist() != null) {
            try {
                int size = mSDKRemoteConfig.getEventBlacklist().length();
                for (int i = 0; i < size; i++) {
                    if (eventName.equals(mSDKRemoteConfig.getEventBlacklist().get(i))) {
                        SALog.i(TAG, "remote config: " + eventName + " is ignored by remote config");
                        return true;
                    }
                }
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
        }
        return false;
    }

    /**
     * 将 json 格式的字符串转成 SensorsDataSDKRemoteConfig 对象，并处理默认值
     *
     * @param config String
     * @return SensorsDataSDKRemoteConfig
     */
    protected SensorsDataSDKRemoteConfig toSDKRemoteConfig(String config) {
        SensorsDataSDKRemoteConfig sdkRemoteConfig = new SensorsDataSDKRemoteConfig();
        try {
            if (!TextUtils.isEmpty(config)) {
                JSONObject jsonObject = new JSONObject(config);
                sdkRemoteConfig.setOldVersion(jsonObject.optString("v"));

                String configs = jsonObject.optString("configs");
                SecreteKey secreteKey = new SecreteKey("", -1);
                if (!TextUtils.isEmpty(configs)) {
                    JSONObject configObject = new JSONObject(configs);
                    sdkRemoteConfig.setDisableDebugMode(configObject.optBoolean("disableDebugMode", false));
                    sdkRemoteConfig.setDisableSDK(configObject.optBoolean("disableSDK", false));
                    sdkRemoteConfig.setAutoTrackMode(configObject.optInt("autoTrackMode", -1));
                    sdkRemoteConfig.setEventBlacklist(configObject.optJSONArray("event_blacklist"));
                    sdkRemoteConfig.setNewVersion(configObject.optString("nv", ""));
                    sdkRemoteConfig.setEffectMode(configObject.optInt("effect_mode", 0));
                    JSONObject keyObject = configObject.optJSONObject("key");
                    if (keyObject != null) {
                        if (keyObject.has("key_ec") && SensorsDataEncrypt.isECEncrypt()) {
                            String key_ec = keyObject.optString("key_ec");
                            if (!TextUtils.isEmpty(key_ec)) {
                                keyObject = new JSONObject(key_ec);
                            }
                        }

                        secreteKey.key = keyObject.optString("public_key");

                        if (keyObject.has("type")) {
                            String type = keyObject.optString("type");
                            secreteKey.key = type + ":" + secreteKey.key;
                            secreteKey.asymmetricEncryptType = type;
                        }
                        secreteKey.version = keyObject.optInt("pkv");
                        sdkRemoteConfig.setSecretKey(secreteKey);
                    }
                } else {
                    //默认配置
                    sdkRemoteConfig.setDisableDebugMode(false);
                    sdkRemoteConfig.setDisableSDK(false);
                    sdkRemoteConfig.setAutoTrackMode(-1);
                    sdkRemoteConfig.setSecretKey(secreteKey);
                    sdkRemoteConfig.setEventBlacklist(new JSONArray());
                    sdkRemoteConfig.setNewVersion("");
                    sdkRemoteConfig.setEffectMode(0);
                }
                return sdkRemoteConfig;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return sdkRemoteConfig;
    }

    /**
     * 全埋点类型是否被在线控制忽略
     *
     * @param autoTrackEventType 全埋点类型
     * @return true 表示该类型被忽略，false 表示不被忽略，null 表示使用本地代码配置
     */
    public Boolean isAutoTrackEventTypeIgnored(int autoTrackEventType) {
        if (mSDKRemoteConfig != null) {
            if (mSDKRemoteConfig.getAutoTrackMode() != SensorsDataSDKRemoteConfig.REMOTE_EVENT_TYPE_NO_USE) {
                if (mSDKRemoteConfig.getAutoTrackMode() == 0) {
                    return true;
                }
                return mSDKRemoteConfig.isAutoTrackEventTypeIgnored(autoTrackEventType);
            }
        }
        return null;
    }

    public static boolean isSDKDisabledByRemote() {
        if (mSDKRemoteConfig == null) {
            return false;
        }
        return mSDKRemoteConfig.isDisableSDK();
    }

    /**
     * 全埋点是否被在线控制禁止
     *
     * @return false 表示全部全埋点被禁止，true 表示部分未被禁止，null 表示使用本地代码配置
     */
    public Boolean isAutoTrackEnabled() {
        if (mSDKRemoteConfig != null) {
            if (mSDKRemoteConfig.getAutoTrackMode() == 0) {
                SALog.i(TAG, "remote config: AutoTrackMode is closing by remote config");
                return false;
            } else if (mSDKRemoteConfig.getAutoTrackMode() > 0) {
                return true;
            }
        }
        return null;
    }

    /**
     * 获取远程配置的 Url
     *
     * @param enableConfigV 是否在 Url 中携带 v 和 ve 参数，false 表示不携带
     * @return 远程配置的 Url
     */
    protected String buildRemoteUrl(boolean enableConfigV) {
        String remoteUrl = null;
        boolean configV = enableConfigV;
        String serverUlr = mSensorsDataAPI.getServerUrl();
        String configOptionsRemoteUrl = null;
        if (mSAConfigOptions != null) {
            configOptionsRemoteUrl = mSAConfigOptions.mRemoteConfigUrl;
        }

        if (!TextUtils.isEmpty(configOptionsRemoteUrl)
                && Patterns.WEB_URL.matcher(configOptionsRemoteUrl).matches()) {
            remoteUrl = configOptionsRemoteUrl;
            SALog.i(TAG, "SAConfigOptions remote url is " + remoteUrl);
        } else if (!TextUtils.isEmpty(serverUlr) && Patterns.WEB_URL.matcher(serverUlr).matches()) {
            int pathPrefix = serverUlr.lastIndexOf("/");
            if (pathPrefix != -1) {
                remoteUrl = serverUlr.substring(0, pathPrefix);
                remoteUrl = remoteUrl + "/config/Android.conf";
            }
            SALog.i(TAG, "SensorsDataAPI remote url is " + remoteUrl);
        } else {
            SALog.i(TAG, String.format(Locale.CHINA, "ServerUlr: %s, SAConfigOptions remote url: %s",
                    serverUlr, configOptionsRemoteUrl));
            SALog.i(TAG, "Remote config url verification failed");
            return null;
        }

        //再次检查是否应该在请求中带 v，比如在禁止分散请求的情况下，SDK 升级了或者公钥为空，此时应该不带 v
        if (configV && (SensorsDataUtils.checkVersionIsNew(mContext, mSensorsDataAPI.getSDKVersion()) ||
                (mSensorsDataEncrypt != null && mSensorsDataEncrypt.isPublicSecretKeyNull()))) {
            configV = false;
        }
        Uri configUri = Uri.parse(remoteUrl);
        Uri.Builder builder = configUri.buildUpon();
        if (!TextUtils.isEmpty(remoteUrl) && configV) {
            String oldVersion = null;
            String newVersion = null;
            SensorsDataSDKRemoteConfig SDKRemoteConfig = mSDKRemoteConfig;
            if (SDKRemoteConfig != null) {
                oldVersion = SDKRemoteConfig.getOldVersion();
                newVersion = SDKRemoteConfig.getNewVersion();
                SALog.i(TAG, "The current config: " + SDKRemoteConfig.toString());
            }
            // remoteUrl 中如果存在 v，则不追加参数。nv、app_id、project 都是如此
            if (!TextUtils.isEmpty(oldVersion) && TextUtils.isEmpty(configUri.getQueryParameter("v"))) {
                builder.appendQueryParameter("v", oldVersion);
            }
            if (!TextUtils.isEmpty(newVersion) && TextUtils.isEmpty(configUri.getQueryParameter("nv"))) {
                builder.appendQueryParameter("nv", newVersion);
            }
        }
        if (!TextUtils.isEmpty(serverUlr) && TextUtils.isEmpty(configUri.getQueryParameter("project"))) {
            Uri uri = Uri.parse(serverUlr);
            String project = uri.getQueryParameter("project");
            if (!TextUtils.isEmpty(project)) {
                builder.appendQueryParameter("project", project);
            }
        }
        if (TextUtils.isEmpty(configUri.getQueryParameter("app_id"))) {
            String appId = AppInfoUtils.getProcessName(mContext);
            builder.appendQueryParameter("app_id", appId);
        }
        builder.build();
        remoteUrl = builder.toString();
        SALog.i(TAG, "Android remote config url is " + remoteUrl);
        return remoteUrl;
    }

    /**
     * 子线程中请求网络
     *
     * @param enableConfigV 是否携带版本号
     * @param callback 请求回调接口
     */
    protected void requestRemoteConfig(boolean enableConfigV, HttpCallback.StringCallback callback) {
        try {
            String configUrl = buildRemoteUrl(enableConfigV);
            if (TextUtils.isEmpty(configUrl)) return;
            new RequestHelper.Builder(HttpMethod.GET, configUrl)
                    .callback(callback)
                    .execute();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public enum RandomTimeType {
        RandomTimeTypeWrite, // 创建分散请求时间
        RandomTimeTypeClean, // 移除分散请求时间
        RandomTimeTypeNone    // 不处理分散请求时间
    }
}
