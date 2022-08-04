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

package com.sensorsdata.analytics.android.sdk.util;

import android.content.Context;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.event.EventProcessor;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.core.event.TrackEventProcessor;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstDay;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentSuperProperties;
import com.sensorsdata.analytics.android.sdk.internal.beans.InternalConfigOptions;
import com.sensorsdata.analytics.android.sdk.listener.SAEventListener;
import com.sensorsdata.analytics.android.sdk.plugin.property.PropertyPluginManager;
import com.sensorsdata.analytics.android.sdk.remote.BaseSensorsDataSDKRemoteManager;
import com.sensorsdata.analytics.android.sdk.remote.SensorsDataRemoteManager;
import com.sensorsdata.analytics.android.sdk.useridentity.UserIdentityAPI;

import java.util.ArrayList;
import java.util.List;

public class SAContextManager {
    private final Context mContext;
    private List<SAEventListener> mEventListenerList;
    /* AndroidID */
    private String mAndroidId;
    private final PersistentFirstDay mFirstDay;
    private final PersistentSuperProperties mSuperProperties;
    /* 远程配置管理 */
    BaseSensorsDataSDKRemoteManager mRemoteManager;
    UserIdentityAPI mUserIdentityAPI;
    private final PropertyPluginManager mPluginManager;
    private EventProcessor mTrackEventProcessor;
    private InternalConfigOptions mInternalConfigs;

    public SAContextManager(SensorsDataAPI sensorsDataAPI, InternalConfigOptions internalConfigs) {
        mInternalConfigs = internalConfigs;
        this.mContext = internalConfigs.context;
        this.mFirstDay = (PersistentFirstDay) PersistentLoader.loadPersistent(DbParams.PersistentName.FIRST_DAY);
        this.mSuperProperties = (PersistentSuperProperties) PersistentLoader.loadPersistent(DbParams.PersistentName.SUPER_PROPERTIES);
        // init RemoteManager
        mRemoteManager = new SensorsDataRemoteManager(sensorsDataAPI);
        mRemoteManager.applySDKConfigFromCache();
        // init UserIdentityAPI
        mUserIdentityAPI = new UserIdentityAPI(this);
        // init plugin manager
        // 注册属性插件
        mPluginManager = new PropertyPluginManager(sensorsDataAPI, internalConfigs);
        mTrackEventProcessor = new TrackEventProcessor(internalConfigs);
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

    public void setUserIdentityAPI(UserIdentityAPI mUserIdentityAPI) {
        this.mUserIdentityAPI = mUserIdentityAPI;
    }

    /**
     * 获取 AndroidID
     *
     * @return AndroidID
     */
    public String getAndroidId() {
        if (TextUtils.isEmpty(mAndroidId)) {
            mAndroidId = SensorsDataUtils.getAndroidID(mContext);
        }
        return mAndroidId;
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

    public PersistentFirstDay getFirstDay() {
        return mFirstDay;
    }

    public PersistentSuperProperties getSuperProperties() {
        return mSuperProperties;
    }

    public PropertyPluginManager getPluginManager() {
        return mPluginManager;
    }

    public Context getContext() {
        return mContext;
    }

    public InternalConfigOptions getInternalConfigs() {
        return mInternalConfigs;
    }

    public void trackEvent(InputData inputData) {
        mTrackEventProcessor.trackEvent(inputData);
    }
}
