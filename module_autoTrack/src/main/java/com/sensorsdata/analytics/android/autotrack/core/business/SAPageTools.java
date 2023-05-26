/*
 * Created by dengshiwei on 2022/07/08.
 * Copyright 2015－2021 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.autotrack.core.business;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.ScreenAutoTracker;
import com.sensorsdata.analytics.android.sdk.SensorsDataAutoTrackAppViewScreenUrl;

import org.json.JSONObject;

public class SAPageTools {
    /* last page Url */
    private static String mLastScreenUrl;
    /* 前向页面地址，与 mLastScreenUrl 在及时性上有区别 */
    private static String sReferrer;
    private static long sReferrerUrlTime;
    /*current page url */
    private static String mCurrentScreenUrl;
    /* last page Title */
    private static String mReferrerTitle;
    private static long sReferrerTitleTime;
    /* current page Title */
    private static String mCurrentTitle;
    /* current page property */
    private static long sTrackPropertiesTime;
    private static JSONObject sCurrentScreenTrackProperties;
    private static JSONObject sLastTrackProperties;

    public static String getLastScreenUrl() {
        return mLastScreenUrl;
    }

    public static void setLastScreenUrl(String lastScreenUrl) {
        mLastScreenUrl = lastScreenUrl;
    }

    public static String getReferrer() {
        return sReferrer;
    }

    public static String getReferrerTitle() {
        return mReferrerTitle;
    }

    public static String getCurrentTitle() {
        return mCurrentTitle;
    }

    public static void setCurrentTitle(String currentScreenTitle) {
        if (System.currentTimeMillis() - sReferrerTitleTime >= 400) {// update referrer
            mReferrerTitle = mCurrentTitle;
            sReferrerTitleTime = System.currentTimeMillis();
        }
        mCurrentTitle = currentScreenTitle;
    }

    public static void setCurrentScreenTrackProperties(JSONObject currentScreenTrackProperties) {
        if (System.currentTimeMillis() - sTrackPropertiesTime >= 400) {// update referrer
            sLastTrackProperties = sCurrentScreenTrackProperties;
            sTrackPropertiesTime = System.currentTimeMillis();
        }
        sCurrentScreenTrackProperties = currentScreenTrackProperties;
    }

    public static String getCurrentScreenUrl() {
        return mCurrentScreenUrl;
    }

    public static void setCurrentScreenUrl(String currentScreenUrl) {
        if (System.currentTimeMillis() - sReferrerUrlTime >= 400) {// update referrer
            sReferrer = mCurrentScreenUrl;
            sReferrerUrlTime = System.currentTimeMillis();
        }
        mLastScreenUrl = mCurrentScreenUrl;
        mCurrentScreenUrl = currentScreenUrl;
    }

    public static JSONObject getLastTrackProperties() {
        return sLastTrackProperties;
    }

    /**
     * get ScreenUrl
     *
     * @param object activity/fragment
     * @return screenUrl
     */
    public static String getScreenUrl(Object object) {
        if (object == null) {
            return null;
        }
        String screenUrl = null;
        try {
            if (object instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) object;
                screenUrl = screenAutoTracker.getScreenUrl();
            } else {
                SensorsDataAutoTrackAppViewScreenUrl autoTrackAppViewScreenUrl = object.getClass().getAnnotation(SensorsDataAutoTrackAppViewScreenUrl.class);
                if (autoTrackAppViewScreenUrl != null) {
                    screenUrl = autoTrackAppViewScreenUrl.url();
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        if (screenUrl == null) {
            screenUrl = object.getClass().getCanonicalName();
        }
        return screenUrl;
    }

}
