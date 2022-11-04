/*
 * Created by dengshiwei on 2022/09/08.
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

package com.sensorsdata.analytics.android.sdk.advert.impl;

import static com.sensorsdata.analytics.android.sdk.advert.SAAdvertConstants.TAG;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.core.SACoreHelper;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.advert.deeplink.DeepLinkManager;
import com.sensorsdata.analytics.android.sdk.advert.monitor.SensorsDataAdvertActivityLifeCallback;
import com.sensorsdata.analytics.android.sdk.advert.oaid.SAOaidHelper;
import com.sensorsdata.analytics.android.sdk.advert.plugin.LatestUtmPlugin;
import com.sensorsdata.analytics.android.sdk.advert.plugin.SAAdvertAppStartPlugin;
import com.sensorsdata.analytics.android.sdk.advert.plugin.SAAdvertAppViewScreenPlugin;
import com.sensorsdata.analytics.android.sdk.advert.scan.SAAdvertScanHelper;
import com.sensorsdata.analytics.android.sdk.advert.utils.ChannelUtils;
import com.sensorsdata.analytics.android.sdk.advert.utils.SAAdvertUtils;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.deeplink.SensorsDataDeepLinkCallback;
import com.sensorsdata.analytics.android.sdk.deeplink.SensorsDataDeferredDeepLinkCallback;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.monitor.SensorsDataLifecycleMonitorManager;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class SAAdvertProtocolImpl {
    private SensorsDataAdvertActivityLifeCallback mLifecycleCallback;
    private final Context mContext;
    private final SAConfigOptions mOptions;
    private final SAContextManager mSAContextManager;
    // $AppDeeplinkLaunch 是否携带设备信息
    private boolean mEnableDeepLinkInstallSource;
    private SAAdvertAppStartPlugin mStartPlugin;
    private SAAdvertAppViewScreenPlugin mViewScreenPlugin;
    private LatestUtmPlugin mLatestUtmPlugin;

    public SAAdvertProtocolImpl(SAContextManager contextManager) {
        mSAContextManager = contextManager;
        mContext = contextManager.getContext();
        mOptions = contextManager.getInternalConfigs().saConfigOptions;
        init();
    }

    private void init() {
        mStartPlugin = new SAAdvertAppStartPlugin();
        mViewScreenPlugin = new SAAdvertAppViewScreenPlugin();
        mLatestUtmPlugin = new LatestUtmPlugin();
        ChannelUtils.setSourceChannelKeys(mOptions.channelSourceKeys);
        if (AppInfoUtils.isMainProcess(mContext, null)) {
            ChannelUtils.commitRequestDeferredDeeplink(!ChannelUtils.isExistRequestDeferredDeeplink());
        }
    }

    protected void delayExecution() {
        try {
            if (mOptions.getDeeplinkCallback() != null) {
                DeepLinkManager.setDeferredDeepLinkCallback(mOptions.getDeeplinkCallback());
                if (mSAContextManager.getInternalConfigs().context instanceof Activity) {
                    if (mLifecycleCallback != null) {
                        mLifecycleCallback.onActivityStarted((Activity) mSAContextManager.getInternalConfigs().context); //延迟初始化监听 onActivityStarted 处理
                    }
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public <T> T invokeModuleFunction(String methodName, Object... argv) {
        switch (methodName) {
            case Modules.Advert.METHOD_TRACK_INSTALLATION:
                if (argv.length == 3) {
                    trackInstallation((String) argv[0], (JSONObject) argv[1], (Boolean) argv[2]);
                } else if (argv.length == 2) {
                    trackInstallation((String) argv[0], (JSONObject) argv[1], false);
                } else {
                    trackInstallation((String) argv[0], null, false);
                }
                break;
            case Modules.Advert.METHOD_TRACK_DEEPLINK_LAUNCH:
                if (argv.length == 2) {
                    trackDeepLinkLaunch((String) argv[0], (String) argv[1]);
                } else {
                    trackDeepLinkLaunch((String) argv[0], null);
                }
                break;
            case Modules.Advert.METHOD_TRACK_CHANNEL_EVENT:
                if (argv.length == 2) {
                    trackChannelEvent((String) argv[0], (JSONObject) argv[1]);
                } else {
                    trackChannelEvent((String) argv[0], null);
                }
                break;
            case Modules.Advert.METHOD_ENABLE_DEEPLINK_INSTALL_SOURCE:
                enableDeepLinkInstallSource((Boolean) argv[0]);
                break;
            case Modules.Advert.METHOD_SET_DEEPLINK_CALLBACK:
                DeepLinkManager.setDeepLinkCallback((SensorsDataDeepLinkCallback) argv[0]);
                break;
            case Modules.Advert.METHOD_SET_DEEPLINK_COMPLETION:
                DeepLinkManager.setDeferredDeepLinkCallback((SensorsDataDeferredDeepLinkCallback) argv[0]);
                break;
            case Modules.Advert.METHOD_REQUEST_DEFERRED_DEEPLINK:
                requestDeferredDeepLink((JSONObject) argv[0]);
                break;
            case Modules.Advert.METHOD_MERGE_CHANNEL_EVENT_PROPERTIES:
                return (T) mergeChannelEventProperties((String) argv[0], (JSONObject) argv[1]);
            case Modules.Advert.METHOD_GET_LATEST_UTM_PROPERTIES:
                return (T) ChannelUtils.getLatestUtmProperties();
            case Modules.Advert.METHOD_REMOVE_DEEPLINK_INFO:
                ChannelUtils.removeDeepLinkInfo((JSONObject) argv[0]);
                break;
            case Modules.Advert.METHOD_COMMIT_REQUEST_DEFERRED_DEEPLINK:
                ChannelUtils.commitRequestDeferredDeeplink((Boolean) argv[0]);
                break;
            case Modules.Advert.METHOD_HANDLER_SCAN_URI:
                SAAdvertScanHelper.scanHandler((Activity) argv[0], (Uri) argv[1]);
                break;
        }
        return null;
    }

    public void delayInitTask() {
        SACoreHelper.getInstance().trackQueueEvent(new Runnable() {
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

    public void registerPropertyPlugin() {
        mSAContextManager.getPluginManager().registerPropertyPlugin(mStartPlugin);
        mSAContextManager.getPluginManager().registerPropertyPlugin(mViewScreenPlugin);
        mSAContextManager.getPluginManager().registerPropertyPlugin(mLatestUtmPlugin);
    }

    public void unregisterPropertyPlugin() {
        mSAContextManager.getPluginManager().unregisterPropertyPlugin(mStartPlugin);
        mSAContextManager.getPluginManager().unregisterPropertyPlugin(mViewScreenPlugin);
        mSAContextManager.getPluginManager().unregisterPropertyPlugin(mLatestUtmPlugin);
    }

    public void registerLifeCallback() {
        if (mLifecycleCallback == null) {
            mLifecycleCallback = new SensorsDataAdvertActivityLifeCallback(mOptions);
        }
        SensorsDataLifecycleMonitorManager.getInstance().addActivityLifeCallback(mLifecycleCallback);
    }

    public void unregisterLifecycleCallback() {
        if (mLifecycleCallback != null) {
            SensorsDataLifecycleMonitorManager.getInstance().removeActivityLifeCallback(mLifecycleCallback);
        }
    }

    private void trackDeepLinkLaunch(String deepLinkUrl, final String oaid) {
        final JSONObject properties = new JSONObject();
        try {
            properties.put("$deeplink_url", deepLinkUrl);
            properties.put("$time", new Date(System.currentTimeMillis()));
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        SACoreHelper.getInstance().trackQueueEvent(new Runnable() {
            @Override
            public void run() {
                if (mEnableDeepLinkInstallSource) {
                    try {
                        properties.put("$ios_install_source", ChannelUtils.getDeviceInfo(mContext,
                                SAAdvertUtils.getIdentifier(mContext), oaid == null ? SAOaidHelper.getOpenAdIdentifier(mContext) : oaid));
                    } catch (JSONException e) {
                        SALog.printStackTrace(e);
                    }
                }
                SACoreHelper.getInstance().trackEvent(new InputData().setEventName("$AppDeeplinkLaunch").setProperties(properties));
            }
        });
    }

    private void trackInstallation(final String eventName, JSONObject properties, final boolean disableCallback) {
        try {
            if (!AppInfoUtils.isMainProcess(mContext, null)) {
                return;
            }
            // trackInstallation only on main process
            final JSONObject eventProperties = new JSONObject();
            JSONUtils.mergeJSONObject(properties, eventProperties);
            SADataHelper.addTimeProperty(eventProperties);
            SACoreHelper.getInstance().trackQueueEvent(new Runnable() {
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
                                    String androidId = SAAdvertUtils.getIdentifier(mContext);
                                    String installSource;
                                    String oaid;
                                    if (eventProperties.has("$oaid")) {
                                        oaid = eventProperties.optString("$oaid");
                                        installSource = ChannelUtils.getDeviceInfo(mContext, androidId, oaid);
                                        SALog.i(TAG, "properties has oaid " + oaid);
                                    } else {
                                        oaid = SAOaidHelper.getOpenAdIdentifier(mContext);
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
                            // first step: track
                            SACoreHelper.getInstance().trackEvent(new InputData().setEventType(EventType.TRACK).setEventName(eventName).setProperties(eventProperties));
                            // second step: profile_set_once or profile_set
                            JSONObject profileProperties = new JSONObject();
                            // profile need remove $ios_install_disable_callback 字段
                            eventProperties.remove("$ios_install_disable_callback");
                            JSONUtils.mergeJSONObject(eventProperties, profileProperties);
                            profileProperties.put("$first_visit_time", new java.util.Date());
                            SACoreHelper.getInstance().trackEvent(new InputData().setEventType(EventType.PROFILE_SET_ONCE).setProperties(profileProperties));

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

    private void trackChannelEvent(final String eventName, JSONObject properties) {
        if (mOptions.isAutoAddChannelCallbackEvent()) {
            SensorsDataAPI.sharedInstance().track(eventName, properties);
            return;
        }
        final JSONObject eventProperties = new JSONObject();
        JSONUtils.mergeJSONObject(properties, eventProperties);
        SADataHelper.addTimeProperty(eventProperties);
        SACoreHelper.getInstance().trackQueueEvent(new Runnable() {
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
                                        ChannelUtils.getDeviceInfo(mContext, SAAdvertUtils.getIdentifier(mContext), oaid));
                                SALog.i(TAG, "properties has oaid " + oaid);
                            } else {
                                eventProperties.put("$channel_device_info",
                                        ChannelUtils.getDeviceInfo(mContext, SAAdvertUtils.getIdentifier(mContext), SAOaidHelper.getOpenAdIdentifier(mContext)));
                            }
                        }
                        if (eventProperties.has("$oaid")) {
                            eventProperties.remove("$oaid");
                        }
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }

                    SACoreHelper.getInstance().trackEvent(new InputData().setEventType(EventType.TRACK).setEventName(eventName).setProperties(eventProperties));
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }

    private void enableDeepLinkInstallSource(boolean enable) {
        mEnableDeepLinkInstallSource = enable;
        DeepLinkManager.enableDeepLinkInstallSource(enable);
    }

    private void requestDeferredDeepLink(final JSONObject params) {
        SACoreHelper.getInstance().trackQueueEvent(new Runnable() {
            @Override
            public void run() {
                try {
                    if (ChannelUtils.isRequestDeferredDeeplink()) {
                        DeepLinkManager.requestDeferredDeepLink(mContext, params, SAAdvertUtils.getIdentifier(mContext)
                                , SAOaidHelper.getOpenAdIdentifier(mContext), SensorsDataAPI.sharedInstance().getPresetProperties(), mOptions.getCustomADChannelUrl(), mOptions.isSaveDeepLinkInfo());
                        ChannelUtils.commitRequestDeferredDeeplink(false);
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }

    private JSONObject mergeChannelEventProperties(String eventName, JSONObject properties) {
        if (mOptions.isAutoAddChannelCallbackEvent()) {
            return ChannelUtils.checkOrSetChannelCallbackEvent(eventName, properties, mContext);
        }
        return properties;
    }
}
