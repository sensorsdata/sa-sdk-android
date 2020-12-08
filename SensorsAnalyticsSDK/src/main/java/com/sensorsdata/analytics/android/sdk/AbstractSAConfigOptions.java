/*
 * Created by dengshiwei on 2019/04/18.
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

import javax.net.ssl.SSLSocketFactory;

/**
 * SDK 配置抽象类
 */
abstract class AbstractSAConfigOptions {
    /**
     * 请求配置地址，默认从 ServerUrl 解析
     */
    public String mRemoteConfigUrl;

    /**
     * 远程配置请求最小间隔时长，单位：小时，默认 24
     */
    public int mMinRequestInterval = 24;

    /**
     * 远程配置请求最大间隔时长，单位：小时，默认 48
     */
    public int mMaxRequestInterval = 48;

    /**
     * 禁用随机时间请求远程配置
     */
    public boolean mDisableRandomTimeRequestRemoteConfig;

    /**
     * 设置 SSLSocketFactory
     */
    public SSLSocketFactory mSSLSocketFactory;

    /**
     * 数据上报服务器地址
     */
    String mServerUrl;

    /**
     * AutoTrack 类型
     */
    int mAutoTrackEventType;

    /**
     * 是否开启 TrackAppCrash
     */
    boolean mEnableTrackAppCrash;

    /**
     * 两次数据发送的最小时间间隔，单位毫秒
     */
    int mFlushInterval;

    /**
     * 本地缓存日志的最大条目数
     */
    int mFlushBulkSize;

    /**
     * 本地缓存上限值，单位 byte，默认为 32MB：32 * 1024 * 1024
     */
    long mMaxCacheSize = 32 * 1024 * 1024L;

    /**
     * 点击图是否可用
     */
    boolean mHeatMapEnabled;

    /**
     * 点击图对话框是否可用
     */
    boolean mHeatMapConfirmDialogEnabled;

    /**
     * 可视化全埋点是否可用
     */
    boolean mVisualizedEnabled;

    /**
     * 可视化全埋点对话框是否可用
     */
    boolean mVisualizedConfirmDialogEnabled;

    /**
     * 是否开启打印日志
     */
    boolean mLogEnabled;

    /**
     * 开启 RN 采集
     */
    boolean mRNAutoTrackEnabled;

    /**
     * 采集屏幕方向
     */
    boolean mTrackScreenOrientationEnabled;

    /**
     * 网络上传策略
     */
    int mNetworkTypePolicy = SensorsNetworkType.TYPE_3G | SensorsNetworkType.TYPE_4G | SensorsNetworkType.TYPE_WIFI | SensorsNetworkType.TYPE_5G;

    /**
     * AnonymousId，匿名 ID
     */
    String mAnonymousId;

    /**
     * 是否使用上次启动时保存的 utm 属性.
     */
    boolean mEnableSaveDeepLinkInfo = false;

    /**
     * 是否自动进行 H5 打通
     */
    boolean isAutoTrackWebView;

    /**
     * WebView 是否支持 JellyBean
     */
    boolean isWebViewSupportJellyBean;

    /**
     * 是否在手动埋点事件中自动添加渠道匹配信息
     */
    boolean isAutoAddChannelCallbackEvent;

    /**
     * 是否开启多渠道匹配，开启后 trackInstallation 中由 profile_set_once 操作改为 profile_set 。
     */
    boolean mEnableMultipleChannelMatch = false;

    /**
     * 是否子进程上报数据
     */
    boolean isSubProcessFlushData = false;

    /**
     * 是否开启加密
     */
    boolean mEnableEncrypt = false;

    /**
     * 密钥存储相关接口
     */
    IPersistentSecretKey mPersistentSecretKey;

    /**
     * 关闭数据采集，默认开启数据采集
     */
    boolean isDataCollectEnable = true;
}