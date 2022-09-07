/*
 * Created by chenru on 2022/4/25 下午5:05.
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

package com.sensorsdata.analytics.android.sdk.advert;

import static com.sensorsdata.analytics.android.sdk.advert.SAAdvertConstants.TAG;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SAEventManager;
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
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.core.mediator.ModuleConstants;
import com.sensorsdata.analytics.android.sdk.core.mediator.advert.SAAdvertModuleProtocol;
import com.sensorsdata.analytics.android.sdk.core.mediator.protocol.SAScanListener;
import com.sensorsdata.analytics.android.sdk.deeplink.SensorsDataDeepLinkCallback;
import com.sensorsdata.analytics.android.sdk.deeplink.SensorsDataDeferredDeepLinkCallback;
import com.sensorsdata.analytics.android.sdk.monitor.SensorsDataLifecycleMonitorManager;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class SAAdvertProtocolImpl implements SAAdvertModuleProtocol, SAScanListener {
    private SensorsDataAdvertActivityLifeCallback mLifecycleCallback;
    private boolean mEnable = false;
    private Context mContext;
    private SAConfigOptions mOptions;
    private SAContextManager mSAContextManager;
    // $AppDeeplinkLaunch 是否携带设备信息
    private boolean mEnableDeepLinkInstallSource;
    private SAAdvertAppStartPlugin mStartPlugin;
    private SAAdvertAppViewScreenPlugin mViewScreenPlugin;
    private LatestUtmPlugin mLatestUtmPlugin;

    @Override
    public void install(SAContextManager contextManager) {
        mSAContextManager = contextManager;
        mContext = contextManager.getContext();
        mOptions = contextManager.getInternalConfigs().saConfigOptions;
        init();
    }

    private void init() {
        mStartPlugin = new SAAdvertAppStartPlugin();
        mViewScreenPlugin = new SAAdvertAppViewScreenPlugin();
        mLatestUtmPlugin = new LatestUtmPlugin();
        registerPropertyPlugin();
        ChannelUtils.setSourceChannelKeys(mOptions.channelSourceKeys);
        if (!mOptions.isDisableSDK()) {
            setModuleState(true);
        }
        if (AppInfoUtils.isMainProcess(mContext, null)) {
            ChannelUtils.commitRequestDeferredDeeplink(!ChannelUtils.isExistRequestDeferredDeeplink());
        }
    }

    @Override
    public void setModuleState(boolean enable) {
        if (mEnable != enable) {
            if (enable) {
                delayInitTask();
                registerLifeCallback();
                registerPropertyPlugin();
            } else {
                unregisterLifecycleCallback();
                unregisterPropertyPlugin();
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
        SAEventManager.getInstance().trackQueueEvent(new Runnable() {
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
                SAEventManager.getInstance().trackEvent(new InputData().setEventName("$AppDeeplinkLaunch").setProperties(properties));
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
            // trackInstallation only on main process
            final JSONObject eventProperties = new JSONObject();
            JSONUtils.mergeJSONObject(properties, eventProperties);
            SADataHelper.addTimeProperty(eventProperties);
            SAEventManager.getInstance().trackQueueEvent(new Runnable() {
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
                            SAEventManager.getInstance().trackEvent(new InputData().setEventType(EventType.TRACK).setEventName(eventName).setProperties(eventProperties));
                            // second step: profile_set_once or profile_set
                            JSONObject profileProperties = new JSONObject();
                            // profile need remove $ios_install_disable_callback 字段
                            eventProperties.remove("$ios_install_disable_callback");
                            JSONUtils.mergeJSONObject(eventProperties, profileProperties);
                            profileProperties.put("$first_visit_time", new java.util.Date());
                            SAEventManager.getInstance().trackEvent(new InputData().setEventType(EventType.PROFILE_SET_ONCE).setProperties(profileProperties));

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
        JSONUtils.mergeJSONObject(properties, eventProperties);
        SADataHelper.addTimeProperty(eventProperties);
        SAEventManager.getInstance().trackQueueEvent(new Runnable() {
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

                    SAEventManager.getInstance().trackEvent(new InputData().setEventType(EventType.TRACK).setEventName(eventName).setProperties(eventProperties));
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


    @Override
    public String getModuleName() {
        return ModuleConstants.ModuleName.ADVERT_NAME;
    }

    @Override
    public boolean isEnable() {
        return mEnable;
    }

    @Override
    public int getPriority() {
        return 5;
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

    private void registerPropertyPlugin() {
        mSAContextManager.getPluginManager().registerPropertyPlugin(mStartPlugin);
        mSAContextManager.getPluginManager().registerPropertyPlugin(mViewScreenPlugin);
        mSAContextManager.getPluginManager().registerPropertyPlugin(mLatestUtmPlugin);
    }

    private void unregisterPropertyPlugin() {
        mSAContextManager.getPluginManager().unregisterPropertyPlugin(mStartPlugin);
        mSAContextManager.getPluginManager().unregisterPropertyPlugin(mViewScreenPlugin);
        mSAContextManager.getPluginManager().unregisterPropertyPlugin(mLatestUtmPlugin);
    }
}
