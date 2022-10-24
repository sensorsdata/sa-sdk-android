/*
 * Created by yuejiangzhong on 2022/01/20.
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

package com.sensorsdata.analytics.android.sdk.monitor;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.listener.SAFunctionListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TrackMonitor {

    private List<SAFunctionListener> mFunctionListener;
    private JSONObject cacheData;

    private TrackMonitor() {

    }

    private static class SingletonHolder {
        private static final TrackMonitor INSTANCE = new TrackMonitor();
    }

    private void call(String function, JSONObject jsonObject) {
        if (TextUtils.isEmpty(function) || mFunctionListener == null) {
            return;
        }
        for (SAFunctionListener listener : mFunctionListener) {
            listener.call(function, jsonObject);
        }
    }

    public static TrackMonitor getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void addFunctionListener(SAFunctionListener functionListener) {
        try {
            if (this.mFunctionListener == null) {
                mFunctionListener = new ArrayList<>();
            }
            if (functionListener != null && !mFunctionListener.contains(functionListener)) {
                mFunctionListener.add(functionListener);
            }
            JSONObject jsonObject = cacheData;
            if (jsonObject != null) {
                call("trackEvent", jsonObject);
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    public void removeFunctionListener(SAFunctionListener functionListener) {
        if (mFunctionListener != null && functionListener != null) {
            mFunctionListener.remove(functionListener);
        }
    }

    public void callTrack(JSONObject eventObject) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("eventJSON", eventObject);
            if ("$AppStart".equals(eventObject.optString("event"))) {
                if (mFunctionListener == null) {
                    cacheData = jsonObject;
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            cacheData = null;
                        }
                    }, 2 * 1000);
                    return;
                }
            }
            call("trackEvent",jsonObject);
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
    }

    public void callResetAnonymousId(String newDistinctId) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("distinctId", newDistinctId);
            call("resetAnonymousId", jsonObject);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public void callLogin(String loginId) {
        if (mFunctionListener == null) {
            return;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("distinctId", loginId);
            call("login",jsonObject);
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
    }

    public void callLogout() {
        call("logout", null);
    }

    public void callIdentify(String distinctId) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("distinctId", distinctId);
            call("identify", jsonObject);
        } catch (JSONException e) {
           SALog.printStackTrace(e);
        }
    }

    public void callEnableDataCollect() {
        call("enableDataCollect", null);
    }
}
