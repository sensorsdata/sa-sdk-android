/*
 * Created by dengshiwei on 2021/07/04.
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

package com.sensorsdata.analytics.android.sdk.core;

import android.content.Context;

import com.sensorsdata.analytics.android.sdk.AnalyticsMessages;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.internal.beans.InternalConfigOptions;
import com.sensorsdata.analytics.android.sdk.core.event.EventProcessor;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.core.event.TrackEventProcessor;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstDay;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.listener.SAEventListener;
import com.sensorsdata.analytics.android.sdk.plugin.encrypt.SAStoreManager;
import com.sensorsdata.analytics.android.sdk.plugin.property.PropertyPluginManager;
import com.sensorsdata.analytics.android.sdk.remote.BaseSensorsDataSDKRemoteManager;
import com.sensorsdata.analytics.android.sdk.remote.SensorsDataRemoteManager;
import com.sensorsdata.analytics.android.sdk.useridentity.UserIdentityAPI;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class SAContextManager {
    private final Context mContext;
    private List<SAEventListener> mEventListenerList;
    /* AndroidID */
    private String mAndroidId;
    private final SensorsDataAPI mSensorsDataAPI;
    private final PersistentFirstDay mFirstDay;
    private final PropertyPluginManager mPluginManager;
    private final EventProcessor mTrackEventProcessor;
    private final InternalConfigOptions mInternalConfigs;
    private AnalyticsMessages mMessages;
    /* 远程配置管理 */
    BaseSensorsDataSDKRemoteManager mRemoteManager;
    UserIdentityAPI mUserIdentityAPI;

    public SAContextManager(SensorsDataAPI sensorsDataAPI, InternalConfigOptions internalConfigs) {
        this.mSensorsDataAPI = sensorsDataAPI;
        mInternalConfigs = internalConfigs;
        this.mContext = internalConfigs.context;
        DbAdapter.getInstance(this);
        mMessages = AnalyticsMessages.getInstance(mContext, sensorsDataAPI, mInternalConfigs);
        mTrackEventProcessor = new TrackEventProcessor(this);
        this.mFirstDay = PersistentLoader.getInstance().getFirstDayPst();
        // 1. init plugin manager for advert module
        mPluginManager = new PropertyPluginManager(sensorsDataAPI, this);
        // 2. init store manager
        SAStoreManager.getInstance().registerPlugins(mInternalConfigs.saConfigOptions.getStorePlugins(), mContext);
        SAStoreManager.getInstance().upgrade();
        // 3. The step for use SAStoreManager load data,  user identity
        mUserIdentityAPI = new UserIdentityAPI(this);
        // 4. init module service for encrypt sp
        SAModuleManager.getInstance().installService(this);
        // 5. init RemoteManager, it use Identity、track、SAStoreManager
        mRemoteManager = new SensorsDataRemoteManager(sensorsDataAPI, this);
        mRemoteManager.applySDKConfigFromCache();
    }

    /**
     * 获取 SDK 事件监听回调
     *
     * @return 事件监听回调
     */
    public List<SAEventListener> getEventListenerList() {
        return mEventListenerList;
    }

    /**
     * SDK 事件回调监听，目前用于弹窗业务
     *
     * @param eventListener 事件监听
     */
    public void addEventListener(SAEventListener eventListener) {
        try {
            if (this.mEventListenerList == null) {
                this.mEventListenerList = new ArrayList<>();
            }
            this.mEventListenerList.add(eventListener);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 移除 SDK 事件回调监听
     *
     * @param eventListener 事件监听
     */
    public void removeEventListener(SAEventListener eventListener) {
        try {
            if (mEventListenerList != null && mEventListenerList.contains(eventListener)) {
                this.mEventListenerList.remove(eventListener);
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    public BaseSensorsDataSDKRemoteManager getRemoteManager() {
        return mRemoteManager;
    }

    public void setRemoteManager(BaseSensorsDataSDKRemoteManager mRemoteManager) {
        this.mRemoteManager = mRemoteManager;
    }

    public UserIdentityAPI getUserIdentityAPI() {
        return mUserIdentityAPI;
    }

    public SensorsDataAPI getSensorsDataAPI() {
        return mSensorsDataAPI;
    }

    public boolean isFirstDay(long eventTime) {
        String firstDay = mFirstDay.get();
        if (firstDay == null) {
            return true;
        }
        try {
            String current = TimeUtils.formatTime(eventTime, TimeUtils.YYYY_MM_DD);
            return firstDay.equals(current);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return true;
    }

    public PropertyPluginManager getPluginManager() {
        return mPluginManager;
    }

    public void trackEvent(InputData inputData) {
        mTrackEventProcessor.trackEvent(inputData);
    }

    public Context getContext() {
        return mContext;
    }

    public InternalConfigOptions getInternalConfigs() {
        return mInternalConfigs;
    }

    public AnalyticsMessages getAnalyticsMessages() {
        return mMessages;
    }
}
