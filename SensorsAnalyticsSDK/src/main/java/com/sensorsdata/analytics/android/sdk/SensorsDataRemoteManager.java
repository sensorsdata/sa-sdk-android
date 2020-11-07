/*
 * Created by yuejianzhong on 2020/07/22.
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

package com.sensorsdata.analytics.android.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Patterns;

import com.sensorsdata.analytics.android.sdk.data.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentRemoteSDKConfig;
import com.sensorsdata.analytics.android.sdk.encrypt.SecreteKey;
import com.sensorsdata.analytics.android.sdk.encrypt.SensorsDataEncrypt;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;

class SensorsDataRemoteManager {

    private static final String SHARED_PREF_REQUEST_TIME = "sensorsdata.request.time";
    private static final String SHARED_PREF_REQUEST_TIME_RANDOM = "sensorsdata.request.time.random";
    private static final String TAG = "SA.SensorsDataRemoteManager";

    private Context mContext;
    // 每次启动 App 时，最多尝试三次
    private CountDownTimer mPullSDKConfigCountDownTimer;
    private SAConfigOptions mSAConfigOptions;
    private SensorsDataEncrypt mSensorsDataEncrypt;
    private boolean mDisableDefaultRemoteConfig;

    private PersistentRemoteSDKConfig mPersistentRemoteSDKConfig;
    private static SensorsDataSDKRemoteConfig mSDKRemoteConfig;
    private SensorsDataAPI mSensorsDataAPI;
    private SharedPreferences mSharedPreferences;

    SensorsDataRemoteManager(
            Context context,
            SAConfigOptions saConfigOptions,
            SensorsDataEncrypt sensorsDataEncrypt,
            boolean disableDefaultRemoteConfig,
            SensorsDataAPI sensorsDataAPI) {
        this.mContext = context;
        this.mSAConfigOptions = saConfigOptions;
        this.mSensorsDataEncrypt = sensorsDataEncrypt;
        this.mDisableDefaultRemoteConfig = disableDefaultRemoteConfig;
        this.mPersistentRemoteSDKConfig = (PersistentRemoteSDKConfig) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.REMOTE_CONFIG);
        this.mSensorsDataAPI = sensorsDataAPI;
        this.mSharedPreferences = SensorsDataUtils.getSharedPreferences(mContext);
    }

    /**
     * 是否发起随机请求
     *
     * @return false 代表不发，true 代表发送随机请求
     */
    private boolean isRequestValid() {
        boolean isRequestValid = true;
        try {
            long lastRequestTime = mSharedPreferences.getLong(SHARED_PREF_REQUEST_TIME, 0);
            int randomTime = mSharedPreferences.getInt(SHARED_PREF_REQUEST_TIME_RANDOM, 0);
            if (lastRequestTime != 0 && randomTime != 0) {
                float requestInterval = SystemClock.elapsedRealtime() - lastRequestTime;
                // 当前的时间减去上次请求的时间，为间隔时间，当间隔时间小于随机时间，则不请求后端
                if (requestInterval > 0 && requestInterval / 1000 < randomTime * 3600) {
                    isRequestValid = false;
                }
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return isRequestValid;
    }

    boolean ignoreEvent(String eventName) {
        if (mSDKRemoteConfig != null && mSDKRemoteConfig.getEventBlacklist() != null) {
            try {
                int size = mSDKRemoteConfig.getEventBlacklist().length();
                for (int i = 0; i < size; i++) {
                    if (eventName.equals(mSDKRemoteConfig.getEventBlacklist().get(i))) {
                        SALog.i(TAG, eventName + " is ignored by remote config");
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
     * 缓存远程控制随机时间
     */
    private void writeRemoteRequestRandomTime() {
        if (mSAConfigOptions == null) {
            return;
        }
        //默认情况下，随机请求时间为最小时间间隔
        int randomTime = mSAConfigOptions.mMinRequestInterval;
        long currentTime = SystemClock.elapsedRealtime();
        //最大时间间隔大于最小时间间隔时，生成随机时间
        if (mSAConfigOptions.mMaxRequestInterval > mSAConfigOptions.mMinRequestInterval) {
            randomTime += new Random().nextInt(mSAConfigOptions.mMaxRequestInterval - mSAConfigOptions.mMinRequestInterval + 1);
        }
        mSharedPreferences.edit()
                .putLong(SHARED_PREF_REQUEST_TIME, currentTime)
                .putInt(SHARED_PREF_REQUEST_TIME_RANDOM, randomTime)
                .apply();
    }

    /**
     * 清除远程控制随机时间的本地缓存
     */
    private void cleanRemoteRequestRandomTime() {
        mSharedPreferences.edit()
                .putLong(SHARED_PREF_REQUEST_TIME, 0)
                .putInt(SHARED_PREF_REQUEST_TIME_RANDOM, 0)
                .apply();
    }

    /**
     * 将 json 格式的字符串转成 SensorsDataSDKRemoteConfig 对象，并处理默认值
     *
     * @param config String
     * @return SensorsDataSDKRemoteConfig
     */
    private SensorsDataSDKRemoteConfig toSDKRemoteConfig(String config) {
        SensorsDataSDKRemoteConfig sdkRemoteConfig = new SensorsDataSDKRemoteConfig();
        try {
            if (!TextUtils.isEmpty(config)) {
                JSONObject jsonObject = new JSONObject(config);
                sdkRemoteConfig.setOldVersion(jsonObject.optString("v"));

                String configs = jsonObject.optString("configs");
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

                        String publicKey = keyObject.optString("public_key");
                        if (keyObject.has("type")) {
                            publicKey = keyObject.optString("type") + ":" + publicKey;
                        }
                        sdkRemoteConfig.setPublicKey(publicKey);
                        sdkRemoteConfig.setPkv(keyObject.optInt("pkv"));
                    }
                } else {
                    //默认配置
                    sdkRemoteConfig.setDisableDebugMode(false);
                    sdkRemoteConfig.setDisableSDK(false);
                    sdkRemoteConfig.setAutoTrackMode(-1);
                    sdkRemoteConfig.setPublicKey("");
                    sdkRemoteConfig.setPkv(-1);
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

    void pullSDKConfigFromServer() {
        if (mSAConfigOptions == null) {
            return;
        }

        // 关闭随机请求或者分散的最小时间大于最大时间时，清除本地时间，请求后端
        if (mSAConfigOptions.mDisableRandomTimeRequestRemoteConfig ||
                mSAConfigOptions.mMinRequestInterval > mSAConfigOptions.mMaxRequestInterval) {
            requestRemoteConfig(RemoteConfigHandleRandomTimeType.RandomTimeTypeClean, true);
            SALog.i(TAG, "Request remote config because disableRandomTimeRequestRemoteConfig or minHourInterval and maxHourInterval error，Please check the value");
            return;
        }

        //开启加密并且传入秘钥为空的，强制请求后端，此时请求中不带 v
        if (mSensorsDataEncrypt != null && mSensorsDataEncrypt.isPublicSecretKeyNull()) {
            requestRemoteConfig(RemoteConfigHandleRandomTimeType.RandomTimeTypeWrite, false);
            SALog.i(TAG, "Request remote config because encrypt key is null");
            return;
        }

        //满足分散请求逻辑时，请求后端
        if (isRequestValid()) {
            requestRemoteConfig(RemoteConfigHandleRandomTimeType.RandomTimeTypeWrite, true);
            SALog.i(TAG, "Request remote config because satisfy the random request condition");
        }
    }

    void requestRemoteConfig(RemoteConfigHandleRandomTimeType randomTimeType, final boolean enableConfigV) {
        if (mSensorsDataAPI != null && !mSensorsDataAPI.isNetworkRequestEnable()) {
            SALog.i(TAG, "Close network request");
            return;
        }

        if (mDisableDefaultRemoteConfig) {
            SALog.i(TAG, "disableDefaultRemoteConfig is true");
            return;
        }

        switch (randomTimeType) {
            case RandomTimeTypeWrite:
                writeRemoteRequestRandomTime();
                break;
            case RandomTimeTypeClean:
                cleanRemoteRequestRandomTime();
                break;
            default:
                break;
        }

        if (mPullSDKConfigCountDownTimer != null) {
            mPullSDKConfigCountDownTimer.cancel();
            mPullSDKConfigCountDownTimer = null;
        }

        mPullSDKConfigCountDownTimer = new CountDownTimer(90 * 1000, 30 * 1000) {
            @Override
            public void onTick(long l) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //无网络
                        if (!NetworkUtils.isNetworkAvailable(mContext)) {
                            SALog.i(TAG, "Network connection is unavailable");
                            return;
                        }

                        InputStreamReader in = null;
                        HttpURLConnection urlConnection = null;
                        try {
                            URL url;
                            String configUrl = buildRemoteUrl(enableConfigV);
                            if (TextUtils.isEmpty(configUrl)) return;

                            url = new URL(configUrl);
                            urlConnection = (HttpURLConnection) url.openConnection();
                            if (urlConnection == null) {
                                SALog.i(TAG, String.format("can not connect %s, it shouldn't happen", url.toString()), null);
                                return;
                            }
                            if (mSensorsDataAPI != null &&
                                    mSensorsDataAPI.getSSLSocketFactory() != null &&
                                    urlConnection instanceof HttpsURLConnection) {
                                ((HttpsURLConnection) urlConnection).setSSLSocketFactory(mSensorsDataAPI.getSSLSocketFactory());
                            }
                            int responseCode = urlConnection.getResponseCode();
                            SALog.i(TAG, "remote config responseCode = " + responseCode);
                            //配置没有更新
                            if (responseCode == 304 || responseCode == 404) {
                                resetPullSDKConfigTimer();
                                return;
                            }

                            if (responseCode == 200) {
                                resetPullSDKConfigTimer();

                                in = new InputStreamReader(urlConnection.getInputStream());
                                BufferedReader bufferedReader = new BufferedReader(in);
                                StringBuilder result = new StringBuilder();
                                String data;
                                while ((data = bufferedReader.readLine()) != null) {
                                    result.append(data);
                                }
                                data = result.toString();
                                if (!TextUtils.isEmpty(data)) {
                                    SensorsDataSDKRemoteConfig sdkRemoteConfig = toSDKRemoteConfig(data);
                                    try {
                                        if (mSensorsDataEncrypt != null && sdkRemoteConfig.getPublicKey() != null) {
                                            mSensorsDataEncrypt.saveSecretKey(sdkRemoteConfig.getPublicKey(), sdkRemoteConfig.getPkv());
                                        }
                                    } catch (Exception e) {
                                        SALog.printStackTrace(e);
                                    }

                                    setSDKRemoteConfig(sdkRemoteConfig);
                                }
                                SALog.i(TAG, "Remote config response data is " + data);
                            }
                        } catch (Exception e) {
                            SALog.printStackTrace(e);
                        } finally {
                            try {
                                if (in != null) {
                                    in.close();
                                }

                                if (urlConnection != null) {
                                    urlConnection.disconnect();
                                }
                            } catch (Exception e) {
                                //ignored
                            }
                        }
                    }
                }, ThreadNameConstants.THREAD_GET_SDK_REMOTE_CONFIG).start();
            }

            @Override
            public void onFinish() {
            }
        };
        mPullSDKConfigCountDownTimer.start();
    }

    /**
     * 获取远程配置的 Url
     *
     * @param enableConfigV 是否在 Url 中携带 v 和 ve 参数，false 表示不携带
     * @return 远程配置的 Url
     */
    private String buildRemoteUrl(boolean enableConfigV) {
        String configUrl = null;
        boolean configV = enableConfigV;
        String serverUlr = mSensorsDataAPI.getServerUrl();
        String configOptionsRemoteUrl = null;
        if (mSAConfigOptions != null) {
            configOptionsRemoteUrl = mSAConfigOptions.mRemoteConfigUrl;
        }

        if (!TextUtils.isEmpty(configOptionsRemoteUrl)
                && Patterns.WEB_URL.matcher(configOptionsRemoteUrl).matches()) {
            configUrl = configOptionsRemoteUrl;
            SALog.i(TAG, "SAConfigOptions remote url is " + configUrl);
        } else if (!TextUtils.isEmpty(serverUlr) && Patterns.WEB_URL.matcher(serverUlr).matches()) {
            int pathPrefix = serverUlr.lastIndexOf("/");
            if (pathPrefix != -1) {
                configUrl = serverUlr.substring(0, pathPrefix);
                configUrl = configUrl + "/config/Android.conf";
            }
            SALog.i(TAG, "SensorsDataAPI remote url is " + configUrl);
        } else {
            SALog.i(TAG, String.format(Locale.CHINA, "ServerUlr: %s, SAConfigOptions remote url: %s",
                    serverUlr, configOptionsRemoteUrl));
            SALog.i(TAG, "Remote config url verification failed");
            return null;
        }

        //再次检查是否应该在请求中带 v，比如在禁止分散请求的情况下，SDK 升级了或者公钥为空，此时应该不带 v
        if (configV && (SensorsDataUtils.checkVersionIsNew(mContext, SensorsDataAPI.VERSION) ||
                (mSensorsDataEncrypt != null && mSensorsDataEncrypt.isPublicSecretKeyNull()))) {
            configV = false;
        }

        Uri.Builder builder = Uri.parse(configUrl).buildUpon();
        if (!TextUtils.isEmpty(configUrl) && configV) {
            String oldVersion = null;
            String newVersion = null;
            SensorsDataSDKRemoteConfig SDKRemoteConfig = mSDKRemoteConfig;
            if (SDKRemoteConfig != null) {
                oldVersion = SDKRemoteConfig.getOldVersion();
                newVersion = SDKRemoteConfig.getNewVersion();
                SALog.i(TAG, "The current config: " + SDKRemoteConfig.toString());
            }

            if (!TextUtils.isEmpty(oldVersion)) {
                builder.appendQueryParameter("v", oldVersion);
            }
            if (!TextUtils.isEmpty(newVersion)) {
                builder.appendQueryParameter("nv", newVersion);
            }
        }

        if (!TextUtils.isEmpty(serverUlr)) {
            Uri uri = Uri.parse(serverUlr);
            String project = uri.getQueryParameter("project");
            if (!TextUtils.isEmpty(project)) {
                builder.appendQueryParameter("project", project);
            }
        }

        String appId = AppInfoUtils.getProcessName(mContext);
        builder.appendQueryParameter("app_id", appId);
        builder.build();
        configUrl = builder.toString();
        SALog.i(TAG, "Android remote config URL:" + configUrl);
        return configUrl;
    }

    void resetPullSDKConfigTimer() {
        try {
            if (mPullSDKConfigCountDownTimer != null) {
                mPullSDKConfigCountDownTimer.cancel();
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        } finally {
            mPullSDKConfigCountDownTimer = null;
        }
    }

    /**
     * 更新 SensorsDataSDKRemoteConfig
     *
     * @param sdkRemoteConfig SensorsDataSDKRemoteConfig 在线控制 SDK 的配置
     */
    private void setSDKRemoteConfig(SensorsDataSDKRemoteConfig sdkRemoteConfig) {
        try {
            //版本号不一致时，才会返回数据，此时上报事件
            JSONObject eventProperties = new JSONObject();
            String remoteConfigString = sdkRemoteConfig.toJson().toString();
            eventProperties.put("$app_remote_config", remoteConfigString);
            SensorsDataAPI.sharedInstance().trackInternal("$AppRemoteConfigChanged", eventProperties);
            SensorsDataAPI.sharedInstance().flushSync();
            mPersistentRemoteSDKConfig.commit(remoteConfigString);
            SALog.i(TAG, "Save remote data");
            //值为 1 时，表示在线控制立即生效
            if (1 == sdkRemoteConfig.getEffectMode()) {
                applySDKConfigFromCache();
                SALog.i(TAG, "The remote configuration takes effect immediately");
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 从本地缓存中读取最新的 SDK 配置信息
     */
    void applySDKConfigFromCache() {
        try {
            SensorsDataSDKRemoteConfig sdkRemoteConfig = toSDKRemoteConfig(mPersistentRemoteSDKConfig.get());
            SALog.i(TAG, "Cache remote config is " + sdkRemoteConfig.toString());
            if (mSensorsDataAPI != null) {
                //关闭 debug 模式
                if (sdkRemoteConfig.isDisableDebugMode()) {
                    mSensorsDataAPI.setDebugMode(SensorsDataAPI.DebugMode.DEBUG_OFF);
                    SALog.i(TAG, "Set DebugOff Mode");
                }

                if (sdkRemoteConfig.isDisableSDK()) {
                    try {
                        mSensorsDataAPI.flush();
                        SALog.i(TAG, "DisableSDK is true");
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            }
            mSDKRemoteConfig = sdkRemoteConfig;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 全埋点是否被在线控制禁止
     *
     * @return false 表示全部全埋点被禁止，true 表示部分未被禁止，null 表示使用本地代码配置
     */
    Boolean isAutoTrackEnabled() {
        if (mSDKRemoteConfig != null) {
            if (mSDKRemoteConfig.getAutoTrackMode() == 0) {
                SALog.i(TAG, "AutoTrackMode is closing by remote config");
                return false;
            } else if (mSDKRemoteConfig.getAutoTrackMode() > 0) {
                return true;
            }
        }
        return null;
    }

    static boolean isSDKDisabledByRemote() {
        if (mSDKRemoteConfig == null) {
            return false;
        }
        return mSDKRemoteConfig.isDisableSDK();
    }

    /**
     * 全埋点类型是否被在线控制忽略
     *
     * @param autoTrackEventType 全埋点类型
     * @return true 表示该类型被忽略，false 表示不被忽略，null 表示使用本地代码配置
     */
    Boolean isAutoTrackEventTypeIgnored(int autoTrackEventType) {
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

    enum RemoteConfigHandleRandomTimeType {
        RandomTimeTypeWrite, // 创建分散请求时间
        RandomTimeTypeClean, // 移除分散请求时间
        RandomTimeTypeNone    // 不处理分散请求时间
    }
}
