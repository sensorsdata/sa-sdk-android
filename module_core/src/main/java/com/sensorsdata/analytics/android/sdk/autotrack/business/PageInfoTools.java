/*
 * Created by dengshiwei on 2022/06/25.
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

package com.sensorsdata.analytics.android.sdk.autotrack.business;

import org.json.JSONObject;

public class PageInfoTools {
    /* last page Url */
    private static String mLastScreenUrl;
    /*current page url */
    private static String mCurrentScreenUrl;
    /* last page Title */
    private static String mReferrerScreenTitle;
    /* current page Title */
    private static String mCurrentScreenTitle;
    /* last page property */
    private static JSONObject mLastScreenTrackProperties;
    /* current page property */
    private static JSONObject mCurrentScreenTrackProperties;

    public static String getLastScreenUrl() {
        return mLastScreenUrl;
    }

    public static void setLastScreenUrl(String lastScreenUrl) {
        mLastScreenUrl = lastScreenUrl;
    }

    public static String getReferrerScreenTitle() {
        return mReferrerScreenTitle;
    }

    public static String getCurrentScreenTitle() {
        return mCurrentScreenTitle;
    }

    public static void setCurrentScreenTitle(String currentScreenTitle) {
        mReferrerScreenTitle = mCurrentScreenTitle;
        mCurrentScreenTitle = currentScreenTitle;
    }

    public static void setCurrentScreenTrackProperties(JSONObject currentScreenTrackProperties) {
        mLastScreenTrackProperties = mCurrentScreenTrackProperties;
        mCurrentScreenTrackProperties = currentScreenTrackProperties;
    }

    public static String getCurrentScreenUrl() {
        return mCurrentScreenUrl;
    }

    public static void setCurrentScreenUrl(String currentScreenUrl) {
        mLastScreenUrl = mCurrentScreenUrl;
        mCurrentScreenUrl = currentScreenUrl;
    }

    public static JSONObject getCurrentScreenTrackProperties() {
        return mCurrentScreenTrackProperties;
    }

    public static JSONObject getLastScreenTrackProperties() {
        return mLastScreenTrackProperties;
    }
}
