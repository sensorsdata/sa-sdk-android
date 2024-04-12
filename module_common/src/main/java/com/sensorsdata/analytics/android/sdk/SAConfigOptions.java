/*
 * Created by dengshiwei on 2019/03/11.
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

package com.sensorsdata.analytics.android.sdk;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.core.business.exposure.SAExposureConfig;
import com.sensorsdata.analytics.android.sdk.deeplink.SensorsDataDeferredDeepLinkCallback;
import com.sensorsdata.analytics.android.sdk.encrypt.IPersistentSecretKey;
import com.sensorsdata.analytics.android.sdk.encrypt.SAEncryptListener;
import com.sensorsdata.analytics.android.sdk.plugin.encrypt.StorePlugin;
import com.sensorsdata.analytics.android.sdk.plugin.property.SAPropertyPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

public final class SAConfigOptions extends AbstractSAConfigOptions implements Cloneable {
    /**
     * 是否设置点击图开关
     */
    boolean mInvokeHeatMapEnabled;

    /**
     * 是否设置可视化全埋点开关
     */
    boolean mInvokeVisualizedEnabled;

    /**
     * 是否设置打印日志
     */
    boolean mInvokeLog;

    /**
     * 私有构造函数
     */
    private SAConfigOptions() {
    }

    /**
     * 获取 SAOptionsConfig 实例
     *
     * @param serverUrl，数据上报服务器地址
     */
    public SAConfigOptions(String serverUrl) {
        this.mServerUrl = serverUrl;
    }

    /**
     * 设置远程配置请求地址
     *
     * @param remoteConfigUrl，远程配置请求地址
     * @return SAOptionsConfig
     */
    public SAConfigOptions setRemoteConfigUrl(String remoteConfigUrl) {
        this.mRemoteConfigUrl = remoteConfigUrl;
        return this;
    }

    /**
     * 设置数据上报地址
     *
     * @param serverUrl，数据上报地址
     * @return SAOptionsConfig
     */
    public SAConfigOptions setServerUrl(String serverUrl) {
        this.mServerUrl = serverUrl;
        return this;
    }

    /**
     * 设置 AutoTrackEvent 的类型，可通过 '|' 进行连接
     *
     * @param autoTrackEventType 开启的 AutoTrack 类型
     * @return SAOptionsConfig
     */
    public SAConfigOptions setAutoTrackEventType(int autoTrackEventType) {
        this.mAutoTrackEventType = autoTrackEventType;
        return this;
    }

    /**
     * 设置是否开启 AppCrash 采集，默认是关闭的
     *
     * @return SAOptionsConfig
     */
    public SAConfigOptions enableTrackAppCrash() {
        this.mEnableTrackAppCrash = true;
        return this;
    }

    /**
     * 设置两次数据发送的最小时间间隔，最小值 5 秒
     *
     * @param flushInterval 时间间隔，单位毫秒
     * @return SAOptionsConfig
     */
    public SAConfigOptions setFlushInterval(int flushInterval) {
        this.mFlushInterval = Math.max(5 * 1000, flushInterval);
        return this;
    }

    /**
     * 设置本地缓存日志的最大条目数
     *
     * @param flushBulkSize 缓存数目
     * @return SAOptionsConfig
     */
    public SAConfigOptions setFlushBulkSize(int flushBulkSize) {
        this.mFlushBulkSize = Math.max(50, flushBulkSize);
        return this;
    }

    /**
     * 设置本地缓存上限值，单位 byte，默认为 32MB：32 * 1024 * 1024，最小 16MB：16 * 1024 * 1024，若小于 16MB，则按 16MB 处理。
     *
     * @param maxCacheSize 单位 byte
     * @return SAOptionsConfig
     */
    public SAConfigOptions setMaxCacheSize(long maxCacheSize) {
        this.mMaxCacheSize = Math.max(16 * 1024 * 1024, maxCacheSize);
        return this;
    }

    /**
     * 设置远程配置请求最小间隔时长
     *
     * @param minRequestInterval 最小时长间隔，单位：小时，默认 24，合法区间在(0, 7*24] 之间
     * @return SAOptionsConfig
     */
    public SAConfigOptions setMinRequestInterval(int minRequestInterval) {
        //设置最小时长间隔的合法区间为 0 到 7*24 小时
        if (minRequestInterval > 0) {
            this.mMinRequestInterval = Math.min(minRequestInterval, 7 * 24);
        }
        return this;
    }

    /**
     * 设置远程配置请求最大间隔时长
     *
     * @param maxRequestInterval 最大时长间隔，单位：小时，默认 48，合法区间在(0, 7*24] 之间
     * @return SAOptionsConfig
     */
    public SAConfigOptions setMaxRequestInterval(int maxRequestInterval) {
        //设置最大时长间隔合法区间为 0 到 7*24 小时
        if (maxRequestInterval > 0) {
            this.mMaxRequestInterval = Math.min(maxRequestInterval, 7 * 24);
        }
        return this;
    }

    /**
     * 禁用分散请求远程配置
     *
     * @return SAOptionsConfig
     */
    public SAConfigOptions disableRandomTimeRequestRemoteConfig() {
        this.mDisableRandomTimeRequestRemoteConfig = true;
        return this;
    }

    /**
     * 禁用辅助工具
     *
     * @return SAOptionsConfig
     */
    public SAConfigOptions disableDebugAssistant() {
        this.mDisableDebugAssistant = true;
        return this;
    }

    /**
     * 设置点击图是否可用
     *
     * @param enableHeatMap 点击图是否可用
     * @return SAOptionsConfig
     */
    public SAConfigOptions enableHeatMap(boolean enableHeatMap) {
        this.mHeatMapEnabled = enableHeatMap;
        this.mInvokeHeatMapEnabled = true;
        return this;
    }

    /**
     * 设置可视化全埋点自定义属性是否可用
     *
     * @param enableVisualizedProperties 可视化全埋点自定义属性功能是否可用
     * @return SAConfigOptions
     */
    public SAConfigOptions enableVisualizedProperties(boolean enableVisualizedProperties) {
        this.mVisualizedPropertiesEnabled = enableVisualizedProperties;
        return this;
    }

    /**
     * 设置可视化全埋点是否可用
     *
     * @param enableVisualizedAutoTrack 可视化全埋点是否可用
     * @return SAOptionsConfig
     */
    public SAConfigOptions enableVisualizedAutoTrack(boolean enableVisualizedAutoTrack) {
        this.mVisualizedEnabled = enableVisualizedAutoTrack;
        this.mInvokeVisualizedEnabled = true;
        return this;
    }

    /**
     * 是否打印日志
     *
     * @param enableLog 是否开启打印日志
     * @return SAOptionsConfig
     */
    public SAConfigOptions enableLog(boolean enableLog) {
        this.mLogEnabled = enableLog;
        this.mInvokeLog = true;
        return this;
    }

    /**
     * 是否开启屏幕方向采集
     *
     * @param enableScreenOrientation 是否开启屏幕方向采集
     * @return SAOptionsConfig
     */
    public SAConfigOptions enableTrackScreenOrientation(boolean enableScreenOrientation) {
        this.mTrackScreenOrientationEnabled = enableScreenOrientation;
        return this;
    }

    /**
     * 设置数据的网络上传策略
     *
     * @param networkTypePolicy 数据的网络上传策略
     * @return SAOptionsConfig
     */
    public SAConfigOptions setNetworkTypePolicy(int networkTypePolicy) {
        this.mNetworkTypePolicy = networkTypePolicy;
        return this;
    }

    /**
     * 设置是否保存 utm 属性
     *
     * @param enableSave boolean 默认 false 不保存
     * @return SAConfigOptions
     */
    public SAConfigOptions enableSaveDeepLinkInfo(boolean enableSave) {
        this.mEnableSaveDeepLinkInfo = enableSave;
        return this;
    }

    /**
     * 用户需采集渠道信息自定义属性 key 值，可传多个。
     *
     * @param channels 渠道信息自定义属性 key 值
     * @return SAConfigOptions
     */
    public SAConfigOptions setSourceChannels(String... channels) {
        channelSourceKeys = channels;
        return this;
    }

    /**
     * 开启自动打通所有的 WebView H5 功能。目前支持的 Android 系统自带的 WebView 以及腾讯的 x5WebView.
     *
     * @param isSupportJellyBean 是否支持 API level 16 及以下的版本。因为 API level 16 及以下的版本，addJavascriptInterface 有安全漏洞，请谨慎使用。
     * @return SAConfigOptions
     */
    public SAConfigOptions enableJavaScriptBridge(boolean isSupportJellyBean) {
        this.isAutoTrackWebView = true;
        this.isWebViewSupportJellyBean = isSupportJellyBean;
        return this;
    }

    /**
     * 是否在手动埋点事件中自动添加渠道匹配信息
     *
     * @param isAutoAddChannelCallbackEvent true: 开启，false: 不开启，默认是 false
     * @return SAConfigOptions
     */
    public SAConfigOptions enableAutoAddChannelCallbackEvent(boolean isAutoAddChannelCallbackEvent) {
        this.isAutoAddChannelCallbackEvent = isAutoAddChannelCallbackEvent;
        return this;
    }

    /**
     * 开启埋点开启加密，上报时已是密文。
     *
     * @param enableEncrypt 是否开启加密
     * @return SAConfigOptions
     */
    public SAConfigOptions enableEncrypt(boolean enableEncrypt) {
        this.mEnableEncrypt = enableEncrypt;
        return this;
    }

    /**
     * 开启传输加密
     *
     * @return SAConfigOptions
     */
    public SAConfigOptions enableTransportEncrypt() {
        this.mTransportEncrypt = true;
        return this;
    }

    /**
     * 密钥回调监听
     *
     * @param persistentSecretKey 密钥回调监听
     * @return SAConfigOptions
     */
    public SAConfigOptions persistentSecretKey(IPersistentSecretKey persistentSecretKey) {
        mPersistentSecretKey = persistentSecretKey;
        return this;
    }

    /**
     * 是否多进程上报数据
     *
     * @return SAConfigOptions
     */
    public SAConfigOptions enableSubProcessFlushData() {
        this.isSubProcessFlushData = true;
        return this;
    }

    /**
     * 设置 SSLSocketFactory，HTTPS 请求连接时需要使用
     *
     * @param SSLSocketFactory 证书
     * @return SAConfigOptions
     */
    public SAConfigOptions setSSLSocketFactory(SSLSocketFactory SSLSocketFactory) {
        this.mSSLSocketFactory = SSLSocketFactory;
        return this;
    }

    /**
     * 是否开启 push 点击的采集
     *
     * @param enableTrackPush 是否开启 push 点击的采集
     * @return SAConfigOptions
     */
    public SAConfigOptions enableTrackPush(boolean enableTrackPush) {
        this.mEnableTrackPush = enableTrackPush;
        return this;
    }

    /**
     * 是否关闭 SDK
     *
     * @param disableSDK 是否关闭 SDK
     * @return SAConfigOptions
     */
    public SAConfigOptions disableSDK(boolean disableSDK) {
        this.isDisableSDK = disableSDK;
        return this;
    }

    /**
     * 是否开启页面停留时长
     *
     * @param isTrackPageLeave 是否开启页面停留时长
     * @return SAConfigOptions
     */
    @Deprecated
    public SAConfigOptions enableTrackPageLeave(boolean isTrackPageLeave) {
        return enableTrackPageLeave(isTrackPageLeave, false);
    }

    /**
     * 是否开启页面停留时长
     *
     * @param isTrackPageLeave 是否开启页面停留时长
     * @param isTrackFragmentPageLeave 是否采集 Fragment 页面停留时长，需开启页面停留时长采集
     * @return SAConfigOptions
     */
    public SAConfigOptions enableTrackPageLeave(boolean isTrackPageLeave, boolean isTrackFragmentPageLeave) {
        this.mIsTrackPageLeave = isTrackPageLeave;
        this.mIsTrackFragmentPageLeave = isTrackFragmentPageLeave;
        return this;
    }

    /**
     * 指定哪些 Activity/Fragment 不采集页面停留时长
     * 指定 Activity/Fragment 的格式为：****.class
     *
     * @param ignoreList activity/Fragment 列表
     * @return SAConfigOptions
     */
    public SAConfigOptions ignorePageLeave(List<Class<?>> ignoreList) {
        mIgnorePageLeave = ignoreList;
        return this;
    }

    /**
     * 注册自定义加密插件
     *
     * @param encryptListener 自定义加密实现接口
     * @return SAConfigOptions
     */
    public SAConfigOptions registerEncryptor(SAEncryptListener encryptListener) {
        if (encryptListener == null
                || TextUtils.isEmpty(encryptListener.asymmetricEncryptType())
                || TextUtils.isEmpty(encryptListener.symmetricEncryptType())) {
            return this;
        }
        if (!mEncryptors.contains(encryptListener)) {
            mEncryptors.add(0, encryptListener);
        }
        return this;
    }

    /**
     * 注册存储插件，适用于 SP 加密
     *
     * @param plugin 自定义插件
     * @return SAConfigOptions
     */
    public SAConfigOptions registerStorePlugin(StorePlugin plugin) {
        if (mStorePlugins == null) {
            mStorePlugins = new ArrayList<>();
        }
        mStorePlugins.add(plugin);
        return this;
    }

    /**
     * 将 $device_id 修改为 $anonymization_id
     *
     * @return SAConfigOptions
     */
    public SAConfigOptions disableDeviceId() {
        this.mDisableDeviceId = true;
        return this;
    }

    /**
     * 设置 DeepLink 请求 url
     *
     * @param url 请求 URL 地址
     * @return SAConfigOptions
     */
    public SAConfigOptions setCustomAdChannelUrl(String url) {
        this.mCustomADChannelUrl = url;
        return this;
    }

    @Override
    public SAConfigOptions clone() {
        SAConfigOptions copyObject = this;
        try {
            copyObject = (SAConfigOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            SALog.printStackTrace(e);
        }
        return copyObject;
    }

    /**
     * 是否开启 session_id 的采集
     *
     * @param enableSession 是否开启 Session 采集
     * @return SAConfigOptions
     */
    public SAConfigOptions enableSession(boolean enableSession) {
        this.mEnableSession = enableSession;
        return this;
    }

    /**
     * 是 event_session 切割的超时时间
     *
     * @param time 超时时间，单位秒
     * @return SAConfigOptions
     */
    public SAConfigOptions setEventSessionTimeout(int time) {
        this.mEventSessionTimeout = time;
        return this;
    }

    public SAConfigOptions registerPropertyPlugin(SAPropertyPlugin property) {
        if (this.mPropertyPlugins == null) {
            this.mPropertyPlugins = new ArrayList<>();
        }
        this.mPropertyPlugins.add(property);
        return this;
    }

    /**
     * 获取曝光配置信息
     *
     * @return 返回曝光全局配置信息
     */
    public SAExposureConfig getExposureConfig() {
        return mExposureConfig;
    }

    /**
     * 设置曝光配置信息
     *
     * @param exposureConfig 曝光配置信息
     * @return 返回全局配置信息
     */
    public SAConfigOptions setExposureConfig(SAExposureConfig exposureConfig) {
        mExposureConfig = exposureConfig;
        return this;
    }

    /**
     * 设置匿名 ID
     *
     * @param anonymousId 匿名 ID
     * @return SAOptionsConfig
     */
    public SAConfigOptions setAnonymousId(String anonymousId) {
        this.mAnonymousId = anonymousId;
        return this;
    }

    /**
     * 设置 DeepLink 与 Deferred DeepLink接口回调
     *
     * @param callback DeepLink 与 Deferred DeepLink接口回调
     * @return SAConfigOptions
     */
    public SAConfigOptions setDeepLinkCompletion(SensorsDataDeferredDeepLinkCallback callback) {
        this.mDeeplinkCallback = callback;
        return this;
    }

    /**
     * 注册限制性属性 key
     *
     * @param limitKeys 限制性属性 key
     * @return SAConfigOptions
     */
    public SAConfigOptions registerLimitKeys(Map<String, String> limitKeys) {
        this.mLimitKeys = limitKeys;
        return this;
    }

    /**
     * 实时数据发送事件列表
     *
     * @param listInstantEvents 实时事件列表
     * @return SAConfigOptions
     */
    public SAConfigOptions setInstantEvents(List<String> listInstantEvents) {
        mListInstantEvents = listInstantEvents;
        return this;
    }

    public SAConfigOptions setAdvertConfig(SAAdvertisingConfig config) {
        this.mSATConfig = config;
        return this;
    }

    /**
     * 关闭打点定时器
     * @return SAConfigOptions
     */
    public SAConfigOptions disableAppEndTimer() {
        this.mDisableAppEndTimer = true;
        return this;
    }
}