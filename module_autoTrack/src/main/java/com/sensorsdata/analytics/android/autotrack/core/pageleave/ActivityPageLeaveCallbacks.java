/*
 * Created by dengshiwei on 2022/07/06.
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

package com.sensorsdata.analytics.android.autotrack.core.pageleave;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.autotrack.utils.AppPageLeaveUtils;
import com.sensorsdata.analytics.android.sdk.SAEventManager;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.monitor.SensorsDataActivityLifecycleCallbacks;
import com.sensorsdata.analytics.android.sdk.SensorsDataExceptionHandler;
import com.sensorsdata.analytics.android.autotrack.core.business.SAPageTools;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Activity 页面停留时长
 */
public class ActivityPageLeaveCallbacks implements SensorsDataActivityLifecycleCallbacks.SAActivityLifecycleCallbacks, SensorsDataExceptionHandler.SAExceptionListener {
    private static final String START_TIME = "sa_start_time";
    private final HashMap<Integer, JSONObject> mResumedActivities = new HashMap<>();
    private List<Class<?>> mIgnoreList;
    // 弹窗页面
    private final String DIALOG_ACTIVITY = "com.sensorsdata.sf.ui.view.DialogActivity";
    private final boolean mIsEmpty;

    public ActivityPageLeaveCallbacks(List<Class<?>> ignoreList) {
        if (ignoreList != null && !ignoreList.isEmpty()) {
            mIgnoreList = ignoreList;
            mIsEmpty = false;
        } else {
            mIsEmpty = true;
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (!ignorePage(activity)) {
            trackActivityStart(activity);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        try {
            int hashCode = activity.hashCode();
            if (mResumedActivities.containsKey(hashCode)) {
                JSONObject properties = mResumedActivities.get(hashCode);
                String referrer = properties == null ? "" : properties.optString("$referrer");
                long startTime = properties == null ? 0 : properties.optLong(START_TIME);
                properties = SAPageTools.getActivityPageInfo(activity);
                properties.put(START_TIME, startTime);
                String url = SAPageTools.getScreenUrl(activity);
                properties.put("$url", url);
                if (!TextUtils.isEmpty(referrer)) {
                    properties.put("$referrer", referrer);
                }
                trackAppPageLeave(properties);
                mResumedActivities.remove(hashCode);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    @Override
    public void onNewIntent(Intent intent) {

    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        try {
            Iterator<Integer> keyCodes = mResumedActivities.keySet().iterator();
            while (keyCodes.hasNext()) {
                int hashCode = keyCodes.next();
                JSONObject properties = mResumedActivities.get(hashCode);
                if (properties == null) {
                    continue;
                }
                trackAppPageLeave(properties);
                keyCodes.remove();
            }
        } catch (Exception exception) {
            SALog.printStackTrace(exception);
        }
    }

    private void trackActivityStart(Activity activity) {
        try {
            if (DIALOG_ACTIVITY.equals(activity.getClass().getCanonicalName())) {
                return;
            }
            JSONObject properties = SAPageTools.getActivityPageInfo(activity);
            String url = SAPageTools.getScreenUrl(activity);
            properties.put("$url", url);
            String referrer = AppPageLeaveUtils.getLastScreenUrl();
            if (!properties.has("$referrer") && !TextUtils.isEmpty(referrer)) {
                properties.put("$referrer", referrer);
            }
            properties.put(START_TIME, SystemClock.elapsedRealtime());
            mResumedActivities.put(activity.hashCode(), properties);
            AppPageLeaveUtils.setLastScreenUrl(url);
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
    }

    private void trackAppPageLeave(final JSONObject properties) {
        try {
            long resumeTime = properties.optLong(START_TIME);
            properties.remove(START_TIME);
            Float duration = TimeUtils.duration(resumeTime, SystemClock.elapsedRealtime());
            if (duration < 0.05) {
                return;
            }
            properties.put("event_duration", duration);
            SAEventManager.getInstance().trackQueueEvent(new Runnable() {
                @Override
                public void run() {
                    SensorsDataAPI.sharedInstance().getSAContextManager().
                            trackEvent(new InputData().setEventName("$AppPageLeave").setProperties(properties).setEventType(EventType.TRACK));
                }
            });
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private boolean ignorePage(Object fragment) {
        if (!mIsEmpty) {
            return mIgnoreList.contains(fragment.getClass());
        }
        return false;
    }
}
