/*
 * Created by wangzhuozhou on 2015/08/01.
 * Copyright 2015－2019 Sensors Data Inc.
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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SensorsDataSDKRemoteConfig {
    /**
     * config 版本号
     */
    private String v;

    /**
     * 是否关闭 debug 模式
     */
    private boolean disableDebugMode;

    /**
     * 是否关闭 AutoTrack
     */
    private int autoTrackMode;

    /**
     * 是否关闭 SDK
     */
    private boolean disableSDK;

    private List<SensorsDataAPI.AutoTrackEventType> mAutoTrackEventTypeList;

    public SensorsDataSDKRemoteConfig() {
        this.disableDebugMode = false;
        this.disableSDK = false;
        this.autoTrackMode = -1;
    }

    public String getV() {
        return v;
    }

    public void setV(String v) {
        this.v = v;
    }

    public boolean isDisableDebugMode() {
        return disableDebugMode;
    }

    public void setDisableDebugMode(boolean disableDebugMode) {
        this.disableDebugMode = disableDebugMode;
    }

    public boolean isDisableSDK() {
        return disableSDK;
    }

    public void setDisableSDK(boolean disableSDK) {
        this.disableSDK = disableSDK;
    }

    public int getAutoTrackMode() {
        return autoTrackMode;
    }

    protected boolean isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType eventType) {
        if (autoTrackMode == -1) {
            return false;
        }

        if (autoTrackMode == 0) {
            return true;
        }

        if (this.mAutoTrackEventTypeList.contains(eventType)) {
            return false;
        }

        return true;
    }

    protected List<SensorsDataAPI.AutoTrackEventType> getAutoTrackEventTypeList() {
        return mAutoTrackEventTypeList;
    }

    public void setAutoTrackMode(int autoTrackMode) {
        this.autoTrackMode = autoTrackMode;

        if (this.autoTrackMode == -1) {
            mAutoTrackEventTypeList = null;
            return;
        }

        if (this.mAutoTrackEventTypeList == null) {
            this.mAutoTrackEventTypeList = new ArrayList<>();
        }

        if ((this.autoTrackMode & SensorsDataAPI.AutoTrackEventType.APP_START.getEventValue()) == SensorsDataAPI.AutoTrackEventType.APP_START.getEventValue()) {
            mAutoTrackEventTypeList.add(SensorsDataAPI.AutoTrackEventType.APP_START);
        }

        if ((this.autoTrackMode & SensorsDataAPI.AutoTrackEventType.APP_END.getEventValue()) == SensorsDataAPI.AutoTrackEventType.APP_END.getEventValue()) {
            mAutoTrackEventTypeList.add(SensorsDataAPI.AutoTrackEventType.APP_END);
        }

        if ((this.autoTrackMode & SensorsDataAPI.AutoTrackEventType.APP_CLICK.getEventValue()) == SensorsDataAPI.AutoTrackEventType.APP_CLICK.getEventValue()) {
            mAutoTrackEventTypeList.add(SensorsDataAPI.AutoTrackEventType.APP_CLICK);
        }

        if ((this.autoTrackMode & SensorsDataAPI.AutoTrackEventType.APP_VIEW_SCREEN.getEventValue()) == SensorsDataAPI.AutoTrackEventType.APP_VIEW_SCREEN.getEventValue()) {
            mAutoTrackEventTypeList.add(SensorsDataAPI.AutoTrackEventType.APP_VIEW_SCREEN);
        }

        if (this.autoTrackMode == 0) {
            mAutoTrackEventTypeList.clear();
        }
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("v", v);
            JSONObject configObject = new JSONObject();
            configObject.put("disableDebugMode", disableDebugMode);
            configObject.put("autoTrackMode", autoTrackMode);
            configObject.put("disableSDK", disableSDK);
            jsonObject.put("configs", configObject);
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
        return jsonObject;
    }

    @Override
    public String toString() {
        return "{ v=" + v + ", disableDebugMode=" + disableDebugMode + ", disableSDK=" + disableSDK + ", autoTrackMode=" + autoTrackMode + "}";
    }
}
