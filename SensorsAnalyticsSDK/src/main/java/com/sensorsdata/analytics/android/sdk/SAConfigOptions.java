/*
 * Created by dengshiwei on 2019/03/11.
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

import com.sensorsdata.analytics.android.sdk.encrypt.IPersistentSecretKey;
import com.sensorsdata.analytics.android.sdk.util.ChannelUtils;

import javax.net.ssl.SSLSocketFactory;

public final class SAConfigOptions extends AbstractSAConfigOptions {
    /**
     * 是否设置点击图开关
     */
    boolean mInvokeHeatMapEnabled;

    /**
     * 是否设置点击图对话框
     */
    boolean mInvokeHeatMapConfirmDialog;

    /**
     * 是否设置点击图证书检查
     */
    boolean mInvokeHeatMapSSLCheck;

    /**
     * 是否设置可视化全埋点开关
     */
    boolean mInvokeVisualizedEnabled;

    /**
     * 是否设置可视化全埋点对话框
     */
    boolean mInvokeVisualizedConfirmDialog;

    /**
     * 是否设置点击图证书检查
     */
    boolean mInvokeVisualizedSSLCheck;

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
     * 设置点击图提示对话框是否可用
     *
     * @param enableDialog 对话框状态是否可用
     * @return SAOptionsConfig
     */
    public SAConfigOptions enableHeatMapConfirmDialog(boolean enableDialog) {
        this.mHeatMapConfirmDialogEnabled = enableDialog;
        this.mInvokeHeatMapConfirmDialog = true;
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
     * 设置可视化全埋点提示对话框是否可用
     *
     * @param enableDialog 对话框状态是否可用
     * @return SAOptionsConfig
     */
    public SAConfigOptions enableVisualizedAutoTrackConfirmDialog(boolean enableDialog) {
        this.mVisualizedConfirmDialogEnabled = enableDialog;
        this.mInvokeVisualizedConfirmDialog = true;
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
     * 是否开启 RN 数据采集
     *
     * @param enableRN 是否开启 RN 采集
     * @return SAOptionsConfig
     */
    public SAConfigOptions enableReactNativeAutoTrack(boolean enableRN) {
        this.mRNAutoTrackEnabled = enableRN;
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
        ChannelUtils.setSourceChannelKeys(channels);
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
     * 是否开启多渠道匹配，开启后 trackInstallation 中由 profile_set_once 操作改为 profile_set 。
     *
     * @param enableMultipleChannelMatch true 开启 false 关闭
     * @return 是否开启多渠道匹配
     */
    public SAConfigOptions enableMultipleChannelMatch(boolean enableMultipleChannelMatch) {
        this.mEnableMultipleChannelMatch = enableMultipleChannelMatch;
        return this;
    }

    /**
     * 是否开启加密
     *
     * @param enableEncrypt 是否开启加密
     * @return SAConfigOptions
     */
    public SAConfigOptions enableEncrypt(boolean enableEncrypt) {
        this.mEnableEncrypt = enableEncrypt;
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
     * 禁用数据采集
     *
     * @return SAConfigOptions
     */
    public SAConfigOptions disableDataCollect() {
        this.isDataCollectEnable = false;
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
}