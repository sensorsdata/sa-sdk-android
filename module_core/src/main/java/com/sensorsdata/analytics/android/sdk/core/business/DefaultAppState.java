/*
 * Created by dengshiwei on 2022/08/12.
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

package com.sensorsdata.analytics.android.sdk.core.business;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.business.timer.EventTimerManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.visual.SAVisual;
import com.sensorsdata.analytics.android.sdk.util.AppStateTools;

public class DefaultAppState implements AppStateTools.AppState {
    private final SensorsDataAPI mSensorsDataInstance;
    private boolean resumeFromBackground = false;

    public DefaultAppState(SensorsDataAPI instance) {
        this.mSensorsDataInstance = instance;
    }

    @Override
    public void onForeground() {
        try {
            EventTimerManager.getInstance().appBecomeActive();
            //从后台恢复，从缓存中读取 SDK 控制配置信息
            if (resumeFromBackground) {
                //先从缓存中读取 SDKConfig
                mSensorsDataInstance.getSAContextManager().getRemoteManager().applySDKConfigFromCache();
                mSensorsDataInstance.resumeTrackScreenOrientation();
                try {
                    SAVisual.resumeHeatMapService();
                    SAVisual.resumeVisualService();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
            //每次启动 App，重新拉取最新的配置信息
            mSensorsDataInstance.getSAContextManager().getRemoteManager().pullSDKConfigFromServer();
            resumeFromBackground = true;
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    @Override
    public void onBackground() {
        try {
            mSensorsDataInstance.stopTrackScreenOrientation();
            mSensorsDataInstance.getSAContextManager().getRemoteManager().resetPullSDKConfigTimer();
            SAVisual.stopHeatMapService();
            SAVisual.stopVisualService();
            EventTimerManager.getInstance().appEnterBackground();
            mSensorsDataInstance.clearLastScreenUrl();
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
