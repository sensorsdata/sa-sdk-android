/*
 * Created by chenru on 2022/4/25 下午5:05(format year/.
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

package com.sensorsdata.analytics.android.advert;

import static com.sensorsdata.analytics.android.advert.SAAdvertConstants.TAG;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import com.sensorsdata.analytics.android.advert.deeplink.DeepLinkManager;
import com.sensorsdata.analytics.android.advert.monitor.SensorsDataAdvertActivityLifeCallback;
import com.sensorsdata.analytics.android.advert.oaid.SAOaidHelper;
import com.sensorsdata.analytics.android.advert.plugin.SAAdvertPluginManager;
import com.sensorsdata.analytics.android.advert.utils.ChannelUtils;
import com.sensorsdata.analytics.android.advert.utils.SAAdvertScanHelper;
import com.sensorsdata.analytics.android.advert.utils.SAAdvertUtils;
import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SAEventManager;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.mediator.ModuleConstants;
import com.sensorsdata.analytics.android.sdk.core.mediator.advert.SAAdvertModuleProtocol;
import com.sensorsdata.analytics.android.sdk.core.mediator.protocol.SAScanListener;
import com.sensorsdata.analytics.android.sdk.deeplink.SensorsDataDeepLinkCallback;
import com.sensorsdata.analytics.android.sdk.deeplink.SensorsDataDeferredDeepLinkCallback;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.monitor.SensorsDataLifecycleMonitorManager;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class SAAdvertProtocolImpl implements SAAdvertModuleProtocol, SAScanListener {
    private SensorsDataAdvertActivityLifeCallback mLifecycleCallback;
    private boolean mEnable = false;
    private Context mContext;
    private SAConfigOptions mOptions;
    private SAAdvertPluginManager mPluginManager;

    // $AppDeeplinkLaunch 是否携带设备信息
    private boolean mEnableDeepLinkInstallSource;

    @Override
    public void install(Context context, SAConfigOptions options) {
        mContext = context;
        mOptions = options;
        init();
    }

    private void init() {
        mPluginManager = new SAAdvertPluginManager();
        ChannelUtils.setSourceChannelKeys(mOptions.channelSourceKeys);
        if (!mOptions.isDisableSDK()) {
            setModuleState(true);
        }
        if (AppInfoUtils.isMainProcess(mContext, null)) {
            if (!ChannelUtils.isExistRequestDeferredDeeplink()) {
                ChannelUtils.commitRequestDeferredDeeplink(true);
            } else {
                ChannelUtils.commitRequestDeferredDeeplink(false);
            }
        }
    }

    @Override
    public void setModuleState(boolean enable) {
        if (mEnable != enable) {
            if (enable) {
                delayInitTask();
                registerLifeCallback();
                mPluginManager.registerPlugin();
            } else {
                unregisterLifecycleCallback();
                mPluginManager.unregisterPlugin();
            }
            mEnable = enable;
        }
    }

    private void delayInitTask() {
        SAEventManager.getInstance().trackQueueEvent(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mOptions.isSaveDeepLinkInfo()) {
                        ChannelUtils.loadUtmByLocal();
                    } else {
                        ChannelUtils.clearLocalUtm();
                    }
                } catch (Exception ex) {
                    SALog.printStackTrace(ex);
                }
            }
        });
    }

    private void registerLifeCallback() {
        if (mLifecycleCallback == null) {
            mLifecycleCallback = new SensorsDataAdvertActivityLifeCallback(mOptions);
        }
        SensorsDataLifecycleMonitorManager.getInstance().addActivityLifeCallback(mLifecycleCallback);
    }

    private void unregisterLifecycleCallback() {
        if (mLifecycleCallback != null) {
            SensorsDataLifecycleMonitorManager.getInstance().removeActivityLifeCallback(mLifecycleCallback);
        }
    }

    @Override
    public void trackAppInstall(JSONObject properties, boolean disableCallback) {
        trackInstallation("$AppInstall", properties, disableCallback);
    }

    @Override
    public void trackAppInstall(JSONObject properties) {
        trackAppInstall(properties, false);
    }

    @Override
    public void trackAppInstall() {
        trackAppInstall(null, false);
    }

    @Override
    public void trackDeepLinkLaunch(String deepLinkUrl) {
        trackDeepLinkLaunch(deepLinkUrl, null);
    }

    @Override
    public void trackDeepLinkLaunch(String deepLinkUrl, final String oaid) {
        if (!isEnable()) {
            return;
        }
        final JSONObject properties = new JSONObject();
        try {
            properties.put("$deeplink_url", deepLinkUrl);
            properties.put("$time", new Date(System.currentTimeMillis()));
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        SensorsDataAPI.sharedInstance().transformTaskQueue(new Runnable() {
            @Override
            public void run() {
                if (mEnableDeepLinkInstallSource) {
                    try {
                        properties.put("$ios_install_source", ChannelUtils.getDeviceInfo(mContext,
                                SAAdvertUtils.getAndroidId(mContext), oaid == null ? SAOaidHelper.getOAID(mContext) : oaid));
                    } catch (JSONException e) {
                        SALog.printStackTrace(e);
                    }
                }
                SensorsDataAPI.sharedInstance().trackInternal("$AppDeeplinkLaunch", properties);
            }
        });
    }

    @Override
    public void trackInstallation(final String eventName, JSONObject properties, final boolean disableCallback) {
        if (!isEnable()) {
            return;
        }
        try {
            if (!AppInfoUtils.isMainProcess(mContext, null)) {
                return;
            }
            //只在主进程触发 trackInstallation
            final JSONObject eventProperties = new JSONObject();
            if (properties != null) {
                SensorsDataUtils.mergeJSONObject(properties, eventProperties);
            }
            SADataHelper.addTimeProperty(eventProperties);
            final String loginId = SensorsDataAPI.sharedInstance().getLoginId();
            SensorsDataAPI.sharedInstance().transformTaskQueue(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean firstTrackInstallation = SAAdvertUtils.isFirstTrackInstallation(disableCallback);
                        if (firstTrackInstallation) {
                            boolean isCorrectTrackInstallation = false;
                            try {
                                if (!ChannelUtils.hasUtmProperties(eventProperties)) {
                                    ChannelUtils.mergeUtmByMetaData(mContext, eventProperties);
                                }

                                if (!ChannelUtils.hasUtmProperties(eventProperties)) {
                                    String androidId = SAAdvertUtils.getAndroidId(mContext);
                                    String installSource;
                                    String oaid;
                                    if (eventProperties.has("$oaid")) {
                                        oaid = eventProperties.optString("$oaid");
                                        installSource = ChannelUtils.getDeviceInfo(mContext, androidId, oaid);
                                        SALog.i(TAG, "properties has oaid " + oaid);
                                    } else {
                                        oaid = SAOaidHelper.getOAID(mContext);
                                        installSource = ChannelUtils.getDeviceInfo(mContext, androidId, oaid);
                                    }

                                    if (eventProperties.has("$gaid")) {
                                        installSource = String.format("%s##gaid=%s", installSource, eventProperties.optString("$gaid"));
                                    }
                                    isCorrectTrackInstallation = ChannelUtils.isGetDeviceInfo(mContext, androidId, oaid);
                                    eventProperties.put("$ios_install_source", installSource);
                                }
                                if (eventProperties.has("$oaid")) {
                                    eventProperties.remove("$oaid");
                                }

                                if (eventProperties.has("$gaid")) {
                                    eventProperties.remove("$gaid");
                                }

                                if (disableCallback) {
                                    eventProperties.put("$ios_install_disable_callback", disableCallback);
                                }
                            } catch (Exception e) {
                                SALog.printStackTrace(e);
                            }
                            final String distinctId = SensorsDataAPI.sharedInstance().getDistinctId();
                            // 先发送 track
                            SAEventManager.getInstance().trackEvent(EventType.TRACK, eventName, eventProperties, null, distinctId, loginId, null);
                            // 再发送 profile_set_once 或者 profile_set
                            JSONObject profileProperties = new JSONObject();
                            // 用户属性需要去掉 $ios_install_disable_callback 字段
                            eventProperties.remove("$ios_install_disable_callback");
                            SensorsDataUtils.mergeJSONObject(eventProperties, profileProperties);
                            profileProperties.put("$first_visit_time", new java.util.Date());
                            SAEventManager.getInstance().trackEvent(EventType.PROFILE_SET_ONCE, null, profileProperties, null, distinctId, loginId, null);

                            SAAdvertUtils.setTrackInstallation(disableCallback);
                            ChannelUtils.saveCorrectTrackInstallation(isCorrectTrackInstallation);
                        }
                        SensorsDataAPI.sharedInstance().flush();
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void trackInstallation(String eventName, JSONObject properties) {
        trackInstallation(eventName, properties, false);
    }

    @Override
    public void trackInstallation(String eventName) {
        trackInstallation(eventName, null, false);
    }

    @Override
    public void trackChannelEvent(String eventName) {
        trackChannelEvent(eventName, null);
    }

    @Override
    public void trackChannelEvent(final String eventName, JSONObject properties) {
        if (!isEnable()) {
            return;
        }
        if (mOptions.isAutoAddChannelCallbackEvent()) {
            SensorsDataAPI.sharedInstance().track(eventName, properties);
            return;
        }
        final JSONObject eventProperties = new JSONObject();
        if (properties != null) {
            SensorsDataUtils.mergeJSONObject(properties, eventProperties);
        }
        SADataHelper.addTimeProperty(eventProperties);
        SensorsDataAPI.sharedInstance().transformTaskQueue(new Runnable() {
            @Override
            public void run() {
                try {
                    try {
                        eventProperties.put("$is_channel_callback_event", ChannelUtils.isFirstChannelEvent(eventName));
                        if (!ChannelUtils.hasUtmProperties(eventProperties)) {
                            ChannelUtils.mergeUtmByMetaData(mContext, eventProperties);
                        }
                        if (!ChannelUtils.hasUtmProperties(eventProperties)) {
                            if (eventProperties.has("$oaid")) {
                                String oaid = eventProperties.optString("$oaid");
                                eventProperties.put("$channel_device_info",
                                        ChannelUtils.getDeviceInfo(mContext, SAAdvertUtils.getAndroidId(mContext), oaid));
                                SALog.i(TAG, "properties has oaid " + oaid);
                            } else {
                                eventProperties.put("$channel_device_info",
                                        ChannelUtils.getDeviceInfo(mContext, SAAdvertUtils.getAndroidId(mContext), SAOaidHelper.getOAID(mContext)));
                            }
                        }
                        if (eventProperties.has("$oaid")) {
                            eventProperties.remove("$oaid");
                        }
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }

                    // 先发送 track
                    SAEventManager.getInstance().trackEvent(EventType.TRACK, eventName, eventProperties, null);
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }

    @Override
    public void enableDeepLinkInstallSource(boolean enable) {
        mEnableDeepLinkInstallSource = enable;
        DeepLinkManager.enableDeepLinkInstallSource(enable);
    }

    @Override
    public void setDeepLinkCallback(SensorsDataDeepLinkCallback deepLinkCallback) {
        DeepLinkManager.setDeepLinkCallback(deepLinkCallback);
    }

    @Override
    public void setDeepLinkCompletion(SensorsDataDeferredDeepLinkCallback callback) {
        DeepLinkManager.setDeferredDeepLinkCallback(callback);
    }

    @Override
    public void requestDeferredDeepLink(final JSONObject params) {
        if (!isEnable()) {
            return;
        }
        SAEventManager.getInstance().trackQueueEvent(new Runnable() {
            @Override
            public void run() {
                try {
                    if (ChannelUtils.isRequestDeferredDeeplink()) {
                        DeepLinkManager.requestDeferredDeepLink(mContext, params, SAAdvertUtils.getAndroidId(mContext)
                                , SAOaidHelper.getOAID(mContext), SensorsDataAPI.sharedInstance().getPresetProperties(), mOptions.getCustomADChannelUrl(), mOptions.isSaveDeepLinkInfo());
                        ChannelUtils.commitRequestDeferredDeeplink(false);
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }


    @Override
    public String getModuleName() {
        return ModuleConstants.ModuleName.ADVERT_NAME;
    }

    @Override
    public boolean isEnable() {
        return mEnable;
    }

    @Override
    public boolean handlerScanUri(Activity activity, Uri uri) {
        return SAAdvertScanHelper.scanHandler(activity, uri);
    }

    @Override
    public JSONObject mergeChannelEventProperties(String eventName, JSONObject properties) {
        if (mOptions.isAutoAddChannelCallbackEvent()) {
            return ChannelUtils.checkOrSetChannelCallbackEvent(eventName, properties, mContext);
        }
        return properties;
    }

    @Override
    public JSONObject getLatestUtmProperties() {
        return ChannelUtils.getLatestUtmProperties();
    }

    @Override
    public void removeDeepLinkInfo(JSONObject properties) {
        ChannelUtils.removeDeepLinkInfo(properties);
    }

    @Override
    public void commitRequestDeferredDeeplink(boolean isRequest) {
        ChannelUtils.commitRequestDeferredDeeplink(isRequest);
    }
}
