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

import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentRemoteSDKConfig;
import com.sensorsdata.analytics.android.sdk.network.HttpCallback;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONObject;

import java.security.SecureRandom;

/**
 * SDK 初始化及线上使用时，采集控制管理类
 */
public class SensorsDataRemoteManager extends BaseSensorsDataSDKRemoteManager {

    private static final String SHARED_PREF_REQUEST_TIME = "sensorsdata.request.time";
    private static final String SHARED_PREF_REQUEST_TIME_RANDOM = "sensorsdata.request.time.random";
    private static final String TAG = "SA.SensorsDataRemoteManager";

    // 每次启动 App 时，最多尝试三次
    private CountDownTimer mPullSDKConfigCountDownTimer;

    private PersistentRemoteSDKConfig mPersistentRemoteSDKConfig;
    private SharedPreferences mSharedPreferences;

    public SensorsDataRemoteManager(
            SensorsDataAPI sensorsDataAPI) {
        super(sensorsDataAPI);
        this.mPersistentRemoteSDKConfig = (PersistentRemoteSDKConfig) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.REMOTE_CONFIG);
        this.mSharedPreferences = SensorsDataUtils.getSharedPreferences(mContext);
        SALog.i(TAG, "Construct a SensorsDataRemoteManager");
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
            randomTime += new SecureRandom().nextInt(mSAConfigOptions.mMaxRequestInterval - mSAConfigOptions.mMinRequestInterval + 1);
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

    @Override
    public void pullSDKConfigFromServer() {
        if (mSAConfigOptions == null) {
            return;
        }

        // 关闭随机请求或者分散的最小时间大于最大时间时，清除本地时间，请求后端
        if (mSAConfigOptions.mDisableRandomTimeRequestRemoteConfig ||
                mSAConfigOptions.mMinRequestInterval > mSAConfigOptions.mMaxRequestInterval) {
            requestRemoteConfig(RandomTimeType.RandomTimeTypeClean, true);
            SALog.i(TAG, "remote config: Request remote config because disableRandomTimeRequestRemoteConfig or minHourInterval greater than maxHourInterval");
            return;
        }

        //开启加密并且传入秘钥为空的，强制请求后端，此时请求中不带 v
        if (mSensorsDataEncrypt != null && mSensorsDataEncrypt.isPublicSecretKeyNull()) {
            requestRemoteConfig(RandomTimeType.RandomTimeTypeWrite, false);
            SALog.i(TAG, "remote config: Request remote config because encrypt key is null");
            return;
        }

        //满足分散请求逻辑时，请求后端
        if (isRequestValid()) {
            requestRemoteConfig(RandomTimeType.RandomTimeTypeWrite, true);
            SALog.i(TAG, "remote config: Request remote config because satisfy the random request condition");
        }
    }

    @Override
    public void requestRemoteConfig(RandomTimeType randomTimeType, final boolean enableConfigV) {
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
                requestRemoteConfig(enableConfigV, new HttpCallback.StringCallback() {
                    @Override
                    public void onFailure(int code, String errorMessage) {
                        // 304 状态码为后端配置未更新，此时不需要重试
                        // 205 状态码表示后端环境未同步配置，此时需要重试，代码不需要做特殊处理
                        if (code == 304 || code == 404) {
                            resetPullSDKConfigTimer();
                        }
                        SALog.i(TAG, "Remote request failed,responseCode is " + code +
                                ",errorMessage is " + errorMessage);
                    }

                    @Override
                    public void onResponse(String response) {
                        resetPullSDKConfigTimer();
                        if (!TextUtils.isEmpty(response)) {
                            SensorsDataSDKRemoteConfig sdkRemoteConfig = toSDKRemoteConfig(response);
                            try {
                                if (mSensorsDataEncrypt != null) {
                                    mSensorsDataEncrypt.saveSecretKey(sdkRemoteConfig.getSecretKey());
                                }
                            } catch (Exception e) {
                                SALog.printStackTrace(e);
                            }

                            setSDKRemoteConfig(sdkRemoteConfig);
                        }
                        SALog.i(TAG, "Remote request was successful,response data is " + response);
                    }

                    @Override
                    public void onAfter() {

                    }
                });
            }

            @Override
            public void onFinish() {
            }
        };
        mPullSDKConfigCountDownTimer.start();
    }

    @Override
    public void resetPullSDKConfigTimer() {
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
    @Override
    protected void setSDKRemoteConfig(SensorsDataSDKRemoteConfig sdkRemoteConfig) {
        try {
            //版本号不一致时，才会返回数据，此时上报事件
            JSONObject eventProperties = new JSONObject();
            String remoteConfigString = sdkRemoteConfig.toJson().toString();
            eventProperties.put("$app_remote_config", remoteConfigString);
            SensorsDataAPI.sharedInstance().trackInternal("$AppRemoteConfigChanged", eventProperties);
            SensorsDataAPI.sharedInstance().flush();
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
    @Override
    public void applySDKConfigFromCache() {
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
}
