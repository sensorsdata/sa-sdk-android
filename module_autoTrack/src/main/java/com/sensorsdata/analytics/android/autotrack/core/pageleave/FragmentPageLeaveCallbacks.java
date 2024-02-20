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

import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;

import com.sensorsdata.analytics.android.autotrack.core.autotrack.SAFragmentLifecycleCallbacks;
import com.sensorsdata.analytics.android.autotrack.core.business.SAPageTools;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.SACoreHelper;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;
import com.sensorsdata.analytics.android.sdk.exceptions.SensorsDataExceptionHandler;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.util.Dispatcher;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.SAFragmentUtils;
import com.sensorsdata.analytics.android.sdk.util.SAPageInfoUtils;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Fragment 页面停留时长监听
 */
public class FragmentPageLeaveCallbacks implements SAFragmentLifecycleCallbacks, SensorsDataExceptionHandler.SAExceptionListener {
    private final HashMap<Integer, JSONObject> mResumedFragments = new HashMap<>();
    private List<Class<?>> mIgnoreList;
    private final boolean mIsEmpty;

    private static final String START_TIME = "sa_start_time";

    public FragmentPageLeaveCallbacks(List<Class<?>> ignoreList) {
        if (ignoreList != null && !ignoreList.isEmpty()) {
            mIgnoreList = ignoreList;
            mIsEmpty = false;
        } else {
            mIsEmpty = true;
        }
    }

    @Override
    public void onCreate(Object object) {

    }

    @Override
    public void onViewCreated(Object object, View rootView, Bundle bundle) {

    }

    @Override
    public void onStart(Object object) {

    }

    @Override
    public void onResume(Object object) {
        if (!ignorePage(object) && SAFragmentUtils.isFragmentVisible(object)) {
            trackFragmentStart(object);
        }
    }

    @Override
    public void onPause(Object object) {
        try {
            int hashCode = object.hashCode();
            if (mResumedFragments.containsKey(hashCode)) {
                trackAppPageLeave(object);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void onStop(Object object) {

    }

    @Override
    public void onHiddenChanged(Object object, boolean hidden) {
        if (!ignorePage(object)) {
            if (SAFragmentUtils.isFragmentVisible(object)) {
                trackFragmentStart(object);
            } else {
                trackAppPageLeave(object);
            }
        }
    }

    @Override
    public void setUserVisibleHint(Object object, boolean isVisibleToUser) {
        if (!ignorePage(object)) {
            if (SAFragmentUtils.isFragmentVisible(object)) {
                trackFragmentStart(object);
            } else {
                trackAppPageLeave(object);
            }
        }
    }

    private void trackAppPageLeave(Object object) {
        try {
            int hashCode = object.hashCode();
            if (mResumedFragments.containsKey(hashCode)) {
                JSONObject properties = mResumedFragments.get(hashCode);
                mResumedFragments.remove(hashCode);
                trackPageLeaveEvent(properties);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    private void trackFragmentStart(final Object object) {
        try {
            final String url = SAPageTools.getScreenUrl(object);
            final int hashCode = object.hashCode();
            final JSONObject pageInfo = SAPageInfoUtils.getFragmentPageInfo(null, object);
            Dispatcher.getInstance().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject properties = new JSONObject();
                        properties.put(START_TIME, SystemClock.elapsedRealtime());
                        properties.put("$url", url);
                        if (SensorsDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_VIEW_SCREEN)
                                || !SensorsDataAPI.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
                            SAPageTools.setCurrentScreenUrl(url);
                        }
                        String referrer = SAPageTools.getReferrer();
                        if (!TextUtils.isEmpty(referrer)) {
                            properties.put("$referrer", referrer);
                        }
                        properties.put("$referrer_title", SAPageTools.getReferrerTitle());

                        JSONUtils.mergeJSONObject(pageInfo, properties);
                        SALog.i("SA.FragmentPageLeave", "trackFragmentStart = " + properties);
                        mResumedFragments.put(hashCode, properties);
                    } catch (JSONException e) {
                        SALog.printStackTrace(e);
                    }
                }
            }, 300);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        try {
            Iterator<Integer> keyCodes = mResumedFragments.keySet().iterator();
            while (keyCodes.hasNext()) {
                int hashCode = keyCodes.next();
                JSONObject properties = mResumedFragments.get(hashCode);
                if (properties == null) {
                    continue;
                }
                trackPageLeaveEvent(properties);
                keyCodes.remove();
            }
        } catch (Exception exception) {
            SALog.printStackTrace(exception);
        }
    }

    private void trackPageLeaveEvent(final JSONObject properties) {
        try {
            long resumeTime = properties.optLong(START_TIME);
            properties.remove(START_TIME);
            Float duration = TimeUtils.duration(resumeTime, SystemClock.elapsedRealtime());
            if (duration < 0.05) {
                return;
            }
            properties.put("event_duration", duration);
            SACoreHelper.getInstance().trackQueueEvent(new Runnable() {
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
